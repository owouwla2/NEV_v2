# NEV-v2 首轮测试缺陷清单（Defects）

> 治理 Run：`20260524T100632Z-4c549a73` → `20260524T105506Z-73a6759a`
> 基线 HEAD：`15ae238` (wave-4-end-7-g15ae238) / 2026-05-24
> 严重度定义：P0=阻断；P1=核心功能受损；P2=业务可用但偏离设计；P3=不影响功能的规范/文档/噪声

---

## 严重度汇总

| 严重度 | 数量 | 编号 |
|---|---:|---|
| P0 | 0 | — |
| P1 | 3 | P1-001 / P1-002 / P1-003 |
| P2 | 3 | P2-001 / P2-002 / P2-003 |
| P3 | 3 | P3-001 / P3-002 / P3-003 |
| **合计** | **9** | |

---

## P1-001 · 链上 verifyEvent 永远失败，overallVerified=false

- **严重度**：P1（核心宣传卖点失效）
- **触发用例**：FR-CHN-02 / FR-SCN-02
- **现象**：
  - 公开扫码 `/public/scan/BAT-DEMO-001` 返回 `overallVerified=false`、`verifiedEvents=0`、`totalEvents=4`、`chainEventCount=3`
  - 4 条事件全部 `chainVerified=false`
- **根因（已定位）**：
  - 直接调 `POST http://localhost:5002/WeBASE-Front/trans/handle` 用 `getEventCount(BAT-DEMO-001)` 返回：
    ```json
    {"code":201151,"data":null,"errorMessage":"Invalid method getEventCount , supported functions are: []"}
    ```
  - **WeBASE-Front 端 `LifecycleTrace` 合约的 ABI 未注册**（`supported functions are: []` — 空函数表）
  - 即使数据库 `nev_contract_config` 表里有完整 ABI，但 WeBASE 自身缓存里没有
- **影响范围**：
  - 所有 `/public/scan/{traceNumber}` 反验徽章永远不亮
  - 所有 verifyEvent 校验链下数据完整性的途径失效
  - 业务"链上链下双写校验"承诺无法兑现
- **复现步骤**：
  1. `curl http://localhost:9280/public/scan/BAT-DEMO-001 | jq .data.overallVerified` → false
  2. `curl -X POST -H 'Content-Type: application/json' -d '{"groupId":1,"contractAddress":"0xf71701365b8b35d4a03a12ecc51edf5fd5797b08","contractName":"LifecycleTrace","funcName":"getEventCount","funcParam":["BAT-DEMO-001"],"user":"0x6933f6d76d71b7ca66f70f3faf6b108a10697aa2"}' http://localhost:5002/WeBASE-Front/trans/handle`
- **建议修复（留二轮）**：
  - 通过 WeBASE-Web (5000) 重新导入 `LifecycleTrace` ABI（菜单：合约 → 合约 IDE / 已部署合约 → 重新导入 ABI），或
  - 编写自动化脚本在后端启动时调 WeBASE 注册 ABI 接口，把 `nev_contract_config.abi` 推送给 WeBASE
- **证据**：`evidence/api-scan-BAT-DEMO-001.json` + `evidence/webase-getEventCount-fail.json`

---

## P1-002 · 业务模块单元测试源码完全缺失

- **严重度**：P1（无测试基线，回归无保障）
- **触发用例**：FR-UNI-01
- **现象**：
  - `find backend -path '*/src/test/java/*.java'` 仅 4 个文件，全部位于 `backend/ruoyi-admin/src/test/java/com/nev/test/`（RuoYi 自带 demo 测试，与 NEV 业务无关）
  - `backend/nev-modules/{nev-blockchain,nev-battery,nev-carbon,nev-marketplace}` 5 个业务模块 **零** 测试源
- **根因**：开发期未补单元测试（Wave 1-4 计划聚焦交付，测试未纳入 DoD）
- **影响范围**：
  - 后端 38 模块整体覆盖率近 0%
  - JaCoCo 未配置，覆盖率快照无法自动生成
  - 任何后续修改无回归保障
- **复现步骤**：
  ```
  cd backend/nev-modules && mvn test -am   # 输出：Tests are skipped 全部模块
  ```
