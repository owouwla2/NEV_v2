# NEV-v2 项目交接文档（HANDOFF）

> **本文档用途**：当你切换到新的 Claude Code 对话框时，把整段对话上下文压缩到这一份文件里。
> 新对话框只需要让 Claude 先读完本文档，就能无损接上之前的所有进度、约定、踩过的坑。
>
> **创建日期**：2026-05-15
> **最后更新**：2026-05-23
> **当前进度**：Wave 2 完成（D8-D14 全部交付，公开扫码端点链上校验通过；准备进入 Wave 3 商城+碳核算）
> **位置**：`E:/Study/IdeaProjects/NEV-v2/HANDOFF.md`

---

## 0. 新对话框开场白模板（你直接复制）

```
我在做 NEV-v2 项目，一个基于区块链的新能源汽车动力电池全产业链溯源平台。
项目状态：Wave 1 D1 已完成，准备开始 D2。

请先读：
1. E:/Study/IdeaProjects/NEV-v2/HANDOFF.md（必读，全部内容）
2. E:/Study/IdeaProjects/NEV-v2/docs/requirements/2026-05-14-requirement-analysis-greenfield-rebuild-new-repo-nev-v2-ruoyi-vu.md
3. E:/Study/IdeaProjects/NEV-v2/docs/plans/2026-05-14-execution-plan-xl-wave-nev-v2-ruoyi-vue-plus-mysql-sa-token-myba-execution-plan.md
4. E:/Study/IdeaProjects/NEV-v2/docs/reference/README.md

参考项目（绝对路径只读访问，不要修改）：
- 老仓 NEV：E:/Study/IdeaProjects/NEV/（前一代实现，业务模型参考）
- 比赛参考：E:/Study/IdeaProjects/NEV/2026037462/（三层合约设计 + 业务功能参考）

约定：
- 始终用中文回答
- 遵循 canonical vibe 治理流程，重大动作前先 $vibe-want / $vibe-how / $vibe-do
- 不归档老仓 NEV，按需对照参考

读完后告诉我可以继续 Wave 1 D2。
```

---

## 1. 项目档案

### 1.1 三仓关系

```
E:/Study/IdeaProjects/
├── NEV/                    ← 老仓（前一代 MongoDB+Spring 单模块实现）
│   │                         保留不归档，可启动作对照参考
│   │                         端口：backend=9180 / admin-web=8020 / user-app=5173
│   ├── docs/               ← 历史文档，新仓已复制必要部分
│   └── 2026037462/         ← 比赛参考项目（参赛资源，含合约源码 + 文档 PDF）
│                             RuoYi 3.9.1 二开，作品文档在内
│
└── NEV-v2/                 ← ⭐ 新仓（你现在的工作目录）
    │                         绿地重建，基于 RuoYi-Vue-Plus 5.6.1
    │                         端口：backend=9280 / admin-web=8120 / user-app=5273
    │                         MySQL=6306（避开冲突）/ Redis=6379（共用）
    ├── backend/            ← Spring Boot 多模块（5.6.1 + JDK 21 + Sa-Token + MyBatis-Plus）
    ├── docs/               ← 治理文档 + 参考索引
    │   ├── requirements/   ← 冻结的需求文档（2026-05-14）
    │   ├── plans/          ← XL 执行计划（4 Wave / 30 天）
    │   ├── reference/      ← 老仓和参考项目的索引
    │   ├── legacy/         ← 老仓 analysis 报告备份
    │   ├── architecture/   ← 待写：ER 图（Wave 1 D4）
    │   ├── contracts/      ← 待写：合约设计说明（Wave 2 D8）
    │   └── api/            ← 待写：API 文档（Wave 4 D27）
    ├── outputs/runtime/    ← vibe 治理证据（运行时生成）
    └── HANDOFF.md          ← 本文档
```

### 1.2 项目本质

- **类型**：比赛作品（首要交付）+ 后期改造为毕业设计
- **作者身份**：区块链工程专业学生（个人独立开发）
- **工期**：1 个月内完成首版交付
- **质量门槛**：毕设可推演（高于 demo，低于生产）
- **业务定位**：动力电池全产业链溯源（生产→流通→零售→消费→回收）

---

## 2. 已完成进度（Wave 1 D1）

### 2.1 git commit 历史

```
78d5a86  refactor(backend): rename package org.dromara → com.nev (NEV-v2 namespace)
                                                                  ⭐ HEAD = tag: wave-1-d1-renamed
a38c5c8  chore(backend): upgrade JDK 17 → 21
a39204b  chore: import ruoyi-vue-plus 5.6.1 as backend/ skeleton
                                                                  tag: wave-1-d1-foundation
2c782a6  chore: init NEV-v2 repository skeleton
```

可用回滚点：
- `git reset --hard 2c782a6` → 退到只有文档
- `git reset --hard wave-1-d1-foundation` → 退到未改包名（含 JDK 21）
- `git reset --hard wave-1-d1-renamed` → 当前位置

### 2.2 D1 完成的 12 个子任务

| # | 任务 | 结果 |
|---|---|---|
| 1 | 创建 `E:/Study/IdeaProjects/NEV-v2/` + git init + docs 目录 | ✓ |
| 2 | 复制需求文档 + 执行计划 + 老仓 analysis 报告 | ✓ |
| 3 | 写 `docs/reference/README.md`（老仓 + 比赛参考索引）| ✓ |
| 4 | 写根 `README.md` + `.gitignore` | ✓ |
| 5 | 克隆 ruoyi-vue-plus 5.X → backend/（实际 release 5.6.1）| ✓ |
| 6 | 删除嵌套 .git，作为独立提交历史 | ✓ |
| 7 | JDK 17 → 21（pom.xml + Dockerfile）| ✓ |
| 8 | 全局替换 617 个 .java 文件包路径（2717 处）| ✓ |
| 9 | 全局替换 92 个配置文件包路径（46 处） | ✓ |
| 10 | 处理边界情况：`logging.level` / `mapperPackage` / `typeAliasesPackage` | ✓ |
| 11 | 物理移动 32 个目录 `org/dromara/*` → `com/nev/*` | ✓ |
| 12 | 启动类改名：`DromaraApplication` → `NevApplication`、`DromaraServletInitializer` → `NevServletInitializer` | ✓ |
| 13 | 根 pom.xml：groupId `com.nev` / artifactId `nev-backend` / name `NEV-v2 Backend` | ✓ |
| 14 | 4 个子模块 pom.xml 父引用：`ruoyi-vue-plus` → `nev-backend` | ✓ |
| 15 | **mvn clean compile -T 4 → BUILD SUCCESS（33 模块 / 41.5s）** | ✓ |

### 2.3 Wave 1 D2 完成清单（2026-05-16）

| # | 任务 | 结果 |
|---|---|---|
| 1 | 写 `deploy/docker/docker-compose.yml`（MySQL 8.0.42 端口 13306 + Redis 7.4.1 端口 6379）| ✓ |
| 2 | 写 `deploy/docker/.env.example` + `.gitignore` 排除 `.env` 和 `volumes/` | ✓ |
| 3 | Docker 容器启动并 healthy | ✓ |
| 4 | 改 `application.yml` 端口 → 9280 | ✓ |
| 5 | 改 `application-dev.yml` 数据源 → `localhost:13306/nev_v2` + Redis db=1 | ✓ |
| 6 | 导入 SQL：`ry_vue_5.X.sql` + `ry_workflow.sql` + `ry_job.sql`（59 张表）| ✓ |
| 7 | 开发环境关闭接口加密 `api-decrypt.enabled: false` | ✓ |
| 8 | 开发环境关闭验证码 `captcha.enable: false` | ✓ |
| 9 | `mvn install -DskipTests` 全量编译通过（33 模块）| ✓ |
| 10 | 启动 backend 端口 9280 正常 | ✓ |
| 11 | **admin/admin123 登录成功，返回 JWT token** | ✓ |
| 12 | Token 访问受保护接口 `/system/user/getInfo` 返回 200 | ✓ |

### 2.4 Wave 1 D3-D4 完成清单（2026-05-19）

| # | 任务 | 结果 |
|---|---|---|
| 1 | 写 `backend/script/sql/nev_v2_business.sql`（20 张业务表，对齐需求文档 §5.2） | ✓ |
| 2 | 7 个分组覆盖：用户扩展 / 电池溯源 / 商城 / 支付与地址 / 以旧换新 / 碳模块 / 区块链配置 | ✓ |
| 3 | 公共字段全部对齐 RuoYi 风格：`create_by/update_by bigint(20)` + `tenant_id` + `del_flag` | ✓ |
| 4 | Docker 容器健康（mysql/redis），`docker exec` 执行业务 SQL 无报错 | ✓ |
| 5 | 数据库验证：`select count(*) from information_schema.tables where ...` = 20 张 | ✓ |
| 6 | 写 `backend/script/sql/nev_v2_seed.sql`（6 角色 + 20 菜单 + 角色绑定矩阵） | ✓ |
| 7 | 6 个业务角色入库：producer/distributor/retailer/merchant/consumer/recycler（role_id 11-16） | ✓ |
| 8 | 20 个业务菜单入库（menu_id 2001-2061，6 目录 + 14 页面） | ✓ |
| 9 | 角色菜单绑定按需求文档 §3.3 矩阵：producer=7 / distributor=5 / retailer=8 / merchant=5 / consumer=10 / recycler=8 / superadmin=20 | ✓ |
| 10 | admin 通过 sys_role_menu 自动获得全部 20 项业务菜单（select ... from sys_menu where menu_id between 2001 and 2999） | ✓ |

