# NEV-v2 XL 执行计划（4 Wave / 30 天）

> **治理来源**：canonical vibe session `20260514T142916Z-fec5c2d3`（bounded stop = `xl_plan`，grade floor = XL）
> **回溯需求**：`docs/requirements/2026-05-14-requirement-analysis-greenfield-rebuild-new-repo-nev-v2-ruoyi-vu.md`
> **创建日期**：2026-05-14
> **状态**：FROZEN（冻结，进入 plan_execute 后回溯到本计划）
> **替代文件**：本文件取代 vibe runtime 自动生成的关键词占位版

---

## 0. 元信息与内部等级决定

| 项 | 值 |
|---|---|
| 计划等级 | **内部 XL** —— wave-sequential + 步骤级 bounded parallel |
| 总工期 | **30 天**（4 Wave，每 Wave 7-8 天） |
| 执行人 | 单人（区块链工程学生） |
| 执行模式 | `interactive_governed` |
| 启动条件 | 需求文档已冻结 ✓ |
| 终止条件 | Wave 4 末通过演示验收，所有 phase cleanup 完成 |

### XL 等级合理性

虽然单人开发，但等级定为 XL 是因为：
- 工作量超过单一 service 范围（覆盖 4 业务 + 7 角色 + 合约 + 双前端）
- 存在天然的**步骤级 bounded parallel 机会**（合约编写 ↔ 后端骨架；admin-web ↔ user-app）
- 需要 wave 级里程碑 + 验证关卡，避免 1 个月走偏

---

## 1. Wave 总览

```
Day 1───────────Day 7      Day 8──────────Day 14    Day 15─────────Day 21    Day 22────────Day 30
│                         │                        │                        │
│ Wave 1                  │ Wave 2                 │ Wave 3                 │ Wave 4
│ 地基铺设                │ 区块链 + 电池溯源      │ 商城 + 碳模块          │ 前端 + 联调 + 文档
│                         │                        │                        │
└─ 后端骨架可登录         └─ /api/admin/battery    └─ 4 块 API 全可用       └─ 演示彩排通过
   20 张表建好               能创建电池并上链        碳积分流水产生            admin/user 双端跑通
   7 角色菜单初始化          扫码免登录可查         以旧换新闭环            文档 + ER 图齐全
```

| Wave | 主题 | Day | 关键交付 | 风险 |
|---|---|---|---|---|
| W1 | Foundation | 1-7 | 后端骨架可登录 + 20 张 nev_* 表 + 7 角色 + demo seed | 学习曲线 |
| W2 | Battery + Blockchain | 8-14 | 3 合约部署 + nev-battery + nev-blockchain 模块 | 合约调试 |
| W3 | Marketplace + Carbon | 15-21 | 商城闭环 + 碳核算 + 以旧换新 + 碳积分 | 状态机复杂度 |
| W4 | Frontend + Polish | 22-30 | admin-web + user-app 端到端 + 文档 + 演示彩排 | 联调集成 |

- Governance scope: root
- Root run id: 20260514T142916Z-fec5c2d3
- Entry intent: vibe-how
- Requested stop stage: xl_plan
- Requested grade floor: XL
- Frozen route pack: orchestration-core
- Frozen route skill: vibe
- Frozen route mode: confirm_required
- Router/runtime skill mismatch: False
- Execution topology companion: E:\Study\IdeaProjects\NEV\outputs\runtime\vibe-sessions\20260514T142916Z-fec5c2d3\execution-topology.json
## Anti-Proxy-Goal-Drift Controls
Prefill from the frozen requirement doc where available. Only diverge with explicit justification.

### Primary Objective
execution-plan xl-wave nev-v2 ruoyi-vue-plus mysql sa-token mybatis-plus solidit...

### Non-Objective Proxy Signals
- single sample pass only
- current test green only
- demo success only

### Validation Material Role
validation_only

### Declared Tier
Tier C

### Intended Scope
scenario_specific

### Abstraction Layer Target
_author_to_declare_

### Completion State Target
partial

### Generalization Evidence Plan
- Reuse the requirement-declared proof boundary as the starting point.
- cases: []
- note: add independent evidence before generalized completion claims

## Internal Grade Decision
- Grade: XL
- User-facing runtime remains fixed; grade is internal only.
- `vibe` remains the governor and final authority for execution flow.

## Wave Plan
- Wave 1: skeleton, intent freeze, and requirement validation
- Wave 2: implementation decomposition and bounded ownership assignment
- Wave 3: verification, reconciliation, and cleanup handoff

