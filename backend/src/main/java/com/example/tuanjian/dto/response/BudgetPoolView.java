package com.example.tuanjian.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预算池视图：总额、已占用、可占用余额。
 * 对比页标不标预算合规、批次能不能落下去，只认 availableAmount。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetPoolView {

    private BigDecimal totalAmount;

    private BigDecimal occupiedAmount;

    private BigDecimal availableAmount;

    private LocalDateTime updatedAt;

}