**vibe 治理痕迹**：本轮启动 canonical vibe-do-it（stop=plan_execute）创建了 session `20260519T142922Z-218f92b8`，四件证据齐全（host-launch-receipt / runtime-input-packet / governance-capsule / stage-lineage），但 router 状态 `route_mode=confirm_required, confidence=0.45, route_reason=legacy_fallback_guard`，未推进到 plan_execute。**按 FAQ Q3 D3-D4 SQL 是按需求文档 §5.2/§3.3 已定计划的纯执行性任务，简化跳过完整 vibe 流程**，session 保留作为治理痕迹。后续重大决策（合约设计 D8）必须重新走完整 vibe。

### 2.5 D3-D4 关键发现

- **vibe entry-id 笔误**：HANDOFF.md §3.3 写的 `vibe-do` 是错的，正确名是 `vibe-do-it`（详见 `config/vibe-entry-surfaces.json`）。已在 §3.3 修正。
- **stage 名称完整列表**：`skeleton_check → deep_interview → requirement_doc → xl_plan → plan_execute → phase_cleanup`，HANDOFF 旧版只列了 3 个（缺 deep_interview/plan_execute/skeleton_check）。
- **router 需 host-decision 协议**：runtime 在 `skeleton_check` 后等待 host 提供 `decision_kind=route_selection, decision_action=accept_primary/select_skill` 的 JSON 决策，需配合 `--continue-from-run-id` 重入。本轮没走这一步。
- **RuoYi `create_by/update_by` 是 bigint(20)**（用户 ID），需求文档 §5.3 写的 `VARCHAR(64)` 不对，会破坏 MyBatis-Plus 字段自动填充。新表全部按 RuoYi 风格落 bigint(20)。
- **menu_id 安全范围**：现存最大 menu_id=1623，本轮用 2001-2061，毕设阶段扩展可继续 2100+。
- **role_id 安全范围**：已占用 1（superadmin）/ 3 / 4，本轮用 11-16，后续如需新角色用 20+。

### 2.6 D2 关键发现（踩坑记录）

### 2.7 Wave 1 D5-D7 完成清单（2026-05-20）

| # | 任务 | 结果 |
|---|---|---|
| 1 | 写 `backend/script/sql/nev_v2_seed_demo.sql`（分离基础 seed 与演示数据，便于生产部署只导基础 seed） | ✓ |
| 2 | 6 个 demo 用户入库（user_id 101-106：producer1/distributor1/retailer1/merchant1/consumer1/recycler1，密码统一 admin123，复用 RuoYi admin 的 BCrypt hash） | ✓ |
| 3 | `sys_user_role` 绑定 6 用户到对应业务角色 11-16 | ✓ |
| 4 | `sys_nev_user_ext` 写入 6 行业务扩展（user_type + 占位 wallet_address `0x101..0001~0006` + 企业信息） | ✓ |
| 5 | `nev_emission_factor` 灌 15 条排放因子（RAW 5/MFG 4/TRANS 3/USE 2/EOL 1），含中国电网平均电力 0.5703 kgCO2eq/kWh + 关键金属碳排放（锂/钴/镍/铜/铝），来源 CLCD/Ecoinvent/IPCC/生态环境部 | ✓ |
| 6 | 商品分类字典 `nev_product_category`：BATTERY/ACCESSORY/SERVICE（sys_dict_type id=100，sys_dict_data 1001-1003） | ✓ |
| 7 | `mvn clean install -DskipTests -T 4` 全量编译通过（33 模块） | ✓ |
| 8 | 启动 backend 端口 9280 正常 | ✓ |
| 9 | producer1 / admin123 登录返回 JWT token（userId=101, roles=['producer']） | ✓ |
| 10 | `/system/user/getInfo` 返回 4 项权限：`nev:battery:register, nev:battery:query, nev:carbon:footprint, nev:blockchain:trace` —— 完全匹配 D4 设计的权限矩阵 | ✓ |
| 11 | `/system/menu/getRouters` 返回 3 个目录（Battery / Carbon / Blockchain）+ 4 个子页面，共 7 项，匹配 producer=7 项授权 | ✓ |

### 2.8 D5-D7 关键发现

- **演示数据分文件好处**：`nev_v2_seed.sql`（基础：角色+菜单）和 `nev_v2_seed_demo.sql`（演示：用户+因子+字典）分开，生产部署可只导基础 seed 不污染。
- **统一密码 BCrypt**：6 个 demo 用户复用 RuoYi admin 的 hash `$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2`，明文都是 admin123，免去运行时生成 hash。
- **wallet_address 占位**：写入 `0x101..0101 ~ 0x101..0106` 占位地址，Wave 2 D11 部署 RoleManager 合约后用真实 FISCO BCOS account 回填。
- **`sys_nev_user_ext` 主键即外键**：以 user_id 直接做主键（不是独立 id），1:1 关联 sys_user 不需要额外 join 列。
- **producer 菜单数 7 = 3 目录 + 4 页面**：getRouters 自动按 parent_id 折叠成 children 树，无需后端额外处理。RuoYi 路由系统自动隐藏当前角色无权限的子菜单。
- **`getInfo` permissions 返回**：只包含 `menu_type='C'/'F'` 的 perms 字段（目录不带 perms），所以 producer 拿到 4 项权限 = 4 个 C 菜单，符合 RuoYi 设计。
- **demo 用户登录密码同 admin**：用户都用 `admin123`，验证完全等同 D2 admin 登录路径，无新协议变更。

### 2.9 Wave 1 全部交付清单

```
D1  ✓  RuoYi 5.6.1 脚手架 + JDK 21 + 包名 com.nev（wave-1-d1-renamed）
D2  ✓  Docker (MySQL 13306 + Redis 6379) + admin/admin123 登录（wave-1-d2-running）
D3  ✓  20 张 nev_* 业务表 SQL + 入库验证
D4  ✓  6 业务角色 + 20 业务菜单 + 权限矩阵（wave-1-d4-schema）
D5  ✓  6 demo 用户（每角色 1 个）+ 15 排放因子
D6  ✓  商品分类字典 + 全部 demo seed 入库
D7  ✓  全量编译 + producer1 登录验证 + 菜单授权矩阵端到端通过（wave-1-end）
```

**总计可启动可登录可演示的最小可用版本**：MySQL 79 张表（59 sys_* + 20 nev_*）、7 角色（admin+6 业务）、7 用户、20 业务菜单、15 排放因子。

### 2.27 Wave 2 D14 完成清单（2026-05-23）

| # | 任务 | 结果 |
|---|---|---|
| 1 | application.yml `security.excludes` 加 `/public/**` → 让公开端点免鉴权 | ✓ |
| 2 | `BatteryScanVO` 新增：电池基础 + 链上整体校验状态（overallVerified/totalEvents/verifiedEvents/chainEventCount）+ 事件时间线（每条带 chainVerified 标记） | ✓ |
| 3 | `BatteryScanService.scan(traceNumber)`：查 nev_battery + nev_battery_lifecycle（按 version asc）→ 调链上 `getEventCount` + 每条 `verifyEvent` → 汇总返回 | ✓ |
| 4 | `PublicScanController` GET `/public/scan/{traceNumber}` 无 @SaCheckLogin/@SaCheckRole | ✓ |
| 5 | **修复 ContractInvoker.parseResponse bug**：WeBASE `/trans/handle` 对 view 函数直接返回 JSON 数组（`["4"]` / `["true"]`），原代码只查 root.output/data/result 字段拿不到 → 加 array 根节点直接作 output 的分支 | ✓ |
| 6 | `mvn -q clean install -DskipTests -T 4` 全量编译通过 | ✓ |
| 7 | smoke test：无 token 调 GET `/public/scan/BAT-DEMO-002` → HTTP 200，返回 4 事件时间线 | ✓ |
| 8 | `overallVerified=true`、`totalEvents=4`、`verifiedEvents=4`、`chainEventCount=4`、4 条事件 `chainVerified=true` —— **消费者扫码端到端真实完整性证明** | ✓ |
| 9 | smoke test 反例：调 `/public/scan/NOT-EXIST` → code=500 + msg=`溯源编号 [NOT-EXIST] 不存在` | ✓ |

### 2.28 D14 关键发现