## Delivery Acceptance Plan
- Freeze downstream product acceptance inside the governed requirement doc and reuse it rather than inventing closeout claims later.
- Emit a per-run delivery-acceptance report during `phase_cleanup` so runtime/process success is kept separate from project-delivery success.
- Delivery-acceptance report: E:\Study\IdeaProjects\NEV\outputs\runtime\vibe-sessions\20260514T142916Z-fec5c2d3\delivery-acceptance-report.json
- If manual spot checks are declared in the requirement doc, final completion wording stays blocked until they are cleared or explicitly downgraded to manual review.
- Release truth aggregation remains an outer-layer gate; this run emits the per-run delivery-truth report only.

## Artifact Review Strategy
- If the frozen requirement doc declares `Artifact Review Requirements`, execution must leave behind explicit artifact-review evidence rather than relying on generic completion wording.
- Artifact review may be recorded inline in `phase-execute.json` or through a dedicated `artifact-review.json` sidecar, but one of those governed surfaces must exist when direct artifact review is required.
- Product acceptance stays blocked when required artifact review remains missing, partial, degraded, or manual-review-only.

## Code Task TDD Evidence Plan
- Reuse the frozen `Code Task TDD Evidence Requirements` section from the requirement doc rather than inventing late closeout claims.
- Reuse the frozen `Code Task TDD Exceptions` section when strict failing-first sequencing is intentionally exempted.
- Map each frozen requirement or exception to an implementation step, a targeted verification command, and a proof artifact.
- If strict failing-first sequencing is blocked, execution must record the bounded reason and fallback evidence explicitly.

## Baseline Document Quality Mapping
- Use the frozen `Baseline Document Quality Dimensions` section in the requirement doc as the authoritative list of document-artifact quality dimensions that artifact review must cover before a document delivery can claim full completion.
- Track each baseline document dimension through artifact-review annotations so the delivery-acceptance report can show which structure, formatting, completeness, reference integrity, layout stability, and output fidelity expectations were inspected.
- Treat missing document-dimension coverage as a manual-review-required hit and keep this mapping separate from UI baselines and code-task TDD evidence.

## Baseline UI Quality Mapping
- Use the frozen `Baseline UI Quality Dimensions` section in the requirement doc as the authoritative list of dimensions that artifact review must cover before a UI delivery can claim full completion.
- Track each baseline dimension through execution and artifact-review annotations so the delivery-acceptance report can show which structure, interaction, state, consistency, responsiveness, and fidelity expectations were inspected.
- Treat missing dimension coverage as a manual-review-required hit and include explicit mapping steps or targeted verification units that drive reviewers to capture the evidence the requirement doc established.

## Task-Specific Acceptance Mapping
- Reuse frozen task-specific acceptance extensions from the requirement doc instead of inventing late closeout criteria.
- Keep base delivery truth separate from task-specific expectations so each can be inspected independently during review.

## Research Augmentation Plan
- Preserve any frozen research augmentation sources from the requirement doc so later reviewers can tell which external standards strengthened the brief.
- Research augmentation may strengthen rough asks, but it must not replace the user-owned requirement surface.

## Execution Topology Snapshot
- Delegation mode: selective_parallel_child_lanes
- Review mode: checkpoint_per_step
- Specialist execution mode: native_bounded_units
- Max parallel units: 2
- Wave `wave-1` has 4 executable step(s).
  Step `wave-1-specialist-pre_execution-serial-1` -> mode `sequential`, units `1`.
  Step `wave-1-parallel` -> mode `bounded_parallel`, units `2`.
  Step `wave-1-specialist-in_execution-parallel` -> mode `bounded_parallel`, units `4`.
  Step `wave-1-specialist-post_execution-parallel` -> mode `bounded_parallel`, units `1`.

## Specialist Decision Plan
- The governed runtime must keep one explicit specialist decision surface from freeze through delivery acceptance.
- Frozen decision state: approved_dispatch
- Frozen resolution mode: approved_dispatch
- Frozen decision notes: Bounded specialist recommendations were surfaced and auto-promoted into approved dispatch.

## Specialist Skill Dispatch Plan
- Specialist routing is mandatory and bounded inside governed `vibe`; it does not transfer runtime authority away from vibe.
- Eligible specialist recommendations should auto-promote into `approved_dispatch` by default.
- Before specialist execution starts, governed `vibe` emits one unified disclosure for the effective `approved_dispatch` set using each skill's real `native_skill_entrypoint`.
- Each specialist must be invoked through its native workflow, input contract, and validation style.
- Specialist outputs remain subordinate to the frozen requirement and the governed plan.
- Dispatch webthinker-deep-research as specialist_assist.
  Binding profile: default; dispatch phase: in_execution; lane policy: inherit_grade; parallel in XL: True
  Write scope: specialist:webthinker-deep-research; review mode: native_contract; execution priority: 50
  Reason: top ranked specialist candidate from pack 'ruc-nlpir-augmentation' via keyword_ranked
  Required inputs: bounded specialist subtask contract, frozen requirement context, relevant source files or domain artifacts
  Expected outputs: bounded specialist findings or code changes, verification notes aligned with the specialist skill
  Verification: Preserve the specialist skill's native workflow, boundaries, and validation style.
