# NEV-v2 架构总览

> **状态**：Wave 4 D28（首版交付）后整理
> **作者意图**：让维护者一图看懂模块分层、跨模块解耦机制、链上链下双写校验链路、5 角色端到端业务时序

---

## 1. 模块分层

```
┌───────────────────────────────────────────────────────────────────────────┐
│                            apps/admin-web (Vite + Vue3)                    │
│   shadcn-vue (New York/zinc) + Tailwind 4 + Reka UI + lucide-vue-next      │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │ LoginView · DashboardView · 12 业务页 · PublicScanView（免登录）       │  │
│  │ 路由：vue-router 4，动态注册（import.meta.glob）+ Pinia 持久化         │  │
│  │ HTTP：axios + 双 header（Authorization + clientid hash）              │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────────────┘
                                     │
                                Vite proxy
                                     │ :8120 → :9280
                                     ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                          backend/ruoyi-admin (port 9280)                   │
│              Spring Boot 3.5.14 + Sa-Token 1.45 + MyBatis-Plus 3.5.16      │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │   controller →  service  →  mapper  →  MySQL 8.0.42 (port 13306)     │  │
│  │              ↑           ↑                                            │  │
│  │   Sa-Token 拦截     雪花 ID 自动填充                                   │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
│                                                                            │
│  ┌──────────── nev-modules（业务） ─────────────┐                          │
│  │ nev-battery     │ nev-marketplace            │                          │
│  │ nev-carbon      │ nev-blockchain（基础设施）  │                          │
│  └────────────────────────────────────────────┘                          │
└───────────────────────────────────────────────────────────────────────────┘
        │                                              │
        │ keccak256(dataHash)                          │ 业务读写
        ▼                                              ▼
┌──────────────────────────────────┐         ┌──────────────────────────┐
│ FISCO BCOS group_1 (4 节点 PBFT) │         │  MySQL 8 (nev-v2-mysql)  │
│ via WeBASE-Front 5002            │         │  redis (db=1)            │
│                                  │         │                          │
│ LifecycleTrace                   │         │ 业务表：                  │
│   ┌──────────────────────────┐   │         │ - battery                │
│   │ BatteryRegistry          │   │         │ - lifecycle_event        │
│   │   ┌──────────────────┐   │   │         │ - product / cart / order │
│   │   │ RoleManager      │   │   │         │ - trade_in               │
│   │   └──────────────────┘   │   │         │ - carbon_footprint /     │
│   └──────────────────────────┘   │         │   carbon_credit          │
│ 部署地址 0xf717...7b08            │         └──────────────────────────┘
└──────────────────────────────────┘
```

---

## 2. nev-modules 业务分层

| 模块 | 职责 | 关键 service | 链上调用 |
|---|---|---|---|
| `nev-blockchain` | WeBASE-Front HTTP 客户端 + ABI 解析 + 7 角色钱包注册启动器 | `ContractInvoker`, `ContractRegistryBootstrap`, `DataHashCalculator` | 提供原语：`call/sendTx` |
| `nev-battery` | 电池数字身份 + 全生命周期事件 + 公开扫码（BatteryScanVO） | `BatteryService`（含 register / transfer / scan） | PRODUCED / IN_USE / RECYCLED |
| `nev-marketplace` | 商品 / 购物车 / 订单 4 态 + 触发 SOLD + 发碳积分 | `OrderService.confirm()` | SOLD |
| `nev-carbon` | GB-T 24067 5 阶段计算 + 个人碳积分流水 | `CarbonCalculatorService`, `CarbonCreditService` | 仅读，不写链 |

---

## 3. 跨模块解耦机制

为避免 `nev-battery ↔ nev-carbon` 等模块互相 `compile` 依赖造成循环，采用 **api 接口包 + `ObjectProvider` 延迟注入**：

```
nev-battery
  ├── service/BatteryService.scan(traceNumber) 
  │       └── carbonEnricher.enrich(vo)  ← 可选注入，没有就跳过
  └── api/CarbonScanEnricher.java         ← 只有接口定义

nev-carbon
  └── service/CarbonScanEnricherImpl.java implements CarbonScanEnricher
        └── @Component（Spring 启动时注册）

ObjectProvider<CarbonScanEnricher>  ← 由 Spring 解析；
                                       不强制要求 nev-carbon 在 classpath
```

**好处**：

- `nev-carbon` 撤掉时 `nev-battery` 仍可编译运行（碳数据不再附加，但扫码主流程正常）
- 跨模块字段扩展无需修改主模块（开闭原则）

同模式应用于：

- `BatteryService.recordSoldByMerchant` ← `OrderService.confirm` 调
- `BatteryService.recordRecycledByTradeIn` ← 以旧换新 service 调
- `CarbonCreditService.awardFromOrder` ← `OrderService.confirm` 调

---

## 4. 链上链下双写 + 校验机制