- **WeBASE `/trans/handle` view 函数响应**：写链调用返回 `{transactionHash, ...}` 对象；view 函数直接返回 `["xxx"]` JSON 数组作为根（getRole→`["2"]`、hasRole→`["true"]`、getEventCount→`["4"]`）。`ContractInvoker.isSuccess()` 和 `parseResponse()` 都加了 array 根节点的兜底分支。
- **二维码生成留前端**：本轮 backend 不集成 zxing/qrcode，前端 plus-ui（D22）用 `qrcode.js` 直接把 traceNumber 渲染成 PNG。nev_battery.qr_code_path 字段留作 Wave 4 OSS 存储路径。
- **二维码内容设计**：建议前端用 `${SCAN_BASE_URL}/scan/${traceNumber}` 编码（如 `https://nev.demo/scan/BAT-DEMO-002`），消费者扫码直接跳转到溯源详情页。当前 backend 公开端点路径 `/public/scan/{traceNumber}`，前端做一层 redirect。
- **rate limit 暂不集成**：公开端点存在 DDOS 风险，本轮 demo 由 nginx/网关层兜底；ruoyi-common-ratelimiter（Redis lua 实现）可在 Wave 3+ 补 `@RateLimiter(key="public-scan", count=60, time=60)` 等注解。
- **chainVerified false 触发条件**：dataHash 被篡改、版本号错位、链上事件缺失、ContractInvoker 异常等都会让单条事件 chainVerified=false，`overallVerified` 只在全部 4 个条件都通过时才 true。这是消费者识别数据被篡改的核心契约。

### 2.29 Wave 2 全程总结（D8-D14）

```
D8  ✓  3 Solidity 合约设计（RoleManager / BatteryRegistry / LifecycleTrace）（wave-2-d8-contracts）
D9  ✓  docker solc 0.6.10 编译产出 14.7KB LifecycleTrace.bin（wave-2-d9-compiled）
D10 ✓  nev-blockchain 模块 + Spring 6 RestClient WeBASE 客户端（wave-2-d10-blockchain-module）
D11 ✓  部署到 FISCO BCOS + 6 角色链上授权 + admin1 .p12 → Front 5002（wave-2-d11-deployed）
D12 ✓  nev-battery 模块 + 启动期 abi 注入 + producer 注册电池（wave-2-d12-battery-register）
D13 ✓  distributor/retailer/recycler 3 事件端点 + 链上链下 version 对齐（wave-2-d13-lifecycle-events）
D14 ✓  公开扫码端点 + 实时链上校验 + Wave 2 收尾（wave-2-end）
```

**总计**：
- 1 个智能合约部署：LifecycleTrace = `0xf71701365b8b35d4a03a12ecc51edf5fd5797b08`
- 7 个签名用户私钥导入 Front 5002 本地
- 2 个新 Maven 模块：nev-blockchain + nev-battery（35 个模块总计）
- 8 个 REST 端点：1 producer + 1 distributor + 1 retailer + 1 recycler + 1 public + 3 auth
- 完整业务链：producer 注册 → distributor 接收 → retailer 售出 → recycler 回收
- 链上数据完整性：4 阶段事件 keccak256 + verifyEvent 双重校验

**最小可演示版本**：消费者打开浏览器 `http://localhost:9280/public/scan/BAT-DEMO-002`，**无需登录**即可看到电池完整生命周期 4 个事件 + 链上每条事件验证状态，证明数据未被篡改。



| # | 任务 | 结果 |
|---|---|---|
| 1 | 3 个事件 DTO：TransferInDTO（IN_USE，fromOwnerId）/ SellDTO（SOLD，orderNo+consumerId）/ ReceiveDTO（RECYCLED，soh 0-100） + 通用 BatteryEventVO | ✓ |
| 2 | BatteryService 抽 `appendEvent()` 私有 helper：电池存在校验 → dataHash 计算 → 链上 addEvent → 写 lifecycle + 更新 nev_battery 状态字段。版本号自动从 DB 取 max+1 | ✓ |
| 3 | BatteryService 加 3 个公共方法：transferIn / sell / receive（每个内部调 appendEvent，分别带 EVT_IN_USE/EVT_SOLD/EVT_RECYCLED + 各自 dataHash 输入函数） | ✓ |
| 4 | 3 个 Controller：DistributorBatteryController @SaCheckRole("distributor") POST /distributor/battery/transfer-in；RetailerBatteryController @SaCheckRole("retailer") POST /retailer/battery/sell；RecyclerBatteryController @SaCheckRole("recycler") POST /recycler/battery/receive | ✓ |
| 5 | **修复链上 version 错位 bug**：原 register 只调 registerBattery（写 batteries mapping），但未 addEvent，导致 lifecycleEvents 数组缺 PRODUCED 这一行，version 链上链下错位。修复：register 后立即再调 addEvent(PRODUCED) 把 v1 写入数组 | ✓ |
| 6 | `mvn -q clean install -DskipTests -T 4` 全量编译通过（35 模块） | ✓ |
| 7 | **端到端 4 阶段 smoke test（BAT-DEMO-002）**：producer1 register → distributor1 transfer-in → retailer1 sell(consumerId=105) → recycler1 receive(soh=78) | ✓ |
| 8 | MySQL nev_battery_lifecycle 4 行（v1 PRODUCED / v2 IN_USE / v3 SOLD / v4 RECYCLED），各角色 operator_id 与登录用户一致 | ✓ |
| 9 | MySQL nev_battery 最终状态：current_status=RECYCLED, current_role=recycler, current_owner_id=106 | ✓ |
| 10 | 链上 `getEventCount("BAT-DEMO-002")` 返回 `["4"]` | ✓ |
| 11 | 链上 `verifyEvent(v=1~4, hash)` 4 次全部返回 `["true"]` | ✓ |
| 12 | 链上 `getLatestEvent` 返回 v4 RECYCLED + operator=recycler1 wallet 0xe3ca5516... | ✓ |

### 2.26 D13 关键发现

- **WeBASE 钱包名 vs sys_user 用户名**：D11 给 WeBASE-Web 5000 创建签名用户时因 12 字符长度上限用了短名（`distrib1`/`merchant1`...），但 sys_user 表里登录名仍是完整名（`distributor1`/`merchant1`）。**API 测试登录用 sys_user 用户名，链上签名用 sys_nev_user_ext.wallet_address**，两者通过 user_id 关联。
- **registerBattery 不会自动写 lifecycleEvents**：D8 合约设计时 BatteryRegistry.registerBattery 只 emit BatteryRegistered（写 batteries mapping 字典）；LifecycleTrace.lifecycleEvents 数组要靠 addEvent 单独写入。后端必须在 register 后多调一次 `addEvent(traceNumber, PRODUCED, dataHash)` 让链上 lifecycle 数组从 v=1 开始记录，否则 IN_USE 链上 version=1 / backend version=2 永远错位。
- **状态机宽松校验**：本轮 demo 不强制 PRODUCED→IN_USE→SOLD→RECYCLED 严格顺序，只校验 currentStatus != DISMANTLED。后续 D17 marketplace 接入后再加严格状态机。
- **SOLD 转移所有权**：sell 操作后 nev_battery.current_owner_id 从 retailer 切到 consumerId，current_role 切 "consumer"。链上 operator 仍是 retailer（链上记的是触发事件的人，所有权转移是链下数据）。
- **appendEvent 函数式抽象**：用 `Function<Date, byte[]> hashFn` + `Function<Date, String> payloadFn` 把每个事件的 hash 计算 + payload 序列化注入到 helper，避免 6 个事件类型重复样板代码。



| # | 任务 | 结果 |
|---|---|---|
| 1 | 用户从 WeBASE-Web 5000 一次性导出 7 个 .p12（admin1 + 6 demo 用户），全部空密码 | ✓ |
| 2 | Python 脚本批量调 `POST /privateKey/importP12` 把 6 个新用户导入 Front 5002 本地私钥库（admin1 D11 已导） | ✓ |
| 3 | Front 5002 本地用户从 5 个（4 历史 + admin1）扩到 **11 个**（含 7 个 NEV-v2 用户） | ✓ |
| 4 | 启动 backend：ContractRegistryBootstrap 自动注册 LifecycleTrace；HTTP 9280 ready | ✓ |
| 5 | producer1 / admin123 登录，拿到 JWT token | ✓ |
| 6 | `POST /producer/battery/register` 注册 BAT-DEMO-001（NEV-LFP-280-2026, 85 kWh, 400V） | ✓ |
| 7 | 接口返回：`code=200, id=2058073529223593985, dataHash=0xe422e122..., txHash=0x1860de076d..., chainStatus=SUCCESS` | ✓ |
| 8 | MySQL `nev_battery`：BAT-DEMO-001 行存在，current_status=PRODUCED，producer_id=101 | ✓ |
| 9 | MySQL `nev_battery_lifecycle`：1 行（event_type=PRODUCED, version=1, operator_id=101, operator_role=producer, data_hash + tx_hash 写入） | ✓ |
| 10 | 链上 `getBatteryInfo("BAT-DEMO-001")` 返回 traceNumber + dataHash(base64) + producer(=producer1 wallet) + producedAt | ✓ |
| 11 | **`verifyBattery("BAT-DEMO-001", 0xe422e122...) -> ["true"]`** 链上链下 dataHash 完整性证明通过 | ✓ |

### 2.23 D12 业务流端到端图