- Dispatch scholarly-publishing as specialist_assist.
  Binding profile: deliverable; dispatch phase: post_execution; lane policy: bounded_parallel; parallel in XL: True
  Write scope: specialist:deliverable:scholarly-publishing; review mode: checkpoint_after_step; execution priority: 70
  Reason: top ranked specialist candidate from pack 'scholarly-publishing-workflow' via fallback_task_default
  Required inputs: bounded specialist subtask contract, frozen requirement context, relevant source files or domain artifacts
  Expected outputs: bounded specialist findings or code changes, verification notes aligned with the specialist skill
  Verification: Preserve the specialist skill's native workflow, boundaries, and validation style.
- Dispatch brainstorming as specialist_assist.
  Binding profile: default; dispatch phase: in_execution; lane policy: inherit_grade; parallel in XL: True
  Write scope: specialist:brainstorming; review mode: native_contract; execution priority: 50
  Reason: pack stage assistant from 'orchestration-core'
  Required inputs: bounded specialist subtask contract, frozen requirement context, relevant source files or domain artifacts
  Expected outputs: bounded specialist findings or code changes, verification notes aligned with the specialist skill
  Verification: Preserve the specialist skill's native workflow, boundaries, and validation style.
- Dispatch writing-plans as specialist_assist.
  Binding profile: planning; dispatch phase: pre_execution; lane policy: serial; parallel in XL: False
  Write scope: specialist:planning; review mode: native_contract; execution priority: 10
  Reason: pack stage assistant from 'orchestration-core'
  Required inputs: bounded specialist subtask contract, frozen requirement context, relevant source files or domain artifacts
  Expected outputs: bounded specialist findings or code changes, verification notes aligned with the specialist skill
  Verification: Preserve the specialist skill's native workflow, boundaries, and validation style.
- Dispatch autonomous-builder as specialist_assist.
  Binding profile: default; dispatch phase: in_execution; lane policy: inherit_grade; parallel in XL: True
  Write scope: specialist:autonomous-builder; review mode: native_contract; execution priority: 50
  Reason: pack stage assistant from 'orchestration-core'
  Required inputs: bounded specialist subtask contract, frozen requirement context, relevant source files or domain artifacts
  Expected outputs: bounded specialist findings or code changes, verification notes aligned with the specialist skill
  Verification: Preserve the specialist skill's native workflow, boundaries, and validation style.
- Dispatch cancel-ralph as specialist_assist.
  Binding profile: default; dispatch phase: in_execution; lane policy: inherit_grade; parallel in XL: True
  Write scope: specialist:cancel-ralph; review mode: native_contract; execution priority: 50
  Reason: pack stage assistant from 'orchestration-core'
  Required inputs: bounded specialist subtask contract, frozen requirement context, relevant source files or domain artifacts
  Expected outputs: bounded specialist findings or code changes, verification notes aligned with the specialist skill
  Verification: Preserve the specialist skill's native workflow, boundaries, and validation style.

## Specialist Consultation
These are specialists resolved for plan-time handling under governed `vibe` before this execution plan was frozen. Depending on policy, they may be consulted live or routed for direct current-session loading.
- Consulted Skill: webthinker-deep-research
  Why now: top ranked specialist candidate from pack 'ruc-nlpir-augmentation' via keyword_ranked
  Loaded from: C:\Users\Administrator\.claude\skills\vibe\bundled\skills\webthinker-deep-research\SKILL.runtime-mirror.md
- Consulted Skill: scholarly-publishing
  Why now: top ranked specialist candidate from pack 'scholarly-publishing-workflow' via fallback_task_default
  Loaded from: C:\Users\Administrator\.claude\skills\vibe\bundled\skills\scholarly-publishing\SKILL.runtime-mirror.md
- Consulted Skill: brainstorming
  Why now: pack stage assistant from 'orchestration-core'
  Loaded from: C:\Users\Administrator\.claude\skills\vibe\bundled\skills\brainstorming\SKILL.runtime-mirror.md

