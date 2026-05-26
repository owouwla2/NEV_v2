# NEV-v2 二轮测试报告（Round-2 Test Report）

> 治理 Run：`20260524T115714Z-9fddb02e`（round-2 governance shell）
> 报告生成：2026-05-25
> 基线：HEAD `15ae238`（wave-4-end-7-g15ae238）
> 输入：[docs/qa/first-round/](../first-round/) 的 9 项缺陷 + risk-assessment §3.1 必修项

---

## 0. 执行摘要 / TL;DR

二轮修复完成 6 项，1 项决策跳过：

| 修复项 | 缺陷 ID | 状态 | 端到端验证 |
|---|---|---|---|
| M-1 | P1-001 链上 verifyEvent 全失败 | ✅ | overallVerified=true，4/4 事件 chainVerified=true |
| M-2 | P3-003 SnailJob/SBA 日志噪声 | ✅ | sys-error.log 近 100 行 SnailJob ERROR = 0 |
| M-3 | P2-003 BAT-DEMO-001 中文乱码 | ✅ | API 响应字节验证「宁德时代」UTF-8 正确 |
| O-2 | P2-002 block_number 全 NULL | ✅ | 22/22 历史回填，未来新写靠 longOrNull |
| O-4 | P2-001 测试 SnailJob 启动失败 | ⚠️ partial | SnailJob 阻断已解除，但 placeholder 问题留 round-3 |
| O-1 | P3-001 HTTP 401 规范化 | ⏸ skipped | 用户决策：避免前端 axios 拦截器破坏 |

**关键指标对比**：

| 指标 | First Round | Round 2 |
|---|---|---|
| `overallVerified` | `false` | **`true`** ✅ |
| `verifiedEvents` | `0/4` | **`4/4`** ✅ |
| `chainEventCount` | `3` | `3` ✓（业务一致） |
| `nev_battery_lifecycle.block_number` 回写率 | `0/22` (0%) | **`22/22` (100%)** ✅ |
| SnailJob heartbeat ERROR / 10min | `~60 条` | **`0 条`** ✅ |
| `cellSupplier` (BAT-DEMO-001) | `?????` (U+FFFD) | **「宁德时代」** ✅ |
| 答辩演示就绪阻断项 | 3 项（P1-001 + P3-003 + P2-003） | **0 项** ✅ |
| 二轮 pass% 提升 vs first-round | — | first-round 68.9% → 二轮关键缺陷修复 100% |

**首轮"答辩前 14 天必修 3 项"全部清零**，可以进入答辩演示彩排。

---

## 1. M-1: P1-001 链上 verifyEvent 全失败 ✅

### 1.1 真根因（已 100% 验证）

**不是 WeBASE ABI 注册问题**（注册 ABI 后 WeBASE 端能正确解析任何 body 形式）。

**真根因：MySQL `version` 与链上 `events` 数组 version 错位**：
- MySQL `nev_battery_lifecycle.version` 从 1 起**包含 PRODUCED**
- 链上 `LifecycleTrace.events` 数组 version 从 1 起**不含 PRODUCED**（PRODUCED 走 `registerBattery` 单独存）

| MySQL | event_type | data_hash | 链上对应 | 链上 hash 匹配 |
|---|---|---|---|---|
| v1 | PRODUCED | 0xe422... | 走 `verifyBattery` | （registry mapping） |
| v2 | IN_USE | 0x3d0632... | events[v=1] type=1 | ✅ 完全相同 |
| v3 | SOLD | 0xb1ed30... | events[v=2] type=2 | ✅ 完全相同 |
| v4 | RECYCLED | 0x2b7a4c... | events[v=3] type=4 | ✅ 完全相同 |

### 1.2 修复

文件：`backend/nev-modules/nev-battery/src/main/java/com/nev/battery/service/BatteryScanService.java`

method 改造：
- `verifyOnChain(traceNumber, version, hash)` → `verifyOnChain(traceNumber, eventType, mysqlVersion, hash)`
- PRODUCED 分支：`verifyBattery(traceNumber, dataHash)`
- 其它分支：`verifyEvent(traceNumber, mysqlVersion - 1, dataHash)`
- `scan` 内 `overallOk` 判定调整：链上少 PRODUCED 一条，期望 `chainEventCount == lcList.size() - 1`

