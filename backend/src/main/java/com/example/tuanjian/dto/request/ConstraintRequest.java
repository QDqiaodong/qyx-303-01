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
public class ConstraintRequest {

    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @NotNull(message = "预算上限不能为空")
    @Positive(message = "预算上限必须为正数")
    private BigDecimal budgetLimit;

    @NotNull(message = "最大出行天数不能为空")
    @Positive(message = "最大出行天数必须为正数")
    private Integer maxDurationDays;

    @NotNull(message = "参与人数不能为空")
    @Positive(message = "参与人数必须为正数")
    private Integer participantCount;

    private String requiredActivities;

}