Deferred specialist follow-up stayed separate from execution dispatch and remains advisory until execution-time approval.
- Deferred to execution: writing-plans (max_consults_per_window_reached)
- Deferred to execution: autonomous-builder (max_consults_per_window_reached)
- Deferred to execution: cancel-ralph (max_consults_per_window_reached)

## Unified Specialist Lifecycle Disclosure This unified disclosure keeps routing truth, consultation truth, and execution truth separate while showing one user-readable specialist timeline.  ### discussion_routing - Skill: webthinker-deep-research   State: routed   Why now: top ranked specialist candidate from pack 'ruc-nlpir-augmentation' via keyword_ranked   Loaded from: C:\Users\Administrator\.claude\skills\vibe\bundled\skills\webthinker-deep-research\SKILL.runtime-mirror.md - Skill: scholarly-publishing   State: routed   Why now: top ranked specialist candidate from pack 'scholarly-publishing-workflow' via fallback_task_default   Loaded from: C:\Users\Administrator\.claude\skills\vibe\bundled\skills\scholarly-publishing\SKILL.runtime-mirror.md - Skill: brainstorming   State: routed   Why now: pack stage assistant from 'orchestration-core'   Loaded from: C:\Users\Administrator\.claude\skills\vibe\bundled\skills\brainstorming\SKILL.runtime-mirror.md - Skill: writing-plans   State: routed   Why now: pack stage assistant from 'orchestration-core'   Loaded from: C:\Users\Administrator\.claude\skills\vibe\bundled\skills\writing-plans\SKILL.runtime-mirror.md - Skill: autonomous-builder   State: routed   Why now: pack stage assistant from 'orchestration-core'   Loaded from: C:\Users\Administrator\.claude\skills\vibe\bundled\skills\autonomous-builder\SKILL.runtime-mirror.md - Skill: cancel-ralph   State: routed   Why now: pack stage assistant from 'orchestration-core'   Loaded from: C:\Users\Administrator\.claude\skills\vibe\bundled\skills\cancel-ralph\SKILL.runtime-mirror.md  ### discussion_consultation - Skill: webthinker-deep-research   State: routed_pending_current_session   Why now: top ranked specialist candidate from pack 'ruc-nlpir-augmentation' via keyword_ranked   Loaded from: C:\Users\Administrator\.claude\skills\vibe\bundled\skills\webthinker-deep-research\SKILL.runtime-mirror.md - Skill: scholarly-publishing   State: routed_pending_current_session   Why now: top ranked specialist candidate from pack 'scholarly-publishing-workflow' via fallback_task_default   Loaded from: C:\Users\Administrator\.claude\skills\vibe\bundled\skills\scholarly-publishing\SKILL.runtime-mirror.md - Skill: brainstorming   State: routed_pending_current_session   Why now: pack stage assistant from 'orchestration-core'   Loaded from: C:\Users\Administrator\.claude\skills\vibe\bundled\skills\brainstorming\SKILL.runtime-mirror.md  ### planning_consultation - Skill: webthinker-deep-research   State: routed_pending_current_session   Why now: top ranked specialist candidate from pack 'ruc-nlpir-augmentation' via keyword_ranked   Loaded from: C:\Users\Administrator\.claude\skills\vibe\bundled\skills\webthinker-deep-research\SKILL.runtime-mirror.md - Skill: scholarly-publishing   State: routed_pending_current_session   Why now: top ranked specialist candidate from pack 'scholarly-publishing-workflow' via fallback_task_default   Loaded from: C:\Users\Administrator\.claude\skills\vibe\bundled\skills\scholarly-publishing\SKILL.runtime-mirror.md - Skill: brainstorming   State: routed_pending_current_session   Why now: pack stage assistant from 'orchestration-core'   Loaded from: C:\Users\Administrator\.claude\skills\vibe\bundled\skills\brainstorming\SKILL.runtime-mirror.md

## Memory Context
Bounded stage-aware memory context injected into execution planning:
- Disclosure level: decision_and_relation_focused
- Capsule [ae2db7390a4cab3e] Cognee relation: verify-fisco-bcos-e2e-blockchain-anchoring-chain-record-tx-hash specified_by 2026-04-21-verify-fisco-bcos-e2e-blockchain-an...
  Owner: Cognee
  Why now: Matched Cognee memory for xl_plan.
  Expansion Ref: E:\Study\IdeaProjects\NEV\outputs\runtime\vibe-sessions\20260514T142916Z-fec5c2d3\memory-backend\cognee-read-response.json#ae2db7390a4cab3e
  Summary: Cognee relation: verify-fisco-bcos-e2e-blockchain-anchoring-chain-record-tx-hash specified_by 2026-04-21-verify-fisco-bcos-e2e-blockchain-anchoring-chain-record-tx-hash.md
  Summary: specified_by
