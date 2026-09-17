package com.example.tuanjian.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "team_building_plan")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamBuildingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String planName;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 200)
    private String transportation;

    @Column(nullable = false, length = 200)
    private String venue;

    @Column(nullable = false, length = 500)
    private String projects;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal costPerPerson;

    @Column(nullable = false)
    private Integer durationDays;

    @Column(nullable = false)
    private Integer minParticipants;

    @Column(nullable = false)
    private Integer maxParticipants;

    @Column(nullable = false, length = 200)
    private String suitableActivities;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}