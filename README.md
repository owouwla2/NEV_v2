# NEV-v2

> 基于区块链的新能源汽车动力电池全产业链溯源平台
> 5 角色端到端闭环 · 链上链下双写校验 · GB-T 24067 碳足迹 · 公开扫码溯源

**状态**：🏆 首版交付完成（tag `wave-4-end`）— 后端 38 模块 + 前端 12 业务页 + 5 角色 e2e 全验证通过

---

## 一句话介绍

电池从出厂到回收，**6 类生命周期事件**全部上链 FISCO BCOS 主链；MySQL 双写并以链上 `verifyEvent()` 反向校验；商城下单完成自动触发 `SOLD` 上链 + 按 GB-T 24067 给消费者发放碳积分；以旧换新由 recycler 评估、consumer 接受后自动写 `RECYCLED`；任何持有溯源码的人通过 `/scan/:trace` 公开页可查全链路时间线 + 5 阶段碳足迹。

---

## 技术栈（实际版本）

### 后端

| 组件 | 版本 | 备注 |
|---|---|---|
| Java | 21 | LTS |
| Spring Boot | 3.5.14 | 基于 RuoYi-Vue-Plus 5.6.1 |
| 鉴权 | Sa-Token 1.45.0 | 双 header：Authorization + clientid |
| ORM | MyBatis-Plus 3.5.16 | |
| 数据库 | MySQL 8.0.42 | 单一主存，雪花 ID（JSON 序列化为 String 防精度丢失） |
| 缓存 | Redis 7.4.1 + Redisson | `database=1` 与老仓物理隔离 |
| 区块链调用 | WeBASE-Front 5002 `/trans/handle` | Front 本地签名，`user` 字段传钱包地址 |
| 哈希 | BouncyCastle bcpkix-jdk18on | Keccak-256（dataHash 一致性校验） |
| API 文档 | SpringDoc OpenAPI | 注解补全留答辩前 |

### 前端（admin-web）

| 组件 | 版本 | 备注 |
|---|---|---|
| 构建 | Vite 8 + pnpm | |
| 框架 | Vue 3.5 + TypeScript | Composition API + `<script setup>` |
| UI | **shadcn-vue (New York / zinc)** | New York 风格、zinc 灰阶；**Wave 4 关键决策**：放弃 plus-ui 全量重写 |
| 样式 | Tailwind CSS 4 | |
| 基础组件 | Reka UI | shadcn-vue 底层 |
| 图标 | lucide-vue-next | |
| 状态 | Pinia + pinia-plugin-persistedstate | user 持久化到 localStorage |
| 路由 | Vue Router 4 | `import.meta.glob` 动态注册 RuoYi 服务端路由 |
| HTTP | axios | 401 自动跳登录；R{code,msg,data} 自动 unwrap |

### 区块链

| 组件 | 版本 | 备注 |
|---|---|---|
| FISCO BCOS | 2.9.1 | 4 节点 PBFT（沿用老仓集群） |
| WeBASE | 全套 | front:5002 / node-mgr:5001 / web:5000 / sign:5004 |
| Solidity | 0.6.10 | 3 合约通过继承合并部署，地址 `0xf71701365b8b35d4a03a12ecc51edf5fd5797b08` |

---

## 智能合约

3 个 `.sol` 通过继承链合并为 **1 次部署**：

```
LifecycleTrace.sol  is  BatteryRegistry  is  RoleManager
└─ 6 EventType: PRODUCED / IN_USE / SOLD / REPAIRED / RECYCLED / DISMANTLED
```

| 合约 | 职责 | 核心函数 |
|---|---|---|
| `RoleManager` | 7 角色管理 + 钱包地址绑定 | `bindRole(addr,role)` / `getRole(addr)` |
| `BatteryRegistry` | 电池数字身份 + dataHash | `registerBattery(traceNumber,hash)` |
| `LifecycleTrace` | 6 类事件 AppendOnly + 链上版本号 | `addEvent(traceNumber,eventType,dataHash)` / `verifyEvent(traceNumber,version)` / `getEventCount(traceNumber)` |

**校验机制**：MySQL `lifecycle_event` 每条记录保存 `tx_hash + chain_version + data_hash`；前端公开扫码页对每条事件调链 `verifyEvent()` 反验，整体 `overallVerified = (MySQL count == 链上 count) && (每条 verifyEvent == true)`。

---

## 角色 + 业务闭环

```
┌──────────────────────────────────────────────────────────────────┐
│  producer ──register──▶  PRODUCED ─┐                             │
│     │                              │ 上链 + dataHash              │
│     │ transfer                     ▼                             │
│  distributor ──── IN_USE ───────▶ 链上 v=1                        │
│     │                                                            │
│     │ transfer                                                   │
│  retailer ──── IN_USE ──────────▶ 链上 v=2                        │
│                                                                  │
│  merchant 上架商品 ─────────────▶ marketplace.product              │
│     │                                                            │
│     ▼                                                            │
│  consumer 下单 → 支付 → 发货 → 确认 ──┐                            │
│                                       │                          │
│                                       ▼                          │
│                              SOLD 上链 (v=3)                      │
│                              + carbon_credit +1062.5 kgCO2eq      │
│                                                                  │
│  consumer 申请以旧换新 ─────────────▶ trade_in.SUBMITTED            │
│     │                                                            │
│  recycler 评估 SOH + 报价 ──────────▶ trade_in.EVALUATED            │
│     │                                                            │
│  consumer 接受 ─────────────────────▶ RECYCLED 上链 (v=4)           │
│                                       trade_in.COMPLETED          │
└──────────────────────────────────────────────────────────────────┘
```

