package com.example.tuanjian.service;

import com.example.tuanjian.dto.request.GroupBatchLandingRequest;
import com.example.tuanjian.entity.GroupBatch;

import java.math.BigDecimal;
import java.util.List;

public interface GroupBatchService {

    /**
     * 落地成团：按当次对比口径重新校验方案（天数/人数/活动 + 池子余额），
     * 写批次台账并从预算池扣钱，两件事在同一个事务里同时做成。
     * 同一天同一场地已有生效批次、或并发抢占时，抛冲突异常且预算不扣。
     */
    GroupBatch land(GroupBatchLandingRequest request, String templateBudgetNote);

    List<GroupBatch> listBatches(String status);

    GroupBatch getBatch(Long id);

    /**
     * 方案人均费用被改动后调用：按落地当时的成团人数用新费用重算，
     * 对不上当初扣下额度的生效批次一律置失效、钱退回池子。
     * 返回被置失效的批次数。
     */
    int invalidateBatchesByPlanCostChange(Long planId, BigDecimal newCostPerPerson);

    /**
     * 对接场地供应商：只允许生效批次；失效批次（钱已退回）禁止对接。
     */
    GroupBatch contactSupplier(Long id);

    static String activeKey(java.time.LocalDate travelDate, String venue) {
        return travelDate.toString() + ':' + venue;
    }

}