- **建议修复（留二轮起步）**：
  - 优先补：BatteryScanService、CarbonCalcService、OrderService、TradeInService 的单元测试
  - pom.xml 引入 jacoco-maven-plugin 并设置覆盖率阈值（建议起步 30%，目标 60%）
- **证据**：mvn test stdout（inline 已记录）

---

## P1-003 · `backend/pom.xml` 默认 `<skipTests>true</skipTests>`

- **严重度**：P1（即使有测试也不会跑）
- **触发用例**：FR-UNI-02
- **现象**：`backend/pom.xml:60` 行：`<skipTests>true</skipTests>`
- **影响**：与 P1-002 叠加 — 即使将来补了测试，CI/本地 `mvn test` 默认也不会运行；必须显式 `-DskipTests=false`
- **建议修复（留二轮）**：
  - 改为 `<skipTests>false</skipTests>`，并在 CI 中保留默认配置
  - 若希望本地开发跳过，建议用 Maven profile（`<profile id="fast"><skipTests>true</skipTests></profile>`）而不是全局默认
- **证据**：grep 输出 inline

---

## P2-001 · 强制运行单元测试时 SnailJob client 启动失败拖垮 Spring Context

- **严重度**：P2（测试环境不可用）
- **触发用例**：FR-UNI-03
- **现象**：
  - `mvn -pl ruoyi-admin test -DskipTests=false` 触发 4 个 demo 测试
  - 全部 ERROR，根因栈顶：`com.aizuda.snailjob.client.common.exception.SnailJobClientException: snail-job server start error`
  - `Failed to load ApplicationContext` 导致后续测试全部 ERROR
- **根因**：
  - 测试启动 `NevApplication` 完整上下文
  - `SnailJobStartListener` 自动 run，试图连接 SnailJob server，未禁用且没有 mock
  - SnailJob server 在本地未启动 → 抛 `snail-job server start error` → Spring Context 启动失败
- **影响**：
  - 即使在 P1-003 修复（开启 `<skipTests>false</skipTests>`）后，所有 SpringBootTest 仍会因 SnailJob 问题失败
  - sys-error.log 已经显示 SnailJob client 每 10 秒一次 grpc heartbeat 失败（合计 200+ ERROR 行）— 测试环境同样会被波及
- **建议修复（留二轮）**：
  - test profile 中配置 `snail-job.enabled=false` 或在 `application-test.yml` 关闭 SnailJob client autoConfig
  - 或为测试基类添加 `@TestPropertySource` 关闭 SnailJob client
- **证据**：`evidence/surefire-tag-test-failure.txt`

---

## P2-002 · `nev_battery_lifecycle.block_number` 全部为 NULL

- **严重度**：P2（区块号未回写，链上证据不完整）
- **触发用例**：FR-CHN-03
- **现象**：
  - `SELECT COUNT(*), COUNT(block_number) FROM nev_battery_lifecycle` → 22 / 0（22 条事件，0 个 block_number 回写）
  - 但 `tx_hash` 全部非空（22/22），说明 `addEvent()` 链上交易确实发出且拿到了 tx_hash
- **影响**：
  - 链上时间线无法按区块号排序 / 链上回放
  - 公开扫码页响应中 `blockNumber:null`，前端无法显示区块号
- **建议修复（留二轮）**：
  - `ContractInvoker` / `BlockchainService` 在发起 `addEvent` 后从 receipt 提取 `blockNumber` 并更新 lifecycle 记录
  - 或者新增定时任务回填历史 tx_hash 对应的 block_number
- **证据**：MySQL stdout inline

---

## P2-003 · 部分 API JSON 响应中文 GBK 乱码

- **严重度**：P2（影响公开扫码 / 商品列表的展示）
- **触发用例**：FR-ENC-02 / FR-ENC-03
- **现象**：
  - `/public/scan/BAT-DEMO-001` 响应中 PRODUCED 事件 payload 字段：`"cellSupplier":"����ʱ��"`（应为"宁德时代"或类似中文厂商名）
  - `/public/product/list` 响应中部分 title：`"NEV ���ܳ��׮ 7kW ���ð�"`（应为"NEV 智能充电桩 7kW 标准版"）
