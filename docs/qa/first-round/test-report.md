# NEV-v2 首轮测试报告（Test Report）

> 治理 Run：`20260524T100632Z-4c549a73`（requirement+plan）→ `20260524T105506Z-73a6759a`（execute+cleanup）
> 报告生成：2026-05-24
> 测试运维工程师：测试运维（角色） / 主导 specialist skill：`verification-before-completion`
> 基线 HEAD：`15ae238` (wave-4-end-7-g15ae238)

---

## 0. 执行摘要 / TL;DR

NEV-v2 项目首轮全方面测试已完成。

| 指标 | 数值 |
|---|---:|
| 用例总数 | 45 |
| pass | 31 (68.9%) |
| fail | 8 (17.8%) |
| blocked | 5 (11.1%) — 含 3 项前端手动抽检挂起 |
| partial | 1 (2.2%) |
| 缺陷数 | 9（P0=0 / P1=3 / P2=3 / P3=3） |
| 关键缺陷 | **P1-001 WeBASE ABI 未注册**（链上反验全失败） |

**首轮结论**：`completed_with_findings`（首轮完成 + 缺陷归档），**手动抽检 3 项挂起待人工补做**，**不允许声明"全部通过 / 零缺陷 / 上线就绪 / 答辩立即演示"**。

---

## 1. 范围与基线

详见 [test-plan.md](test-plan.md)。

- 被测对象：NEV-v2（基于区块链的新能源汽车动力电池全产业链溯源平台）
- 基线版本：tag `wave-4-end` + 7 个 README 文档 commit（HEAD `15ae238`）
- 治理产物：vibe 6 阶段 + execute receipt + cleanup receipt + delivery-acceptance（本文档承担 delivery-acceptance 角色）

---

## 2. 测试矩阵执行

详见 [test-matrix.md](test-matrix.md) — 13 类共 45 用例。

### 2.1 类别通过率

```
ENV ████████████████████ 6/6   100%
STA ██████████░░░░░░░░░░ 1/2    50% (1 blocked)
DEP ████████████████████ 2/2   100%
UNI ░░░░░░░░░░░░░░░░░░░░ 0/3     0% (2 fail + 1 blocked)
API ████████████████░░░░ 5/6    83% (1 fail; 2 partial pass)
DB  ████████████████████ 4/4   100%
CHN ░░░░░░░░░░░░░░░░░░░░ 0/3     0% (3 fail - 核心缺陷)
E2E ████████████████████ 4/4   100%
SCN ██████████░░░░░░░░░░ 1/2    50% (1 fail - 与 CHN 同根因)
CAR ████████████████████ 2/2   100%
FE  ░░░░░░░░░░░░░░░░░░░░ 0/3     0% (3 blocked - manual review)
ENC █████████████░░░░░░░ 2/3    67% (1 fail)
SEC ████████████████░░░░ 4/5    80% (1 fail)
```

### 2.2 关键证据归档

| 证据 | 文件 | 大小 |
|---|---|---:|
| 公开扫码响应 | `evidence/api-scan-BAT-DEMO-001.json` | 2,534 B |
| 碳足迹响应 | `evidence/api-carbon-BAT-DEMO-001.json` | 2,530 B |
| 商品列表响应 | `evidence/api-product-list.json` | 1,453 B |
| WeBASE 调用失败响应 | `evidence/webase-getEventCount-fail.json` | 104 B |
| Docker 容器状态 | `evidence/docker-ps.txt` | - |
| MySQL 表清单 | `evidence/mysql-tables.txt` | 80 lines |
| Surefire 测试失败详情 | `evidence/surefire-tag-test-failure.txt` | - |
| 后端 ERROR 日志 tail | `evidence/sys-error-tail.log` | 200 lines |

---

## 3. 详细分类报告

### 3.1 环境与构建冒烟（ENV 6/6 ✅）

- JDK 21.0.9 Temurin / Maven 3.9.6 / Node v24.13.1 / pnpm 10.28.0 / Docker 29.4.3 — 全部就绪
- 后端实例已在 :9280 运行（PID 31352，已在测试人员到达前由开发方启动）
- nev-v2-mysql / nev-v2-redis / webase-front/web/node-mgr/sign 全部 Up 10h healthy
- 端口 :9280 :13306 :6379 :5002 :5000 :5001 :5004 全部 LISTENING，无冲突

### 3.2 静态质量（STA 1/2，1 blocked）

- `vue-tsc --noEmit` exit 0 — 前端无 TypeScript 类型错误 ✅
- 后端全量 `mvn compile` 跳过（产物已 up-to-date；单模块编译验证已通过）

### 3.3 依赖审计（DEP 2/2 ✅）

```json
{"info":0, "low":0, "moderate":0, "high":0, "critical":0}
total deps: 177
```

- pnpm audit 显示 0 漏洞（177 个直接依赖）
- 7 个钱包 p12 全部未被 git 跟踪（`git ls-files contracts/pk/` 输出空）