```
producer1 浏览器登录
  └→ POST /auth/login (clientId/sys_client) -> JWT token
       └→ POST /producer/battery/register
            ├─ @SaCheckRole("producer") 通过
            ├─ LoginHelper.getUserId() = 101
            ├─ SysNevUserExtMapper.selectById(101).walletAddress = 0x501d135c...
            ├─ unique(traceNumber=BAT-DEMO-001) 通过
            ├─ INSERT nev_battery(id=2058..., status=PRODUCED, producer_id=101)
            ├─ DataHashCalculator.produced(
            │     traceNumber, 101, producedAt, specJson(model/serialNo/...)
            │   ) = keccak256 = 0xe422e122...8371
            ├─ ContractInvoker.invokeAs(
            │     "0x501d135c...",  // 切到 producer1 私钥签名
            │     "LifecycleTrace",
            │     "registerBattery",
            │     ["BAT-DEMO-001", "0xe422...8371"]
            │   )
            │     └→ WeBASE-Front 5002 /trans/handle
            │          └→ Front 本地 producer1 私钥签名 + 上链 FISCO BCOS group_1
            │               └→ tx_hash=0x1860de076d...
            ├─ INSERT nev_battery_lifecycle(
            │     event_type=PRODUCED, version=1,
            │     data_hash=0xe422e122..., tx_hash=0x1860de076d...
            │   )
            └← return BatteryRegisterVO { ... chainStatus=SUCCESS }

链上回查：
  /trans/handle  getBatteryInfo("BAT-DEMO-001")
    -> [traceNumber, dataHash(base64), producer, producedAt]
    -> producer = producer1 wallet  ✓
    -> dataHash(base64 -> hex) == 0xe422e122...8371  ✓

  /trans/handle  verifyBattery("BAT-DEMO-001", 0xe422...8371)
    -> ["true"]  ✓ 链上链下 dataHash 完整性证明通过
```

### 2.24 D12 总结

**Wave 2 D8-D12 完整链路**：
- D8 设计 3 个 Solidity 合约
- D9 docker solc 编译产出 14.7KB LifecycleTrace.bin
- D10 nev-blockchain 模块 + WeBASE-Front HTTP 客户端
- D11 部署到 FISCO BCOS + 链上 6 角色授权 + admin1 .p12 导入 Front
- D12 nev-battery 业务模块 + 完整 producer 注册流 + 链上链下数据校验通过

**总计**：35 个 Maven 模块、4 个 Solidity 合约文件、3 个新业务 Java 包（nev-blockchain.client/service/util/config + nev-battery.controller/service/domain/mapper/dto），所有 producer1 注册新电池操作均能：
- MySQL 留库（nev_battery + nev_battery_lifecycle）
- 上链留痕（LifecycleTrace.registerBattery 写入 + 自动同步全部三层合约的状态）
- 任何角色可通过 verifyBattery 验证电池数据未被篡改



| # | 任务 | 结果 |
|---|---|---|
| 1 | 新建 `backend/nev-modules/nev-battery` Maven 子模块，依赖 nev-blockchain + ruoyi-common-* | ✓ |
| 2 | nev-modules/pom.xml 注册 nev-battery 模块；ruoyi-admin/pom.xml 引入 nev-battery 依赖 | ✓ |
| 3 | nev-blockchain 新增 `NevContractConfigDO` 实体 + `NevContractConfigMapper`（MyBatis-Plus） | ✓ |
| 4 | nev-blockchain 新增 `ContractRegistryBootstrap`（@EventListener(ApplicationReadyEvent) 启动期从 nev_contract_config 表 enabled='1' 行批量注入 ContractAddressResolver） | ✓ |
| 5 | ContractAddressResolver 重构：增加 `Registration` 内存注册表 + `register()` 方法；resolveAddress/abi/userAddress/path 全部优先查注册表，退回 properties 占位 | ✓ |
| 6 | ContractInvoker 增加 `invokeAs/queryAs` 重载（运行时传入 userAddress 覆盖 resolver 默认值，支持业务层切换调用者） | ✓ |
| 7 | nev-blockchain 新增 `DataHashCalculator`（基于 BouncyCastle Keccak.Digest256，按 design.md §5 规约实现 6 个 EventType 的 keccak256 拼接） | ✓ |
| 8 | nev-battery 新增 `NevBatteryDO` + `NevBatteryLifecycleDO` + `SysNevUserExtDO` 实体（继承 BaseEntity + MyBatis-Plus 注解） | ✓ |
| 9 | nev-battery 新增 3 个 Mapper（BaseMapper 直接用） | ✓ |
| 10 | nev-battery 新增 `BatteryRegisterDTO` + `BatteryRegisterVO`；`BatteryService.register()` 完整业务流（鉴权 + 唯一性 + 写库 + dataHash + 链上调用 + lifecycle 落库） | ✓ |
| 11 | nev-battery 新增 `ProducerBatteryController` POST /producer/battery/register，`@SaCheckRole("producer")` | ✓ |
| 12 | BlockchainAutoConfiguration + BatteryAutoConfiguration 双 `@MapperScan` 把 Mapper 注入 SqlSessionFactory | ✓ |
| 13 | `mvn -q clean install -DskipTests -T 4` 全量编译通过（35 模块） | ✓ |
| 14 | 启动 backend 验证：ContractRegistryBootstrap 日志显示 `registered contract: name=LifecycleTrace address=0xf717...abiLen=6746` + `已从数据库注册 1 个合约` | ✓ |

### 2.20 D12 关键设计

- **abi 来源：表 > 配置**：`ContractRegistryBootstrap` 启动期先吃 nev_contract_config 表，把所有 enabled='1' 的合约注册到内存。如果表空（开发期），退回 application.yml 的 `nev.blockchain.contracts.*`。生产部署时配置可以全部省略。
- **调用者切换：方法重载，无 ThreadLocal**：`ContractInvoker.invokeAs(userAddress, ...)` 让业务层显式传入当前用户的 wallet_address。避免 ThreadLocal 泄漏，类型安全。`invoke()` 不传则用 defaultUserAddress（admin1）。
- **dataHash 规约固化在工具类**：`DataHashCalculator.produced(traceNumber, producerId, producedAt, specJson)` 等 6 个静态方法对应 6 个 EventType，字段顺序在类里写死。**改顺序 = 历史 verify 全部失效**，要改先改 design.md §5 + 给完整数据迁移方案。
- **specJson 用 Jackson `ObjectMapper` 序列化**：BatteryService 内的 `canonicalSpec()` 用 LinkedHashMap 固定字段顺序（model / serialNo / capacityKwh / voltage / cellSupplier / cellType / bmsInfo），确保 keccak256 输入跨时间稳定。
- **暂未启用 MyBatis-Plus 字段填充**：BaseEntity 用了 @TableField(fill = FieldFill.INSERT)，需要全局 MetaObjectHandler。本轮 BatteryService 在 insert 前手工设了 tenantId / delFlag，create_by / create_time 让 MyBatis-Plus 自带 fill 处理（如果配置了）。生产前确认 RuoYi 默认提供的 InjectionMetaObjectHandler 已注入。

### 2.21 D12 待办（D12-6 业务 smoke test）

- ⏸ 需要导出 producer1 的 .p12（同 admin1 路径，空密码）到 `contracts/pk/`
- ⏸ Python 调 `POST /privateKey/importP12?userName=producer1&p12Password=` 导入 Front 5002
- ⏸ producer1 / admin123 登录 backend，拿 token + clientid
- ⏸ POST /producer/battery/register {traceNumber=BAT-DEMO-001, model=..., serialNo=..., capacityKwh=85.0, ...}
- ⏸ 验证：返回 200 + 链上 `getBatteryInfo("BAT-DEMO-001")` 返回与 backend 计算一致的 dataHash



| # | 任务 | 结果 |
|---|---|---|
| 1 | WeBASE-Web 5000 部署 LifecycleTrace 合约（含三层继承字节码） | ✓ |
| 2 | WeBASE-Web 5000 创建 7 个签名用户（admin1/producer1/distrib1/retailer1/merchant1/consumer1/recycler1） | ✓ |
| 3 | 7 个钱包地址记录到 `contracts/address/address.md`（公开数据，可入 git） | ✓ |
| 4 | 用户导出 admin1 .p12 私钥 → `contracts/pk/admin1_key_*.p12`（已 gitignore） | ✓ |
| 5 | 通过 `POST /privateKey/importP12?userName=admin1&p12Password=` 把 admin1 私钥导入 Front 5002 本地（空密码） | ✓ |
| 6 | 写 `backend/script/sql/nev_v2_d11_chain_binding.sql`：INSERT nev_contract_config + 6 UPDATE sys_nev_user_ext（含 6.7KB abi inline） | ✓ |
| 7 | docker exec 执行 SQL → 表写入验证：contract_address 正确、abi_len=6746、6 个用户 wallet_address 全部从占位 0x101... 改为真链地址 | ✓ |
| 8 | 通过 Python 脚本调 `/WeBASE-Front/trans/handle` 6 次 grantRole（admin1 签名，PRODUCER/DISTRIBUTOR/RETAILER/MERCHANT/CONSUMER/RECYCLER=2-7） | ✓ |
| 9 | 链上验证：getRole(admin1)=1 / getRole(producer1)=2 / hasRole(producer1, 2)=true / getRole(recycler1)=7 | ✓ |
| 10 | ContractInvoker 重构：HTTP 路径从 `/trans/handleWithSign` → `/trans/handle`（Front 本地私钥签名，因 WeBASE-Sign 5004 用户列表为空，Web 5000 创建的用户没同步到 Sign） | ✓ |
| 11 | BlockchainProperties 字段重命名：`defaultSignUserId` → `defaultUserAddress`，语义从 Sign UUID 改为 wallet address | ✓ |
| 12 | application-dev.yml 回填：`defaultUserAddress=0x6933...`，`LifecycleTrace.address=0xf71701...` | ✓ |
| 13 | `mvn -q clean install -DskipTests -T 4` 全量编译通过 | ✓ |

