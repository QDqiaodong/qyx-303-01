package com.example.tuanjian.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预算进出流水：每一笔从池子里的占用或退回都必须留痕，
 * 与批次状态变更在同一个数据库事务里做成。
 */
@Entity
@Table(name = "budget_transaction")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetTransaction {

    /** 落地成团占用预算 */
    public static final String TYPE_HOLD = "HOLD";
    /** 批次失效退回预算 */
    public static final String TYPE_REFUND = "REFUND";
    /** 池子总额调整（充值） */
    public static final String TYPE_ADJUST = "ADJUST";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "plan_id")
    private Long planId;

    /** 占用记正数、退回记负数（对占用额的影响方向） */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(length = 300)
    private String remark;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

}
