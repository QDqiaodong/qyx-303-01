package com.example.tuanjian.repository;

import com.example.tuanjian.entity.BudgetTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetTransactionRepository extends JpaRepository<BudgetTransaction, Long> {

    List<BudgetTransaction> findAllByOrderByCreatedAtDesc();

    List<BudgetTransaction> findByBatchIdOrderByCreatedAtAsc(Long batchId);

}