```
┌────────────── 写路径（任一上链事件） ───────────────┐
│ 1. service 准备业务字段（事务 begin）              │
│ 2. dataHash = keccak256(field1|field2|...|fieldN) │
│    │                                              │
│    └──→ BouncyCastle Keccak.Digest256              │
│                                                    │
│ 3. ContractInvoker.send(addEvent, ..., dataHash)  │
│    │                                              │
│    └──→ POST /trans/handle  (Front 本地签名)        │
│         返回 tx_hash + block_number + return     │
│                                                    │
│ 4. MySQL 插入 lifecycle_event(                    │
│       trace_number, event_type, operator_role,    │
│       data_hash, tx_hash, chain_version, ...)     │
│ 5. 事务 commit（任一步失败整体回滚 + 不进 chain）   │
└────────────────────────────────────────────────────┘

┌────────────── 读路径（公开扫码 /scan/:trace） ──────┐
│ 1. 读 MySQL battery + lifecycle_event[]            │
│ 2. 链上读 LifecycleTrace.getEventCount(trace)      │
│    → chainEventCount                               │
│ 3. 对每条事件：                                     │
│    LifecycleTrace.verifyEvent(trace, version)      │
│    → bool chainVerified                            │
│ 4. overallVerified =                               │
│        chainEventCount == MySQL.length             │
│     && 每条 chainVerified == true                  │
│ 5. 前端按 overallVerified 显示 绿/红 大徽章         │
└────────────────────────────────────────────────────┘
```

**为什么是"链上链下双写"而非"链上一份**"：

- 业务查询全部走 MySQL（链查询慢、贵、不支持复杂条件）
- 链上只存最小校验信息（trace + eventType + dataHash + version），证伪可信
- MySQL 任何字段被篡改都会让 `keccak256(...)` 与链上 `dataHash` 对不上，扫码页一眼看出

---

## 5. 5 角色端到端业务时序

```
seq actors  producer  distributor  retailer  merchant  consumer  recycler  Chain
                                                                          ===========
   1. register(spec) ───────────────────────────────────────────────────► PRODUCED v=1
                                                                          [dataHash=H1]

   2.            transfer ──────────────────────────────────────────────► IN_USE v=2
                                                                          [dataHash=H2]
   3.                       transfer ──────────────────────────────────► IN_USE v=3
                                                                          [dataHash=H3]

   4. 商城路径：                                  
                                       merchant.upload(product) ────────► (off-chain)
                                                          
                                                consumer.order ──┐
                                                  PENDING→PAID──┤
                                                  →SHIPPED ────┤
                                                  →COMPLETED ──┘────────► SOLD v=4
                                                                          [dataHash=H4]
                                                                          + carbon_credit +X

   5. 以旧换新路径：
                                                consumer.submit ─────────► trade_in.SUBMITTED
                                                          
                                                          recycler.evaluate (SOH+price)
                                                                    ─────► trade_in.EVALUATED
                                                consumer.accept ─────────► RECYCLED v=5
                                                                          [dataHash=H5]
                                                                          trade_in.COMPLETED
```

---

## 6. 数据精度跨语言处理

雪花 ID 是 19 位 Long，JS Number 仅安全到 2^53 (~16 位)。直接 JSON 序列化会丢精度，导致前端拿到的 ID 与后端不一致。

**方案**：在 Jackson 全局把 `Long` 序列化为 `String`：

```
@JsonSerialize(using = ToStringSerializer.class)
private Long batteryId;
```

或全局配置 `JsonComponent + SimpleModule` 注册 Long→String 序列化器。

**前端约定**：所有 ID 字段（`battery.id`, `order.id` 等）一律按字符串处理，比较用 `===` 字符串等值。

---

## 7. 鉴权链路（Sa-Token 双 header）

```
浏览器 → axios 拦截器：
   Authorization: Bearer <satoken>
   clientid: e5cd7e4891bf95d1d19206ce24a7b32e   ← 写死的设备指纹

后端 → Sa-Token 拦截器：
   1. 校验 clientid 是否匹配（不匹配 → 401）
   2. 解析 satoken → 拿到 userId + roleKey[]
   3. @SaCheckRole("producer") 决定能否进 controller
```

- 登录返回的 `satoken` 是 hash，不是 JWT；服务端在 Redis（db=1）做映射
- 401 由 axios 响应拦截器统一捕获 → `router.push('/login')`
- 公开扫码端点 `/scan/:trace` 在 `nev_unauth_paths` 白名单内，免鉴权

---

## 8. 配置 / 启动顺序依赖

```
docker-compose up -d        # MySQL + Redis 起来
        │
        ▼
backend mvn spring-boot:run
        │
        ├── Spring Boot 启动
        ├── Sa-Token 初始化（连 Redis db=1）
        ├── MyBatis-Plus 启动（连 MySQL）
        ├── ContractRegistryBootstrap.run()
        │     ├── 读 nev_contract_config → 拿合约地址
        │     ├── 注册 7 钱包到 WeBASE-Front 本地私钥库（幂等）
        │     └── 把 admin1 加为链上 ADMIN（幂等）
        └── ready @ :9280
        │
        ▼
admin-web pnpm dev          # :8120，proxy /api → :9280
```

**幂等保证**：`ContractRegistryBootstrap` 在重启时检查链上 `getRole(addr)` 已 ≠ NONE 就跳过 `grantRole`，重启 1000 次不会反复发送授权交易。

---

## 9. 后续扩展位

| 类别 | 当前 | 可扩展 |
|---|---|---|
| 合约 | 单 `LifecycleTrace` 部署 | 多签 `addEventMultiSig` / ZK `verifyOwnershipZk` / Merkle 批量 |
| 业务模块 | 4 个 nev-modules | 维修工单 / 召回 / 梯次利用 / 拆解 4 个毕设阶段补 |
| 前端 | admin-web 单端 | 答辩前补充 H5 公开扫码独立部署，免下载即扫即查 |
| 数据 | MySQL 单写 | 可加 ClickHouse 做事件流分析（参考 2026037462） |
| 国际化 | 仅中文 | i18n 接口已预留，毕设阶段补英文 |
