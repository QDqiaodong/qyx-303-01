package com.example.tuanjian.service;

import com.example.tuanjian.dto.request.ConstraintRequest;
import com.example.tuanjian.dto.request.PlanCreateRequest;
import com.example.tuanjian.dto.response.PlanCompareResult;
import com.example.tuanjian.entity.TeamBuildingPlan;

import java.util.List;

public interface TeamBuildingPlanService {

    TeamBuildingPlan createPlan(PlanCreateRequest request);

    TeamBuildingPlan updatePlan(Long id, PlanCreateRequest request);

    void deletePlan(Long id);

    TeamBuildingPlan getPlanById(Long id);

    List<TeamBuildingPlan> getAllPlans();

    List<PlanCompareResult> comparePlans(ConstraintRequest constraint);

}