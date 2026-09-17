package com.example.tuanjian.controller;

import com.example.tuanjian.dto.request.ConstraintRequest;
import com.example.tuanjian.dto.request.PlanCreateRequest;
import com.example.tuanjian.dto.response.PlanCompareResult;
import com.example.tuanjian.entity.TeamBuildingPlan;
import com.example.tuanjian.service.BudgetService;
import com.example.tuanjian.service.TeamBuildingPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TeamBuildingPlanController {

    private final TeamBuildingPlanService planService;
    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<TeamBuildingPlan> createPlan(@Valid @RequestBody PlanCreateRequest request) {
        TeamBuildingPlan plan = planService.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(plan);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamBuildingPlan> getPlanById(@PathVariable Long id) {
        TeamBuildingPlan plan = planService.getPlanById(id);
        return ResponseEntity.ok(plan);
    }

    @GetMapping
    public ResponseEntity<List<TeamBuildingPlan>> getAllPlans() {
        List<TeamBuildingPlan> plans = planService.getAllPlans();
        return ResponseEntity.ok(plans);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeamBuildingPlan> updatePlan(@PathVariable Long id, @Valid @RequestBody PlanCreateRequest request) {
        TeamBuildingPlan plan = planService.updatePlan(id, request);
        return ResponseEntity.ok(plan);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable Long id) {
        planService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/compare")
    public ResponseEntity<List<PlanCompareResult>> comparePlans(@Valid @RequestBody ConstraintRequest constraint) {
        List<PlanCompareResult> results = planService.comparePlans(constraint);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/compare/filter")
    public ResponseEntity<Map<String, Object>> compareAndFilter(@Valid @RequestBody ConstraintRequest constraint) {
        List<PlanCompareResult> allResults = planService.comparePlans(constraint);
        List<PlanCompareResult> compliantResults = allResults.stream()
                .filter(PlanCompareResult::getIsAllCompliant)
                .toList();

        return ResponseEntity.ok(Map.of(
                "allResults", allResults,
                "compliantResults", compliantResults,
                "totalCount", allResults.size(),
                "compliantCount", compliantResults.size(),
                // 预算裁决口径：池子里还没被占住的余额；模板上限只随请求体用于建模板和对账展示
                "budgetPool", budgetService.getPool()
        ));
    }

}