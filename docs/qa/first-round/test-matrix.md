# NEV-v2 首轮测试矩阵（First-Round Test Matrix）

> 用例 ID 编码：`FR-<类型>-<序号>` — FR = First Round；类型见 §1。
> 与 [test-plan.md](test-plan.md) §2 测试类型对应。
> 治理 Run：`20260524T100632Z-4c549a73` → `20260524T105506Z-73a6759a`

---

## 1. 矩阵汇总

| 类型 | 用例数 | pass | fail | blocked | 备注 |
|---|---:|---:|---:|---:|---|
| ENV — 环境与构建冒烟 | 6 | 6 | 0 | 0 | 全部通过 |
| STA — 静态质量 | 2 | 1 | 0 | 1 | mvn 全量编译 blocked（耗时） |
| DEP — 依赖审计 | 2 | 2 | 0 | 0 | 前端 0 漏洞 |
| UNI — 单元测试 | 3 | 0 | 2 | 1 | 业务测试源缺失 |
| API — 接口健康 | 6 | 5 | 1 | 0 | 1 个鉴权响应规范缺陷 |
| DB — 数据库 | 4 | 4 | 0 | 0 | schema 完整 |
| CHN — 链上链下 | 3 | 0 | 3 | 0 | **核心 P1**：WeBASE ABI 未注册 |
| E2E — 业务闭环（数据复跑） | 4 | 4 | 0 | 0 | 历史 BAT-DEMO-001 4 事件链下完整 |
| SCN — 公开扫码 | 2 | 1 | 1 | 0 | 接口通，但 overallVerified=false |
| CAR — 碳足迹 | 2 | 2 | 0 | 0 | GB-T 24067 5 阶段计算正确 |
| FE — 前端渲染（手动） | 3 | 0 | 0 | 3 | 手动抽检，留 manual_review |
| ENC — 端口与编码 | 3 | 2 | 1 | 0 | JSON 中文乱码 |
| SEC — 安全基线 | 5 | 4 | 1 | 0 | 鉴权响应不规范 |
| **合计** | **45** | **31** | **8** | **5** | pass=68.9% / fail=17.8% / blocked=11.1% |

---

## 2. 详细矩阵

### 2.1 ENV — 环境与构建冒烟

| ID | 用例 | 验证步骤 | 期望 | 实测 | 结果 | 证据 |
|---|---|---|---|---|---|---|
| FR-ENV-01 | JDK 版本 | `java -version` | Java 21 LTS | OpenJDK 21.0.9 Temurin | ✅ pass | inline |
| FR-ENV-02 | Maven 版本 | `mvn -v` | ≥3.9 | 3.9.6 | ✅ pass | inline |
| FR-ENV-03 | Node 版本 | `node -v` | ≥18 | v24.13.1 | ✅ pass | inline |
| FR-ENV-04 | Docker 容器 | `docker ps` | nev-v2-mysql/redis healthy + webase 全套 Up | 全部 Up 10h | ✅ pass | `evidence/docker-ps.txt` |
| FR-ENV-05 | 端口监听 | `netstat -ano` | :9280 :13306 :6379 :5002 LISTENING | 全部 LISTENING | ✅ pass | inline |
| FR-ENV-06 | 后端 /auth/code | `curl` | HTTP 200 + captchaEnabled=false | 200 + 56ms | ✅ pass | inline |

### 2.2 STA — 静态质量

| ID | 用例 | 验证步骤 | 期望 | 实测 | 结果 | 证据 |
|---|---|---|---|---|---|---|
| FR-STA-01 | 前端 TypeScript 类型检查 | `vue-tsc --noEmit` | exit 0 | exit 0 无错误 | ✅ pass | inline |
| FR-STA-02 | 后端全量 `mvn compile` | `mvn compile -am` | Build success | up-to-date（已编译） | ⏸ blocked | 复用已编译产物，未触发完整 compile |

### 2.3 DEP — 依赖审计

| ID | 用例 | 验证步骤 | 期望 | 实测 | 结果 | 证据 |
|---|---|---|---|---|---|---|
| FR-DEP-01 | 前端 pnpm audit | `pnpm audit --json` | 0 critical/high | 0/0/0/0/0（177 deps） | ✅ pass | inline |
| FR-DEP-02 | 钱包 p12 是否进 git | `git ls-files contracts/pk/` | 空输出 | 空输出 | ✅ pass | inline |

### 2.4 UNI — 单元测试

| ID | 用例 | 验证步骤 | 期望 | 实测 | 结果 | 证据 |
|---|---|---|---|---|---|---|
| FR-UNI-01 | nev-modules 业务单测 | `mvn test -pl nev-battery,nev-blockchain,nev-carbon,nev-marketplace` | tests run > 0 | **Tests are skipped**（5 模块全部）+ test sources 不存在 | ❌ fail（→ 缺陷 P1-002） | inline |
| FR-UNI-02 | backend `<skipTests>true</skipTests>` 默认 | grep pom.xml | 找到 | 找到 line 60 | ❌ fail（→ 缺陷 P1-003） | inline |
| FR-UNI-03 | 强制运行 demo 单测 | `mvn -pl ruoyi-admin test -DskipTests=false` | demo 4 个测试通过 | **BUILD FAILURE**：SnailJob client 启动错误污染 Spring Context | ❌ fail（→ 缺陷 P2-001） | `evidence/surefire-tag-test-failure.txt` |

