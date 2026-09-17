package com.example.tuanjian.repository;

import com.example.tuanjian.entity.GroupBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupBatchRepository extends JpaRepository<GroupBatch, Long> {

    List<GroupBatch> findAllByOrderByCreatedAtDesc();

    List<GroupBatch> findByStatusOrderByCreatedAtDesc(String status);

    List<GroupBatch> findByPlanIdAndStatusOrderByCreatedAtDesc(Long planId, String status);

    /**
     * 同一场地同一天是否已有生效批次。唯一索引 uk_group_batch_active_slot
     * 是最终兜底，两个并发落批次时只有一个能插入成功。
     */
    boolean existsByActiveKey(String activeKey);

}
