package com.example.tuanjian.service.impl;

import com.example.tuanjian.dto.request.ConstraintRequest;
import com.example.tuanjian.entity.ConstraintCondition;
import com.example.tuanjian.repository.ConstraintConditionRepository;
import com.example.tuanjian.service.ConstraintConditionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConstraintConditionServiceImpl implements ConstraintConditionService {

    private final ConstraintConditionRepository constraintRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_KEY_PREFIX = "tuanjian:constraint:";
    private static final int REDIS_EXPIRE_HOURS = 24;

    @Override
    @Transactional
    public ConstraintCondition createTemplate(ConstraintRequest request) {
        ConstraintCondition constraint = ConstraintCondition.builder()
                .templateName(request.getTemplateName())
                .budgetLimit(request.getBudgetLimit())
                .maxDurationDays(request.getMaxDurationDays())
                .participantCount(request.getParticipantCount())
                .requiredActivities(request.getRequiredActivities())
                .status("ACTIVE")
                .build();
        ConstraintCondition saved = constraintRepository.save(constraint);
        saveToRedis("template:" + saved.getId(), request);
        return saved;
    }

    @Override
    @Transactional
    public ConstraintCondition updateTemplate(Long id, ConstraintRequest request) {
        ConstraintCondition constraint = constraintRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("约束模板不存在: " + id));
        constraint.setTemplateName(request.getTemplateName());
        constraint.setBudgetLimit(request.getBudgetLimit());
        constraint.setMaxDurationDays(request.getMaxDurationDays());
        constraint.setParticipantCount(request.getParticipantCount());
        constraint.setRequiredActivities(request.getRequiredActivities());
        ConstraintCondition saved = constraintRepository.save(constraint);
        saveToRedis("template:" + saved.getId(), request);
        return saved;
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        if (!constraintRepository.existsById(id)) {
            throw new NoSuchElementException("约束模板不存在: " + id);
        }
        constraintRepository.deleteById(id);
        deleteFromRedis("template:" + id);
    }

    @Override
    public ConstraintCondition getTemplateById(Long id) {
        return constraintRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("约束模板不存在: " + id));
    }

    @Override
    public List<ConstraintCondition> getAllTemplates() {
        return constraintRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public void saveToRedis(String key, ConstraintRequest constraint) {
        String redisKey = REDIS_KEY_PREFIX + key;
        try {
            String json = objectMapper.writeValueAsString(constraint);
            redisTemplate.opsForValue().set(redisKey, json, REDIS_EXPIRE_HOURS, TimeUnit.HOURS);
            log.info("Saved constraint to Redis: {}", redisKey);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize constraint for Redis", e);
        }
    }

    @Override
    public ConstraintRequest getFromRedis(String key) {
        String redisKey = REDIS_KEY_PREFIX + key;
        String json = redisTemplate.opsForValue().get(redisKey);
        if (json != null) {
            try {
                return objectMapper.readValue(json, ConstraintRequest.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize constraint from Redis", e);
            }
        }
        return null;
    }

    @Override
    public void deleteFromRedis(String key) {
        String redisKey = REDIS_KEY_PREFIX + key;
        redisTemplate.delete(redisKey);
        log.info("Deleted constraint from Redis: {}", redisKey);
    }

}