### 2.17 D11 关键发现（踩坑记录）

- **WeBASE 三组件的私钥不互通**：WeBASE-Web 5000 创建用户走 NodeManager 5001 的数据库，但**没同步到 WeBASE-Sign 5004**（查 `/user/list/.../1/100` 得 totalCount=0）；WeBASE-Front 5002 调 `/trans/handleWithSign` 时报 303002 "user does not exist"。解决：导出 .p12 后通过 `/privateKey/importP12` 导入 Front 本地私钥库 → 改用 `/trans/handle` 路径（user=address，Front 本地签名）。
- **导出的 .p12 默认空密码**：WeBASE-Web 1.5.x 在"导出 P12 私钥"时如果不输密码就是空字符串，`p12Password=` 直接导入成功。
- **`/trans/handle` 写 + 读统一**：view 函数（getRole/hasRole/...）和写状态函数（grantRole）都走同一个接口，WeBASE-Front 根据 ABI 自动判断是否需要打包交易。view 函数返回值直接以 JSON 数组返回（如 `["2"]` `["true"]`）。
- **`/trans/handle` 写链返回的 txHash/blockNumber 为 null**：实测 6 次 grantRole 全部 HTTP 200 + code:0，但响应里没有 txHash/blockNumber 字段。验证方式：用 view 函数（getRole）查链上状态，确认数据已写入。后续业务层如果需要 txHash 写回 nev_battery_lifecycle，需要单独调 web3 transaction API。
- **私钥安全 .gitignore**：加 `contracts/pk/`、`*.p12`、`*.pem` 规则，私钥绝不入 git。.gitignore 第 60-65 行。
- **enum 在 ABI 中以 uint8 暴露**：调 grantRole 传 `[address, 2]`（int），不能传 `[address, "PRODUCER"]`（string）。同样 hasRole 返回的 `true`/`false` 也是 ABI 解码后的字面量。
- **ContractAddressResolver.registerAbi 没在启动期注入**：D10 阶段写的 startup 注入还没接 nev_contract_config 表，所以即使 SDK 编译通过，启动期 Spring 加载时 `abiCache` 为空。D12 起需要：
  - 加 `NevContractConfigMapper` 读 nev_contract_config 表
  - 加 `@PostConstruct` 启动期把表中所有 enabled=1 的合约 abi 注册到 ContractAddressResolver
  本轮 D11 没做（仅 Python 脚本调用验证链上 OK，Java SDK 真实调用留 D12）。

### 2.18 链上部署最终状态（D11 末）

```
合约：LifecycleTrace = 0xf71701365b8b35d4a03a12ecc51edf5fd5797b08
        (三层继承全字节码：RoleManager + BatteryRegistry + LifecycleTrace)

链上 7 个角色 (链上 Role enum 值)：
  admin1     = 0x6933f6d76d71b7ca66f70f3faf6b108a10697aa2 → 1 ADMIN
  producer1  = 0x501d135cc0c493ea423b5799bea95d2b1bd55d8c → 2 PRODUCER
  distrib1   = 0x133dc9f28eb0da0d595104659d4d686e1fdd294d → 3 DISTRIBUTOR
  retailer1  = 0x7a02799727b975cdbde2a8b443b5b6ea7b24c0ce → 4 RETAILER
  merchant1  = 0x4cae790a0f2393c37008147c937411720aac510f → 5 MERCHANT
  consumer1  = 0x8385dd8e88641e7e50914369f9ab4cfcb65fc623 → 6 CONSUMER
  recycler1  = 0xe3ca5516bb6cfac3faaf1883ddf987c7d9ee5eb9 → 7 RECYCLER

链下数据库：
  nev_contract_config 表：1 行（LifecycleTrace + abi 6746 字节）
  sys_nev_user_ext 表：6 个 demo 用户 wallet_address 全部回填真链地址（user_id 101-106）

WeBASE-Front 5002 本地私钥库：
  admin1（已导入，0x6933...），其他 6 个用户私钥还在 WeBASE-Web 5000
  D12 起按需导出导入（producer1 调 registerBattery 时必须）
```



| # | 任务 | 结果 |
|---|---|---|
| 1 | 新建聚合模块 `backend/nev-modules/pom.xml`（packaging=pom，声明 nev-blockchain 子模块） | ✓ |
| 2 | 新建业务模块 `backend/nev-modules/nev-blockchain/pom.xml`（依赖 ruoyi-common-core/web/mybatis/json/log） | ✓ |
| 3 | 根 `backend/pom.xml` 注册新聚合模块（modules 段加 nev-modules） | ✓ |
| 4 | `ruoyi-admin/pom.xml` 引入 nev-blockchain 依赖（让启动类扫描到本模块的 @Component） | ✓ |
| 5 | 5 个核心类：`BlockchainProperties`（@ConfigurationProperties("nev.blockchain")）+ `ChainCallResult`（@Builder DTO）+ `ContractAddressResolver`（缓存 ABI + 占位地址）+ `ContractInvoker`（Spring 6 RestClient 调 WeBASE-Front HTTP）+ `BlockchainAutoConfiguration`（@ComponentScan 入口） | ✓ |
| 6 | application-dev.yml 追加 `nev.blockchain.*` 配置段（webaseFrontUrl=http://localhost:5002, groupId=1, contracts.LifecycleTrace.address 占位 0x000...） | ✓ |
| 7 | `mvn install -pl nev-modules/nev-blockchain -am -T 4` 编译通过（2.7 秒） | ✓ |
| 8 | `mvn -q clean install -DskipTests -T 4` 全量编译通过（34 模块 = 原 33 + nev-blockchain） | ✓ |

### 2.15 D10 关键发现

- **不引区块链 SDK**：老仓 NEV/2026037462 用 webase-app-sdk + OkHttp；本仓改用 **Spring 6 RestClient**（零额外依赖），原因：RuoYi 全局已用 Spring MVC + Jackson，RestClient 是同栈最佳选择；webase-app-sdk 在本仓 BOM 里也没管理，引入要额外加版本号 + 解决依赖冲突。
- **不生成 web3j Java wrapper**：老仓也没生成，直接走 WeBASE-Front 的 ABI 反射接口（`/trans/handleWithSign`、`/trans/query-transaction`），运行时根据 nev_contract_config 表的 ABI 动态调用任意方法，灵活性远高于 wrapper。
- **HTTP 路径划分**：写链走 `/trans/handleWithSign`（WeBASE-Sign 自动签名）；读链走 `/trans/query-transaction`（不消耗 gas，无 txHash）。`ContractInvoker.invoke()` vs `query()` 两个方法对应。
- **WeBASE-Front 响应格式宽松解析**：`ContractInvoker.parseResponse()` 优先看 `code` → 退回看 `statusOK` → 再退回看 `error`，避免不同 WeBASE 版本字段差异。
- **ContractAddressResolver 分两阶段实现**：D10 用内存 + properties 占位（registerAbi 启动期注入）；D11 部署后扩展查 nev_contract_config 表，优先级：表 > properties。
- **@ConfigurationProperties + AutoConfiguration 模式**：`BlockchainAutoConfiguration` 通过 `@EnableConfigurationProperties(BlockchainProperties.class)` + `@ComponentScan("com.nev.blockchain")` 自动装配，无需 ruoyi-admin 显式 import，只要 ruoyi-admin pom 依赖 nev-blockchain 就生效。
- **nev-modules 命名约定**：与 ruoyi-modules 并列，未来加 nev-battery / nev-marketplace / nev-carbon 都进这里，保持 ruoyi-* 前缀不变作为对上游脚手架的鸣谢。



| # | 任务 | 结果 |
|---|---|---|
| 1 | 检查环境：WeBASE-Front 5002 / WeBASE-Web 5000 在跑；Docker `ethereum/solc:0.6.10` 镜像本地已有（12.6MB） | ✓ |
| 2 | 选用 Docker 编译方案（不污染本地 + 不依赖 WeBASE 编译流程） | ✓ |
| 3 | 修复 LifecycleTrace.sol 中非法 NatSpec tag：`@SaCheckRole` → `Sa-Token SaCheckRole`（solc 0.6.10 严格 NatSpec 解析） | ✓ |
| 4 | `MSYS_NO_PATHCONV=1 docker run --rm -v <ctx>:/sources ethereum/solc:0.6.10 --bin --abi --optimize --overwrite -o /sources/build /sources/LifecycleTrace.sol` 一行编译 | ✓ |
| 5 | 编译产物（contracts/build/）：RoleManager .bin 2.8KB / BatteryRegistry .bin 9.3KB / **LifecycleTrace .bin 14.7KB** + 3 个 .abi | ✓ |
| 6 | LifecycleTrace.abi 验证：18 个函数（addEvent/registerBattery/grantRole/revokeRole/hasRole/verify*/get*）+ 4 个事件（BatteryRegistered/LifecycleEventAdded/RoleGranted/RoleRevoked） | ✓ |
| 7 | 验证了"三层继承部署一份"设计：LifecycleTrace.bin 字节码已聚合三个合约的所有方法 | ✓ |

