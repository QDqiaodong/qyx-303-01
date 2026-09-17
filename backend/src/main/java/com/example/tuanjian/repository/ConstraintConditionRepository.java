package com.example.tuanjian.repository;

import com.example.tuanjian.entity.ConstraintCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConstraintConditionRepository extends JpaRepository<ConstraintCondition, Long> {

    List<ConstraintCondition> findByTemplateNameContaining(String templateName);

    List<ConstraintCondition> findAllByOrderByCreatedAtDesc();

}