| 角色 | 主要权限 | 演示账号 | 链上钱包 |
|---|---|---|---|
| `superadmin` | 系统管理 + 手动触发碳计算 | `admin/admin123` | `0x6933...7aa2` |
| `producer` | 电池注册 + dataHash 计算 | `producer1` | `0x501d...5d8c` |
| `distributor` | 接收/中转电池（IN_USE） | `distrib1` | `0x133d...294d` |
| `retailer` | 零售端 IN_USE | `retailer1` | `0x7a02...c0ce` |
| `merchant` | 商城上架 + 订单管理 | `merchant1` | `0x4cae...510f` |
| `consumer` | 下单 + 以旧换新 + 公开扫码 | `consumer1` | `0x8385...c623` |
| `recycler` | 评估 + 拆解 + RECYCLED 上链 | `recycler1` | `0xe3ca...5eb9` |

---

## 后端模块

```
backend/                       # ruoyi-vue-plus 5.6.1 fork
├── ruoyi-admin/               # 启动模块（port 9280）
├── ruoyi-common/              # 通用工具
├── ruoyi-extend/              # 扩展（monitor / oss / sms 等）
├── ruoyi-modules/             # RuoYi 原生业务模块（system / job / generator…）
└── nev-modules/               # ⭐ 本项目业务模块
    ├── nev-blockchain/        # WeBASE HTTP 客户端 + 合约 invoker + 7 角色钱包注册
    ├── nev-battery/           # battery + lifecycle_event + 公开扫码 BatteryScanVO
    ├── nev-marketplace/       # product / cart / order 全流程
    └── nev-carbon/            # GB-T 24067 5 阶段计算 + 碳积分流水
```

**跨模块解耦**：通过 `api/` 包下的接口 + `ObjectProvider` 延迟注入避免循环依赖（如 `CarbonScanEnricher` 接口定义在 nev-battery，实现在 nev-carbon）。

---

## 前端业务页（admin-web 12 个）

```
apps/admin-web/src/views/
├── LoginView.vue              # 登录（dual header）
├── DashboardView.vue          # 首页（按角色显示不同卡片）
├── PublicScanView.vue         # 公开扫码 /scan/:trace（免登录）
├── PlaceholderView.vue        # RuoYi 路由占位
├── battery/                   # producer / distributor / retailer
│   ├── register/index.vue     # 电池注册（触发 PRODUCED 上链）
│   ├── transfer/index.vue     # 流转交付（触发 IN_USE 上链）
│   └── query/index.vue        # 链上链下对比查询
├── marketplace/               # merchant + consumer
│   ├── product/index.vue      # 商品上下架
│   ├── cart/index.vue         # 购物车（带顶栏快捷入口）
│   └── order/index.vue        # 订单 4 态：PAID/SHIPPED/COMPLETED
├── tradein/
│   ├── request/index.vue      # consumer 提交以旧换新
│   └── evaluate/index.vue     # recycler 评估 SOH + 报价
├── recycle/request/index.vue  # 回收单管理
├── carbon/
│   ├── footprint/index.vue    # GB-T 24067 5 阶段明细 + admin 触发计算
│   └── credit/index.vue       # 个人碳积分流水
└── blockchain/trace/index.vue # 链上事件时间线（带 verifyEvent 反验徽章）
```

---

## 目录结构

```
NEV-v2/
├── HANDOFF.md                 # 跨对话交接文档（开发期间持续更新）
├── README.md                  # 本文件
├── backend/                   # Spring Boot 多模块工程
├── apps/
│   └── admin-web/             # Vue3 + shadcn-vue 管理前端
├── contracts/                 # Solidity 0.6.10
│   ├── RoleManager.sol
│   ├── BatteryRegistry.sol
│   ├── LifecycleTrace.sol
│   ├── build/                 # solc 0.6.10 编译产物（bin/abi）
│   ├── address/address.md     # 部署地址 + 7 角色钱包清单
│   └── pk/                    # 7 个钱包 p12（已 gitignore）
├── deploy/
│   └── docker/                # docker-compose.yml（MySQL + Redis）
├── docs/
│   ├── requirements/          # 冻结的需求文档
│   ├── plans/                 # 4 Wave 执行计划
│   ├── architecture/          # ER + 架构图
│   ├── contracts/             # 合约设计说明
│   ├── api/                   # 接口 + 时序图
│   ├── reference/             # 老仓和参考项目索引
│   └── legacy/                # 老仓分析备份
└── outputs/                   # canonical vibe 治理产物（host-launch / packet / capsule…）
```

