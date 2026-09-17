package com.example.tuanjian.controller;

import com.example.tuanjian.dto.request.GroupBatchLandingRequest;
import com.example.tuanjian.entity.GroupBatch;
import com.example.tuanjian.service.GroupBatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GroupBatchController {

    private final GroupBatchService groupBatchService;

    /**
     * 落地成团：从当次对比里挑一份方案，写出行日期和成团人数，
     * 批次台账和预算扣款在同一事务内同时做成。
     */
    @PostMapping("/land")
    public ResponseEntity<GroupBatch> land(@Valid @RequestBody GroupBatchLandingRequest request) {
        GroupBatch batch = groupBatchService.land(request, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(batch);
    }

    /** 批次台账：可按状态过滤 ACTIVE / INVALID */
    @GetMapping
    public ResponseEntity<List<GroupBatch>> listBatches(
            @RequestParam(value = "status", required = false) String status) {
        return ResponseEntity.ok(groupBatchService.listBatches(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupBatch> getBatch(@PathVariable Long id) {
        return ResponseEntity.ok(groupBatchService.getBatch(id));
    }

    /**
     * 对接场地供应商：只有生效批次允许；
     * 已因费用变化失效的批次返回冲突错误，不能再拿去对接。
     */
    @PostMapping("/{id}/contact-supplier")
    public ResponseEntity<GroupBatch> contactSupplier(@PathVariable Long id) {
        return ResponseEntity.ok(groupBatchService.contactSupplier(id));
    }

}