### 2.13 D9 关键发现

- **Git Bash 路径转换坑**：默认会把 `/sources` 改写成 `C:/Program Files/Git/sources`，导致 docker 容器内找不到挂载点。解决：命令前加 `MSYS_NO_PATHCONV=1`。
- **solc 0.6.10 NatSpec 严格**：不允许 `@xxx` 形式的非法 tag（只接受 @title/@dev/@param/@return/@notice/@author）。注释里引用业务代码标识符（如 @SaCheckRole）必须去掉 @ 或用反引号包裹。
- **优化标志**：加 `--optimize` 后 LifecycleTrace.bin 从约 18KB 降到 14.7KB（约 20% 字节码瘦身），未来部署 gas 也省。
- **Docker solc 速度**：本地有镜像时编译 < 5 秒，比 `npm i -g solc` 安装快得多。
- **build/ 产物入 git**：编译产物仅 ~50KB，提交进 git 可作 D11 部署直接源；后续 D10 web3j 生成 Java wrapper 也以这些 .abi/.bin 为输入。



| # | 任务 | 结果 |
|---|---|---|
| 1 | 读老仓 `E:/Study/IdeaProjects/NEV/2026037462/.../contracts/*.sol`（3 个合约 + 设计文档）作底座 | ✓ |
| 2 | 写 `contracts/RoleManager.sol`：enum Role 8 项（含 NONE）覆盖新仓 7 角色制 + grantRole/revokeRole/hasRole + 5 个 modifier | ✓ |
| 3 | 写 `contracts/BatteryRegistry.sol`：继承 RoleManager，用 `string traceNumber` 作 mapping key（对齐 nev_battery.trace_number），仅存 dataHash + producer + producedAt + exists 4 字段 | ✓ |
| 4 | 写 `contracts/LifecycleTrace.sol`：继承 BatteryRegistry，enum EventType 6 项（PRODUCED/IN_USE/SOLD/REPAIRED/RECYCLED/DISMANTLED），AppendOnly + 自增 version | ✓ |
| 5 | 写 `docs/contracts/design.md`：三层继承图 + 角色映射表 + 事件映射表 + 后端调用契约 + keccak256 输入规约 + 与老仓差异表 + 毕设扩展位 | ✓ |
| 6 | 3 个合约均预留毕设扩展位（多签 / ZK / Merkle 批量 / 事件回滚），仅注释不实现 | ✓ |
| 7 | 用 `block.timestamp` 替代 `now`（消除编译告警，二者等价） | ✓ |
| 8 | event 同时 emit `string indexed`（哈希后供过滤）+ `string`（明文供后端） | ✓ |

### 2.11 D8 关键发现

- **三层继承部署 1 份**：部署最终的 `LifecycleTrace` 一个合约即可，字节码自带 RoleManager + BatteryRegistry 所有方法。老仓已验证可行。
- **`string` 作 mapping key 在 FISCO BCOS 可用**：虽然 gas 略贵于 uint256，但溯源场景 trace_number 本就是业务唯一编号，链上链下 ID 完全对齐，省后端映射工作。
- **`string indexed` 字段会被 keccak256 哈希**：indexed 后无法还原明文，所以同时 emit 一个非 indexed 的 string，后端订阅事件 callback 解析明文。
- **enum 在 ABI 中暴露为 uint8**：后端 web3j wrapper 调用时传 `BigInteger.valueOf(2)` 对应 `PRODUCER`，不能直接传 Java 枚举名。
- **链上权限粒度策略**：链上只校验"调用者有任意有效角色 + 电池已注册 + 哈希非空"；"哪个角色能触发哪个事件"的精细判断放后端 RuoYi `@SaCheckRole` 层（更易迭代，无需改链）。
- **keccak256 输入规约必须固定**：字段顺序 + 分隔符 + 时间格式 + JSON canonical 全部写死在 `docs/contracts/design.md §5`，后续任何修改都会让历史 verify 失效（必须重新刷链）。
- **老仓用 SHA256 新仓用 keccak256**：keccak256 是 EVM 原生操作码（gas 极便宜，36 + 6/word），且 web3j `Hash.sha3()` 直接生成，无需引入 SHA256 库。


- **登录请求体格式**：`clientId` 必须在 body 中（不是 header），值为 `sys_client` 表的 `client_id` 哈希值
- **正确登录 payload**：
  ```json
  {
    "clientId": "e5cd7e4891bf95d1d19206ce24a7b32e",
    "grantType": "password",
    "username": "admin",
    "password": "admin123",
    "tenantId": "000000"
  }
  ```
- **受保护接口调用**：需要 `Authorization: Bearer <token>` + `clientid: <client_id>` 两个 header
- **MySQL 端口**：最终用 13306（避开老仓 WeBASE 23306 和默认 3306）
- **Redis 隔离**：共用 6379 端口，通过 `database=1` 与老仓隔离
- **SQL 导入**：必须加 `--default-character-set=utf8mb4`，否则中文昵称超长报错

---

## 3. 治理上下文（canonical vibe）

### 3.1 已完成的 vibe sessions

| Session ID | Stage | 触发命令 | 产物 |
|---|---|---|---|
| `20260513T143556Z-2a16103a` | requirement_doc（停止） | `$vibe` | 老仓 NEV/docs/requirements/2026-05-13-* |
| `20260514T134803Z-053c7f3a` | requirement_doc（停止） | `$vibe` 重新做需求分析 | docs/requirements/2026-05-14-*.md |
| `20260514T142916Z-fec5c2d3` | xl_plan（停止） | `$vibe-how` 排执行计划 | docs/plans/2026-05-14-*-execution-plan.md |

### 3.2 vibe runtime 已知 bug（必须知道）

> ⚠️ `phase_cleanup` 阶段（即一气推到 `$vibe` 完整流程的最后阶段）有 PowerShell 桥工件持久化 bug，会在 `Wait-VibeArtifactSet` 处崩溃。
>
> **应对**：分段跑 `$vibe-want`（停在 requirement_doc）→ `$vibe-how`（停在 xl_plan）→ `$vibe-do`（plan_execute），不一气推到 phase_cleanup。

### 3.3 canonical 启动命令模板（Windows PowerShell）

```powershell
cd "C:/Users/Administrator/.claude/skills/vibe"
$env:PYTHONPATH = "C:/Users/Administrator/.claude/skills/vibe/apps/vgo-cli/src"
py -3 -m vgo_cli.main canonical-entry `
  --repo-root "C:\Users\Administrator\.claude\skills\vibe" `
  --artifact-root "E:\Study\IdeaProjects\NEV-v2" `   # ⭐ 切到新仓
  --host-id "claude-code" `
  --entry-id "vibe-do-it" `                           # 可选：vibe / vibe-what-do-i-want / vibe-how-do-we-do / vibe-do-it / vibe-upgrade
  --prompt "<keyword intent text>" `
  --requested-stage-stop plan_execute                 # 或 requirement_doc / xl_plan / phase_cleanup
```

⚠️ **关键**：`--artifact-root` 必须传 `E:\Study\IdeaProjects\NEV-v2`（新仓），不能传老仓。

---

## 4. 关键决策一览（已锁定，不要重新讨论）

### 4.1 技术栈

| 项 | 值 | 备注 |
|---|---|---|
| JDK | **21**（LTS） | 已升级，pom.xml 第 20 行 + Dockerfile 第 2-3 行 |
| Spring Boot | **3.5.14** | 跟随 RuoYi-Vue-Plus 5.6.1 默认 |
| 框架 | **RuoYi-Vue-Plus 5.6.1**（Dromara 社区版） | 不用官方 RuoYi-Vue3，不用 RuoYi-Vue 经典 |
| 鉴权 | **Sa-Token 1.45.0** | 不用 Spring Security |
| ORM | **MyBatis-Plus 3.5.16** | 不用裸 MyBatis，不写 XML（除非必要） |
| 缓存 | **Redis + Redisson 3.52.0** | 自带分布式锁、限流、多级缓存 |
| 数据库 | **MySQL 8.0+** 单一主存 | 不用 MongoDB（老仓用 MongoDB，新仓不要） |
| API 文档 | **SpringDoc 2.8.17** | 自动生成 Swagger |
| 区块链 SDK | **webase-app-sdk 1.5.5** | FISCO BCOS via WeBASE-Front |
| Solidity | **0.6.10** | 升级自老仓的 0.4.25 |
| 前端 | Vue3 + TS + Element Plus + Vite（plus-ui） | Wave 4 D22 克隆 |
| 用户端 | uni-app（H5 + 小程序） | Wave 4 D22 从老仓 user-app-v2 复制 |

### 4.2 包名 / artifactId

