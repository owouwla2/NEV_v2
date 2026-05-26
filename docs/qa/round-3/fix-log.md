# NEV-v2 三轮修复日志（Round-3 Fix Log）

> 治理 Run：`20260526T010924Z-3dc8e66a`（round-3 governance shell）
> 修复执行时间：2026-05-26
> 基线：HEAD `f38101d`（round-2 终板）
> 输入：[docs/qa/round-2/fix-log.md](../round-2/fix-log.md) Continuation Notes "下次起点" + first-round defects.md 剩余项

---

## 修复进度总表

| ID | 缺陷 | 严重度 | 修复状态 | 验证状态 |
|---|---|---|---|---|
| **M-A** | P1-003 backend/pom.xml skipTests=true | P1 | ✅ 翻转为 false | ✅ mvn test 不再跳过 |
| **M-B** | B-1 长期防御文档（Hyper-V 端口） | P1（开发环境） | ✅ README + .env.example + docker-compose.yml 同步 16379 + 警告 | ✅ grep 验证 |
| **M-C** | M-3 同类污染清理（BAT-DEMO-002/003 + 18 处 remark） | P2 | ✅ 5 行污染数据全部 UPDATE | ✅ polluted_remaining = 0 |
| **M-D** | P3-002 README/HANDOFF 文档术语对齐 | P3 | ✅ 加 nev_ 前缀 + 路径速查表 | ✅ grep 验证 |
| **M-E** | O-4 完整完成测试基础设施 | P2 | ✅ application.yml→application-test.yml + @ActiveProfiles("test") | ✅ TagUnitTest 1/1 pass |

**out-of-scope round-3**: P1-002（业务模块单元测试源补齐，留 round-4 多日工作）

---

## M-A · P1-003 翻转 skipTests

### 修改

文件：`backend/pom.xml:60`

```diff
- <skipTests>true</skipTests>
+ <skipTests>false</skipTests>
```

### 验证

`mvn test` 现在默认执行测试（之前是 `Tests are skipped`）。需要跳过时显式 `-DskipTests=true`。

证据：`evidence/m-a-skipTests-flipped.txt`

---

## M-B · B-1 长期防御文档

### 修改 1：`README.md` 端口规划表

- Redis 端口从 6379 改为 **16379**
- 端口表下方新增 "⚠️ Windows Hyper-V 端口保留陷阱" 警告块
- 含排查命令 `netsh int ipv4 show excludedportrange protocol=tcp` + fix-log 引用

### 修改 2：`deploy/docker/.env.example`

```diff
-# 默认 6379 与老仓 Redis 端口相同；如老仓 Redis 已在跑，把端口改为 6479（或先关老仓）
-REDIS_PORT=6379
+# NEV-v2 fix B-1 (round-3 M-B): 默认 6379 在 Windows 10/11 + Hyper-V/WSL2 启用时经常被
+# 系统保留（动态保留区如 6305-7404），导致 docker-compose up 报 bind: forbidden by access permissions
+# 推荐使用 16379 避开整个保留范围
+REDIS_PORT=16379
```

### 修改 3：`deploy/docker/docker-compose.yml`

- header 注释更新：明确 16379 是 NEV-v2 fix B-1 + 排查命令 + 旧 6479 也不可用警告
- `ports: ["${REDIS_PORT:-16379}:6379"]`（默认值同步）

### 验证

`evidence/m-b-m-d-readme-snippets.txt` 含所有 16379 + Hyper-V + 路径速查 grep 结果。

---

## M-C · M-3 同类污染清理

### 修复

通过 `INSTR(HEX(payload), 'EFBFBD') > 0` 全扫，定位到 5 行损坏数据，按推测的原始中文 UPDATE：

| trace_number | event_type | version | 字段 | 旧值（含 U+FFFD） | 新值 |
|---|---|---|---|---|---|
| BAT-DEMO-001 | IN_USE | 2 | remark | `??? -> ???` | `生产端 -> 分销端` |
| BAT-DEMO-001 | SOLD | 3 | remark | `???订单` | `线上订单` |
| BAT-DEMO-001 | RECYCLED | 4 | remark | `3 ?使用后回收` | `3 年使用后回收` |
| BAT-DEMO-002 | PRODUCED | 1 | cellSupplier | `???时代` | `宁德时代` |
| BAT-DEMO-003 | PRODUCED | 1 | cellSupplier | `????` | `比亚迪` |

### 验证

```
polluted_remaining = 0
```

证据：`evidence/m-c-before-pollution-scan.txt` + `evidence/m-c-after-pollution-zero.txt`

---

## M-D · P3-002 README/HANDOFF 文档术语对齐

### 修改 1：README.md 表名简写补齐

