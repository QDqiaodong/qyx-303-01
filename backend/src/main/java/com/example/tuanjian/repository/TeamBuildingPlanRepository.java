package com.example.tuanjian.repository;

import com.example.tuanjian.entity.TeamBuildingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamBuildingPlanRepository extends JpaRepository<TeamBuildingPlan, Long> {

    List<TeamBuildingPlan> findByPlanNameContaining(String planName);

    List<TeamBuildingPlan> findAllByOrderByCreatedAtDesc();

}