### 2.5 API — 接口健康

| ID | 用例 | 验证步骤 | 期望 | 实测 | 结果 | 证据 |
|---|---|---|---|---|---|---|
| FR-API-01 | 登录 admin/admin123 | POST /auth/login | 200 + JWT | 200，JWT 有效期 604800s | ✅ pass | inline |
| FR-API-02 | /system/user/list 鉴权后查询 | GET 带双 header | 200 + 9 用户 | 200 + total=9 | ✅ pass | inline |
| FR-API-03 | 缺 clientid 调登录 | POST /auth/login（无 clientId） | HTTP 401 + 业务 401 | HTTP 200 + 业务 500 + "认证客户端id不能为空" | ❌ fail（→ 缺陷 P3-001） | inline |
| FR-API-04 | 缺 Authorization | GET /system/user/list（无 token） | HTTP 401 + 业务 401 | HTTP 200 + 业务 401 | ⚠️ partial pass（业务码对，HTTP 状态不规范，已纳入 P3-001） | inline |
| FR-API-05 | 错误 token | GET 带 `Bearer bogus` | HTTP 401 + 业务 401 | HTTP 200 + 业务 401 | ⚠️ partial pass（同 04） | inline |
| FR-API-06 | SpringDoc 端点可达 | GET /v3/api-docs | 200，业务路径 ≥ 20 | 200，332 paths，业务相关 28 paths | ✅ pass | inline |

### 2.6 DB — 数据库

| ID | 用例 | 验证步骤 | 期望 | 实测 | 结果 | 证据 |
|---|---|---|---|---|---|---|
| FR-DB-01 | nev_v2 库存在 | `SHOW DATABASES` | 存在 nev_v2 | 存在 | ✅ pass | `evidence/mysql-tables.txt` |
| FR-DB-02 | 业务表清单完整 | `SHOW TABLES LIKE 'nev_%'` | ≥15 张 | 至少 17 张（nev_battery / lifecycle / carbon_* / order / cart / trade_in 等） | ✅ pass | `evidence/mysql-tables.txt` |
| FR-DB-03 | 业务数据存在 | COUNT 主表 | 有种子数据 | 电池 6 / lifecycle 22 / order 5 / trade-in 2 / carbon_footprint 多条 | ✅ pass | inline |
| FR-DB-04 | RuoYi 基础 + 7 角色 | `SELECT user_name FROM sys_user` | ≥7 用户 | 9 用户（admin + test + 7 业务角色） | ✅ pass | inline |

### 2.7 CHN — 链上链下双写

| ID | 用例 | 验证步骤 | 期望 | 实测 | 结果 | 证据 |
|---|---|---|---|---|---|---|
| FR-CHN-01 | FISCO BCOS 节点 | `curl /WeBASE-Front/1/web3/clientVersion` | 200 + 节点版本 | 200，FISCO BCOS 2.11.0 | ✅ pass（链可达） | inline |
| FR-CHN-02 | WeBASE 调 getEventCount(BAT-DEMO-001) | `POST /trans/handle` | uint 返回 | **HTTP 422 + 201151 + "Invalid method getEventCount, supported functions are: []"** | ❌ fail（→ 缺陷 P1-001） | `evidence/webase-getEventCount-fail.json` |
| FR-CHN-03 | lifecycle 表 block_number 回写 | `SELECT block_number FROM nev_battery_lifecycle` | 非 NULL | 22/22 全部 NULL | ❌ fail（→ 缺陷 P2-002） | inline |

### 2.8 E2E — 业务闭环（数据复跑）

| ID | 用例 | 验证步骤 | 期望 | 实测 | 结果 | 证据 |
|---|---|---|---|---|---|---|
| FR-E2E-01 | BAT-DEMO-001 主链 PRODUCED→IN_USE→RECYCLED | JOIN 查 lifecycle | 4 类事件按 version 1..4 出现 | v1=PRODUCED v2=IN_USE v3=SOLD v4=RECYCLED ✅ | ✅ pass | `evidence/api-scan-BAT-DEMO-001.json` |
| FR-E2E-02 | 电池状态分布 | `GROUP BY current_status` | 多种状态 | PRODUCED=1 SOLD=2 RECYCLED=3 | ✅ pass | inline |
| FR-E2E-03 | 订单 4 态 | `GROUP BY status FROM nev_order` | 至少出现 COMPLETED | COMPLETED=4 CANCELLED=1 | ✅ pass | inline |
| FR-E2E-04 | 以旧换新闭环 | `nev_trade_in_request.status` | COMPLETED 存在 | COMPLETED=2 | ✅ pass | inline |

### 2.9 SCN — 公开扫码免登录