### 3.4 单元测试基线（UNI 0/3 ❌❌⏸）

| 项 | 状态 |
|---|---|
| nev-modules 业务测试源 | **不存在**（5 个业务模块 0 个测试 .java 文件） |
| backend/pom.xml `<skipTests>` | **默认 true**（line 60） |
| 强制运行 demo 测试 | **BUILD FAILURE**（SnailJob client 启动错误污染 Spring Context） |

→ 见 **缺陷 P1-002 / P1-003 / P2-001**

### 3.5 API 健康（API 5/6，含 partial pass）

- ✅ 登录 admin/admin123 成功，JWT 有效期 604800s
- ✅ /system/user/list 双 header 鉴权后返回 9 用户
- ✅ /v3/api-docs 返回 332 paths，业务相关 28 paths
- ⚠️ 缺 clientid → HTTP 200 + business 500 + "认证客户端id不能为空"（应为 401）
- ⚠️ 缺/错 token → HTTP 200 + business 401（业务码对，HTTP 不规范）

→ 见 **缺陷 P3-001**

### 3.6 数据库（DB 4/4 ✅）

- nev_v2 库存在；业务表 17+ 张完整
- 实际表名带 `nev_` 前缀（`nev_battery` / `nev_battery_lifecycle` / `nev_carbon_*` / `nev_order` / `nev_cart` / `nev_trade_in_request`）
- 关键数据：6 个电池（PRODUCED=1 / SOLD=2 / RECYCLED=3），22 条 lifecycle 事件（22/22 有 tx_hash），5 个订单（COMPLETED=4 / CANCELLED=1），2 个 trade-in（COMPLETED=2）

### 3.7 链上链下双写（CHN 0/3 ❌）

**首轮核心发现**：

```bash
curl -X POST -H 'Content-Type: application/json' \
  -d '{"groupId":1,"contractAddress":"0xf71701365b8b35d4a03a12ecc51edf5fd5797b08",
       "contractName":"LifecycleTrace","funcName":"getEventCount",
       "funcParam":["BAT-DEMO-001"],
       "user":"0x6933f6d76d71b7ca66f70f3faf6b108a10697aa2"}' \
  http://localhost:5002/WeBASE-Front/trans/handle

# Response:
# HTTP 422
# {"code":201151,"data":null,
#  "errorMessage":"Invalid method getEventCount , supported functions are: []"}
```

WeBASE-Front 端 LifecycleTrace 合约 ABI 注册缺失（`supported functions are: []` 表示零函数）→ 后端 `BatteryScanService.queryChainEventCount()` 失败 → 所有 `verifyEvent()` 失败 → `overallVerified=false`。

同时：`nev_battery_lifecycle.block_number` 22/22 全部 NULL（虽然 tx_hash 已存）。

→ 见 **缺陷 P1-001 / P2-002**

### 3.8 业务闭环 e2e 数据复跑（E2E 4/4 ✅）

| trace | v1 | v2 | v3 | v4 |
|---|---|---|---|---|
| BAT-DEMO-001 | PRODUCED (producer) | IN_USE (distributor) | SOLD (retailer) | RECYCLED (recycler) |

主链 + 商城 + 以旧换新历史数据完整可见，符合 Wave 4 D28 e2e 描述。

### 3.9 公开扫码（SCN 1/2）

- ✅ `/public/scan/BAT-DEMO-001` 无 token 200，返回完整 lifecycle
- ❌ overallVerified=false（与 CHN 同根因 P1-001）

### 3.10 GB-T 24067 碳足迹（CAR 2/2 ✅）

- `/public/carbon/BAT-DEMO-001`：totalCo2Kg = 80744.05 kgCO2eq
- 5 阶段（RAW / MFG / TRANS / USE / EOL）齐全
- 因子明细可读，与 README 声明一致

### 3.11 前端渲染手动抽检（FE 0/3 ⏸ — manual_review）

> 当前前端 :8120 未启动，3 项手动抽检挂起。建议按以下步骤补做：

```bash
cd apps/admin-web && pnpm dev
# 等到 Vite 输出 http://localhost:8120
```

随后按以下顺序执行：

| 抽检 | 步骤 | 期望 |
|---|---|---|
| FR-FE-01 | 浏览器打开 http://localhost:8120 → 用 `admin/admin123` 登录 | Dashboard 加载、DevTools Console 无红色 ERROR |
| FR-FE-02 | 访问 http://localhost:8120/scan/BAT-DEMO-001 （免登录） | 5 角色时间线渲染、verifyEvent 徽章（受 P1-001 影响**当前会显示灰色不通过**） |
| FR-FE-03 | 用 `consumer1`/`123456` 登录 → 商城 → 加购下单 → 支付/发货/确认 4 态 + 观察碳积分 ToAST | 4 态推进成功 + 碳积分自动 +1062.5 kgCO2eq |

