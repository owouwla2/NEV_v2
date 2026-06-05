# NEV-v2 Round-4 修复日志（Round-4 Fix Log）

> 工程收尾时间：2026-06-05
> 输入：Round-3 `test-report.md §7` 长期项 + Round-4 冻结需求/计划
> 历史治理 Run：`20260528T123835Z-863b1823`（已有 runtime 产物，但本次不声明 canonical vibe 完整成功，仅归档工程验证事实）

---

## 修复进度总表

| ID | 项 | 状态 | 验证 |
|---|---|---|---|
| **R4-A** | P1-002 业务模块单测补齐 | ✅ 完成 | 10 个测试类 / 75 tests / 0 failure |
| **R4-B** | JaCoCo 覆盖率报告 + 30% 行覆盖率基线 | ✅ 完成 | 4 个 NEV 模块均通过 `jacoco:check` |
| **R4-C** | M-3 输入层防御：拒绝 U+FFFD（UTF-8 `EFBFBD` 解码结果） | ✅ 完成 | DTO/参数注解 + 反射测试覆盖 |
| **R4-D** | O-1 HTTP 401 规范化复审 | ✅ review-only | 保留 RuoYi/Sa-Token body-code 设计，不改 HTTP status |

---

## R4-A · P1-002 业务模块单测补齐

新增业务单测覆盖 4 个 NEV 业务模块：

| 模块 | 测试类 | 用例数 |
|---|---|---:|
| `nev-battery` | `BatteryScanServiceTest`, `BatteryServiceTest` | 9 |
| `nev-blockchain` | `ContractInvokerLongOrNullTest`, `ContractAddressResolverTest`, `DataHashCalculatorTest` | 17 |
| `nev-carbon` | `CarbonCalculatorServiceTest` | 14 |
| `nev-marketplace` | `OrderServiceTest`, `ProductServiceTest`, `TradeInServiceTest`, `InputEncodingValidationTest` | 35 |

关键覆盖点：

- `BatteryScanService`：空 trace、防不存在电池、按 event type 分流 `verifyEvent` / `verifyBattery`、链上校验失败影响 `overallVerified`。
- `BatteryService`：商城 SOLD、以旧换新 RECYCLED、拆解后拒绝追加事件、链上失败不落库。
- `ContractInvoker.longOrNull`：数字、十进制字符串、hex 字符串、空/非法值。
- `ContractAddressResolver`：runtime 注册优先、properties fallback、默认 user address、配置异常。
- `DataHashCalculator`：Keccak-256 hex、null 字段序列化、zero padding。
- `CarbonCalculatorService`：GB-T 24067 五阶段、LFP/NCM 原材料公式、运输/使用/EOL 公式、因子缺失容错。
- `OrderService`：取消、支付、发货、确认收货状态机与越权/非法状态。
- `ProductService`：商家档案自动创建、越权商品拒绝、公开搜索映射、坏 JSON 图片容错。
- `TradeInService`：评估、接受、拒绝状态机，接受后触发 RECYCLED。
- `InputEncodingValidationTest`：输入编码防御注解和控制器入口触发条件。

所有测试类均带 `@Tag("dev")`，匹配父 POM 的 surefire `<groups>${profiles.active}</groups>` 默认过滤规则。

---

## R4-B · JaCoCo 覆盖率基线

`backend/pom.xml` 新增/调整：

- `jacoco-maven-plugin` `prepare-agent`：测试前注入 agent，生成 `target/jacoco.exec`。
- `jacoco-maven-plugin` `report`：`test` 阶段生成 `target/site/jacoco/` 报告。
- `jacoco-maven-plugin` `check`：`LINE COVEREDRATIO >= 0.30`。
- surefire `argLine` 改为 `@{argLine} -Dfile.encoding=UTF-8`，避免 JaCoCo agent 被覆盖。
- `failIfNoTests=false`，父/聚合模块和空测试模块不会因无测试源失败。

4 个 NEV 模块增加 `spring-boot-starter-test` test 依赖，使 JUnit 5 / Mockito / AssertJ 可用。

最终行覆盖率：

| 模块 | Line covered | Line missed | Line coverage |
|---|---:|---:|---:|
| `nev-battery` | 154 | 135 | 53.29% |
| `nev-blockchain` | 89 | 111 | 44.50% |
| `nev-carbon` | 105 | 214 | 32.92% |
| `nev-marketplace` | 289 | 429 | 40.25% |
| **target total** | **637** | **889** | **41.74%** |

---

## R4-C · 输入层编码防御

新增公共 Bean Validation 约束：

- `NoReplacementChar`
- `NoReplacementCharValidator`

防御对象是 Unicode replacement character `U+FFFD`，它是 UTF-8 字节 `EFBFBD` 的解码结果。也就是说：后端拒绝已经损坏成 `�` 的输入，避免再次写入 MySQL payload / remark / snapshot 字段。

覆盖范围：

- Battery 写入 DTO：`BatteryRegisterDTO`, `TransferInDTO`, `SellDTO`, `ReceiveDTO`。
- Marketplace 写入/查询 DTO：`AddressDTO`, `OrderCreateDTO`, `OrderQueryDTO`, `ProductQueryDTO`, `ProductSaveDTO`, `TradeInEvaluateDTO`, `TradeInSubmitDTO`。
- `ProductSaveDTO.images` 对 `List` 元素使用 type-use 约束：`List<@NoReplacementChar String>`。
- 查询入口补 `@Valid @ModelAttribute`：consumer order list、merchant order list、public product list。
- 会落库的 `reason` 参数补 `@NoReplacementChar`：consumer order cancel、consumer trade-in reject；对应控制器启用 `@Validated`。

---

## R4-D · O-1 HTTP 401 规范化复审

复审结论：**保持 round-2 决策，不改 HTTP status 行为**。

原因：

- 当前 RuoYi-Vue-Plus / Sa-Token 链路采用 HTTP 200 + body `{code,msg,data}` 的业务错误模型。
- 前端 axios 拦截器按 body `code=401/403` 做跳转和提示。
- 强行把鉴权错误改为 HTTP 401/403，会让 axios 进入非 2xx error 分支；如果没有同步重写前端拦截器和所有调用方，风险大于收益。

可声明：O-1 已完成复审并决策保留现状。

不可声明：HTTP status 已规范化为 401/403。

---

## 验证

最终命令：

```powershell
mvn -pl nev-modules/nev-battery,nev-modules/nev-blockchain,nev-modules/nev-carbon,nev-modules/nev-marketplace -am test -DskipTests=false
```

结果：

- `BUILD SUCCESS`
- `nev-blockchain`: 17 tests
- `nev-battery`: 9 tests
- `nev-carbon`: 14 tests
- `nev-marketplace`: 35 tests
- 目标模块合计：75 tests / 0 failure / 0 error / 0 skipped
- 4 个 NEV 模块均输出：`All coverage checks have been met.`

残留提示：

- Mockito inline mock maker 在 JDK 21 下输出 dynamic-agent warning；当前不影响测试结果，后续可按 Mockito 文档改为显式 javaagent。
- 401 normalization 是 review-only 决策保留，不是代码修复。