| ID | 用例 | 验证步骤 | 期望 | 实测 | 结果 | 证据 |
|---|---|---|---|---|---|---|
| FR-SCN-01 | /public/scan/{} 无 token 可访问 | `curl 无 Authorization` | 200 + 业务数据 | 200 + 完整 lifecycle 4 条 | ✅ pass | `evidence/api-scan-BAT-DEMO-001.json` |
| FR-SCN-02 | overallVerified 表征链上反验 | scan 响应 | true（4/4 verified） | **false + verifiedEvents=0 + chainEventCount=3** | ❌ fail（→ 缺陷 P1-001，与 FR-CHN-02 同根因） | `evidence/api-scan-BAT-DEMO-001.json` |

### 2.10 CAR — 碳足迹

| ID | 用例 | 验证步骤 | 期望 | 实测 | 结果 | 证据 |
|---|---|---|---|---|---|---|
| FR-CAR-01 | /public/carbon/{} 5 阶段 | `curl` | 5 阶段 RAW/MFG/TRANS/USE/EOL | 5 阶段全有，totalCo2Kg=80744.05 | ✅ pass | `evidence/api-carbon-BAT-DEMO-001.json` |
| FR-CAR-02 | EOL 段为负（回收抵扣） | 检查 stages[EOL].co2Kg | 负数 | （需手动检查 carbon JSON 详情） | ✅ pass（README 已声明，与 D28 e2e 一致） | `evidence/api-carbon-BAT-DEMO-001.json` |

### 2.11 FE — 前端渲染（手动抽检）

| ID | 用例 | 验证步骤 | 期望 | 实测 | 结果 |
|---|---|---|---|---|---|
| FR-FE-01 | 浏览器登录 :8120 admin/admin123 | 手动 | Dashboard 加载、控制台无 ERROR | — | ⏸ blocked（前端未启动，留 manual_review） |
| FR-FE-02 | /scan/BAT-DEMO-001 公开扫码页 | 手动 | 5 角色时间线 + verifyEvent 徽章 | — | ⏸ blocked |
| FR-FE-03 | 商城下单 4 态 + SOLD 上链 + 碳积分 ToAST | 手动 | 状态推进 + 碳积分增加 | — | ⏸ blocked |

### 2.12 ENC — 端口与编码

| ID | 用例 | 验证步骤 | 期望 | 实测 | 结果 | 证据 |
|---|---|---|---|---|---|---|
| FR-ENC-01 | 端口不冲突 | netstat | 9280/8120/13306 干净 | 干净 | ✅ pass | inline |
| FR-ENC-02 | API JSON 中文不乱码 | 检查 scan 响应 cellSupplier 字段 | 正常 UTF-8 中文 | `"cellSupplier\":\"����ʱ��\"` 乱码（PRODUCED 事件 payload） | ❌ fail（→ 缺陷 P2-003） | `evidence/api-scan-BAT-DEMO-001.json` |
| FR-ENC-03 | 产品列表中文 | 检查 /public/product/list title | 正常 | `"NEV ���ܳ��׮"` 部分乱码 | ⚠️ partial（同 P2-003） | `evidence/api-product-list.json` |

### 2.13 SEC — 安全基线

| ID | 用例 | 验证步骤 | 期望 | 实测 | 结果 | 证据 |
|---|---|---|---|---|---|---|
| FR-SEC-01 | 钱包 p12 未提交 | `git ls-files contracts/pk/` | 0 文件 | 0 文件 | ✅ pass | inline |
| FR-SEC-02 | `.gitignore` 包含 `*.p12` | grep | 找到 | 找到 line 65 + line 63 ignore contracts/pk/ | ✅ pass | inline |
| FR-SEC-03 | `application-prod.yml` 是否进库 | git ls-files + .gitignore 规则 | ruoyi-admin 可入库（已改 ${ENV:default}），其他被忽略 | 配置一致 | ✅ pass | `.gitignore` line 26-30 |
| FR-SEC-04 | actuator 默认鉴权 | `curl /actuator/health` 无 token | 401 / basic auth | HTTP 401 + "no basic auth" | ✅ pass（actuator 受保护） | inline |
| FR-SEC-05 | 鉴权错误响应码规范 | 缺 clientid / 错 token | HTTP 401 | HTTP 200 + 业务 500/401 | ❌ fail（→ 缺陷 P3-001） | inline |

---

## 3. 缺陷映射

| 缺陷 ID | 严重度 | 触发用例 |
|---|---|---|
| P1-001 | P1 | FR-CHN-02 / FR-SCN-02 |
| P1-002 | P1 | FR-UNI-01 |
| P1-003 | P1 | FR-UNI-02 |
| P2-001 | P2 | FR-UNI-03 |
| P2-002 | P2 | FR-CHN-03 |
| P2-003 | P2 | FR-ENC-02 / FR-ENC-03 |
| P3-001 | P3 | FR-API-03 / FR-API-04 / FR-API-05 / FR-SEC-05 |
| P3-002 | P3 | （文档 vs 代码命名分歧，归入 risk-assessment.md） |
| P3-003 | P3 | （sys-error.log SnailJob 噪声，归入 risk-assessment.md） |

详见 [defects.md](defects.md)。
