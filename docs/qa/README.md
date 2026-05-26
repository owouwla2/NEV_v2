# NEV-v2 测试质量保障（QA）全景索引

> 本目录归档项目所有测试质量保障产物。按"轮次（round）"组织：每轮独立目录，自带 fix-log + test-report + evidence 三件套。
>
> **当前状态**：✅ Round-3 收尾完毕，**演示就绪度 100%**，下一步进入 Round-4（毕设阶段长期项）。

---

## 0. 一句话进度

| 轮次 | 日期 | commit | 关键产物 | 状态 |
|---|---|---|---|---|
| First-round | 2026-05-24 | (在 docs 提交链中) | 45 用例 / 9 缺陷分级 | ✅ 完成 |
| Round-2 | 2026-05-25 | `97e5be4` | 6 项关键修复 + 1 项决策跳过 | ✅ 完成 |
| Round-3 | 2026-05-26 | `891b645` | 5 项收尾修复（5/5 GREEN） | ✅ 完成 |
| Round-4 | 待启动 | — | 业务单测补齐 + JaCoCo + 输入层防御 | ⏸ 长期项 |

---

## 1. First-round（首轮全量测试）

**目录**：[`first-round/`](first-round/)

| 文件 | 用途 |
|---|---|
| [test-plan.md](first-round/test-plan.md) | 测试计划（范围、用例策略） |
| [test-matrix.md](first-round/test-matrix.md) | 45 个测试用例矩阵 |
| [test-report.md](first-round/test-report.md) | 执行结果（pass% 68.9%） |
| [defects.md](first-round/defects.md) | 9 个缺陷分级（3 P1 + 3 P2 + 3 P3） |
| [risk-assessment.md](first-round/risk-assessment.md) | 风险评估（§3.1 必修项） |
| [delivery-acceptance-report.md](first-round/delivery-acceptance-report.md) | 首轮交付验收报告 |

**关键发现**：

| ID | 等级 | 缺陷 | 后续修复 |
|---|---|---|---|
| P1-001 | P1 | 链上 verifyEvent 全失败（overallVerified=false） | round-2 M-1 |
| P1-002 | P1 | 业务模块单元测试源缺失 | ⏸ round-4 |
| P1-003 | P1 | backend/pom.xml `<skipTests>true</skipTests>` | round-3 M-A |
| P2-001 | P2 | 测试 SnailJob 启动失败 | round-2 O-4 partial → round-3 M-E |
| P2-002 | P2 | `nev_battery_lifecycle.block_number` 全 NULL | round-2 O-2 |
| P2-003 | P2 | BAT-DEMO-001 中文乱码（U+FFFD） | round-2 M-3 → round-3 M-C |
| P3-001 | P3 | HTTP 401 规范化未做 | ⏸ round-2 决策跳过 |
| P3-002 | P3 | README/HANDOFF 文档术语不一致 | round-3 M-D |
| P3-003 | P3 | SnailJob/SBA 日志噪声 | round-2 M-2 |

---

## 2. Round-2（关键缺陷修复轮）

**目录**：[`round-2/`](round-2/)
**治理 Run**：`20260524T115714Z-9fddb02e`
**基线**：HEAD `15ae238`（wave-4-end-7-g15ae238）

| 文件 | 用途 |
|---|---|
| [fix-log.md](round-2/fix-log.md) | 6 项修复完整日志（含真根因分析） |
| [test-report.md](round-2/test-report.md) | 二轮回归测试报告 |
| [evidence/](round-2/evidence/) | 修复前后证据（scan API JSON / SQL 查询输出 / 日志 grep） |

**修复矩阵**：

| ID | 缺陷 | 状态 | 验证 |
|---|---|---|---|
| M-1 | P1-001 链上 verify 全失败 | ✅ | `overallVerified=true`，4/4 chainVerified |
| M-2 | P3-003 日志噪声 | ✅ | SnailJob ERROR 0 条 / 10min |
| M-3 | P2-003 中文乱码 | ✅ | `cellSupplier=「宁德时代」` 字节验证 |
| O-2 | P2-002 block_number 回填 | ✅ | 22/22 历史全回填 |
| O-4 | P2-001 测试基设 | ⚠️ partial | SnailJob 阻断已解，placeholder 留 round-3 |
| O-1 | P3-001 HTTP 401 规范化 | ⏸ skipped | 决策：避免破坏前端 axios 拦截器 |
| B-1 | （新发现）Hyper-V 6379 端口陷阱 | ✅ | Redis 切 16379 |

**关键指标对比**：

| 指标 | First Round | Round-2 |
|---|---|---|
| `overallVerified` | false | **true** |
| `verifiedEvents` | 0/4 | **4/4** |
| `block_number` 回写率 | 0/22 (0%) | **22/22 (100%)** |
| SnailJob ERROR / 10min | ~60 条 | **0 条** |
| 答辩演示阻断项 | 3 项 P1 | **0 项** |

---

## 3. Round-3（收尾轮 · 当前终板）

**目录**：[`round-3/`](round-3/)
**治理 Run**：`20260526T010924Z-3dc8e66a`
**基线**：HEAD `f38101d`（round-2 终板）

