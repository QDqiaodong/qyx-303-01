package com.example.tuanjian.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanCompareResult {

    private Long planId;

    private String planName;

    private String transportation;

    private String venue;

    private String projects;

    private BigDecimal costPerPerson;

    private Integer durationDays;

    private Integer minParticipants;

    private Integer maxParticipants;

    private String suitableActivities;

    private BigDecimal totalCost;

    /**
     * 预算合规——以预算池当时未被占住的余额为准（落地口径，唯一裁决标准）。
     */
    private Boolean isBudgetCompliant;

    /**
     * 预算合规——按模板上那道预算上限判断，只供对账的人和当初建模板的数字核对，
     * 不参与"是否合规/能否落地"的裁决。
     */
    private Boolean isTemplateBudgetCompliant;

    private Boolean isDurationCompliant;

    private Boolean isParticipantCountCompliant;

    private Boolean isRequiredActivitiesCompliant;

    private Boolean isAllCompliant;

    private List<String> compliantItems;

    private List<String> nonCompliantItems;

    private Double adaptabilityScore;

    /** 该方案当前仍生效（已占住场地和预算）的落地批次数 */
    private Integer activeBatchCount;

    /** 生效批次的出行日期，yyyy-MM-dd 逗号拼接，用于对比页提示哪些场次已被占 */
    private List<String> activeBatchDates;

}