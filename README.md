# qyx-303 企业团建方案多方案条件对比择优推演系统

## 项目简介
企业团建方案多方案条件对比择优推演系统，包含 Spring Boot 后端、Vue/Vite 前端、MySQL 和 Redis。
在方案登记、约束模板、对比择优之上，新增"落地成团"：从当次对比中挑一份当时合规的方案落成批次，
同时从公司团建预算池扣占预算、占住出行当天的场地。

## 预算口径（重要）
- 模板上的「预算上限」只用于**建模板**，以及对比页给对账的人和当初建模板的数字核对（页面标注为"对账口径"）。
- 对比里**算不算超预算**、批次**能不能落下去**，只认预算池当时**还没被占住的余额**：
  可占用余额 = 池子总额 − 所有生效批次已扣金额之和。两套口径冲突时，落地口径优先。

## 落地成团规则
- 批次台账（group_batch）与预算进出流水（budget_transaction）在同一个数据库事务内做成，少一头整体回滚。
- 同一天、同一场地只允许一条生效批次（active_key 部分唯一索引兜底），并发抢占只有一条成功，后到者收到 409 且预算不重复扣。
- 已落地方案的人均费用被修改：按落地当时成团人数重算，与原扣额对不上的批次立即失效、钱全额退回池子，且不能再对接场地供应商。
- 不允许只在对比结果上勾选"已选定"而背后没有批次、没有预算进出；选定方案的唯一入口是真正落成批次。
- 预算池初始总额由配置 `tuanjian.budget.initial-total` 控制（默认 200000），也可在"落地批次与预算池"页调整。

## 主要 API
- `POST /api/batches/land` 落地成团（planId、travelDate、groupSize、maxDurationDays、requiredActivities）
- `GET  /api/batches?status=ACTIVE|INVALID` 批次台账
- `POST /api/batches/{id}/contact-supplier` 对接场地供应商（失效批次被拒）
- `GET  /api/budget/pool` 预算池（总额/已占用/可占用余额）
- `GET  /api/budget/transactions` 预算进出流水（HOLD/REFUND/ADJUST）
- `POST /api/budget/pool/adjust` 调整池子总额

## 前端访问地址
- 默认地址：http://localhost:8203
- 127.0.0.1：http://127.0.0.1:8203

## 端口
- 前端：8203
- 后端 API：8303
- MySQL：3403
- Redis：6503

## 启动命令
```bash
sh start.sh
```

## 验证命令
```bash
cd backend && mvn compile -q
cd ../frontend && npm ci && npm run build
cd .. && docker compose up -d --build
curl -sS http://localhost:8203
curl -sS http://127.0.0.1:8203
```
