package com.example.tuanjian.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanCreateRequest {

    @NotBlank(message = "方案名称不能为空")
    private String planName;

    private String description;

    @NotBlank(message = "交通方式不能为空")
    private String transportation;

    @NotBlank(message = "场地不能为空")
    private String venue;

    @NotBlank(message = "项目内容不能为空")
    private String projects;

    @NotNull(message = "人均费用不能为空")
    @Positive(message = "人均费用必须为正数")
    private BigDecimal costPerPerson;

    @NotNull(message = "时长天数不能为空")
    @Positive(message = "时长天数必须为正数")
    private Integer durationDays;

    @NotNull(message = "最小人数不能为空")
    @Positive(message = "最小人数必须为正数")
    private Integer minParticipants;

    @NotNull(message = "最大人数不能为空")
    @Positive(message = "最大人数必须为正数")
    private Integer maxParticipants;

    @NotBlank(message = "适配活动不能为空")
    private String suitableActivities;

}