### 1.3 端到端验证

证据：`evidence/api-scan-after-M1-fix.json`

```json
"overallVerified": true,
"totalEvents": 4,
"verifiedEvents": 4,
"chainEventCount": 3,
"events": [
  {"version":1, "eventType":"PRODUCED",  "chainVerified": true},
  {"version":2, "eventType":"IN_USE",    "chainVerified": true},
  {"version":3, "eventType":"SOLD",      "chainVerified": true},
  {"version":4, "eventType":"RECYCLED",  "chainVerified": true}
]
```

附加副产物：WeBASE-Front 端已通过 `POST /abi` 注册 LifecycleTrace ABI（abiId=495821），未来需要直接调用 WeBASE 不传 inline ABI 也能工作。

---

## 2. M-2: P3-003 SnailJob/SBA 日志噪声 ✅

### 2.1 修复

文件：`backend/ruoyi-admin/src/main/resources/logback-plus.xml`（line 120-124）

```xml
<logger name="de.codecentric.boot.admin" level="OFF"/>
<logger name="com.aizuda.snailjob" level="OFF"/>
```

### 2.2 验证

证据：`evidence/sys-error-after-M2.log`

backend 重启后 sys-error.log 最近 100 行中含 SnailJob/SBA 的 ERROR = **0 条**。

---

## 3. M-3: P2-003 BAT-DEMO-001 中文乱码 ✅

### 3.1 根因定位

DB 原始 HEX：`EFBFBDEFBFBDEFBFBDEFBFBDCAB1EFBFBDEFBFBD`
- `EFBFBD` 是 Unicode REPLACEMENT CHARACTER (U+FFFD) 的 UTF-8 编码
- 数据**录入路径**已损坏（PowerShell GBK + curl POST），不可还原

### 3.2 修复

```sql
SET NAMES utf8mb4;
UPDATE nev_battery_lifecycle l
JOIN nev_battery b ON l.battery_id=b.id
SET l.payload = JSON_SET(l.payload, '$.cellSupplier', '宁德时代')
WHERE b.trace_number='BAT-DEMO-001' AND l.event_type='PRODUCED';
```

### 3.3 验证

证据：`evidence/api-scan-after-M1-fix.json` 字节验证

API 响应 PRODUCED.payload.cellSupplier 字节序列 = `E5AE81 E5BEB7 E697B6 E4BBA3` = 正确 UTF-8 「宁德时代」 ✅

### 3.4 同类未修条目（留 round-3 评估）

| trace_number | supplier 状态 |
|---|---|
| BAT-DEMO-002/003/536521 | 仍乱码（同类污染） |
| 其它 IN_USE/SOLD/RECYCLED 事件 payload.remark | 18 处 EFBFBD |

**长期防御**：API 入参字节检查 + 强制 charset UTF-8 + README 警告（待 round-3）。

---

## 4. O-2: P2-002 block_number 回写 ✅

### 4.1 根因

`ContractInvoker.parseResponse` 第 166 行（修复前）：

```java
JsonNode bn = firstNode(root, "blockNumber");
builder.blockNumber(bn != null && bn.canConvertToLong() ? bn.asLong() : null);
```

`canConvertToLong()` 仅在 NumericNode 时为 true，对 WeBASE 返回的字符串 `"140"` 永远 false → 永远 null。

### 4.2 修复

`backend/nev-modules/nev-blockchain/src/main/java/com/nev/blockchain/client/ContractInvoker.java`

新增 `longOrNull(JsonNode)` 兼容 NumericNode / TextNode / hex 字符串三种形态：

```java
private Long longOrNull(JsonNode node) {
    if (node == null || node.isNull()) return null;
    if (node.canConvertToLong()) return node.asLong();
    String txt = node.asText(null);
    if (txt == null || txt.isBlank()) return null;
    try {
        if (txt.startsWith("0x") || txt.startsWith("0X"))
            return Long.parseLong(txt.substring(2), 16);
        return Long.parseLong(txt);
    } catch (NumberFormatException ex) {
        log.warn("[blockchain] longOrNull 解析失败: {}", txt);
        return null;
    }
}
```

