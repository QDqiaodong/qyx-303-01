package com.example.tuanjian.controller;

import com.example.tuanjian.dto.response.BudgetPoolView;
import com.example.tuanjian.entity.BudgetTransaction;
import com.example.tuanjian.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budget")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BudgetPoolController {

    private final BudgetService budgetService;

    /** 预算池现状：总额 / 已占用 / 可占用余额 */
    @GetMapping("/pool")
    public ResponseEntity<BudgetPoolView> getPool() {
        return ResponseEntity.ok(budgetService.getPool());
    }

    /** 预算进出流水（HOLD 占用 / REFUND 退回 / ADJUST 调整） */
    @GetMapping("/transactions")
    public ResponseEntity<List<BudgetTransaction>> getTransactions() {
        return ResponseEntity.ok(budgetService.getTransactions());
    }

    /** 行政追加/调整池子总额 */
    @PostMapping("/pool/adjust")
    public ResponseEntity<BudgetPoolView> adjustTotal(@RequestBody Map<String, Object> body) {
        Object rawTotal = body.get("totalAmount");
        if (rawTotal == null) {
            throw new IllegalArgumentException("totalAmount 不能为空");
        }
        BigDecimal newTotal = new BigDecimal(rawTotal.toString());
        String remark = body.get("remark") == null ? "行政调整预算池总额" : body.get("remark").toString();
        return ResponseEntity.ok(budgetService.adjustTotal(newTotal, remark));
    }

}
