package com.example.tuanjian.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 公司团建预算池，全系统单例（id 固定为 1）。
 * occupiedAmount 等于所有仍生效批次锁定金额之和，
 * 可占用余额 = totalAmount - occupiedAmount。
 */
@Entity
@Table(name = "budget_pool")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetPool {

    public static final Long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "occupied_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal occupiedAmount;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now();
    }

}
