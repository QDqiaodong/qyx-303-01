package com.example.tuanjian.controller;

import com.example.tuanjian.dto.request.ConstraintRequest;
import com.example.tuanjian.entity.ConstraintCondition;
import com.example.tuanjian.service.ConstraintConditionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/constraints")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ConstraintConditionController {

    private final ConstraintConditionService constraintService;

    @PostMapping
    public ResponseEntity<ConstraintCondition> createTemplate(@Valid @RequestBody ConstraintRequest request) {
        ConstraintCondition constraint = constraintService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(constraint);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConstraintCondition> getTemplateById(@PathVariable Long id) {
        ConstraintCondition constraint = constraintService.getTemplateById(id);
        return ResponseEntity.ok(constraint);
    }

    @GetMapping
    public ResponseEntity<List<ConstraintCondition>> getAllTemplates() {
        List<ConstraintCondition> constraints = constraintService.getAllTemplates();
        return ResponseEntity.ok(constraints);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConstraintCondition> updateTemplate(@PathVariable Long id, @Valid @RequestBody ConstraintRequest request) {
        ConstraintCondition constraint = constraintService.updateTemplate(id, request);
        return ResponseEntity.ok(constraint);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        constraintService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cache/{key}")
    public ResponseEntity<Void> saveToRedis(@PathVariable String key, @RequestBody ConstraintRequest constraint) {
        constraintService.saveToRedis(key, constraint);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/cache/{key}")
    public ResponseEntity<ConstraintRequest> getFromRedis(@PathVariable String key) {
        ConstraintRequest constraint = constraintService.getFromRedis(key);
        if (constraint == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(constraint);
    }

    @DeleteMapping("/cache/{key}")
    public ResponseEntity<Void> deleteFromRedis(@PathVariable String key) {
        constraintService.deleteFromRedis(key);
        return ResponseEntity.ok().build();
    }

}