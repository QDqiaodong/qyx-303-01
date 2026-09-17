package com.example.tuanjian.service.impl;

import com.example.tuanjian.dto.request.ConstraintRequest;
import com.example.tuanjian.dto.request.PlanCreateRequest;
import com.example.tuanjian.dto.response.PlanCompareResult;
import com.example.tuanjian.entity.GroupBatch;
import com.example.tuanjian.entity.TeamBuildingPlan;
import com.example.tuanjian.exception.BusinessConflictException;
import com.example.tuanjian.repository.GroupBatchRepository;
import com.example.tuanjian.repository.TeamBuildingPlanRepository;
import com.example.tuanjian.service.BudgetService;
import com.example.tuanjian.service.GroupBatchService;
import com.example.tuanjian.service.TeamBuildingPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamBuildingPlanServiceImpl implements TeamBuildingPlanService {

    private final TeamBuildingPlanRepository planRepository;
    private final GroupBatchRepository batchRepository;
    private final BudgetService budgetService;
    private final GroupBatchService groupBatchService;

    @Override
    @Transactional
    public TeamBuildingPlan createPlan(PlanCreateRequest request) {
        TeamBuildingPlan plan = TeamBuildingPlan.builder()
                .planName(request.getPlanName())
                .description(request.getDescription())
                .transportation(request.getTransportation())
                .venue(request.getVenue())
                .projects(request.getProjects())
                .costPerPerson(request.getCostPerPerson())
                .durationDays(request.getDurationDays())
                .minParticipants(request.getMinParticipants())
                .maxParticipants(request.getMaxParticipants())
                .suitableActivities(request.getSuitableActivities())
                .build();
        return planRepository.save(plan);
    }

    @Override
    @Transactional
    public TeamBuildingPlan updatePlan(Long id, PlanCreateRequest request) {
        TeamBuildingPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("方案不存在: " + id));
        BigDecimal oldCostPerPerson = plan.getCostPerPerson();
        plan.setPlanName(request.getPlanName());
        plan.setDescription(request.getDescription());
        plan.setTransportation(request.getTransportation());
        plan.setVenue(request.getVenue());
        plan.setProjects(request.getProjects());
        plan.setCostPerPerson(request.getCostPerPerson());
        plan.setDurationDays(request.getDurationDays());
        plan.setMinParticipants(request.getMinParticipants());
        plan.setMaxParticipants(request.getMaxParticipants());
        plan.setSuitableActivities(request.getSuitableActivities());
        TeamBuildingPlan saved = planRepository.save(plan);

        // 人均费用被改动：所有仍生效的落地批次按落地人数重算，对不上原扣额的立即失效并退款，
        // 与方案更新在同一事务内，费用改成功、批次失效、钱退回同时可见。
        if (request.getCostPerPerson().compareTo(oldCostPerPerson) != 0) {
            int invalidated = groupBatchService.invalidateBatchesByPlanCostChange(id, request.getCostPerPerson());
            if (invalidated > 0) {
                log.info("方案 {} 人均费用由 {} 改为 {}，{} 条生效批次失效并退款",
                        id, oldCostPerPerson, request.getCostPerPerson(), invalidated);
            }
        }
        return saved;
    }

    @Override
    @Transactional
    public void deletePlan(Long id) {
        if (!planRepository.existsById(id)) {
            throw new NoSuchElementException("方案不存在: " + id);
        }
        List<GroupBatch> activeBatches =
                batchRepository.findByPlanIdAndStatusOrderByCreatedAtDesc(id, GroupBatch.STATUS_ACTIVE);
        if (!activeBatches.isEmpty()) {
            throw new BusinessConflictException(
                    "该方案还有 " + activeBatches.size() + " 条生效中的落地批次，不能删除；"
                            + "请先处理对应批次（批次失效退款后才可删除）");
        }
        planRepository.deleteById(id);
    }

    @Override
    public TeamBuildingPlan getPlanById(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("方案不存在: " + id));
    }

    @Override
    public List<TeamBuildingPlan> getAllPlans() {
        return planRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public List<PlanCompareResult> comparePlans(ConstraintRequest constraint) {
        List<TeamBuildingPlan> plans = planRepository.findAll();
        // 落地口径：预算只认池子当时未被占住的余额；模板上限仅用于建模板和对账展示
        BigDecimal availableBudget = budgetService.getPool().getAvailableAmount();

        // 一次性拉出全部生效批次，按方案分组，供对比页提示哪些场次已落地
        List<GroupBatch> activeBatches = batchRepository.findByStatusOrderByCreatedAtDesc(GroupBatch.STATUS_ACTIVE);
        Map<Long, List<GroupBatch>> batchesByPlan = activeBatches.stream()
                .collect(Collectors.groupingBy(GroupBatch::getPlanId));

        List<PlanCompareResult> results = new ArrayList<>();
        for (TeamBuildingPlan plan : plans) {
            results.add(evaluatePlan(plan, constraint, availableBudget,
                    batchesByPlan.getOrDefault(plan.getId(), List.of())));
        }

        results.sort(Comparator.comparing(PlanCompareResult::getAdaptabilityScore).reversed());

        return results;
    }

    private PlanCompareResult evaluatePlan(TeamBuildingPlan plan, ConstraintRequest constraint,
                                           BigDecimal availableBudget, List<GroupBatch> activeBatches) {
        List<String> compliantItems = new ArrayList<>();
        List<String> nonCompliantItems = new ArrayList<>();

        BigDecimal totalCost = plan.getCostPerPerson().multiply(BigDecimal.valueOf(constraint.getParticipantCount()));
        // 是否超预算：只拿池子里还没被占住的余额比
        boolean isBudgetCompliant = totalCost.compareTo(availableBudget) <= 0;
        // 模板那道上限只供对账参考，不参与合规裁决
        boolean isTemplateBudgetCompliant = totalCost.compareTo(constraint.getBudgetLimit()) <= 0;
        if (isBudgetCompliant) {
            compliantItems.add("预算");
        } else {
            nonCompliantItems.add("预算");
        }

        boolean isDurationCompliant = plan.getDurationDays() <= constraint.getMaxDurationDays();
        if (isDurationCompliant) {
            compliantItems.add("出行天数");
        } else {
            nonCompliantItems.add("出行天数");
        }

        boolean isParticipantCountCompliant = 
                constraint.getParticipantCount() >= plan.getMinParticipants() &&
                constraint.getParticipantCount() <= plan.getMaxParticipants();
        if (isParticipantCountCompliant) {
            compliantItems.add("参与人数");
        } else {
            nonCompliantItems.add("参与人数");
        }

        boolean isRequiredActivitiesCompliant = true;
        if (constraint.getRequiredActivities() != null && !constraint.getRequiredActivities().isEmpty()) {
            String[] required = constraint.getRequiredActivities().split(",");
            String suitable = plan.getSuitableActivities();
            for (String activity : required) {
                if (!suitable.contains(activity.trim())) {
                    isRequiredActivitiesCompliant = false;
                    break;
                }
            }
        }
        if (isRequiredActivitiesCompliant) {
            compliantItems.add("必备活动");
        } else {
            nonCompliantItems.add("必备活动");
        }

        boolean isAllCompliant = isBudgetCompliant && isDurationCompliant &&
                                isParticipantCountCompliant && isRequiredActivitiesCompliant;

        double adaptabilityScore = calculateAdaptabilityScore(plan, constraint, availableBudget,
                isBudgetCompliant, isDurationCompliant, isParticipantCountCompliant, isRequiredActivitiesCompliant);

        List<String> activeDates = activeBatches.stream()
                .map(GroupBatch::getTravelDate)
                .map(LocalDate::toString)
                .toList();

        return PlanCompareResult.builder()
                .planId(plan.getId())
                .planName(plan.getPlanName())
                .transportation(plan.getTransportation())
                .venue(plan.getVenue())
                .projects(plan.getProjects())
                .costPerPerson(plan.getCostPerPerson())
                .durationDays(plan.getDurationDays())
                .minParticipants(plan.getMinParticipants())
                .maxParticipants(plan.getMaxParticipants())
                .suitableActivities(plan.getSuitableActivities())
                .totalCost(totalCost)
                .isBudgetCompliant(isBudgetCompliant)
                .isTemplateBudgetCompliant(isTemplateBudgetCompliant)
                .isDurationCompliant(isDurationCompliant)
                .isParticipantCountCompliant(isParticipantCountCompliant)
                .isRequiredActivitiesCompliant(isRequiredActivitiesCompliant)
                .isAllCompliant(isAllCompliant)
                .compliantItems(compliantItems)
                .nonCompliantItems(nonCompliantItems)
                .adaptabilityScore(adaptabilityScore)
                .activeBatchCount(activeBatches.size())
                .activeBatchDates(activeDates)
                .build();
    }

    private double calculateAdaptabilityScore(TeamBuildingPlan plan, ConstraintRequest constraint,
                                              BigDecimal availableBudget,
                                              boolean budgetCompliant, boolean durationCompliant,
                                              boolean participantCompliant, boolean activitiesCompliant) {
        double score = 0.0;
        int passedChecks = 0;

        if (budgetCompliant) {
            passedChecks++;
            // 分母同样只认池子可占用余额，保证打分明细和合规口径一致
            if (availableBudget.signum() > 0) {
                BigDecimal budgetRatio = plan.getCostPerPerson().multiply(BigDecimal.valueOf(constraint.getParticipantCount()))
                        .divide(availableBudget, 4, BigDecimal.ROUND_HALF_UP);
                score += 0.3 * Math.max(0, 1 - budgetRatio.doubleValue());
            }
        }

        if (durationCompliant) {
            passedChecks++;
            score += 0.2;
        }

        if (participantCompliant) {
            passedChecks++;
            int capacityRange = plan.getMaxParticipants() - plan.getMinParticipants();
            if (capacityRange > 0) {
                double capacityUtilization = (double) constraint.getParticipantCount() / capacityRange;
                score += 0.25 * (0.5 + Math.abs(capacityUtilization - 0.5));
            } else {
                score += 0.25;
            }
        }

        if (activitiesCompliant) {
            passedChecks++;
            if (constraint.getRequiredActivities() != null && !constraint.getRequiredActivities().isEmpty()) {
                String[] required = constraint.getRequiredActivities().split(",");
                String suitable = plan.getSuitableActivities();
                int matched = 0;
                for (String activity : required) {
                    if (suitable.contains(activity.trim())) {
                        matched++;
                    }
                }
                score += 0.25 * ((double) matched / required.length);
            } else {
                score += 0.25;
            }
        }

        if (passedChecks == 0) {
            return 0.0;
        }

        return Math.round(score * 100.0) / 100.0;
    }

}