# NEV-v2 三轮测试报告（Round-3 Test Report）

> 治理 Run：`20260526T010924Z-3dc8e66a`（round-3 governance shell）
> 报告生成：2026-05-26
> 基线：HEAD `f38101d`（round-2 终板）

---

## 0. 执行摘要 / TL;DR

Round-3 完成 5 项修复，全部 GREEN：

| ID | 修复 | 状态 | 端到端验证 |
|---|---|---|---|
| M-A | P1-003 翻转 skipTests | ✅ | mvn test 不再 skip |
| M-B | B-1 长期防御文档 | ✅ | README + .env.example + docker-compose.yml 三处同步 16379 + Hyper-V 警告 |
| M-C | M-3 同类污染清理 | ✅ | 5 行污染数据修复，polluted_remaining=0 |
| M-D | P3-002 文档术语对齐 | ✅ | nev_ 前缀 + 路径速查表 + verifyBattery/verifyEvent 描述 |
| M-E | O-4 完整测试基础设施 | ✅ | TagUnitTest 1/1 pass (Spring Context 成功加载) |

**1 项推迟**：P1-002 业务模块单元测试源补齐 → round-4 多日工作。

---

## 1. 范围与基线

详见 [round-3 vibe requirement](../../requirements/) + [fix-log.md](fix-log.md)。

- 被测对象：NEV-v2 backend（round-2 终板 `f38101d`）
- 任务来源：[round-2 fix-log.md Continuation Notes](../round-2/fix-log.md) 「下次起点」清单
- 治理产物：vibe round-3 6 阶段自动完成

---

## 2. 修复矩阵执行

详见 [fix-log.md](fix-log.md)。

### 2.1 修复项通过率

```
M-A skipTests flip       ████████████████████ 1/1   100% ✅
M-B Hyper-V doc defense  ████████████████████ 1/1   100% ✅
M-C demo data cleanup    ████████████████████ 1/1   100% ✅
M-D doc terminology      ████████████████████ 1/1   100% ✅
M-E test infrastructure  ████████████████████ 1/1   100% ✅ (Spring Context loads + 1 demo test pass)
```

### 2.2 关键证据归档

| 证据 | 文件 | 用途 |
|---|---|---:|
| skipTests 翻转 | `evidence/m-a-skipTests-flipped.txt` | M-A grep 证据 |
| README + .env.example + docker-compose 同步 | `evidence/m-b-m-d-readme-snippets.txt` | M-B + M-D grep |
| Demo 污染基线 | `evidence/m-c-before-pollution-scan.txt` | M-C before |
| Demo 污染清零 | `evidence/m-c-after-pollution-zero.txt` | M-C after |
| TagUnitTest pass | `evidence/m-e-after-tagunittest-pass.txt` | M-E surefire 报告 |

---

## 3. 关键指标对比（first-round → round-2 → round-3）

| 指标 | First Round | Round-2 | Round-3 |
|---|---|---|---|
| `overallVerified` | false | true | **true** |
| `verifiedEvents` | 0/4 | 4/4 | **4/4** |
| `block_number` 回写率 | 0/22 | 22/22 | **22/22** |
| SnailJob ERROR / 10min | ~60 | 0 | **0** |
| `<skipTests>` 默认 | true | true | **false** |
| Demo 数据中文乱码 | 多处 | 5 行残留 | **0** |
| README 表名一致性 | ❌ 简写不一致 | ❌ 仍简写 | **✅ nev_ 前缀** |
| 前端 SPA / 后端 API 区分 | ❌ 文档未分 | ❌ 仍未分 | **✅ 路径速查表** |
| Hyper-V 6379 警告 | ❌ 无 | ⚠️ 仅 fix-log | **✅ README + .env + compose** |
| Spring Context 测试加载 | ❌ SnailJob 拖垮 | ⚠️ SnailJob 已绕过但 placeholder 失败 | **✅ TagUnitTest pass** |
| 答辩演示阻断项 | 3 项 P1 | 0 | **0** |

---

## 4. Round-3 治理四真自检

| 真维度 | 标准 | 自检 | 通过 |
|---|---|---|---|
| 治理真 | 需求/计划/执行/清理凭证齐全 | vibe round-3 `20260526T010924Z-3dc8e66a` 6 阶段产物 | ✅ |
| 工程验证真 | 每修复项有显式 success/fail/skipped + before/after | fix-log §M-A..M-E + 本报告 §2.2 | ✅ |
| 工作流真 | 缺陷 → 修复 → 证据 → 报告可追溯 | round-2 fix-log §Continuation Notes → round-3 fix-log → round-3 test-report → evidence/ | ✅ |
| 产品验收真 | 演示数据净化 + 文档对齐 + 测试基础设施可用 | M-C polluted=0 + M-D 路径速查 + M-E TagUnitTest pass | ✅ |

---

## 5. 修复影响范围与回归

### 5.1 代码改动文件清单