- Capsule [b00a4152c0a36f14] Cognee relation: verify-fisco-bcos-e2e-blockchain-anchoring-chain-record-tx-hash planned_in 2026-04-21-verify-fisco-bcos-e2e-blockchain-anch...
  Owner: Cognee
  Why now: Matched Cognee memory for xl_plan.
  Expansion Ref: E:\Study\IdeaProjects\NEV\outputs\runtime\vibe-sessions\20260514T142916Z-fec5c2d3\memory-backend\cognee-read-response.json#b00a4152c0a36f14
  Summary: Cognee relation: verify-fisco-bcos-e2e-blockchain-anchoring-chain-record-tx-hash planned_in 2026-04-21-verify-fisco-bcos-e2e-blockchain-anchoring-chain-record-tx-hash-execution-plan.md
  Summary: planned_in

## Completion Language Rules
- Do not report runtime completion as downstream project delivery unless the delivery-acceptance report returns `PASS`.
- `completed_with_failures`, degraded execution, or pending manual actions must downgrade completion wording.
- Child-governed completion remains local-scope only and cannot justify root-level completion language.

## Ownership Boundaries
- One owner per artifact set.
- Parallel work must use disjoint write scopes.
- Subagent prompts must end with `$vibe`.
- Specialist help stays bounded and native-mode; it must not become a second planner or a second runtime.

## Verification Commands
- Run targeted repo verification for changed surfaces.
- Run runtime contract gate before claiming completion.
- Review the delivery-acceptance report emitted during `phase_cleanup` before using full completion language.
- Re-run mirror sync and parity validation before release claims.

## Rollback Plan
- Revert only the governed-runtime change set if verification fails.
- Do not roll back unrelated user changes.

## Phase Cleanup Contract
- Remove temp artifacts created by the wave.
- Run node audit and cleanup when needed.
- Write cleanup receipt before completion.

---

## 2. Wave 1：地基铺设（Day 1-7）

### 2.1 目标
后端能登录跑通、20 张业务表建好、7 角色 + 菜单初始化、demo seed 可一键导入。

### 2.2 步骤分解（按天）

| Day | 任务 | 类型 | 可并行 |
|---|---|---|---|
| D1 | 创建 `E:/Study/IdeaProjects/NEV-v2/`，git init；克隆 ruoyi-vue-plus 5.X 到 backend/ 并 reset 历史 | 串行 | — |
| D1-2 | IDEA 全局重构：`org.dromara` → `com.nev`；调整 artifactId；改 application.yml（端口 9280、库 nev_v2） | 串行 | — |
| D2 | docker-compose 起 MySQL 8（6306）+ Redis（6379）+ MinIO（暂不用）；跑 ruoyi 自带 SQL | 串行 | — |
| D2-3 | 启动 backend → 访问 `/swagger-ui/index.html` → admin/admin123 登录跑通 | 串行 | — |
| D3-4 | 设计 20 张 nev_* 表 SQL（按需求文档 §5），写到 `backend/src/main/resources/sql/nev_v2_business.sql` | 串行 | 与 D3 合约前置研究并行（A） |
| D4 | 跑建表 SQL，校验 ER 图（用 DBeaver 自动出图） | 串行 | — |
| D5 | 写 `nev_v2_seed.sql`：7 角色 + 菜单 + demo 用户（每角色 2 个）+ 排放因子 + 商品分类 | 串行 | 与 D5 plus-ui 调研并行（B） |
| D5-6 | sys_role 配置 7 角色 + 菜单权限矩阵（admin/producer/distributor/retailer/merchant/consumer/recycler） | 串行 | — |
| D6 | 用 7 个 demo 用户分别登录 swagger，验证菜单/权限差异 | 验证 | — |
| D7 | Wave 1 cleanup：写 phase-1 receipt、git commit、记日志 | 收尾 | — |

**Bounded parallel slot A**（D3 内）：在写表 SQL 同时，提前研究 webase-app-sdk + ContractInvoker 设计，为 Wave 2 节省时间。
**Bounded parallel slot B**（D5 内）：在配菜单权限同时，预先 clone plus-ui，跑通 npm dev，为 Wave 4 节省时间。

### 2.3 验收（Verification）

