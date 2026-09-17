package com.example.tuanjian.service;

import com.example.tuanjian.dto.request.ConstraintRequest;
import com.example.tuanjian.entity.ConstraintCondition;

import java.util.List;

public interface ConstraintConditionService {

    ConstraintCondition createTemplate(ConstraintRequest request);

    ConstraintCondition updateTemplate(Long id, ConstraintRequest request);

    void deleteTemplate(Long id);

    ConstraintCondition getTemplateById(Long id);

    List<ConstraintCondition> getAllTemplates();

    void saveToRedis(String key, ConstraintRequest constraint);

    ConstraintRequest getFromRedis(String key);

    void deleteFromRedis(String key);

}