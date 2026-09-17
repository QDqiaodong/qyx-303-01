package com.example.tuanjian.service.impl;

import com.example.tuanjian.dto.request.GroupBatchLandingRequest;
import com.example.tuanjian.entity.GroupBatch;
import com.example.tuanjian.entity.TeamBuildingPlan;
import com.example.tuanjian.exception.BusinessConflictException;
import com.example.tuanjian.repository.GroupBatchRepository;
import com.example.tuanjian.repository.TeamBuildingPlanRepository;
import com.example.tuanjian.service.BudgetService;
import com.example.tuanjian.service.GroupBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupBatchServiceImpl implements GroupBatchService {

    private final GroupBatchRepository batchRepository;
    private final TeamBuildingPlanRepository planRepository;
    private final BudgetService budgetService;

    private static final DateTimeFormatter BATCH_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    @Transactional
    public GroupBatch land(GroupBatchLandingRequest request, String templateBudgetNote) {
        TeamBuildingPlan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new NoSuchElementException("方案不存在: " + request.getPlanId()));

        int groupSize = request.getGroupSize();
        String activeKey = GroupBatchService.activeKey(request.getTravelDate(), plan.getVenue());

        // 先做规则校验，给出可读错误；并发情况下再由唯一索引兜底（DataIntegrityViolationException）
        if (batchRepository.existsByActiveKey(activeKey)) {
            throw new BusinessConflictException(
                    String.format("场地【%s】在 %s 已有生效的落地批次，不能重复占位",
                            plan.getVenue(), request.getTravelDate()));
        }
        if (groupSize < plan.getMinParticipants() || groupSize > plan.getMaxParticipants()) {
            throw new BusinessConflictException(
                    String.format("成团人数 %d 不在方案支持范围 %d-%d 人内",
                            groupSize, plan.getMinParticipants(), plan.getMaxParticipants()));
        }
        if (plan.getDurationDays() > request.getMaxDurationDays()) {
            throw new BusinessConflictException(
                    String.format("方案时长 %d 天超过当次对比的最大出行天数 %d 天",
                            plan.getDurationDays(), request.getMaxDurationDays()));
        }
        if (!containsRequiredActivities(plan.getSuitableActivities(), request.getRequiredActivities())) {
            throw new BusinessConflictException("方案不满足当次对比的必备活动要求: "
                    + request.getRequiredActivities());
        }

        BigDecimal lockedCostPerPerson = plan.getCostPerPerson();
        BigDecimal lockedAmount = lockedCostPerPerson.multiply(BigDecimal.valueOf(groupSize));

        String batchNo = "TB" + LocalDateTime.now().format(BATCH_NO_FORMAT)
                + String.format("%04d", java.util.concurrent.ThreadLocalRandom.current().nextInt(10000));

        GroupBatch batch = GroupBatch.builder()
                .batchNo(batchNo)
                .planId(plan.getId())
                .planName(plan.getPlanName())
                .venue(plan.getVenue())
                .travelDate(request.getTravelDate())
                .groupSize(groupSize)
                .lockedCostPerPerson(lockedCostPerPerson)
                .lockedAmount(lockedAmount)
                .status(GroupBatch.STATUS_ACTIVE)
                .activeKey(activeKey)
                .build();

        // 先插台账拿到 batchId；唯一索引冲突会在这里抛出，钱还没动
        GroupBatch saved;
        try {
            saved = batchRepository.saveAndFlush(batch);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessConflictException(
                    String.format("场地【%s】在 %s 已被其他批次抢占，请刷新后重试",
                            plan.getVenue(), request.getTravelDate()));
        }

        // 同事务扣款 + 写流水：余额不足抛 BusinessConflictException，整体回滚（台账也回滚）
        String remark = "落地成团 " + batchNo + "：" + plan.getPlanName()
                + "，" + groupSize + " 人 × ¥" + lockedCostPerPerson.toPlainString();
        budgetService.hold(saved.getId(), plan.getId(), lockedAmount,
                templateBudgetNote == null ? remark : remark + "（" + templateBudgetNote + "）");

        log.info("落地成团成功: batchNo={}, plan={}, date={}, venue={}, amount={}",
                batchNo, plan.getId(), request.getTravelDate(), plan.getVenue(), lockedAmount);
        return saved;
    }

    @Override
    public List<GroupBatch> listBatches(String status) {
        if (status == null || status.isBlank()) {
            return batchRepository.findAllByOrderByCreatedAtDesc();
        }
        return batchRepository.findByStatusOrderByCreatedAtDesc(status.trim().toUpperCase());
    }

    @Override
    public GroupBatch getBatch(Long id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("落地批次不存在: " + id));
    }

    @Override
    @Transactional
    public int invalidateBatchesByPlanCostChange(Long planId, BigDecimal newCostPerPerson) {
        List<GroupBatch> activeBatches =
                batchRepository.findByPlanIdAndStatusOrderByCreatedAtDesc(planId, GroupBatch.STATUS_ACTIVE);
        int invalidated = 0;
        for (GroupBatch batch : activeBatches) {
            BigDecimal recalculated = newCostPerPerson.multiply(BigDecimal.valueOf(batch.getGroupSize()));
            // 按落地当时的成团人数重算，对不上当初扣下的额度才失效；费用没变（金额一致）不动它
            if (recalculated.compareTo(batch.getLockedAmount()) != 0) {
                batch.setStatus(GroupBatch.STATUS_INVALID);
                batch.setInvalidReason(GroupBatch.REASON_COST_CHANGED);
                batch.setActiveKey(null);
                batchRepository.save(batch);
                budgetService.refund(batch.getId(), planId, batch.getLockedAmount(),
                        "批次 " + batch.getBatchNo() + " 失效退回：方案人均费用变更为 ¥"
                                + newCostPerPerson.toPlainString()
                                + "，按落地人数 " + batch.getGroupSize()
                                + " 重算为 ¥" + recalculated.toPlainString()
                                + "，与原扣额 ¥" + batch.getLockedAmount().toPlainString() + " 不一致");
                invalidated++;
                log.info("批次因方案费用变化失效: batchNo={}, refund={}",
                        batch.getBatchNo(), batch.getLockedAmount());
            }
        }
        return invalidated;
    }

    @Override
    @Transactional
    public GroupBatch contactSupplier(Long id) {
        GroupBatch batch = getBatch(id);
        if (!GroupBatch.STATUS_ACTIVE.equals(batch.getStatus())) {
            throw new BusinessConflictException(
                    "批次 " + batch.getBatchNo() + " 已失效（预算已退回池子），不能再对接场地供应商");
        }
        return batch;
    }

    private boolean containsRequiredActivities(String suitableActivities, String requiredActivities) {
        if (requiredActivities == null || requiredActivities.isBlank()) {
            return true;
        }
        String suitable = suitableActivities == null ? "" : suitableActivities;
        for (String activity : requiredActivities.split(",")) {
            String trimmed = activity.trim();
            if (!trimmed.isEmpty() && !suitable.contains(trimmed)) {
                return false;
            }
        }
        return true;
    }

}
