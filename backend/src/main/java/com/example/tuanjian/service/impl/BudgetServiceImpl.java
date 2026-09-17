package com.example.tuanjian.service.impl;

import com.example.tuanjian.dto.response.BudgetPoolView;
import com.example.tuanjian.entity.BudgetPool;
import com.example.tuanjian.entity.BudgetTransaction;
import com.example.tuanjian.exception.BusinessConflictException;
import com.example.tuanjian.repository.BudgetPoolRepository;
import com.example.tuanjian.repository.BudgetTransactionRepository;
import com.example.tuanjian.service.BudgetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetServiceImpl implements BudgetService, ApplicationRunner {

    private final BudgetPoolRepository poolRepository;
    private final BudgetTransactionRepository transactionRepository;

    @Value("${tuanjian.budget.initial-total:200000}")
    private BigDecimal initialTotal;

    @Override
    public void run(ApplicationArguments args) {
        initializePoolIfAbsent();
    }

    @Override
    @Transactional
    public void initializePoolIfAbsent() {
        if (poolRepository.findById(BudgetPool.SINGLETON_ID).isEmpty()) {
            BudgetPool pool = BudgetPool.builder()
                    .id(BudgetPool.SINGLETON_ID)
                    .totalAmount(initialTotal)
                    .occupiedAmount(BigDecimal.ZERO)
                    .build();
            poolRepository.save(pool);
            log.info("预算池初始化完成，总额: {}", initialTotal);
        }
    }

    @Override
    public BudgetPoolView getPool() {
        BudgetPool pool = loadPool();
        return toView(pool);
    }

    @Override
    @Transactional
    public void hold(Long batchId, Long planId, BigDecimal amount, String remark) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessConflictException("占用金额必须为正数");
        }
        BudgetPool pool = poolRepository.findByIdForUpdate(BudgetPool.SINGLETON_ID)
                .orElseGet(this::initPoolInsideTransaction);
        BigDecimal available = pool.getTotalAmount().subtract(pool.getOccupiedAmount());
        if (amount.compareTo(available) > 0) {
            throw new BusinessConflictException(
                    String.format("预算池可占用余额不足：需要 ¥%s，当前可占用余额 ¥%s",
                            amount.toPlainString(), available.toPlainString()));
        }
        pool.setOccupiedAmount(pool.getOccupiedAmount().add(amount));
        poolRepository.save(pool);
        transactionRepository.save(BudgetTransaction.builder()
                .batchId(batchId)
                .planId(planId)
                .amount(amount)
                .type(BudgetTransaction.TYPE_HOLD)
                .remark(remark)
                .build());
        log.info("预算占用: batch={}, plan={}, amount={}", batchId, planId, amount);
    }

    @Override
    @Transactional
    public void refund(Long batchId, Long planId, BigDecimal amount, String remark) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessConflictException("退回金额必须为正数");
        }
        BudgetPool pool = poolRepository.findByIdForUpdate(BudgetPool.SINGLETON_ID)
                .orElseGet(this::initPoolInsideTransaction);
        if (amount.compareTo(pool.getOccupiedAmount()) > 0) {
            throw new BusinessConflictException("退回金额不能大于当前已占用预算");
        }
        pool.setOccupiedAmount(pool.getOccupiedAmount().subtract(amount));
        poolRepository.save(pool);
        transactionRepository.save(BudgetTransaction.builder()
                .batchId(batchId)
                .planId(planId)
                .amount(amount.negate())
                .type(BudgetTransaction.TYPE_REFUND)
                .remark(remark)
                .build());
        log.info("预算退回: batch={}, plan={}, amount={}", batchId, planId, amount);
    }

    @Override
    @Transactional
    public BudgetPoolView adjustTotal(BigDecimal newTotal, String remark) {
        if (newTotal == null || newTotal.signum() <= 0) {
            throw new BusinessConflictException("预算池总额必须为正数");
        }
        BudgetPool pool = poolRepository.findByIdForUpdate(BudgetPool.SINGLETON_ID)
                .orElseGet(this::initPoolInsideTransaction);
        if (newTotal.compareTo(pool.getOccupiedAmount()) < 0) {
            throw new BusinessConflictException(
                    String.format("调整后总额 ¥%s 不能低于已被占住的 ¥%s",
                            newTotal.toPlainString(), pool.getOccupiedAmount().toPlainString()));
        }
        BigDecimal delta = newTotal.subtract(pool.getTotalAmount());
        pool.setTotalAmount(newTotal);
        poolRepository.save(pool);
        transactionRepository.save(BudgetTransaction.builder()
                .amount(delta)
                .type(BudgetTransaction.TYPE_ADJUST)
                .remark(remark)
                .build());
        return toView(pool);
    }

    @Override
    public List<BudgetTransaction> getTransactions() {
        return transactionRepository.findAllByOrderByCreatedAtDesc();
    }

    private BudgetPool loadPool() {
        return poolRepository.findById(BudgetPool.SINGLETON_ID)
                .orElseGet(() -> {
                    initializePoolIfAbsent();
                    return poolRepository.findById(BudgetPool.SINGLETON_ID).orElseThrow();
                });
    }

    private BudgetPool initPoolInsideTransaction() {
        BudgetPool pool = BudgetPool.builder()
                .id(BudgetPool.SINGLETON_ID)
                .totalAmount(initialTotal)
                .occupiedAmount(BigDecimal.ZERO)
                .build();
        return poolRepository.save(pool);
    }

    private BudgetPoolView toView(BudgetPool pool) {
        return BudgetPoolView.builder()
                .totalAmount(pool.getTotalAmount())
                .occupiedAmount(pool.getOccupiedAmount())
                .availableAmount(pool.getTotalAmount().subtract(pool.getOccupiedAmount()))
                .updatedAt(pool.getUpdatedAt())
                .build();
    }

}
