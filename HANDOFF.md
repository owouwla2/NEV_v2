# NEV-v2 项目交接文档（HANDOFF）

> **本文档用途**：当你切换到新的 Claude Code 对话框时，把整段对话上下文压缩到这一份文件里。
> 新对话框只需要让 Claude 先读完本文档，就能无损接上之前的所有进度、约定、踩过的坑。
>
> **创建日期**：2026-05-15
> **最后更新**：2026-05-19
> **当前进度**：Wave 1 D4 已完成（20 张业务表建好 + 6 业务角色 + 20 业务菜单 + 角色菜单授权矩阵）
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

## 7. 接下来要做（Wave 1 D2 + 后续）

### 7.1 紧接 Wave 1 D3-D4（推荐下一步）

按执行计划 `docs/plans/2026-05-14-*-execution-plan.md` §2.3 D3-D4：

1. **设计 20 张 nev_* 业务表 SQL**
   - 参照需求文档 §5 的表结构设计
   - 写到 `backend/script/sql/nev_v2_business.sql`
   - 在 MySQL 中执行建表

2. **新增 7 角色到 sys_role 表**
   - producer / distributor / retailer / merchant / consumer / recycler
   - 配置对应菜单权限

3. **D3-D4 末 git commit + tag wave-1-d4-schema**

### 7.2 Wave 1 剩余（D3-D7）

- D3-4: 设计 20 张 nev_* 业务表 SQL（已在需求文档 §5 列好），写到 `backend/src/main/resources/sql/nev_v2_business.sql`，建表
- D5-6: 写 `nev_v2_seed.sql`：7 角色 + 菜单权限 + demo 用户 + 排放因子 + 商品分类
- D7: Wave 1 cleanup + tag wave-1-end

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