| 项 | 值 |
|---|---|
| Java 包名根 | `com.nev` |
| Maven groupId | `com.nev` |
| 顶级 artifactId | `nev-backend` |
| 业务子模块（待建） | `nev-battery` / `nev-marketplace` / `nev-carbon` / `nev-blockchain` |
| 自带子模块（保留 ruoyi- 前缀作鸣谢） | `ruoyi-admin` / `ruoyi-common-*` / `ruoyi-system` / ... |
| 启动类 | `com.nev.NevApplication` |

### 4.3 端口规划（避开老仓冲突）

| 服务 | 老仓 NEV | 新仓 NEV-v2 |
|---|---|---|
| Backend API | 9180 | **9280** |
| Admin Web | 8020 | **8120** |
| User App (H5) | 5173 | **5273** |
| MySQL | 23306（WeBASE 用） | **13306** |
| Redis | 6379 | 6379（共用，database=1 隔离） |
| WeBASE | 5000-5004 | 共用，但合约地址不同 |

### 4.4 角色体系（7 角色完整产业链）

| 角色 ID | 角色名 | 上链事件 |
|---|---|---|
| `admin` | 系统管理员 | （全权） |
| `producer` | 电池生产商 | PRODUCED |
| `distributor` | 经销商 | IN_USE |
| `retailer` | 零售商 / 4S 店 | SOLD |
| `merchant` | 商城商家 | — |
| `consumer` | 终端消费者 | — |
| `recycler` | 回收处理商 | RECYCLED |

### 4.5 数据库表前缀规范

| 类型 | 前缀 | 例子 |
|---|---|---|
| RuoYi 自带 | `sys_*` | `sys_user` `sys_role` `sys_menu` |
| NEV 业务表 | `nev_*` | `nev_battery` `nev_order` `nev_carbon_footprint` |
| RuoYi 扩展（在 sys 上加业务字段） | `sys_nev_*` | `sys_nev_user_ext` |

### 4.6 RESTful 路径分流（按角色）

```
/api/public/**         任意端（免鉴权 + 限速）
/api/admin/**          管理员（admin 角色）
/api/producer/**       生产商
/api/distributor/**    经销商
/api/retailer/**       零售商
/api/merchant/**       商城商家
/api/consumer/**       消费者
/api/recycler/**       回收商
/api/blockchain/**     链上接口（多角色共用）
```

### 4.7 智能合约设计（3 合约 / 6 事件）

| 合约 | 职责 |
|---|---|
| `RoleManager.sol` | 7 角色 + 钱包地址映射 |
| `BatteryRegistry.sol` | 电池数字身份 + dataHash |
| `LifecycleTrace.sol` | 6 事件 + 版本号 + AppendOnly |

事件：`PRODUCED / IN_USE / SOLD / REPAIRED / RECYCLED / DISMANTLED`

**毕设阶段**预留扩展位（本轮不实现）：多签、零知识证明、Merkle 批量上链。

---

## 5. 已确定的行为约定 / feedback memory

> 这些是和用户聊出来的、应该长期遵循的约定。新对话框开始前 Claude 必须知道。

### 5.1 语言
- ✅ **始终用中文回答**

### 5.2 治理流程
- ✅ 重大动作前必须走 canonical vibe（不简化、不跳过）
- ✅ `$vibe-want` → 需求 / `$vibe-how` → 计划 / `$vibe-do` → 执行
- ✅ 不一气推到 phase_cleanup（vibe runtime 有 bug）
- ✅ canonical 启动后必须有四件证据（host-launch-receipt、runtime-input-packet、governance-capsule、stage-lineage）才算真正进入