```bash
# Smoke test 1：后端启动
curl http://localhost:9280/actuator/health
# 期待：{"status":"UP"}

# Smoke test 2：登录
curl -X POST http://localhost:9280/api/public/login -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123","captcha":"...","uuid":"..."}'
# 期待：返回 token

# Smoke test 3：表数量
mysql -h localhost -P 6306 -u root -p nev_v2 -e "SHOW TABLES LIKE 'nev_%';" | wc -l
# 期待：≥ 20

# Smoke test 4：角色数量
mysql -e "SELECT role_key FROM sys_role WHERE del_flag='0';"
# 期待：admin, producer, distributor, retailer, merchant, consumer, recycler
```

### 2.4 Wave 1 验收标准

- [ ] 后端 9280 端口启动成功
- [ ] admin/admin123 能登录获得 token
- [ ] swagger-ui 可访问
- [ ] `nev_*` 表 ≥ 20 张
- [ ] sys_role 含 7 个新角色
- [ ] 每个角色都有至少 1 个 demo 用户
- [ ] consumer 用户访问 admin 接口被拒（403）

### 2.5 Rollback
未通过验收时：回退到上一日的 git commit；不允许进入 Wave 2，宁可 Wave 1 多花 1-2 天

### 2.6 Phase Cleanup（D7）
写 `outputs/runtime/vibe-sessions/<run-id>/phase-1-cleanup.json`，包含：已建表清单、已配角色清单、smoke test 通过截图路径、遗留问题列表、用时 vs 计划差异

---

## 3. Wave 2：Battery + Blockchain（Day 8-14）

### 3.1 目标
3 合约部署到 WeBASE、nev-blockchain 模块封装、nev-battery 模块能创建电池并触发 PRODUCED 上链、扫码免登录可查。

### 3.2 步骤分解
| Day | 任务 | 可并行 |
|---|---|---|
| D8-9 | 编写 3 合约（RoleManager / BatteryRegistry / LifecycleTrace），Hardhat 编译 | 与 D8-9 BlockchainProperties/ContractInvoker 骨架并行（C） |
| D10 上午 | 启动老仓 WeBASE（沿用 5000-5004 端口）；上传新合约源码到 WeBASE-Front | — |
| D10 下午 | 部署 3 合约 → 拿到 3 个新地址 → 写入 `nev_contract_config` 表 | — |
| D11 | 在 RoleManager 合约里 `assignRole()` 7 角色（用 demo 用户的钱包地址） | — |
| D11-12 | nev-blockchain 模块：BlockchainProperties / BlockchainClient / ContractInvoker / RoleService | 与 D11-12 nev-battery entity + mapper 设计并行（D） |
| D13 | nev-battery 模块：BatteryService / BatteryLifecycleService + 上链 PRODUCED 事件 | — |
| D13 | Controllers：`/api/producer/battery/create` `/api/admin/battery/list` `/api/public/scan/{traceNumber}` | — |
| D14 | 端到端验证：producer 创建电池 → 看到 txHash → 扫码看到上链事件 | — |

### 3.3 验收标准
- [ ] 3 合约成功部署到 WeBASE，地址写入数据库
- [ ] 7 角色全部在 RoleManager 合约里注册
- [ ] producer 能成功创建电池并返回 txHash
- [ ] 链上能查到 PRODUCED 事件
- [ ] 公开扫码接口免登录能用，返回完整溯源信息
- [ ] dataHash 校验通过

### 3.4 Rollback
合约部署失败：回退到上次正常部署的 ABI；如 WeBASE 不可用则启用 `blockchain.enabled=false` 降级模式（业务流程仍能跑，只是链上功能不可用）

---

## 4. Wave 3：Marketplace + Carbon（Day 15-21）

### 4.1 目标
商城闭环（商品/购物车/订单/支付）+ 碳核算（5 阶段计算 + 排放因子）+ 以旧换新 + 碳积分流水。

### 4.2 步骤分解
| Day | 任务 | 可并行 |
|---|---|---|
| D15 | nev-marketplace 模块创建：MerchantService / ProductService + entity + mapper | 与 D15 nev-carbon 模块创建并行（E） |
| D16 | CartService（加购/改量/删除）+ Controllers `/api/merchant/product/**` `/api/consumer/cart/**` | 与 D16 EmissionFactorService 并行（E 续） |
| D17 | OrderService 状态机（PENDING→PAID→SHIPPED→DELIVERED→COMPLETED + CANCELLED/REFUNDED） | — |
| D17-18 | PaymentService（模拟支付，直接调用确认接口） | 与 D17-18 CarbonCalculationService（5 阶段公式） 并行（F） |
| D18 | Controllers：`/api/consumer/order/**` `/api/merchant/order/**` | — |
| D19 | TradeInService（以旧换新）+ Controllers `/api/consumer/trade-in/**` `/api/recycler/trade-in/audit` | — |
| D19 | TradeIn 触发 RECYCLED 上链（与 nev-blockchain 联动） | — |
| D20 | CarbonCreditService（碳积分流水）+ Controllers `/api/consumer/carbon/**` `/api/admin/carbon/emission-factor/**` | — |
| D20 | 业务事件触发碳积分发放：以旧换新成功 +50；购买高碳替代电池 +100（规则可配置） | — |
| D21 | 端到端联调：consumer 浏览 → 加购 → 下单 → 支付 → 收货 → 完成；碳积分变化可查 | — |