- `lifecycle_event` → `nev_battery_lifecycle`
- `battery` → `nev_battery`
- 业务表清单全部统一加 `nev_` 前缀

### 修改 2：新增 "路径速查" 段落（M-D 锚点）

`## 后端模块` 之前插入 11 行 markdown 表格，明确区分：

- **前端 SPA 路由**：`/scan/:trace`（浏览器 / Vue Router）
- **后端 API**：`/public/scan/{traceNumber}`、`/producer/battery/register` 等（curl 直调）
- 列出所有 28 个业务 API endpoint 按角色分组

### 修改 3：M-1 校验机制描述更新

原描述 "(MySQL count == 链上 count) && (每条 verifyEvent == true)" 更新为：

```
overallVerified = (MySQL count - PRODUCED == 链上 count) && (每条 verifyEvent/verifyBattery == true)
```

反映 round-2 M-1 修复后真实业务事实。

### 验证

HANDOFF.md grep 显示已经在用 `nev_battery_lifecycle` 等正确名称，无需大改。

---

## M-E · O-4 完整完成测试基础设施

### 设计变更

放弃 round-2 的 `src/test/resources/application.yml`（完全覆盖 main 配置导致 placeholder 失败），改用：

1. 删除旧 `application.yml`，新建 `application-test.yml`
2. 4 个 demo 测试加 `@ActiveProfiles("test")` 注解
3. 清理 stale `target/test-classes/application.yml`

### application-test.yml 必须自包含的字段

- **SnailJob 全套**（即使 enabled=false，placeholder 仍要解析）：group/token/server.host/server.port/namespace/port/host
- **DataSource**：dynamic + master URL + 用户密码（因为测试不会自动加载 application-dev.yml）
- **Redis**：host/port=16379/database=1/password
- **SBA Client**：enabled=false

### 4 个测试类改动

```diff
+import org.springframework.test.context.ActiveProfiles;
+@ActiveProfiles("test")
 @SpringBootTest
 public class TagUnitTest { ... }
```

⚠️ 副作用修复：Python 脚本误替换了 `DemoUnitTest.java` line 24 字符串 literal 中的 @SpringBootTest，已手动 Edit 还原。

### 验证

```
mvn -pl ruoyi-admin test -Dtest='TagUnitTest'
→ Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
→ BUILD SUCCESS
```

证据：`evidence/m-e-after-tagunittest-pass.txt`

### 已知限制（不属 M-E 范畴）

RuoYi 上游 `maven-surefire-plugin` 配置：

```xml
<groups>${profiles.active}</groups>
<excludedGroups>exclude</excludedGroups>
```

按 JUnit5 `@Tag` 过滤，只跑当前 profile 匹配的方法（如 `dev` profile 只跑 `@Tag("dev")`）。AssertUnitTest/DemoUnitTest/ParamUnitTest 的方法没 @Tag → 默认 dev profile 下不跑（不是 M-E 引入的回归，属 RuoYi 设计）。

P1-002（业务模块单元测试源补齐）的测试需要补 @Tag("dev") 才会被默认 mvn test 跑。

---

## Round-3 治理凭证

- vibe round-3 governance shell run: `20260526T010924Z-3dc8e66a`
- 实际修复工作由 host 在 governed scope 内执行并归档至此文档
- 任何后续修复后的 commit 应在 message 中引用对应 P# / M# / O# / B# ID

---

## 今日完成总结

| 指标 | Round-2 终板 | Round-3 终板 |
|---|---|---|
| 已修缺陷 | 6 项 (M-1/M-2/M-3/O-2/O-4 partial/B-1) | + M-A/M-B/M-C/M-D/M-E (共 11 项) |
| `<skipTests>` 默认 | true | **false** |
| 业务表名文档与 DB 一致 | ❌ 简写易混淆 | ✅ 统一 nev_ 前缀 |
| 前端 SPA / 后端 API 路径区分 | ❌ 文档未明 | ✅ 路径速查表 |
| Hyper-V 6379 警告 | ⚠️ 仅 fix-log | ✅ README + .env.example + docker-compose 三处同步 |
| Demo data 中文乱码 | 5 行污染 | **0 行** |
| Demo 单测 SpringBoot Context | ❌ 加载失败 | ✅ TagUnitTest pass |

### 待 round-4 (长期项)

- P1-002 业务模块单元测试源补齐（BatteryScanService / CarbonCalcService / OrderService / TradeInService 等）
- 给现有 4 个 demo 测试或新业务测试加 `@Tag("dev")` 让 mvn test 默认能跑到

### 演示就绪度

**100% READY**（M-1/M-2/M-3 三项必修 round-2 已清零 + round-3 demo 数据净化）。建议立即彩排。