- **可能根因**（待二轮定位）：
  - 数据库写入时编码错误（producer 角色注册电池时浏览器/curl 客户端传入了 GBK 字节流）
  - 或 RuoYi/MyBatis 的 JSON 序列化在某些字段的回路上未正确处理 UTF-8
  - 不是全局乱码（carbon 接口的"锂"、"铜"等汉字正常）→ 写入路径问题更可能
- **影响**：
  - 公开扫码页 / 商城页用户体验差
  - 答辩演示可能出现"科技与狠活"现场翻车
- **建议修复（留二轮）**：
  - 先用 `SELECT payload->>'$.cellSupplier' FROM nev_battery_lifecycle` 直接看数据库存的字节是否已乱码
  - 若 DB 已乱码，定位 PRODUCED 事件录入路径（curl/PowerShell GBK），添加 controller 层编码强制 UTF-8
  - 若 DB 正常但 API 乱码，检查 Jackson + Spring MVC 默认 charset
- **证据**：`evidence/api-scan-BAT-DEMO-001.json` + `evidence/api-product-list.json`

---

## P3-001 · 鉴权负向响应 HTTP 状态码不规范

- **严重度**：P3（违反 REST 约定，不影响功能）
- **触发用例**：FR-API-03 / FR-API-04 / FR-API-05 / FR-SEC-05
- **现象**：
  - 缺 `clientid` 调登录 → `HTTP 200` + 业务 `code:500` + msg "认证客户端id不能为空"
  - 缺 `Authorization` → `HTTP 200` + 业务 `code:401`
  - 错误 token → `HTTP 200` + 业务 `code:401`
- **建议修复（留二轮）**：
  - RuoYi-Vue-Plus 全局异常处理器统一把鉴权类错误映射到 HTTP 401 / 403
  - 业务 code 与 HTTP code 保持一致（401/403/500 等）
- **证据**：inline curl 输出

---

## P3-002 · 文档与实际表名 / 路径命名不一致

- **严重度**：P3（新人/测试人员易困惑）
- **触发**：观察归纳，不属单一用例
- **现象**：
  - README/HANDOFF 多处简写表名为 `battery` / `lifecycle_event`，实际数据库表 `nev_battery` / `nev_battery_lifecycle`
  - README "公开扫码 `/scan/:trace`" 其实是**前端 SPA 路由**，后端 API 实际是 `/public/scan/{traceNumber}` — 文档未明确区分
- **建议修复（留二轮）**：在 README/HANDOFF 表清单中统一带 `nev_` 前缀；前端路由与后端 API 用不同小节呈现
- **证据**：README.md inline + `evidence/mysql-tables.txt`

---

## P3-003 · SnailJob client 每 10 秒一次 grpc heartbeat 错误污染日志

- **严重度**：P3（已知噪声，README 已声明可忽略，但日志体积大）
- **触发**：sys-error.log 长期 tail 观察
- **现象**：`backend/logs/sys-error.log` 平均 ~10 秒新增 1 行 `ClientRegister - heartbeat check requestId:[N] message:[UNAVAILABLE: io exception]`
- **影响**：
  - 日志快速膨胀（当前 sys-error.log 已 367KB）
  - 当真有业务 ERROR 时，被 SnailJob 噪声淹没
- **建议修复（留二轮）**：按 README 已经写好的方案，在 `logback-plus.xml` 添加：
  ```xml
  <logger name="com.aizuda.snailjob" level="OFF"/>
  <logger name="de.codecentric.boot.admin" level="OFF"/>
  ```
  并直接执行（README 写了方案但没改 logback-plus.xml）
- **证据**：`evidence/sys-error-tail.log`

---

## 修复优先级建议（仅供二轮排期参考）

| 优先级 | 缺陷 | 工时估算 |
|---|---|---:|
| 即刻 | P1-001（WeBASE ABI 重新注册） | 0.5 h（WeBASE-Web 操作） |
| 即刻 | P3-003（logback OFF SnailJob 噪声） | 5 min |
| 短期 | P3-001（HTTP 401 规范化） | 1 h |
| 短期 | P2-002（block_number 回写） | 3 h |
| 短期 | P2-003（中文乱码定位） | 3 h（先观察 DB 字节） |
| 中期 | P2-001（test profile 禁用 SnailJob） | 1 h |
| 中期 | P1-003（pom skipTests 翻转） | 5 min |
| 长期 | P1-002（业务单元测试补齐） | 多日 |
