# NEV-v2

> 基于区块链的新能源汽车动力电池全产业链溯源平台
> RuoYi-Vue-Plus 5.X + Spring Boot 3 + Sa-Token + MyBatis-Plus + MySQL + FISCO BCOS + Vue3 + uni-app

---

## 项目背景

本项目是 **NEV 老仓**（`E:/Study/IdeaProjects/NEV/`）的**绿地重建版本**。

- **立项性质**：比赛作品 + 后期改造为毕业设计
- **作者**：区块链工程专业学生
- **工期**：1 个月内完成首版交付
- **质量门槛**：毕设可推演（高于 demo，低于生产）

老仓基于 MongoDB + 自研 Spring Boot 单模块，新仓推倒重建为 RuoYi-Vue-Plus 多模块 + MySQL + Sa-Token + MyBatis-Plus。

---

## 技术栈

### 后端

| 组件 | 版本 | 说明 |
|---|---|---|
| Java | 21 | LTS |
| Spring Boot | 3.x | 来自 RuoYi-Vue-Plus 5.X |
| 鉴权 | Sa-Token | 替代 Spring Security |
| ORM | MyBatis-Plus | 替代裸 MyBatis |
| 数据库 | MySQL 8.0+ | 单一主存 |
| 缓存 | Redis 7.x + Redisson | 分布式锁 + 限流 |
| API 文档 | SpringDoc OpenAPI | 自动生成 Swagger |
| 区块链 SDK | webase-app-sdk 1.5.5 | FISCO BCOS via WeBASE-Front |

### 前端

| 组件 | 版本 | 说明 |
|---|---|---|
| Admin Web | Vue 3 + TS + Element Plus + Vite | 基于 plus-ui |
| User App | uni-app + Vue 3 + Pinia | H5 + 微信小程序，从老仓 user-app-v2 复制 |

### 区块链

| 组件 | 版本 | 说明 |
|---|---|---|
| FISCO BCOS | 2.9.1 | 4 节点 PBFT |
| WeBASE | 全套 | front:5002 / node-mgr:5001 / web:5000 / sign:5004 |
| Solidity | 0.6.10 | 升级自老仓 0.4.25 |

---

## 端口规划

为避免与老仓 NEV 冲突，新仓使用独立端口：

| 服务 | 老仓 NEV | 新仓 NEV-v2 |
|---|---|---|
| 后端 API | 9180 | **9280** |
| Admin 前端 | 8020 | **8120** |
| User H5 | 5173 | **5273** |
| MySQL | 23306 (WeBASE) | **6306** |
| Redis | 6379 | 6379（共用） |
| WeBASE | 5000-5004 | 共用 |

---

## 业务模块（4 大块）

| 模块 | 命名 | 说明 |
|---|---|---|
| 电池溯源 | `nev-battery` | 电池数字身份 + 全生命周期事件 + 二维码 + 扫码查询 |
| 商城 | `nev-marketplace` | 商品/购物车/订单/支付（模拟）/以旧换新 |
| 碳核算 | `nev-carbon` | 5 阶段碳足迹（GHG / GB-T 24067）+ 碳积分流水 |
| 区块链 | `nev-blockchain` | 3 合约调用封装 + 7 角色注册 + dataHash 校验 |

---

## 角色体系（7 角色完整产业链）

| 角色 | 职责 |
|---|---|
| `admin` | 系统管理 |
| `producer` | 电池生产商 |
| `distributor` | 经销商 |
| `retailer` | 零售商 / 4S 店 |
| `merchant` | 商城商家 |
| `consumer` | 终端消费者 |
| `recycler` | 回收处理商 |

---

## 智能合约（3 合约 + 6 事件）

| 合约 | 职责 |
|---|---|
| `RoleManager.sol` | 7 角色管理 + 钱包地址映射 |
| `BatteryRegistry.sol` | 电池数字身份 + dataHash |
| `LifecycleTrace.sol` | 6 类生命周期事件 + 版本号 + AppendOnly |

**事件类型**：`PRODUCED / IN_USE / SOLD / REPAIRED / RECYCLED / DISMANTLED`

毕设阶段预留扩展位：多签接口、零知识证明、Merkle 批量上链。

---

## 目录结构

```
NEV-v2/
├── backend/                    # Spring Boot 多模块工程（Wave 1 D1 克隆 ruoyi-vue-plus）
├── apps/
│   ├── admin-web/              # Vue3 管理前端（Wave 4 克隆 plus-ui）
│   └── user-app/               # uni-app H5（Wave 4 从老仓复制）
├── solidity/                   # Solidity 0.6.10 合约工程（Wave 2 创建）
├── deploy/
│   ├── docker/                 # docker-compose.yml（MySQL + Redis）
│   └── blockchain/             # WeBASE 部署脚本（沿用老仓）
└── docs/
    ├── requirements/           # 冻结的需求文档
    ├── plans/                  # XL 执行计划
    ├── architecture/           # ER 图 + 架构图
    ├── contracts/              # 合约设计说明
    ├── api/                    # API 文档 + 时序图
    ├── reference/              # 老仓和参考项目位置索引
    └── legacy/                 # 老仓的分析报告备份
```

---

## 开发路线（4 Wave / 30 天）

详见 `docs/plans/2026-05-14-execution-plan-xl-wave-nev-v2-ruoyi-vue-plus-mysql-sa-token-myba-execution-plan.md`

| Wave | 日期 | 主题 |
|---|---|---|
| Wave 1 | Day 1-7 | Foundation：后端骨架 + 20 表 + 7 角色 |
| Wave 2 | Day 8-14 | Battery + Blockchain：3 合约 + nev-battery + nev-blockchain |
| Wave 3 | Day 15-21 | Marketplace + Carbon：商城闭环 + 碳核算 + 以旧换新 |
| Wave 4 | Day 22-30 | Frontend + Polish：admin-web + user-app + 文档 + 演示彩排 |

---

## 治理来源

本项目由 canonical vibe 治理流程冻结：
- **需求文档**：`docs/requirements/2026-05-14-requirement-analysis-greenfield-rebuild-new-repo-nev-v2-ruoyi-vu.md`
- **执行计划**：`docs/plans/2026-05-14-execution-plan-xl-wave-nev-v2-ruoyi-vue-plus-mysql-sa-token-myba-execution-plan.md`
- **参考项目索引**：`docs/reference/README.md`

---

## 启动指南（占位）

> Wave 4 完成后填充完整启动流程。当前仅初始化目录结构。

```bash
# 1. 启动基础设施（Wave 1 D2）
cd deploy/docker
docker-compose up -d

# 2. 启动后端（Wave 1 D2）
cd ../../backend
mvn spring-boot:run

# 3. 启动 Admin 前端（Wave 4 D22）
cd ../apps/admin-web
pnpm install && pnpm dev

# 4. 启动 User 前端（Wave 4 D22）
cd ../user-app
pnpm install && pnpm dev:h5
```

---

## License

MIT（沿用 RuoYi-Vue-Plus 协议）
