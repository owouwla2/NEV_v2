# NEV-v2 首轮测试计划（First-Round Test Plan）

> 治理产物：vibe 治理运行时 root run `20260524T100632Z-4c549a73` / `20260524T105506Z-73a6759a`
> 编制：测试运维工程师 / 2026-05-24
> 基线版本：tag `wave-4-end` + 7 个 README 文档 commit（HEAD `15ae238`）

---

## 1. 目标与范围

本计划面向 **NEV-v2 基于区块链的新能源汽车动力电池全产业链溯源平台** 的**第一次首轮全方面测试**，以 **发现 + 复现 + 评级 + 归档** 为目标，**不在本轮就地修复业务缺陷**。

### 1.1 被测对象（冻结）

| 层 | 内容 | 版本 / 位置 |
|---|---|---|
| 后端 | Spring Boot 多模块（38 模块） | Java 21 + SpringBoot 3.5.14 + RuoYi-Vue-Plus 5.6.1，`backend/` |
| 前端 | Admin Web 12 业务页 | Vue 3.5 + shadcn-vue + Vite 8，`apps/admin-web/` |
| 持久化 | MySQL 主库 + Redis 缓存 | MySQL 8.0.42 `nev_v2` @ :13306（容器 `nev-v2-mysql`）；Redis 7.4.1 db=1 @ :6379 |
| 区块链 | FISCO BCOS + WeBASE 集群 | FISCO BCOS 2.11.0（实测，README 写 2.9.1 偏差），WeBASE-Front :5002，合约 `0xf71701365b8b35d4a03a12ecc51edf5fd5797b08` |
| 鉴权 | Sa-Token 双 header | `Authorization: Bearer <jwt>` + `clientid: <client>` |

### 1.2 范围内（in-scope）

- 后端编译 / 启动 / API 健康 / 鉴权
- 前端类型检查 / 依赖审计 / 已构建 `dist/`
- MySQL schema 完整性、业务数据存在性
- 链上链下双写关键路径（PRODUCED / IN_USE / SOLD / RECYCLED）
- 公开扫码免登录链路 `/public/scan/{traceNumber}`
- GB-T 24067 5 阶段碳足迹
- 端口、编码、SnailJob/SBA 噪声日志已知项回归
- 钱包 p12 是否泄漏到 git、双 header 缺失分支、actuator 鉴权

### 1.3 范围外（out-of-scope，明确排除）

| 编号 | 项目 | 理由 |
|---|---|---|
| OOS-1 | 业务代码就地修复 | 首轮只发现归档，不修复 |
| OOS-2 | SpringDoc 注解补全等答辩前 TODO | 工作量大，留答辩前突击 |
| OOS-3 | 性能压测 | 首轮做基线观察，正式压测留二轮 |
| OOS-4 | 智能合约改动 / FISCO BCOS 重新部署 | 不在测试运维职责 |
| OOS-5 | 浏览器自动化（Cypress / Playwright） | 当前无 e2e 自动化脚手架，手动抽检替代 |

---

## 2. 测试类型矩阵（13 类）

| # | 测试类型 | 工具 / 命令 | 产物去向 |
|---|---|---|---|
| 1 | 环境与构建冒烟 | `docker ps` / `netstat -ano` / `curl :9280/auth/code` | `evidence/docker-ps.txt` |
| 2 | 静态质量 | `vue-tsc --noEmit` / `mvn compile`（已 up-to-date） | `evidence/sys-error-tail.log` |
| 3 | 依赖与漏洞扫描 | `pnpm audit` | 见 test-report.md §3.3 |
| 4 | 单元测试基线 | `mvn test -DskipTests=false` | `evidence/surefire-tag-test-failure.txt` |
| 5 | API 健康冒烟 | `curl` Sa-Token 双 header | `evidence/api-*.json` |
| 6 | DB 迁移与种子 | `mysql SHOW TABLES / SELECT COUNT(*)` | `evidence/mysql-tables.txt` |
| 7 | 链上链下双写 | `curl WeBASE-Front trans/handle` + scan API | `evidence/webase-getEventCount-fail.json` + `evidence/api-scan-*.json` |
| 8 | 业务闭环 e2e（数据复跑） | scan API + DB JOIN 检查 4 类事件 | `evidence/api-scan-*.json` |
| 9 | 公开扫码免登录 | `curl /public/scan/{}` 无 token | `evidence/api-scan-*.json` |
| 10 | GB-T 24067 碳足迹 | `curl /public/carbon/{}` | `evidence/api-carbon-*.json` |
| 11 | 前端渲染（手动抽检） | 浏览器登录 + /scan/:trace + 下单 | test-report.md `manual_review` 区 |
| 12 | 端口与编码 | netstat + JSON 中文字段检查 | test-report.md §3.12 |
| 13 | 安全基线 | `git ls-files contracts/pk/` + `.gitignore` + 负向鉴权 | test-report.md §3.13 |

---

## 3. 环境前置条件（实测）

| 条件 | 期望 | 实测 | 状态 |
|---|---|---|---|
| JDK 21 | LTS | OpenJDK 21.0.9 Temurin | ✅ |
| Maven | 3.9+ | 3.9.6 | ✅ |
| Node | 18+ | v24.13.1 | ✅ |
| pnpm | 9+ | 10.28.0 | ✅ |
| Docker | available | 29.4.3 | ✅ |
| 后端 :9280 | LISTENING | LISTENING (PID 31352) | ✅ |
| 前端 :8120 | LISTENING | not listening | ⚠️ 未启动（手动抽检前需起） |
| MySQL :13306 | container healthy | nev-v2-mysql Up 10h healthy | ✅ |
| Redis :6379 | container healthy | nev-v2-redis Up 10h healthy | ✅ |
| WeBASE-Front :5002 | Up | webase-front Up 10h | ✅ |
| WeBASE 合约 ABI 注册 | 已注册 | **空函数列表** | ❌ **缺陷 P1-001 根因** |

---

## 4. 完成判定（治理四真）

| 真 | 标准 | 本轮要求 |
|---|---|---|
| 治理真 | 需求/计划/执行/清理凭证齐全 | vibe 已生成 6 阶段产物 ✅ |
| 工程验证真 | 每一测试项有显式 pass/fail/blocked | test-report.md §3 全部填实 |
| 工作流真 | 计划单元与产物可追溯 | execution-plan + execute-receipt + evidence 三件套 |
| 产品验收真 | 缺陷+覆盖率+风险三件套生成 | defects.md / test-report.md(coverage 段) / risk-assessment.md |

### 4.1 完成措辞策略

- 允许：**"首轮完成（含 P0/P1/P2/P3 缺陷归档）"**
- 不允许：**"测试全部通过"** / **"零缺陷"**

---

## 5. 风险与假设

| 风险 | 缓解 |
|---|---|
| 后端实例 :9280 已在跑（非测试人员启动） | 直接复用 + 锁基线 HEAD `15ae238`，避免重启时序问题 |
| WeBASE ABI 未注册导致 verifyEvent 失败 | 首轮归档为 P1 缺陷，不在本轮修复 ABI |
| 单元测试 `<skipTests>true</skipTests>` 强制开启会启动 NevApplication 拖慢 | 仅做一次抽样，验证 SnailJob 阻断后即停 |
| PowerShell GBK 乱码 / Bash 终端编码 | 使用 bash + UTF-8，且 README 已警示 |

---

## 6. 工时与执行节奏

本轮单人测试运维工程师 **单日窗口**：约 1.5 小时实际执行 + 0.5 小时归档。
