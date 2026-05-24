# 参考项目索引

> 本目录记录开发 NEV-v2 时可参考的外部项目位置和关键参考点。
> 这些项目**不在新仓内**，按需用绝对路径读取参考。

---

## 1. 老仓 NEV（前一代实现）

**位置**：`E:/Study/IdeaProjects/NEV/`

**状态**：保留不归档，可继续启动作为对照参考。

**关键参考点**：

| 模块 | 路径 | 用途 |
|---|---|---|
| 业务模型设计 | `NEV/backend/src/main/java/nev/model/` | 16 个 MongoDB 实体的字段设计参考 |
| 碳计算公式 | `NEV/backend/src/main/java/nev/service/CarbonCalculationService.java` | GHG / GB-T 24067 全生命周期碳足迹核算公式（直接照搬） |
| 订单状态机 | `NEV/backend/src/main/java/nev/service/OrderService.java` | PENDING→PAID→SHIPPED→DELIVERED→COMPLETED 状态转移逻辑 |
| 区块链调用封装 | `NEV/backend/src/main/java/nev/contract/TraceContract.java` | WeBASE-Front HTTP 调用封装思路 |
| ~~用户端 H5~~ | ~~`NEV/apps/user-app-v2/`~~ | **Wave 4 决策：放弃复制**。改由 admin-web 顶部按 `consumer` 角色显示购物车快捷入口 + 独立公开扫码页 `/scan/:trace` 替代 |
| WeBASE 部署 | `NEV/webase-deploy/` `NEV/WeBASE/` `NEV/docker/` | FISCO BCOS 部署脚本和 Docker compose（Wave 2 沿用） |
| Demo seed JSON | `NEV/backend/src/test/resources/demo/mall-demo.seed.json` | 24 用户/8 商家/72 商品/40 订单（参考字段值，Wave 1 转换为 SQL） |

**注意事项**：
- 老仓和新仓**不能同时连同一个 MongoDB**（端口冲突）—— 新仓不用 MongoDB，无问题
- 老仓和新仓**可以共用 WeBASE 部署**，但部署的合约地址不同（新仓用新地址）
- 老仓 admin-web 用 8020 端口，新仓用 8120，避免冲突
- 老仓 backend 用 9180 端口，新仓用 9280，避免冲突

---

## 2. 比赛参考项目 2026037462（链上溯源平台）

**位置**：`E:/Study/IdeaProjects/NEV/2026037462/`

**状态**：参赛作品归档，源码 zip 在 `2026037462-02素材与源码/链上溯源——基于区块链的动力电池智溯源分析平台源代码与素材.zip`。

**项目类型**：RuoYi 3.9.1 二开 + Spring Boot 3.5 + JDK 17 + MySQL + ClickHouse + Solidity 0.6.10

**关键参考点**：

| 模块 | 路径（zip 解压后） | 用途 |
|---|---|---|
| 三层合约设计 | `btld/btld-btld/src/main/java/com/zy/btld/contracts/` | RoleManager + BatteryRegistry + LifecycleTrace 三层结构（**直接照搬**） |
| 区块链调用封装 | `btld/btld-btld/src/main/java/com/zy/btld/client/ContractInvoker.java` | WeBASE-Front 调用封装（参考思路，用 Sa-Token + MyBatis-Plus 重写） |
| 区块链初始化 | `btld/btld-btld/src/main/java/com/zy/btld/config/BlockchainInitializer.java` | 启动时连接 WeBASE 的初始化逻辑 |
| Hash 服务 | `btld/btld-btld/src/main/java/com/zy/btld/service/impl/HashService.java` | 链下→链上 SHA-256 dataHash 校验思路 |
| 业务表结构 | `btld/btld-btld/src/main/resources/mapper/btld/*.xml` | 25+ 业务表的字段设计（**仅参考字段命名**，本项目用 MyBatis-Plus 不用 XML） |
| 决策引擎 | `btld/btld-btld/src/main/java/com/zy/btld/controller/DecisionEngineController.java` 等 | 毕设阶段移植参考，本轮不做 |
| 召回 / 梯次 / 再制造 | `btld/btld-btld/src/main/java/com/zy/btld/controller/Battery*Controller.java` | 毕设阶段移植参考，本轮不做 |

**已落档的对比分析**：见 `docs/legacy/analysis/2026-05-13-nev-vs-2026037462-comparison.md`

**注意事项**：
- 2026037462 的代码**不能直接复制**到新仓（基于 RuoYi 3.9.1 + 裸 MyBatis，不兼容 RuoYi-Vue-Plus 5.X 的 MyBatis-Plus）
- 只参考**设计思路**和 **Solidity 合约源码**（合约与 Java 栈无关，可直接拿来改造）
- 文档（PDF 等）在 `2026037462/2026037462-03设计与开发文档/`，毕设阶段可深入阅读

---

## 3. RuoYi-Vue-Plus 官方仓库（脚手架）

**位置**：`https://github.com/dromara/ruoyi-vue-plus`（远程，需 git clone）

**版本**：`5.X` 分支

**关键参考点**：

| 内容 | 说明 |
|---|---|
| 后端脚手架 | Wave 1 D1 克隆到 `NEV-v2/backend/` |
| 文档 | https://plus-doc.dromara.org/ （Context7 ID: `/dromara/plus-doc`） |
| Sa-Token 用法 | `@SaCheckPermission("nev:xxx:yyy")` |
| MyBatis-Plus 用法 | `LambdaQueryWrapper`、`@TableField`、自动填充 |

---

## 4. ~~plus-ui~~ → shadcn-vue（前端方案变更）

**Wave 4 D22 关键决策（2026-05-24）**：原计划基于 `javalionli/plus-ui`（Element Plus）改造，实测后发现 plus-ui 与 RuoYi-Vue-Plus 5.6.1 配套度差、强行接入会引入大量适配工作；改为 **shadcn-vue 从零搭建**，并把工期顺延 +4 天（D22-D30 → D22-D34）。

**实际方案**：

| 项 | 选择 | 备注 |
|---|---|---|
| 基础组件 | `shadcn-vue`（New York 风格 / zinc 灰阶） | CLI `pnpm dlx shadcn-vue@latest add <component>` 按需生成 |
| 底层 | Reka UI | shadcn-vue 的 headless 底层 |
| 样式 | Tailwind CSS 4 | |
| 图标 | `lucide-vue-next` | shadcn-vue CLI 默认生成 `@lucide/vue` 错误导入名，须手动 `sed` 替换 |
| 菜单/角色/字典/部门 | **自建 + 复用 RuoYi 服务端路由** | 用 `import.meta.glob('@/views/**/*.vue')` 动态注册 |

**为什么不沿用 RuoYi-Vue-Plus 自带前端**：原版用 Element Plus + 老布局，比赛+毕设场景需要更符合 shadcn 设计语言的现代界面。

**`Wave 4 设计源`**：`HANDOFF.md §5.8 + §7.3`

---

## 使用约定

1. **绝对路径优先**：跨仓引用必须用绝对路径（`E:/Study/IdeaProjects/NEV/...`），不用相对路径
2. **不修改老仓**：老仓任何文件都视为只读，不允许写入
3. **不引用 zip 内容**：参考项目 zip 内的代码已在分析报告里被摘录，需要再深入时手动解压
4. **保留出处**：从老仓或参考项目复制思路时，在新仓代码注释里写明 `参考: NEV/.../xxx.java` 或 `参考: 2026037462/.../yyy.java`