### 4.3 验收标准
- [ ] consumer 能完成下单→支付→收货全流程
- [ ] merchant 能上架商品 + 接单发货
- [ ] 订单状态机异常路径被拒绝（已 COMPLETED 不能取消）
- [ ] 以旧换新闭环：consumer 提交→recycler 审核通过→上链 RECYCLED→consumer 碳积分增加
- [ ] 扫码看电池能看到 5 阶段碳足迹明细
- [ ] 碳积分流水表 `nev_carbon_credit_record` 有完整变动记录

---

## 5. Wave 4：Frontend + Polish（Day 22-30）

### 5.1 目标
admin-web + user-app 端到端跑通、文档齐全（ER 图 + API 文档 + 合约设计说明）、演示彩排通过。

### 5.2 步骤分解
| Day | 任务 | 可并行 |
|---|---|---|
| D22 | 克隆 plus-ui 到 `apps/admin-web/`；改 baseURL → 9280；npm dev 跑通登录 | 与 D22 user-app 复制 + baseURL 调整并行（G） |
| D23 | admin-web 配置菜单：电池管理 / 商品管理 / 订单管理 / 碳管理 / 合约配置 | — |
| D23-24 | admin-web 视图：BatteryList / BatteryDetail / OrderList / EmissionFactorList | 与 D23-24 user-app 接口适配（购物车/订单页面 baseURL 重写）并行（H） |
| D25 | merchant 子域：MerchantProductList / MerchantOrderList | — |
| D25 | producer/distributor/retailer/recycler 简化版页面（仅核心 1 个动作） | — |
| D26-27 | 端到端联调：4 个核心场景（演示路径）每个跑通 3 遍 | — |
| D27 | DBeaver 导出 ER 图；SpringDoc 截屏 API；画 4 个核心流程时序图 | 与 D27 写合约设计说明 .md 并行（I） |
| D28 | 写 `docs/architecture/`、`docs/contracts/`、`docs/api/`、根 README | — |
| D29 | 演示彩排 1：从启动 docker-compose 开始，完整 4 场景跑一遍并录屏 | — |
| D29 | 修演示中暴露的 bug | — |
| D30 | 演示彩排 2：彻底走完，确认无 bug；准备演示备份数据 SQL 快照 | — |

### 5.3 验收标准
- [ ] admin-web 7 角色都能登录 + 菜单可见性正确
- [ ] user-app consumer 能完成扫码 / 下单 / 以旧换新 / 看碳积分
- [ ] 4 个演示场景每个不超过 5 分钟、全程无报错
- [ ] ER 图、API 文档、合约设计说明、README 全部存在且完整
- [ ] 演示备份 SQL 快照可在 30 秒内恢复 demo 数据

### 5.4 Rollback / Fallback
如果 D27 末仍有核心场景跑不通，启动**应急砍范围预案**（已在需求文档 R1 风险条目里授权）：
- 砍 distributor / retailer / recycler 的专属页面，只保留这 3 角色的权限定义和 API
- 这 3 角色的演示用 admin 代演（admin 角色拥有全权限）
- 仍要保 producer / merchant / consumer 三个角色的端到端

---

## 6. 跨 Wave 风险登记与缓解

| # | 风险 | 影响 Wave | 缓解 |
|---|---|---|---|
| K1 | RuoYi-Plus 学习曲线超 2 天 | W1 | D1 提前看 dromara/plus-doc 文档；如 D2 仍无法登录，立即降级到 RuoYi-Vue3 官方版（牺牲 Sa-Token，但 Spring Security 更熟） |
| K2 | WeBASE 部署失败或合约 gas 报错 | W2 | 启用 `blockchain.enabled=false` 降级；业务流程仍能跑；演示时单独演示链上验证 |
| K3 | 订单状态机 bug 影响支付 | W3 | 状态机封装为枚举 + 状态转移矩阵，禁止裸字符串；写 5 个 case 的单测 |
| K4 | uni-app 接口适配工作量超估 | W4 | D22 立即评估，超 1 天则放弃部分页面（保留扫码/订单核心，砍 carbon-map / settings 等次要页） |
| K5 | 演示当天意外（断网/数据库挂） | 全部 | 准备 demo 备份 SQL 快照 + 离线演示视频（D30 录） |
| K6 | 个人精力跟不上（生病/课业） | 全部 | 每 Wave 末必有验收，未通过宁可延 1-2 天也不带病推进 |