| 文件 | 用途 |
|---|---|
| [fix-log.md](round-3/fix-log.md) | 5 项收尾修复完整日志 |
| [test-report.md](round-3/test-report.md) | 三轮收尾报告（四真自检） |
| [evidence/](round-3/evidence/) | 修复前后 grep / mvn surefire 证据 |

**修复矩阵**：

| ID | 缺陷 | 状态 | 验证 |
|---|---|---|---|
| M-A | P1-003 翻转 skipTests | ✅ | `mvn test` 不再 skip |
| M-B | B-1 长期防御文档 | ✅ | README + .env.example + docker-compose.yml 三处同步 16379 + Hyper-V 警告 |
| M-C | M-3 同类污染清理 | ✅ | 5 行污染数据 UPDATE，`polluted_remaining=0` |
| M-D | P3-002 文档术语对齐 | ✅ | `nev_` 前缀 + 路径速查表 + verify 描述对齐 |
| M-E | O-4 完整测试基础设施 | ✅ | TagUnitTest 1/1 pass（Spring Context 成功加载） |

**关键指标演进**（first-round → round-2 → round-3）：

| 指标 | First Round | Round-2 | Round-3 |
|---|---|---|---|
| `<skipTests>` 默认 | true | true | **false** |
| Demo 数据中文乱码 | 多处 | 5 行残留 | **0** |
| README 表名一致性 | ❌ 简写不一致 | ❌ 仍简写 | **✅ nev_ 前缀** |
| 前端 SPA / 后端 API 区分 | ❌ 文档未分 | ❌ 仍未分 | **✅ 路径速查表** |
| Hyper-V 6379 警告 | ❌ 无 | ⚠️ 仅 fix-log | **✅ README + .env + compose** |
| Spring Context 测试加载 | ❌ SnailJob 拖垮 | ⚠️ partial | **✅ TagUnitTest pass** |
| 答辩演示阻断项 | 3 项 P1 | 0 | **0** |

**演示就绪度**：**100% READY**。

---

## 4. Round-4（长期项 · 毕设阶段）

**未启动**。不阻断答辩，建议毕设阶段统一推进。

### 4.1 长期工作

| 优先级 | 项 | 估时 | 来源 |
|---|---|---|---|
| 高 | P1-002 业务模块单测补齐 | 多日 | first-round 缺陷 |
|   | - BatteryScanService（M-1 修复后逻辑） | 0.5d | |
|   | - ContractInvoker.longOrNull（O-2 修复后逻辑） | 0.5d | |
|   | - CarbonCalcService（GB-T 24067 5 阶段） | 1d | |
|   | - OrderService / TradeInService（商城 + 以旧换新） | 1d | |
|   | - 给业务测试加 `@Tag("dev")` 让 `mvn test` 默认能跑 | 0.5d | round-3 已知限制 |
| 中 | 引入 JaCoCo 覆盖率报告 + 基线阈值 30% | 0.5d | round-3 建议 |
| 中 | M-3 输入层防御：API 入参拒绝 `EFBFBD` 字节字符串 | 0.5d | round-3 建议 |
| 低 | O-1（P3-001）重新评估：HTTP 401 规范化是否值得做 | 1h（评估） | round-2 决策待复审 |

### 4.2 答辩前可选

- 触发一次 producer1 register 新电池验证 O-2 future write blockNumber 实时回写（会污染 demo 数据，需准备清理脚本）
- 答辩前 3 项前端手动抽检（首轮挂起，已不强制）

---

## 5. 治理凭证（vibe 三轮 session）

| 轮次 | vibe session | 入口 |
|---|---|---|
| First-round | `20260524T103530Z-*`（首轮治理） | `docs/plans/2026-05-24-*-execution-plan.md` |
| Round-2 | `20260524T115714Z-9fddb02e` | `docs/qa/round-2/fix-log.md` 头部 |
| Round-3 | `20260526T010924Z-3dc8e66a` | `docs/plans/2026-05-26-*-execution-plan.md` |

所有 vibe session 产物归档在 `outputs/runtime/vibe-sessions/{session_id}/`。

---

## 6. 合规可声明 vs 不可声明

✅ **可声明**：

- "首轮 9 项缺陷 + round-2 新发现 1 项 = 共 10 项缺陷，已修 8 项 + 1 partial→完整 + 1 决策跳过"
- "Round-3 5 项修复全部 fix-then-verified（M-A/M-B/M-C/M-D/M-E）"
- "测试基础设施可用，SpringBootTest 上下文可成功加载"
- "答辩演示数据净化完毕，演示就绪度 100%"

❌ **不可声明**：

- "全部缺陷已修" — P1-002 业务单测仍未补，留 round-4
- "测试覆盖率达标" — JaCoCo 未引入
- "零文档债务" — 仍有部分上游 RuoYi-Vue-Plus 文档与代码不一致（不在本项目范围）

---

## 7. 与项目主索引的关联

- 项目顶部状态：[README.md](../../README.md) "状态" / "QA" / "开发路线" / "答辩前 TODO" 四处
- 交接接续点：[HANDOFF.md §7](../../HANDOFF.md) "接下来要做（QA Round-4 起）"
- Round-2 关键文档锚点：[README.md L242](../../README.md) Hyper-V 警告块引用 `round-2/fix-log.md §B-1`
- Round-3 关键文档锚点：[README.md L115](../../README.md) "路径速查（NEV-v2 fix P3-002 / round-3 M-D）"