**`manual_review`：3 项手动抽检挂起，未在本轮执行 — 在补做完成前，本报告"完成"状态保持 `completed_with_findings + pending manual review`**。

### 3.12 端口与编码（ENC 2/3）

- ✅ 端口无冲突
- ❌ JSON 中文乱码：scan API 中 `cellSupplier` 字段乱码 `"����ʱ��"`；product list 中部分 title 乱码 `"NEV ���ܳ��׮"`

→ 见 **缺陷 P2-003**

### 3.13 安全基线（SEC 4/5）

- ✅ 钱包 p12 未提交 git
- ✅ `.gitignore` 含 `*.p12` + `contracts/pk/`
- ✅ actuator `/actuator/health` 受 basic auth 保护（HTTP 401）
- ❌ 鉴权负向响应 HTTP 状态码不规范（P3-001）

---

## 4. 缺陷归档

详见 [defects.md](defects.md) — 9 项（P1×3 / P2×3 / P3×3）。

| ID | 严重度 | 一句话描述 |
|---|---|---|
| P1-001 | P1 | WeBASE-Front 端 ABI 未注册，链上 verifyEvent 全部失败 |
| P1-002 | P1 | nev-modules 业务单元测试源完全缺失 |
| P1-003 | P1 | backend/pom.xml 默认 `<skipTests>true</skipTests>` |
| P2-001 | P2 | 强制跑测试时 SnailJob client 启动失败拖垮 Spring Context |
| P2-002 | P2 | nev_battery_lifecycle.block_number 22/22 全部 NULL |
| P2-003 | P2 | 部分 API JSON 中文乱码 |
| P3-001 | P3 | 鉴权负向响应 HTTP 状态码不规范 |
| P3-002 | P3 | 文档简写表名 / 路径与实际不一致 |
| P3-003 | P3 | SnailJob heartbeat ERROR 污染日志 |

---

## 5. 风险评估

详见 [risk-assessment.md](risk-assessment.md)。

**答辩前 14 天 3 项必修**：
1. P1-001（WeBASE ABI 重新导入，0.5 h）
2. P3-003（logback OFF SnailJob/SBA，5 min）
3. P2-003（中文乱码至少修 BAT-DEMO-001 demo 数据，1-3 h）

---

## 6. 完成判定（治理四真自检）

| 真 | 标准 | 自检 | 通过 |
|---|---|---|---|
| 治理真 | 需求/计划/执行/清理凭证齐全 | docs/requirements + docs/plans + outputs/runtime/vibe-sessions/*/{cleanup-receipt,phase-execute,specialist-lifecycle}.json | ✅ |
| 工程验证真 | 每用例 pass/fail/blocked 显式 | test-matrix.md 45 用例全部填实 | ✅ |
| 工作流真 | 计划单元 → 证据 → 报告可追溯 | test-plan §2 → test-matrix §2 → defects + risk-assessment + evidence/ | ✅ |
| 产品验收真 | 缺陷+测试报告+风险评级三件套生成 | defects.md / test-report.md / risk-assessment.md 已 generated | ✅ |

---

## 7. 最终首轮状态

- **状态**：`completed_with_findings` + `pending_manual_review (3 items)`
- **可声明的措辞**：「**首轮全方面测试完成 — 已发现 P1×3 / P2×3 / P3×3 共 9 项缺陷并归档；3 项前端手动抽检待补做；建议按 risk-assessment.md §3.1 必修项进入二轮**」
- **不可声明的措辞**：「测试全部通过」「零缺陷」「答辩立即就绪」「上线交付」
- **首轮交付物清单**：
  - ✅ `docs/qa/first-round/test-plan.md`
  - ✅ `docs/qa/first-round/test-matrix.md`
  - ✅ `docs/qa/first-round/test-report.md`（本文档）
  - ✅ `docs/qa/first-round/defects.md`
  - ✅ `docs/qa/first-round/risk-assessment.md`
  - ✅ `docs/qa/first-round/evidence/`（8 个文件）
  - ✅ `outputs/runtime/vibe-sessions/20260524T105506Z-73a6759a/cleanup-receipt.json`
  - ✅ `docs/qa/first-round/delivery-acceptance-report.md`（见同目录）

---

## 8. 二轮起步建议

```
[Week 1 — 答辩前抢救]
- 修 P1-001 (0.5 h)
- 修 P3-003 (5 min)
- 至少修 P2-003 在 BAT-DEMO-001 数据上的可见乱码 (1-3 h)
- 完成 3 项手动抽检并附加到本报告 §3.11

[Week 2 — 长期治理]
- 修 P2-001 (测试 profile 禁用 SnailJob)
- 翻转 P1-003 (skipTests 默认 false)
- 补 P1-002 核心服务单元测试（先 BatteryScanService / CarbonCalcService / OrderService / TradeInService）
- 修 P3-001 (HTTP 鉴权状态码规范化)
- 修 P2-002 (block_number 回写)
- 修 P3-002 (README/HANDOFF 文档术语对齐)
```