---

## 7. 关键完成语规则

按需求文档 §18 的 Completion Language Policy：
- **不能说**「全部完成」「100% 实现」，除非：所有 Wave 验收通过 + cleanup receipt 齐全 + 演示彩排无 bug
- **必须用降级措辞**当：某 Wave 部分未达验收（"Wave N 部分完成，X 子项延后处理"）
- **必须明示当**：触发了 fallback 预案（"为保证演示，已启用 R1 应急砍范围预案，distributor/retailer/recycler 专属页面用 admin 代演"）

---

## 8. 治理产物清单（plan_execute 阶段产出）

每 Wave 末必产：
- `outputs/runtime/vibe-sessions/<run-id>/phase-N-cleanup.json`
- git commit + tag `wave-N-end`
- Wave 验收截图 / 终端输出存档到 `outputs/runtime/vibe-sessions/<run-id>/wave-N-evidence/`

总收尾必产：
- `outputs/runtime/vibe-sessions/<run-id>/cleanup-receipt.json`
- `outputs/runtime/vibe-sessions/<run-id>/delivery-acceptance-report.json`
- 演示录屏：`outputs/demo-rehearsal-2026-XX-XX.mp4`

---

## 9. 不做（Non-Goals 强化）

本计划严格不做：
- ❌ 决策引擎 / 召回 / 梯次 / 再制造（毕设阶段）
- ❌ ClickHouse / Elasticsearch
- ❌ K8s 部署 / CI/CD
- ❌ 真实支付接入
- ❌ 完整测试覆盖（仅核心 service happy path 测试）
- ❌ 多租户 / 工作流（RuoYi-Plus 自带，配置关闭）
- ❌ 合约创新点实现（仅预留接口）
- ❌ admin-app（实验性应用，不做）
- ❌ knowledge_articles 知识文章（删，需求文档已砍）
- ❌ 国际化英文版

---

## 10. 启动开关 / 决策门

进入 Wave 1 前必须确认：
- [ ] 用户已审阅本计划并授权进入 plan_execute
- [ ] MySQL 8.0+ 可在 6306 端口启动（或调整端口）
- [ ] Docker 可用
- [ ] WeBASE 老仓部署仍可访问（如不可，K2 应急生效）
- [ ] 用户接受 1 个月独立工期 + R1 应急砍范围预案
- [ ] 用户接受本计划在 Wave 2 末和 Wave 3 末的中途决策门（plan_execute 阶段会暂停等审阅）

---

## 11. 治理元数据

| 项 | 值 |
|---|---|
| run_id | 20260514T142916Z-fec5c2d3 |
| governance_scope | root |
| entry_intent | vibe-how |
| requested_stop | xl_plan |
| requested_grade_floor | XL |
| selected_pack | orchestration-core |
| autonomy_mode | interactive_governed |
| inherits_requirement | docs/requirements/2026-05-14-requirement-analysis-greenfield-rebuild-new-repo-nev-v2-ruoyi-vu.md |
| internal_grade_decision | XL |
| wave_count | 4 |
| total_days | 30 |
| bounded_parallel_slots | A B C D E F G H I（共 9 个步骤级并行点） |

---

## 12. 下一步

**本次 bounded stop = `xl_plan`。**

进入下一阶段需要新开一轮：
- `$vibe-do` → 进入 `plan_execute`，按 Wave 1 第一天的 D1 任务开始动手
- 或 `$vibe` → 一气推到 `phase_cleanup`（vibe runtime phase_cleanup 阶段已知 bug，建议分轮跑）

**强烈建议**：开干前先用一轮聊：
1. 确认计划无误
2. 拍板 K1 学习曲线超时是否真的允许降级到 RuoYi-Vue3 官方版（影响后续技术栈）
3. 确认 R1 应急砍范围预案的触发标准（什么时候算"周末进度滞后"）

或者直接说「$vibe-do 开干 Wave 1 D1」让我直接动手。

---

> 本计划由 canonical vibe 治理流程冻结。进入 plan_execute 后，每 Wave 末必须 cleanup + 用户审阅。任何 wave 范围扩展必须先回到 `requirement_doc` 阶段更新需求再回头改本计划。