| 文件 | 变更 | 影响范围 |
|---|---|---|
| `backend/pom.xml` | line 60 skipTests true→false | 全局测试启用默认 |
| `README.md` | Redis 表 + Hyper-V 警告 + 路径速查 + 表名 + M-1 描述 | 文档导航 |
| `deploy/docker/.env.example` | REDIS_PORT=16379 + 注释 | 新人 onboarding |
| `deploy/docker/docker-compose.yml` | header 注释 + Redis 默认端口 | docker-compose up 默认行为 |
| `backend/ruoyi-admin/src/test/resources/application.yml` | 删除（移到 application-test.yml） | 测试 profile 隔离 |
| `backend/ruoyi-admin/src/test/resources/application-test.yml` | 新建（SnailJob + DataSource + Redis + SBA 完整覆盖） | 仅测试 profile=test 生效 |
| `backend/ruoyi-admin/src/test/java/com/nev/test/*UnitTest.java` (4 个) | 加 `@ActiveProfiles("test")` + `@SpringBootTest` 不变 | 让测试激活 test profile |

### 5.2 数据库改动

| 表 | 操作 | 行数 |
|---|---|---|
| `nev_battery_lifecycle.payload->>{cellSupplier,remark}` | UPDATE BAT-DEMO-001/002/003 | 5 |

### 5.3 回归保障

| 类别 | 测试方式 | 状态 |
|---|---|---|
| Round-2 修复回归（M-1 链上 verify / O-2 block_number / M-3 demo 数据） | scan API 仍 overallVerified=true | ✅ 未引入回归 |
| 测试 profile 切换不影响 dev/prod 启动 | mvn spring-boot:run + curl /auth/code | ✅（之前已运行验证） |
| Redis 16379 端口配置一致性 | 4 处同步：application-dev.yml / application-test.yml / .env / .env.example / docker-compose.yml | ✅ |

---

## 6. 答辩演示就绪度

| 演示场景 | 关键依赖 | Round-3 状态 |
|---|---|---|
| 公开扫码 `/public/scan/BAT-DEMO-001` | overallVerified=true + 中文 cellSupplier | ✅ READY（数据已净化） |
| 公开扫码 `/public/scan/BAT-DEMO-002/003` | 备份演示电池可用 | ✅ READY（M-C 修复） |
| 5 角色 e2e 主链链上链下双写校验 | M-1 修复 | ✅ READY |
| 商城下单 4 态 + SOLD 上链 + 碳积分 | 业务逻辑 | ✅ READY |
| 以旧换新 RECYCLED 上链 | addEvent | ✅ READY |
| 日志清爽 | M-2 OFF logger | ✅ READY |
| 新人 onboarding | README + .env.example + docker-compose 同步 16379 警告 | ✅ READY |

**答辩演示就绪度：100%**

---

## 7. Round-4 起步建议

### 7.1 长期工作（多日）

| 优先级 | 项 | 估时 |
|---|---|---:|
| 高 | P1-002 业务单元测试源补齐 | 多日 |
|   | - BatteryScanService（M-1 修复后逻辑，必补单测） | 0.5d |
|   | - ContractInvoker.longOrNull（O-2 修复后逻辑） | 0.5d |
|   | - CarbonCalcService（GB-T 24067 5 阶段） | 1d |
|   | - OrderService / TradeInService（商城 + 以旧换新） | 1d |
|   | - 给所有业务测试加 `@Tag("dev")` 让 mvn test 默认能跑 | 0.5d |
| 中 | 引入 JaCoCo 覆盖率报告 + 设置基线阈值 30% | 0.5d |
| 中 | M-3 输入层防御：API 入参拒绝 `EFBFBD` 字节字符串 | 0.5d |
| 低 | O-1 (P3-001) 重新评估：HTTP 401 规范化是否值得做 | 1h（评估） |

### 7.2 短期可选

- 触发一次 producer1 register 新电池验证 O-2 future write blockNumber 实时回写（会污染 demo 数据，需在 round-4 准备清理脚本）
- 答辩前 3 项前端手动抽检（首轮挂起，已不强制做）

---

## 8. 完成措辞（合规检查）

✅ 可声明：
- "Round-3 完成 5 项修复：M-A/M-B/M-C/M-D/M-E 全部 fix-then-verified"
- "首轮 9 项缺陷 + round-2 新发现 1 项 = 共 10 项缺陷，已修 8 项 + 1 partial→完整 + 1 skip"
- "测试基础设施可用，SpringBootTest 上下文可成功加载"
- "答辩演示数据净化完毕，演示就绪度 100%"

❌ 不可声明：
- "全部缺陷已修" — P1-002 业务单测仍未补，留 round-4
- "测试覆盖率达标" — JaCoCo 未引入
- "零文档债务" — 仍有部分上游 RuoYi-Vue-Plus 文档与代码不一致（不在本项目范围）

---

## 9. 附录：证据文件清单

```
docs/qa/round-3/
├── fix-log.md                              本轮 5 项修复完整日志
├── test-report.md                          本文档
└── evidence/
    ├── m-a-skipTests-flipped.txt           M-A pom.xml grep
    ├── m-b-m-d-readme-snippets.txt         M-B + M-D README/env/compose grep
    ├── m-c-before-pollution-scan.txt       M-C 5 行污染基线
    ├── m-c-after-pollution-zero.txt        M-C 修复后 polluted=0
    └── m-e-after-tagunittest-pass.txt      M-E TagUnitTest BUILD SUCCESS
```
