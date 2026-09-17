package com.example.tuanjian.repository;

import com.example.tuanjian.entity.BudgetPool;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BudgetPoolRepository extends JpaRepository<BudgetPool, Long> {

    /** 行级悲观锁，扣减/退回预算时串行化，杜绝余额被扣成负数或重复扣 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select p from BudgetPool p where p.id = :id")
    Optional<BudgetPool> findByIdForUpdate(Long id);

}
