package com.example.tuanjian;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.tuanjian.dto.request.GroupBatchLandingRequest;
import com.example.tuanjian.dto.request.PlanCreateRequest;
import com.example.tuanjian.entity.TeamBuildingPlan;
import com.example.tuanjian.service.BudgetService;
import com.example.tuanjian.service.TeamBuildingPlanService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TuanjianCompareApplication.class, properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc
class GroupLandingWebConcurrencyTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TeamBuildingPlanService planService;
    @Autowired
    private BudgetService budgetService;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    private Long planA;
    private Long planB;

    @BeforeEach
    void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.createNativeQuery("delete from budget_transaction").executeUpdate();
            entityManager.createNativeQuery("delete from group_batch").executeUpdate();
            entityManager.createNativeQuery("delete from team_building_plan").executeUpdate();
            entityManager.createNativeQuery(
                    "update budget_pool set total_amount=100000, occupied_amount=0 where id=1").executeUpdate();
            entityManager.clear();
        });
        TeamBuildingPlan a = planService.createPlan(plan("方案甲", "湖畔营地", "300.00"));
        TeamBuildingPlan b = planService.createPlan(plan("方案乙", "湖畔营地", "200.00"));
        planA = a.getId();
        planB = b.getId();
    }

    private PlanCreateRequest plan(String name, String venue, String cost) {
        return PlanCreateRequest.builder()
                .planName(name)
                .transportation("大巴")
                .venue(venue)
                .projects("拓展")
                .costPerPerson(new BigDecimal(cost))
                .durationDays(2)
                .minParticipants(10)
                .maxParticipants(60)
                .suitableActivities("户外拓展")
                .build();
    }

    @Test
    void concurrentDifferentPlansSameSlot_oneCreatedOneConflictBudgetChargedOnce() throws Exception {
        int threads = 12;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger created = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            long planId = (i % 2 == 0) ? planA : planB;
            pool.submit(() -> {
                try {
                    start.await();
                    GroupBatchLandingRequest req = GroupBatchLandingRequest.builder()
                            .planId(planId)
                            .travelDate(LocalDate.parse("2028-05-01"))
                            .groupSize(20)
                            .maxDurationDays(3)
                            .requiredActivities("户外拓展")
                            .build();
                    int code = mockMvc.perform(post("/api/batches/land")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(req)))
                            .andReturn().getResponse().getStatus();
                    if (code == 201) created.incrementAndGet();
                    else if (code == 409) conflict.incrementAndGet();
                    else other.incrementAndGet();
                } catch (Exception e) {
                    other.incrementAndGet();
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(created).hasValue(1);
        assertThat(conflict.get() + other.get()).isEqualTo(threads - 1);
        // 关键：不允许出现 500（事务回滚泄漏），后到者必须是 409
        assertThat(other).hasValue(0);

        // 预算只被扣一次：甲 6000 或 乙 4000
        BigDecimal occupied = budgetService.getPool().getOccupiedAmount();
        assertThat(occupied.compareTo(new BigDecimal("6000.00")) == 0
                || occupied.compareTo(new BigDecimal("4000.00")) == 0).isTrue();
    }
}
