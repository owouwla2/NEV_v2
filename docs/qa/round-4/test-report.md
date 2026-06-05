# NEV-v2 四轮测试报告（Round-4 Test Report）

> 报告生成：2026-06-05
> 范围：P1-002 业务单测、JaCoCo 30% 基线、M-3 输入层防御、O-1 401 复审

---

## 0. 执行摘要 / TL;DR

Round-4 已完成本地工程验证：

| ID | 目标 | 状态 | 证据 |
|---|---|---|---|
| R4-A | P1-002 业务模块单测补齐 | ✅ GREEN | 10 类 / 75 tests / 0 failure |
| R4-B | JaCoCo 30% 行覆盖率基线 | ✅ GREEN | 4 个 NEV 模块均通过 check |
| R4-C | 输入层拒绝 U+FFFD / `EFBFBD` 污染 | ✅ GREEN | validator + DTO/controller 反射测试 |
| R4-D | O-1 HTTP 401 规范化复审 | ✅ REVIEWED | 保留 HTTP 200 + body code 设计 |

---

## 1. 测试执行结果

最终验证命令：

```powershell
mvn -pl nev-modules/nev-battery,nev-modules/nev-blockchain,nev-modules/nev-carbon,nev-modules/nev-marketplace -am test -DskipTests=false
```

结果：

| 模块 | 测试类 | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| `nev-battery` | 2 | 9 | 0 | 0 | 0 |
| `nev-blockchain` | 3 | 17 | 0 | 0 | 0 |
| `nev-carbon` | 1 | 14 | 0 | 0 | 0 |
| `nev-marketplace` | 4 | 35 | 0 | 0 | 0 |
| **合计** | **10** | **75** | **0** | **0** | **0** |

---

## 2. 覆盖率结果

JaCoCo line coverage：

| 模块 | Line missed | Line covered | Line coverage |
|---|---:|---:|---:|
| `nev-battery` | 135 | 154 | 53.29% |
| `nev-blockchain` | 111 | 89 | 44.50% |
| `nev-carbon` | 214 | 105 | 32.92% |
| `nev-marketplace` | 429 | 289 | 40.25% |
| **target total** | **889** | **637** | **41.74%** |

所有目标模块均超过 30% 行覆盖率基线，Maven `jacoco:check` 强制通过。

---

## 3. 输入层防御结果

验证点：

- `NoReplacementCharValidator` 对 `null` 和正常中文放行，对 `损坏字符: \uFFFD` 拒绝。
- Battery 写入 DTO 和 marketplace 写入/查询 DTO 的文本字段带 `@NoReplacementChar`。
- `ProductSaveDTO.images` 的每个 URL 元素带 type-use 约束。
- 查询 DTO 参数使用 `@Valid @ModelAttribute`，保证 query binding 后触发校验。
- 会落库的 `reason` 请求参数使用 `@NoReplacementChar`，并通过 `@Validated` 控制器启用方法参数校验。

本轮防御的是已解码成 `U+FFFD` 的损坏输入；原始字节在进入 Spring MVC 之前已完成解码，应用层能稳定识别的是 replacement character 本身。

---

## 4. O-1 401 复审

结论：保留当前 RuoYi-Vue-Plus 风格的 HTTP 200 + body `code=401/403` 业务错误模型。

状态定义：

- ✅ 已完成：复审并记录决策。
- ⏸ 不执行：HTTP status normalization 代码改动。

风险说明：第三方网关或非前端调用方若强依赖 HTTP status 401/403，仍需在网关/适配层单独处理；当前项目内前端 axios 链路不建议改。

---

## 5. 证据文件

| 文件 | 用途 |
|---|---|
| `evidence/r4-maven-verification.txt` | Maven 命令、测试总数、JaCoCo check 结果 |
| `evidence/r4-coverage-summary.txt` | 最终 JaCoCo line coverage 汇总 |
| `evidence/r4-input-defense-review.txt` | 输入层防御覆盖面 |
| `evidence/r4-o1-401-review.txt` | O-1 复审结论 |

---

## 6. 完成措辞

✅ 可声明：

- Round-4 已补齐目标 NEV 业务模块单测，目标模块 75 tests 全绿。
- JaCoCo 已接入，4 个 NEV 模块均强制通过 30% line coverage baseline。
- M-3 已加入输入层防御，拒绝 `U+FFFD` / `EFBFBD` 解码污染再次进入关键 DTO/参数。
- O-1 已复审并明确保留现有 body-code 鉴权错误模型。

❌ 不可声明：

- HTTP 鉴权错误的实际 HTTP status 已改为 401/403。
- 历史 vibe run 已完整治理成功；本报告只证明本地工程验证通过。