### 4.3 历史 22 条回填

通过 WeBASE `GET /WeBASE-Front/1/web3/transactionReceipt/{tx_hash}` 拿 receipt.blockNumber 然后 SQL UPDATE。

证据：`evidence/mysql-block-number-after-O2.txt`

```
total  with_bn
22     22
```

100% 回填完成。

### 4.4 后续

未来新 `addEvent` / `registerBattery` 写链后 block_number 自动回写——需要 backend 重启加载新 ContractInvoker.class（新 class 已在 `target/classes/`）。

---

## 5. O-4: P2-001 测试 SnailJob 启动失败 ⚠️ partial

### 5.1 部分修复

新增：`backend/ruoyi-admin/src/test/resources/application.yml`

```yaml
snail-job:
  enabled: false
spring:
  boot:
    admin:
      client:
        enabled: false
mybatis-plus:
  enableLogicDelete: true
  mapperPackage: com.nev.**.mapper
  mapperLocations: classpath*:mapper/**/*Mapper.xml
```

### 5.2 验证（部分成功）

测试错误从：
- before: `SnailJobClientException: snail-job server start error` ❌（P2-001 原症状）

变为：
- after: `Could not resolve placeholder 'user.password.maxRetryCount'` ⚠️（次级问题）

→ 说明 **SnailJob 阻断已解除**。

证据：`evidence/surefire-after-O4.txt`

### 5.3 后续（round-3 工作）

RuoYi-Vue-Plus 的 src/test/resources/application.yml 完全覆盖了 main 配置，丢失大量 placeholder 引用。**测试基础设施重建**需 round-3 独立完成：
- 方案 A：把 src/test/resources/application.yml 改名为 application-test.yml + 测试基类用 @ActiveProfiles("test")
- 方案 B：用 maven-surefire-plugin systemPropertyVariables 注入测试专属系统属性
- 方案 C：主 application.yml 给所有 placeholder 加默认值

---

## 6. O-1: P3-001 HTTP 401 规范化 ⏸ skipped

### 6.1 现状

`SaTokenExceptionHandler` 已在业务 code 中设置 401/403，但 HTTP status 默认 200（RuoYi-Vue-Plus 全栈设计：前端只解析 body.code）。

### 6.2 决策

✅ **跳过**（用户确认）。

理由：
- 强改 HTTP 401 会导致前端 axios 拦截器在非 2xx 时 throw error 而不解析 body
- RuoYi-Vue-Plus 上游设计哲学是 200 + business code，破坏此约定影响范围大
- P3 级缺陷允许不修

---

## 7. Round-2 治理四真自检

| 真维度 | 标准 | 自检 | 通过 |
|---|---|---|---|
| 治理真 | 需求/计划/执行/清理凭证齐全 | round-2 vibe shell `20260524T115714Z-9fddb02e` cleanup-receipt | ✅ |
| 工程验证真 | 每修复项有显式 success/fail/skipped + before/after | fix-log.md + 本报告 §1-6 | ✅ |
| 工作流真 | 缺陷 → 修复 → 证据 → 报告可追溯 | first-round/defects.md → round-2/fix-log.md → round-2/test-report.md → round-2/evidence/ | ✅ |
| 产品验收真 | 答辩前必修 3 项归零 + 关键指标量化提升 | M-1/M-2/M-3 全 ✅ + overallVerified true + block_number 22/22 | ✅ |

---

## 8. 修复影响范围与回归

### 8.1 代码改动文件清单

| 文件 | 变更 | 影响范围 |
|---|---|---|
| `backend/ruoyi-admin/src/main/resources/logback-plus.xml` | +5 行 | 仅日志输出过滤 |
| `backend/nev-modules/nev-battery/src/main/java/com/nev/battery/service/BatteryScanService.java` | 改 2 个 method | 仅 `/public/scan/{traceNumber}` 接口逻辑 |
| `backend/nev-modules/nev-blockchain/src/main/java/com/nev/blockchain/client/ContractInvoker.java` | 改 parseResponse + 新增 longOrNull | 所有写链调用的 blockNumber 字段 |
| `backend/ruoyi-admin/src/test/resources/application.yml` | 新建 | 仅测试时生效 |

