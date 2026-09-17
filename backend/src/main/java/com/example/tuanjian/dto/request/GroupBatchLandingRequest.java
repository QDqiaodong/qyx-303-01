package com.example.tuanjian.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 从当次对比结果落批次的请求。
 * 模板预算上限（budgetLimit）只用于建模板和对账展示，
 * 能不能落下去只看预算池当时未被占住的余额，因此这里不接收预算上限。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupBatchLandingRequest {

    @NotNull(message = "方案不能为空")
    private Long planId;

    @NotNull(message = "出行日期不能为空")
    private LocalDate travelDate;

    @NotNull(message = "成团人数不能为空")
    @Positive(message = "成团人数必须为正数")
    private Integer groupSize;

    /** 当次对比用的最大出行天数，服务端按方案现值重新校验 */
    @NotNull(message = "最大出行天数不能为空")
    @Positive(message = "最大出行天数必须为正数")
    private Integer maxDurationDays;

    /** 当次对比用的必备活动，服务端按方案现值重新校验 */
    private String requiredActivities;

}