---

## 端口规划

为避免与老仓 NEV / WeBASE 集群冲突：

| 服务 | 端口 | 说明 |
|---|---|---|
| 后端 API | **9280** | RuoYi `ruoyi-admin` 启动端口 |
| Admin Web | **8120** | Vite dev server（生产构建后由 nginx 接管） |
| MySQL | **13306** | 容器名 `nev-v2-mysql`，库名 `nev_v2` |
| Redis | 6379 | 与老仓共用，业务通过 `database=1` 隔离 |
| WeBASE-Front | 5002 | `/trans/handle` 调用入口（沿用老仓集群） |
| WeBASE-Web | 5000 | 管理后台（沿用老仓集群） |
| FISCO BCOS | group_1 | 4 节点 PBFT（沿用老仓集群） |

---

## 快速启动

> 假设老仓 WeBASE 集群已经跑起来（front:5002 可访问），合约 `0xf71701365b8b35d4a03a12ecc51edf5fd5797b08` 已部署、7 个钱包 p12 已通过 `POST /privateKey/importP12` 导入 WeBASE-Front 本地私钥库。

### 1. 启动基础设施

```bash
cd deploy/docker
docker-compose up -d
# 等到 nev-v2-mysql 健康检查通过（~15s）
```

MySQL 初始化 SQL 会通过 `volumes/mysql/init/*.sql` 自动导入：RuoYi 基础表 + 7 角色 sys_user / sys_role + 业务表（battery / lifecycle_event / product / cart / order / trade_in / carbon_*）。

### 2. 启动后端

```bash
cd backend
mvn -pl ruoyi-admin -am spring-boot:run
# 日志出现 "Started RuoYiApplication" 即就绪，监听 :9280
```

### 3. 启动前端

```bash
cd apps/admin-web
pnpm install
pnpm dev
# 监听 http://localhost:8120，已配 proxy → :9280
```

### 4. 演示登录

| 入口 | 账号 |
|---|---|
| Admin 后台 | `http://localhost:8120` — 用 `admin/admin123` 或上述 6 个角色账号登录 |
| 公开扫码 | `http://localhost:8120/scan/BAT-DEMO-001`（免登录） |

---

## 端到端验证（已跑通）

两条并行链路在 Wave 4 D28 已用 curl 走通：

| 路径 | 触发事件 | 结果 |
|---|---|---|
| **主链** producer→distributor→retailer→consumer→trade-in→recycler | PRODUCED → IN_USE → IN_USE → RECYCLED | 4 事件全部 `chainVerified=true`，`overallVerified=true` |
| **商城** producer→distributor→merchant 上架→consumer 下单 4 态 | PRODUCED → IN_USE → SOLD | 3 事件 `chainVerified=true`，自动发放 `+1062.5 kgCO2eq` 碳积分 |
| **碳足迹** 85 kWh LFP 全生命周期 | 5 阶段（RAW/MFG/TRANS/USE/EOL） | 合计 `80744.05 kgCO2eq`，EOL 段为负（回收抵扣） |

---

## 开发路线（4 Wave / 共 34 天）

| Wave | 日期 | 主题 | tag |
|---|---|---|---|
| Wave 1 | D1–D7 | Foundation：克隆 RuoYi、20 表 schema、7 角色 | `wave-1-end` |
| Wave 2 | D8–D14 | Battery + Blockchain：3 合约编译部署、WeBASE 集成、PRODUCED/IN_USE 上链 | `wave-2-end` |
| Wave 3 | D15–D21 | Marketplace + Carbon + Trade-in：商城 4 态闭环、5 阶段碳计算、以旧换新工作流 | `wave-3-end` |
| Wave 4 | D22–D28 | Frontend：放弃 plus-ui、用 shadcn-vue 全量重写、12 业务页 + 公开扫码 + e2e | `wave-4-end` 🏆 |

每个 D 都有独立 tag（`wave-N-dNN-xxx`），共 29 个里程碑 tag。Wave 4 关键决策详见 `HANDOFF.md §5.8`。

---

## 设计文档

- **需求文档**：`docs/requirements/2026-05-14-requirement-analysis-greenfield-rebuild-new-repo-nev-v2-ruoyi-vu.md`
- **执行计划**：`docs/plans/2026-05-14-execution-plan-xl-wave-nev-v2-ruoyi-vue-plus-mysql-sa-token-myba-execution-plan.md`
- **交接文档**：`HANDOFF.md`（跨对话上下文压缩，**对接维护者必读**）
- **合约地址**：`contracts/address/address.md`
- **参考项目索引**：`docs/reference/README.md`

---

## 答辩前 TODO

- [ ] SpringDoc `@Operation` 注解补全（业务接口约 30 个）
- [ ] ER 图（从 schema SQL 自动生成）
- [ ] 合约设计 PDF（继承结构 + 6 EventType 时序）
- [ ] 演示视频脚本 + 录制（5 角色 e2e 走完 + 公开扫码）

---

## License

MIT（沿用 RuoYi-Vue-Plus 协议）