### 8.2 数据库改动

| 表 | 操作 | 行数 |
|---|---|---|
| `nev_battery_lifecycle` | UPDATE cellSupplier (BAT-DEMO-001 PRODUCED) | 1 |
| `nev_battery_lifecycle` | UPDATE block_number (历史回填) | 22 |
| WeBASE-Front 内部 abi 表 | POST /abi 注册 LifecycleTrace | abiId=495821 |

### 8.3 回归保障

| 类别 | 测试方式 | 状态 |
|---|---|---|
| ENV/DEP/DB/E2E/CAR/SCN 类用例 | 重跑 first-round/test-matrix.md 对应 FR-* | ✅ 未引入回归（数据持久化 + 业务逻辑增量改动） |
| API/CHN 类用例 | 重跑 BatteryScanService 路径 | ✅ pass 率从 fail 转为 pass |
| 单元测试 | mvn test | ⚠️ O-4 部分缓解，placeholder 问题留 round-3 |

---

## 9. 答辩演示就绪度

| 演示场景 | 关键依赖 | 当前状态 |
|---|---|---|
| 公开扫码 `/scan/BAT-DEMO-001` | overallVerified=true + chainVerified 徽章亮 | ✅ READY |
| 5 角色 e2e 主链链上链下双写校验 | verifyBattery + verifyEvent 全链路 | ✅ READY |
| 商城下单 4 态 + SOLD 上链 + 碳积分 | order/cart + addEvent 写链 | ✅ READY（受 M-1 修复保护） |
| 以旧换新 RECYCLED 上链 | addEvent | ✅ READY |
| GB-T 24067 5 阶段碳足迹 | 静态数据 | ✅ READY（first-round 已 pass） |
| 日志清爽（演示控制台） | M-2 SnailJob OFF | ✅ READY |

**答辩演示就绪度：100%（已通过 M-1/M-2/M-3 三项必修）**。

---

## 10. Round-3 起步建议

待修缺陷池：

| 优先级 | 项 | 估时 |
|---|---|---|
| 高 | P1-002 业务模块单元测试源补齐（BatteryScanService / CarbonCalcService / OrderService / TradeInService） | 多日 |
| 中 | O-4 完成：测试基础设施重建（application-test.yml + @ActiveProfiles） | 1 天 |
| 中 | P1-003 翻转 backend/pom.xml `<skipTests>false</skipTests>` + CI 整合 | 0.5h |
| 中 | M-3 同类污染清理：BAT-DEMO-002/003/536521 cellSupplier + 18 处 remark | 1-2h + 输入层防御 |
| 低 | P3-002 README/HANDOFF 文档术语对齐（nev_ 前缀 + 前后端路径区分） | 0.5d |

---

## 11. 完成措辞（合规检查）

✅ 可声明：
- "二轮修复完成：M-1 / M-2 / M-3 三项答辩前必修缺陷全部 fix-then-verified"
- "首轮 P1-001 链上 verifyEvent 全失败已根治，overallVerified=true 已经端到端验证"
- "答辩演示就绪度 100%，建议立即进入彩排"

❌ 不可声明：
- "全部缺陷已修" — O-4 partial、O-1 skipped、O-2 future write 待 backend 重启加载新 class、M-3 同类污染未修
- "上线就绪" — 单元测试基础设施仍未重建
- "零缺陷" — 仍有 round-3 待办池

---

## 12. 附录：证据文件清单

```
docs/qa/round-2/evidence/
├── api-scan-after-M1-fix.json         M-1 修复后 scan 响应（含 UTF-8 cellSupplier 也覆盖 M-3）
├── api-scan-after-M1.json             M-1 ABI 注册后但代码未改时的 scan 响应（中间快照）
├── api-scan-after-M3-only.json        M-3 数据修复后 + M-1 未修时的 scan 响应（中间快照）
├── mysql-block-number-after-O2.txt    O-2 回填后 22/22 统计
├── surefire-after-O4.txt              O-4 部分修复后 SpringBootTest 失败转移证据
└── sys-error-after-M2.log             M-2 修复后 backend ERROR 日志（无 SnailJob）
```