### 5.3 仓库管理
- ✅ 老仓 NEV **保留不归档**，可启动作对照
- ✅ 老仓任何文件视为只读，不允许写入
- ✅ 不删 RuoYi-Vue-Plus 自带模块，只在配置里关闭不用的（workflow / demo / extend/*）
- ✅ 子模块保留 `ruoyi-*` 前缀作为对上游脚手架的鸣谢

### 5.4 数据
- ✅ **不迁老仓数据**（用户明确说"都不用了 没用了"）
- ✅ Demo seed 重新编写 SQL 灌入，不复用老仓 MongoDB JSON

### 5.5 工期
- ✅ 1 个月内必须有可演示版本
- ✅ **R1 应急砍范围预案**已授权：D27 末跑不通则砍 distributor/retailer/recycler 专属页面用 admin 代演

### 5.6 合约创新
- ✅ 本轮**不加**合约创新点（多签 / 零知 / Merkle），毕设阶段再加
- ✅ 本轮合约设计**直接参照** `E:/Study/IdeaProjects/NEV/2026037462/` 的三层结构

### 5.7 不做
- ❌ 决策引擎 / 召回 / 梯次 / 再制造（毕设阶段）
- ❌ ClickHouse / Elasticsearch / Kafka
- ❌ K8s / CI/CD
- ❌ 真实支付接入
- ❌ 完整测试覆盖（仅 happy path）
- ❌ admin-app（老仓的实验性应用，不做）
- ❌ knowledge_articles 知识文章

---

## 6. 踩过的坑（避免新对话框重复踩）

### 6.1 bash 脚本 `dirname` 层级错误

**场景**：物理移动 `org/dromara/*` → `com/nev/*` 时，用了错误的 dirname 层级，导致目录被移到 `org/com/nev/*`（多套了一层 org）。

**正确写法**：

```bash
find . -type d -path "*/org/dromara" | while read d; do
  # d = "./xxx/src/main/java/org/dromara"
  src_root=$(dirname $(dirname "$d"))   # ✓ 得到 src/main/java（要 dirname 两次）
  mkdir -p "$src_root/com"
  mv "$d"/* "$src_root/com/nev/"
  rmdir "$d" "$src_root/org" 2>/dev/null
done
```

教训：bash 写完后**先 echo 检查路径**，再真执行。

### 6.2 包名替换边界情况

**陷阱**：
- 第三方依赖也用 `org.dromara` 开头（`org.dromara.sms4j`、`org.dromara.warm`、`org.dromara.snail-job`），**不能一刀切替换**
- `package org.dromara;`（启动类裸 package）正则要单独处理
- `mapperPackage: org.dromara.**.mapper` 这种带通配符的也要单独处理
- `logging.level.org.dromara` 这种独立行也要单独处理
- pom.xml 注释和 issue 模板（`gitee.com/dromara/...` URL）**不应替换**

**正确策略**：
```bash
# 精确正则：只匹配 org.dromara.{本工程包根之一}
PATTERN='org\.dromara\.(common|demo|generator|job|monitor|snailjob|system|test|web|workflow)\b'
find . -name "*.java" -print0 | xargs -0 perl -i -pe "s/$PATTERN/com.nev.\$1/g"
```

### 6.3 git mv 自动检测

**经验**：`mv` + `git add -A` 后 git 会自动识别为 rename。提交时显示 `renamed: A → B`，diff stat 是对称的 insertions/deletions。

### 6.4 Maven `revision` 占位符

RuoYi-Vue-Plus 用 `<version>${revision}</version>`，实际版本在 `<properties>` 中的 `<revision>5.6.1</revision>`。改版本要改 properties，不是改 version 标签。

### 6.5 RuoYi-Vue-Plus 自带 JDK 21 注释行

Dockerfile 第 2-3 行：
```dockerfile
#FROM bellsoft/liberica-openjdk-rocky:17.0.16-cds
FROM bellsoft/liberica-openjdk-rocky:21.0.8-cds
```

RuoYi 作者已预留 JDK 21 镜像，注释互换即可，零风险。

### 6.6 老仓 docs 已有同日 vibe 产物

老仓 `NEV/docs/requirements/` 下已有 `2026-05-13-*` 和 `2026-05-14-*` 文件。这些是历史 vibe sessions 产物，**仍然有用**（作为新仓产物的元数据来源），但新对话框应该读新仓 `NEV-v2/docs/` 的版本。

---

## 7. 接下来要做（Wave 3 D15 起）

### 7.1 紧接 Wave 3 D15-D21（推荐下一步）

Wave 2 已完成（tag `wave-2-end`），进入 **Wave 3：Marketplace + Carbon**。按执行计划：

1. **D15-D17：nev-marketplace 模块**
   - 新建 `backend/nev-modules/nev-marketplace` 子模块
   - 商家上下架商品（nev_product 表）
   - 购物车 / 订单 / 支付（mock 支付）
   - 商品可选绑定 battery_id 实现"电池即商品"
   - merchant / consumer 角色端点

2. **D18-D20：nev-carbon 模块**
   - 新建 `backend/nev-modules/nev-carbon` 子模块
   - **碳计算引擎**：基于 nev_emission_factor（D5-D6 已灌 15 条）+ nev_battery_spec
   - 5 阶段（RAW/MFG/TRANS/USE/EOL）碳足迹自动计算 → 写 nev_carbon_footprint + nev_carbon_stage
   - 老仓 `E:/Study/IdeaProjects/NEV/backend/src/main/java/nev/service/CarbonCalculationService.java` 算法参考
   - 碳积分账户：consumer 完成订单/换新自动扣减/加积分

3. **D21：以旧换新（trade-in）+ Wave 3 收尾**
   - 复用 nev_trade_in_request 表
   - consumer 提交换新申请 → recycler 评估 → 完成

### 7.2 历史 Wave 完成情况

～～Wave 1 D1-D7 全部交付～～ ✓（tag wave-1-end）
～～Wave 2 D8 三合约设计～～ ✓（tag wave-2-d8-contracts）
～～Wave 2 D9 合约编译～～ ✓（tag wave-2-d9-compiled）
～～Wave 2 D10 nev-blockchain 模块～～ ✓（tag wave-2-d10-blockchain-module）
～～Wave 2 D11 链上部署+授权～～ ✓（tag wave-2-d11-deployed）
～～Wave 2 D12 producer 注册电池～～ ✓（tag wave-2-d12-battery-register）
～～Wave 2 D13 distributor/retailer/recycler 事件～～ ✓（tag wave-2-d13-lifecycle-events）
～～Wave 2 D14 公开扫码端点 + Wave 2 收尾～～ ✓（tag wave-2-end）

### 7.3 后续 Wave 概要

| Wave | Day | 主题 | 关键产物 |
|---|---|---|---|
| W2 | 8-14 | Battery + Blockchain | 3 合约部署 + nev-battery + nev-blockchain 模块 |
| W3 | 15-21 | Marketplace + Carbon | 商城闭环 + 碳核算 + 以旧换新 + 碳积分 |
| W4 | 22-30 | Frontend + Polish | admin-web（plus-ui）+ user-app + 文档 + 演示彩排 |

详见 `docs/plans/2026-05-14-*-execution-plan.md`。

---

## 8. 还没拍板、未来要决定的事

1. **WeBASE 部署策略**：和老仓共用（沿用 5000-5004）还是独立部署一套？目前默认共用，新合约部署后地址写入 `nev_contract_config` 表区分。
2. **是否真启用多租户**：RuoYi-Plus 自带多租户，本轮默认关闭，毕设阶段可能启用。
3. **OSS 选型**：本轮可能用本地存储（`/upload` 路径），生产用 MinIO 或阿里云 OSS。RuoYi-Plus 自带 OSS 模块。
4. **i18n 中英文**：默认中文，预留接口但不实做。
5. **测试覆盖率门槛**：本轮只做核心 service happy path，未设强制覆盖率。

---

## 9. 关键文件位置索引

### 9.1 治理产物（必读）

```
docs/requirements/2026-05-14-requirement-analysis-greenfield-rebuild-new-repo-nev-v2-ruoyi-vu.md
docs/plans/2026-05-14-execution-plan-xl-wave-nev-v2-ruoyi-vue-plus-mysql-sa-token-myba-execution-plan.md
docs/reference/README.md                                        ← 老仓 + 参考项目索引
docs/legacy/analysis/2026-05-13-nev-vs-2026037462-comparison.md ← 对比分析报告
```

### 9.2 老仓关键参考（绝对路径，只读）

```
E:/Study/IdeaProjects/NEV/backend/src/main/java/nev/service/CarbonCalculationService.java
    → 碳计算 5 阶段公式（GHG / GB-T 24067），Wave 3 D20 移植

E:/Study/IdeaProjects/NEV/backend/src/main/java/nev/service/OrderService.java
    → 订单状态机（PENDING→PAID→SHIPPED→DELIVERED→COMPLETED），Wave 3 D17 移植

E:/Study/IdeaProjects/NEV/backend/src/main/java/nev/contract/TraceContract.java
    → WeBASE-Front HTTP 调用封装思路，Wave 2 D11 重写

E:/Study/IdeaProjects/NEV/apps/user-app-v2/
    → uni-app 13 页面，Wave 4 D22 复制到新仓 apps/user-app/

E:/Study/IdeaProjects/NEV/webase-deploy/ + WeBASE/ + docker/
    → FISCO BCOS 部署，Wave 2 沿用
```

### 9.3 比赛参考项目关键参考（绝对路径，只读）

```
E:/Study/IdeaProjects/NEV/2026037462/2026037462-02素材与源码/链上溯源——基于区块链的动力电池智溯源分析平台源代码与素材.zip
    → 解压后 btld/btld-btld/src/main/java/com/zy/btld/contracts/
    → 3 层 Solidity 合约（RoleManager / BatteryRegistry / LifecycleTrace），直接照搬设计

E:/Study/IdeaProjects/NEV/2026037462/2026037462-03设计与开发文档/链上溯源——基于区块链的动力电池智溯源分析平台作品文档.pdf
    → 完整作品文档（6.1 MB），包含 ER 图、流程图、表结构
```

### 9.4 新仓代码结构（Wave 1 D1 完成后）

```
backend/
├── pom.xml                          ← groupId=com.nev, artifactId=nev-backend, JDK 21
├── ruoyi-admin/
│   ├── pom.xml
│   ├── Dockerfile                   ← JDK 21
│   └── src/main/
│       ├── java/com/nev/
│       │   ├── NevApplication.java          ← 启动类
│       │   ├── NevServletInitializer.java
│       │   └── web/                         ← 登录/验证码/Index 控制器
│       └── resources/
│           ├── application.yml              ← logging / mybatis-plus 已改 com.nev
│           ├── application-dev.yml          ← ⚠️ D2 要改端口和数据库
│           └── application-prod.yml         ← ⚠️ D2 要改端口和数据库
├── ruoyi-common/                    ← 24 个子模块，全部 com.nev.common.*
├── ruoyi-modules/                   ← 5 个业务模块（system/generator/job/workflow/demo）
└── ruoyi-extend/                    ← 2 个扩展（monitor-admin/snailjob-server）

script/sql/
└── ry_vue_5.X.sql                   ← RuoYi 自带建库 SQL，D2 导入
```

---

## 10. 新对话框第一步检查清单

新对话框启动后，让 Claude 做以下检查，确认上下文已就位：

```bash
# 1. 验证新仓位置
ls -la "E:/Study/IdeaProjects/NEV-v2/"
# 应有：backend/ docs/ README.md HANDOFF.md .gitignore .git/

# 2. 验证 git 历史
cd "E:/Study/IdeaProjects/NEV-v2" && git log --oneline
# 应看到 4 个 commits，最新是 wave-1-d1-renamed 标签

# 3. 验证编译可通过（必跑一次）
cd "E:/Study/IdeaProjects/NEV-v2/backend" && mvn -pl ruoyi-admin -am compile -T 4 -q
# 应输出 BUILD SUCCESS

# 4. 验证老仓和参考项目仍存在
ls "E:/Study/IdeaProjects/NEV/" | head
ls "E:/Study/IdeaProjects/NEV/2026037462/" | head

# 5. 读必读文档
# - HANDOFF.md（本文档）
# - docs/requirements/2026-05-14-*.md
# - docs/plans/2026-05-14-*-execution-plan.md
# - docs/reference/README.md
```

完成上述检查后，Claude 应该说："已就位，可以从 Wave 1 D2 继续。"

---

## 11. FAQ

**Q1：新对话框里 Claude 还会自动用 vibe 治理流程吗？**
A：会，但你需要明确指令（`$vibe-do` 或者在开场白里要求 Claude 遵循治理流程）。Claude 不会自动开启 vibe，必须显式触发。

**Q2：如果新对话框里 Claude 不认识 `$vibe`，怎么办？**
A：检查 `~/.claude/CLAUDE.md` 是否包含 vibe 引导文本。如果没有，参考本仓 `docs/legacy/` 里的 vibe session 路径，让 Claude 阅读 `SKILL.md` 后理解协议。

**Q3：能不能跳过 vibe 治理直接干活？**
A：你可以选择跳过，但**重大决策**（架构、数据库改动、合约设计）建议走 vibe，避免后悔。**纯执行性任务**（按已定计划改代码、调试 bug）可以省略治理。

**Q4：vibe runtime 启动失败怎么办？**
A：见本文档 §3.2。已知 phase_cleanup 阶段有 bug，分段跑即可。如启动完全失败，把 `--artifact-root` 改为 NEV-v2 绝对路径，确保不传 vibe 安装目录。

**Q5：老仓的某个文件我能不能复制到新仓？**
A：可以但要明示。原则：
- 业务模型设计（字段、状态机）→ 看不抄，用 RuoYi 风格重写
- 算法公式（碳计算）→ 直接复制函数体
- 配置（端口、密码）→ 不复制
- 测试数据 → 转 SQL 格式后用
- WeBASE 部署脚本 → 直接复制到 `deploy/blockchain/`

**Q6：如果新对话框里 Claude 修改了老仓文件怎么办？**
A：立刻 `git -C "E:/Study/IdeaProjects/NEV/" diff` 检查 + 还原。老仓必须保持只读。本约定写在 §5.3。

---

## 12. 联系点

- **新对话框开场白**：见本文档 §0
- **最新进度查询**：`cd "E:/Study/IdeaProjects/NEV-v2" && git log --oneline | head -10`
- **下一步任务**：本文档 §7.1（Wave 1 D2）
- **遇到问题先读**：本文档 §6（踩过的坑）

---

> 本文档由 Claude 在 vibe session `20260514T142916Z-fec5c2d3` 之后的执行阶段编写。
> 作为新对话框的"零损失续接"入口。
> 当 Wave 1 D2 完成后，本文档应更新 §2、§7 章节，添加 D2 完成清单。
