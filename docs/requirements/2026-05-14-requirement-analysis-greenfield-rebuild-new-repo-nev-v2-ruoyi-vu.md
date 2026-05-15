# NEV-v2 需求文档（绿地重建）

> **治理来源**：canonical vibe session `20260514T134803Z-053c7f3a`
> **bounded stop**：`requirement_doc`
> **创建日期**：2026-05-14
> **状态**：FROZEN（冻结，后续执行回溯到本文档）
> **替代文件**：本文件取代 vibe runtime 自动生成的关键词占位版

---

## 0. 元信息

| 项 | 值 |
|---|---|
| 项目代号 | **NEV-v2** |
| 仓库位置 | `E:/Study/IdeaProjects/NEV-v2/`（待创建，新仓） |
| 老仓状态 | `E:/Study/IdeaProjects/NEV/` 保留，不归档，可继续启动作参考 |
| 立项性质 | 比赛作品（首要交付） + 后期改造为毕业设计 |
| 作者身份 | 区块链工程专业学生（个人独立开发） |
| 工期 | **1 个月内**完成首版交付 |
| 质量门槛 | **毕设可推演**（高于 demo，低于生产） |

---

## 1. Goal（目标）

构建一个**基于 RuoYi-Vue-Plus 全新栈**、**MySQL 单一数据源**、**FISCO BCOS 区块链溯源**的**新能源汽车动力电池全产业链平台**，覆盖**生产→流通→零售→消费→回收**完整生命周期，1 个月内完成可演示版本，且数据模型、合约设计、文档结构能直接作为毕业设计的素材底座。

**一句话**：NEV-v2 = RuoYi-Vue-Plus 后端 + plus-ui 管理前端 + uni-app 用户端 + Solidity 0.6.10 三层合约 + 7 角色完整产业链 + 4 业务模块（电池溯源 / 商城 / 碳核算 / 区块链）。

---

## 2. Deliverable（交付物）

### 2.1 代码交付

- 全新仓库 `NEV-v2/`，包含：
  - `backend/`：基于 RuoYi-Vue-Plus 5.X 的 Spring Boot 3 多模块工程
  - `apps/admin-web/`：基于 plus-ui 的 Vue3 + TS + Element Plus 管理前端
  - `apps/user-app/`：从老仓 `user-app-v2/` 复制改造的 uni-app H5
  - `solidity/contracts/`：3 份 Solidity 0.6.10 合约
  - `deploy/`：MySQL/Redis docker-compose + WeBASE 部署脚本

### 2.2 文档交付

- 本文档（`docs/requirements/`）
- 执行计划（`docs/plans/`，下一轮 `vibe-how` 产出）
- 数据库 ER 图 + 表字典（`docs/architecture/`）
- API 文档（SpringDoc 自动生成 + 关键流程时序图）
- 合约设计说明（`docs/contracts/`）
- README + 启动指南

### 2.3 演示交付

- 4 块业务全部可演示能跑通：
  - 商城交易闭环
  - 区块链溯源（含上链验证）
  - 碳核算与碳积分
  - 扫码溯源

---

## 3. Stakeholders & Roles（干系人与角色）

### 3.1 系统外部干系人

| 干系人 | 关心点 |
|---|---|
| 学生（开发者本人） | 1 个月内能演示 + 后期可拓为毕设 |
| 比赛评委 | 业务完整性、技术亮点、能否现场演示 |
| 毕业指导老师（未来） | 创新点、查重率、文档完整性 |

### 3.2 系统内部用户角色（**7 角色完整产业链**）

| 角色 ID | 角色名 | 主要职责 | 涉及业务模块 |
|---|---|---|---|
| `admin` | 系统管理员 | 用户/角色/菜单/字典/审计/合约配置 | RuoYi 自带 + 全模块 |
| `producer` | 电池生产商 | 创建电池数字身份、登记生产信息、上链 PRODUCED 事件 | 电池溯源 / 区块链 |
| `distributor` | 经销商 | 接收电池、登记流通信息、上链 IN_USE / TRANSFERRED 事件 | 电池溯源 / 区块链 |
| `retailer` | 零售商 / 4S 店 | 销售给消费者、关联消费者身份、上链 SOLD 事件 | 电池溯源 / 区块链 / 商城 |
| `merchant` | 商城商家 | 上架电池/配件商品、处理订单、提供以旧换新 | 商城 / 碳核算 |
| `consumer` | 终端消费者 | 扫码查询、购买商品、查碳积分、提交以旧换新 | 全模块（C 端入口） |
| `recycler` | 回收处理商 | 接收以旧换新电池、登记回收信息、上链 RECYCLED 事件 | 电池溯源 / 区块链 |

**注**：MVP 阶段 7 角色全部建立 `sys_role` + 菜单权限，但**每个角色专属业务页面只做核心 1-2 个**，深度由毕设阶段补全。

### 3.3 角色权限矩阵（MVP 范围）

| 资源 | admin | producer | distributor | retailer | merchant | consumer | recycler |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| 系统管理 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| 创建电池 | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| 登记流通 | ✅ | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ |
| 商品上架 | ✅ | ❌ | ❌ | ✅ | ✅ | ❌ | ❌ |
| 下单购买 | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ |
| 扫码查询 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 登记回收 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| 上链事件 | ✅ | PRODUCED | IN_USE | SOLD | — | — | RECYCLED |
| 碳积分查询 | ✅ | — | — | — | — | ✅ | — |

权限通过 Sa-Token 注解 `@SaCheckPermission("nev:xxx:yyy")` 实现。

---

## 4. Functional Requirements（功能需求）

### 4.1 模块 A：电池溯源（nev-battery）

**必做功能**：
| 功能 | 角色 | 描述 |
|---|---|---|
| 创建电池数字身份 | producer | 录入电池基本信息（型号、序列号、生产日期、容量、电压等），自动生成 `trace_number` 唯一编码 + 二维码 |
| 登记生产信息 | producer | 录入电芯供应商、模组结构、BMS 信息等，触发 PRODUCED 上链 |
| 登记流通信息 | distributor / retailer | 接收电池、登记物流、交付凭证，触发 IN_USE / SOLD 上链 |
| 登记回收信息 | recycler | 接收以旧换新电池、登记拆解处置方案，触发 RECYCLED 上链 |
| 扫码溯源 | 所有角色 + 免登录 | 通过 `trace_number` 或二维码 URL 查询全生命周期 |
| 电池列表 / 详情 | admin / 各产业角色 | 分页查询、按状态筛选 |

### 4.2 模块 B：商城（nev-marketplace）

**必做功能**：
| 功能 | 角色 | 描述 |
|---|---|---|
| 商品上架 | merchant / retailer | 录入电池/配件商品，关联 trace_number（仅电池类） |
| 商品列表 / 详情 | consumer | 分类浏览、搜索、查看 |
| 购物车 | consumer | 加购、修改数量、删除 |
| 下单 | consumer | 选商品、选地址、生成订单、订单状态机（PENDING → PAID → SHIPPED → DELIVERED → COMPLETED；分支 CANCELLED / REFUNDED） |
| 支付（模拟） | consumer | 模拟支付（直接调用确认接口，不接真实支付网关） |
| 以旧换新 | consumer + recycler | 用户提交旧电池 trace_number → recycler 审核 → 抵扣新订单 |
| 订单管理 | merchant | 商家查看自己订单、确认发货 |

### 4.3 模块 C：碳核算（nev-carbon）

**必做功能**：
| 功能 | 角色 | 描述 |
|---|---|---|
| 全生命周期碳足迹核算 | admin / 各产业角色 | 5 阶段（原材料 / 制造 / 运输 / 使用 / 回收）排放因子计算，遵循 GHG / GB-T 24067 |
| 排放因子库管理 | admin | 录入/维护原料、能源、运输等排放因子 |
| 碳足迹查询 | consumer | 扫码后能看到该电池的碳足迹明细 |
| 碳积分奖励 | consumer | 完成"以旧换新"、"购买高碳排电池替代品"等行为给予碳积分 |
| 碳积分记录 | consumer / admin | 积分明细、累计余额 |

**简化**：本轮不做碳交易市场、碳积分商城兑换（毕设阶段补）。

### 4.4 模块 D：区块链（nev-blockchain）

**必做功能**：
| 功能 | 描述 |
|---|---|
| 合约部署 | admin 触发部署 3 合约到 WeBASE-Front，配置写入 `nev_contract_config` 表 |
| 角色注册上链 | admin 在 `RoleManager` 合约里注册 7 角色 + 链上钱包地址 |
| 生命周期事件上链 | 每次状态变更（PRODUCED / IN_USE / SOLD / RECYCLED）后端自动调用 `LifecycleTrace.addEvent()` |
| 数据完整性校验 | 链下 MySQL 与链上 dataHash 双向校验 API |
| 链上数据查询 | 通过 trace_number 反查链上所有事件 |
| 交易回执查询 | 查询某次上链的 txHash、blockNumber |

**合约设计**：参考 2026037462 三层结构 + 6+ 事件：
- `RoleManager.sol`：7 角色管理 + 钱包地址映射
- `BatteryRegistry.sol`：电池数字身份 + dataHash
- `LifecycleTrace.sol`：6 类事件（PRODUCED / IN_USE / SOLD / REPAIRED / RECYCLED / DISMANTLED）+ 版本号 + AppendOnly

**本轮不加创新点**（多签 / 零知 / Merkle 批量等），毕设时再加。但 `BlockchainProperties.java` 接口预留可扩展位。

### 4.5 边界外（明确不做）

- ❌ 决策引擎 / 多角色决策
- ❌ 电池召回管理
- ❌ 梯次利用 / 再制造
- ❌ ClickHouse 时序大屏
- ❌ 物料估值 / 价格快照
- ❌ 合规监控
- ❌ 模板字段池 / 动态表单
- ❌ 真实支付网关接入
- ❌ 多租户 / 工作流 / OSS（RuoYi-Plus 自带，关闭不用）

这些放到毕设阶段或后续 vibe 轮次再做。

---

## 5. Data Model（数据模型概览，达到表设计级）

### 5.1 表分组与命名约定

| 分组 | 前缀 | 来源 |
|---|---|---|
| RuoYi 系统表 | `sys_*` | RuoYi-Plus 自带 SQL，不动 |
| NEV 业务表 | `nev_*` | 本项目新建 |
| RuoYi 扩展 | `sys_nev_*` | 在 sys 上加业务字段（如 sys_nev_user_ext） |

### 5.2 业务表清单（共 20 张）

#### 用户与角色扩展（2 张）
| 表名 | 主要字段 | 关系 |
|---|---|---|
| `sys_nev_user_ext` | `user_id (FK sys_user)`, `user_type (enum 7 角色)`, `wallet_address`, `wx_openid`, `phone_verified` | 1:1 sys_user |
| `nev_carbon_credit_account` | `user_id`, `balance`, `frozen` | 1:1 sys_user |

#### 电池溯源（4 张）
| 表名 | 主要字段 | 关系 |
|---|---|---|
| `nev_battery` | `id`, `trace_number (uk)`, `model`, `serial_no`, `capacity_kwh`, `voltage`, `producer_id`, `current_owner_id`, `current_role`, `current_status`, `qr_code_path`, `chain_address` | 主表 |
| `nev_battery_lifecycle` | `id`, `battery_id`, `event_type`, `operator_id`, `operator_role`, `data_hash`, `tx_hash`, `block_number`, `version`, `payload (json)`, `event_time` | N:1 battery（生命周期事件） |
| `nev_battery_spec` | `id`, `battery_id`, `cell_supplier`, `module_structure`, `bms_info`, `safety_cert` | 1:1 battery（详细规格） |
| `nev_certification` | `id`, `battery_id`, `cert_type`, `cert_no`, `issuer`, `issue_date`, `expire_date`, `file_path` | N:1 battery |

#### 商城（6 张）
| 表名 | 主要字段 | 关系 |
|---|---|---|
| `nev_merchant` | `id`, `user_id`, `merchant_name`, `business_license`, `contact`, `status` | 1:1 sys_user (merchant 角色) |
| `nev_product` | `id`, `merchant_id`, `category`, `title`, `price`, `stock`, `battery_id (nullable)`, `images (json)`, `status` | N:1 merchant；可选 1:1 battery |
| `nev_cart` | `id`, `user_id`, `merchant_id` | N:1 sys_user |
| `nev_cart_item` | `id`, `cart_id`, `product_id`, `quantity`, `unit_price` | N:1 cart |
| `nev_order` | `id`, `order_no (uk)`, `user_id`, `merchant_id`, `total_amount`, `status (enum)`, `address_snapshot (json)`, `paid_at`, `shipped_at`, `completed_at` | N:1 sys_user, N:1 merchant |
| `nev_order_item` | `id`, `order_id`, `product_id`, `product_snapshot (json)`, `quantity`, `unit_price` | N:1 order |

#### 支付与地址（2 张）
| 表名 | 主要字段 | 关系 |
|---|---|---|
| `nev_payment_record` | `id`, `order_id`, `payment_no (uk)`, `amount`, `method (enum)`, `status`, `paid_at`, `trade_no` | 1:1 order |
| `nev_address` | `id`, `user_id`, `recipient`, `phone`, `province`, `city`, `district`, `detail`, `is_default` | N:1 sys_user |

#### 以旧换新（1 张）
| 表名 | 主要字段 | 关系 |
|---|---|---|
| `nev_trade_in_request` | `id`, `request_no`, `consumer_id`, `old_battery_id`, `new_product_id`, `evaluated_amount`, `recycler_id`, `status (enum)`, `linked_order_id` | 关联 consumer / recycler / battery / order |

#### 碳模块（3 张）
| 表名 | 主要字段 | 关系 |
|---|---|---|
| `nev_carbon_footprint` | `id`, `battery_id`, `total_co2_kg`, `calc_method`, `calc_time` | 1:1 battery |
| `nev_carbon_stage` | `id`, `footprint_id`, `stage (enum: RAW/MFG/TRANS/USE/EOL)`, `co2_kg`, `breakdown (json)` | N:1 footprint，固定 5 阶段 |
| `nev_emission_factor` | `id`, `factor_code`, `factor_name`, `unit`, `value`, `source`, `applicable_stage` | 排放因子库 |
| `nev_carbon_credit_record` | `id`, `user_id`, `change_amount`, `balance_after`, `reason`, `related_id`, `created_at` | N:1 sys_user（碳积分流水） |

#### 区块链配置（1 张）
| 表名 | 主要字段 | 关系 |
|---|---|---|
| `nev_contract_config` | `id`, `contract_name`, `contract_address`, `abi (text)`, `deploy_block`, `deployed_at`, `network`, `enabled` | 配置表 |

### 5.3 公共字段约定（所有 nev_* 表）

```sql
create_by    VARCHAR(64),
create_time  DATETIME,
update_by    VARCHAR(64),
update_time  DATETIME,
remark       VARCHAR(500),
del_flag     CHAR(1) DEFAULT '0',  -- MyBatis-Plus 逻辑删除
tenant_id    VARCHAR(20) DEFAULT '000000'  -- 预留多租户但本轮不启用
```

### 5.4 关键索引

- `nev_battery.trace_number` 唯一索引
- `nev_battery_lifecycle (battery_id, version)` 复合索引
- `nev_order.order_no` 唯一索引
- `nev_payment_record.payment_no` 唯一索引
- `nev_carbon_credit_record (user_id, created_at)` 复合索引
- `sys_nev_user_ext.wallet_address` 唯一索引（如非空）

---

## 6. Smart Contract Design（合约设计）

### 6.1 三层合约

```solidity
// 1. RoleManager.sol —— 角色管理
contract RoleManager {
    enum Role { ADMIN, PRODUCER, DISTRIBUTOR, RETAILER, MERCHANT, CONSUMER, RECYCLER }
    mapping(address => Role) public roles;
    mapping(address => bool) public registered;
    event RoleAssigned(address indexed account, Role role);
    function assignRole(address account, Role role) external onlyAdmin;
    function hasRole(address account, Role role) external view returns (bool);
}

// 2. BatteryRegistry.sol —— 电池数字身份
contract BatteryRegistry {
    struct Battery {
        string traceNumber;
        bytes32 dataHash;           // 链下电池规格快照的 SHA-256
        address producer;
        uint256 producedAt;
        bool exists;
    }
    mapping(string => Battery) public batteries;  // key: trace_number
    event BatteryRegistered(string indexed traceNumber, bytes32 dataHash, address producer);
    function registerBattery(string memory traceNumber, bytes32 dataHash) external onlyProducer;
    function verifyBattery(string memory traceNumber, bytes32 expectedHash) external view returns (bool);
}

// 3. LifecycleTrace.sol —— 生命周期事件
contract LifecycleTrace {
    enum EventType { PRODUCED, IN_USE, SOLD, REPAIRED, RECYCLED, DISMANTLED }
    struct LifecycleEvent {
        string traceNumber;
        EventType eventType;
        uint256 version;        // 单调递增，AppendOnly
        bytes32 dataHash;
        address operator;
        uint256 timestamp;
    }
    mapping(string => LifecycleEvent[]) public events;  // 每个 trace 一个事件数组
    event EventAdded(string indexed traceNumber, EventType eventType, uint256 version, address operator);
    function addEvent(string memory traceNumber, EventType eventType, bytes32 dataHash) external onlyAuthorizedRole;
    function getEvents(string memory traceNumber) external view returns (LifecycleEvent[] memory);
    function getLatestVersion(string memory traceNumber) external view returns (uint256);
}
```

### 6.2 合约-后端契约

| 后端动作 | 触发合约方法 | 上链数据 |
|---|---|---|
| 创建电池 | `BatteryRegistry.registerBattery` + `LifecycleTrace.addEvent(PRODUCED)` | trace_number + SHA-256(规格 JSON) |
| 分配给经销商 | `LifecycleTrace.addEvent(IN_USE)` | trace_number + SHA-256(流通信息) |
| 卖给消费者 | `LifecycleTrace.addEvent(SOLD)` | trace_number + SHA-256(订单信息) |
| 回收登记 | `LifecycleTrace.addEvent(RECYCLED)` | trace_number + SHA-256(回收方案) |

### 6.3 毕设阶段预留扩展位

- `LifecycleTrace` 加多签接口：`addEventMultiSig(traceNumber, eventType, dataHash, signatures[])`
- `BatteryRegistry` 加零知证明接口：`verifyOwnershipZk(traceNumber, proof)`
- 新增 `BatchTrace.sol`：Merkle 根批量上链

本轮**只预留 interface 注释**，不实现。

---

## 7. RESTful 路径分流方案

```
/api/public/**         任意端，无需鉴权（限速）
   POST /login
   GET  /captcha
   GET  /scan/{traceNumber}     扫码免登录预览
   GET  /battery/{traceNumber}/public  电池公开信息

/api/admin/**          管理后台（admin）
   /system/**          RuoYi 自带（user/role/menu/dept/dict）
   /battery/**         电池管理
   /merchant/**        商家管理
   /order/**           订单管理
   /carbon/**          碳模块管理
   /contract/**        合约部署与配置

/api/producer/**       生产商
   /battery/create     创建电池
   /battery/list-mine  我创建的电池

/api/distributor/**    经销商
   /battery/receive    接收电池
   /battery/transfer   转交电池

/api/retailer/**       零售商 / 4S 店
   /battery/sell       销售给消费者
   /product/manage     商品管理

/api/merchant/**       商城商家
   /product/**         商品管理
   /order/**           接单/发货

/api/consumer/**       消费者
   /scan/**            扫码相关
   /marketplace/**     浏览/购买
   /cart/**            购物车
   /order/**           我的订单
   /trade-in/**        以旧换新
   /carbon/**          我的碳积分
   /profile/**         个人中心

/api/recycler/**       回收处理商
   /trade-in/audit     审核以旧换新
   /battery/recycle    登记回收处置

/api/blockchain/**     链上接口（多角色共用）
   GET  /battery/{traceNumber}/events
   POST /battery/{traceNumber}/verify
   GET  /tx/{txHash}
```

---

## 8. Constraints（约束）

### 8.1 硬约束

- **栈**：必须使用 RuoYi-Vue-Plus 5.X 后端 + plus-ui 前端，不替换
- **数据库**：MySQL 8.0+，**不引入** MongoDB / ClickHouse
- **JDK**：21
- **Spring Boot**：3.x
- **Solidity**：0.6.10
- **工期**：1 个月内
- **作者**：个人独立开发

### 8.2 软约束

- 老仓 NEV 保留不归档，可对照启动
- 4 业务模块必须**全部**有可演示路径
- 7 角色必须**全部**建立，但每角色只做必要业务页面
- 文档必须包含 ER 图、API 文档、合约设计说明，达到表设计级
- 端口规划避开老仓：MySQL 6306 / Redis 6379（可共用）/ backend 9280 / admin-web 8120 / user-app 5273
- 单元测试覆盖：核心 Service 至少有"happy path 测试"，不强求完整覆盖

### 8.3 非功能性需求

| 维度 | 要求 |
|---|---|
| 性能 | 列表分页 < 500ms（10 万条数据规模内）；上链 < 5s |
| 并发 | 单机演示场景，不做高并发优化 |
| 可用性 | 演示期间稳定，可中途重启恢复 |
| 安全 | Sa-Token 鉴权、XSS 过滤、SQL 注入防护（MyBatis-Plus 自带）、JWT Token 30 min |
| 可维护性 | 包结构清晰、注释覆盖核心 Service、commit 信息规范 |
| 国际化 | 中文为主，预留 i18n 但不强做英文翻译 |
| 浏览器 | Chrome / Edge 最新版 + 微信内置 H5 |

---

## 9. Acceptance Criteria（验收标准）

### 9.1 功能验收

每块业务必须能演示以下端到端流程：

**A 电池溯源**：
1. producer 登录 → 创建电池 → 看到二维码 → 数据上链（看到 txHash）
2. consumer 扫码 → 看到电池全生命周期 + 链上验证标识

**B 商城**：
1. merchant 登录 → 上架商品
2. consumer 登录 → 加购 → 下单 → 模拟支付 → 看到订单状态变化
3. merchant 确认发货 → consumer 收货

**C 碳核算**：
1. admin 维护排放因子
2. 创建电池时自动核算碳足迹
3. consumer 扫码看到碳足迹明细
4. consumer 完成以旧换新获得碳积分

**D 区块链**：
1. admin 部署 3 合约 → 配置写入数据库
2. admin 在合约里注册 7 角色钱包地址
3. producer 创建电池触发上链
4. 链上数据可查、可验证

### 9.2 文档验收

- [ ] ER 图（含 20 张业务表）
- [ ] 接口文档（SpringDoc 可访问 + 关键流程时序图）
- [ ] 合约设计说明（3 合约函数列表 + 事件 + 后端调用契约）
- [ ] README 启动指南（一条命令拉起所有服务）

### 9.3 演示验收

- 评委现场操作 4 个核心场景，每个 ≤ 5 分钟，全部跑通
- 任意一环出错可用演示备份数据快速恢复

---

## 10. Product Acceptance Criteria（产品验收）

- 系统启动后能登录、能跑通 7 角色的核心路径
- 所有上链动作有 txHash 反馈，链上数据可查
- 演示数据通过 demo seed SQL 一键导入
- 老仓 NEV 在演示期间可作为对照参考（不强制启动）
- 全部代码与文档可独立上传到 GitHub 公开仓（无敏感信息硬编码）

---

## 11. Manual Spot Checks（人工验收点）

- 二维码扫描：用真实手机扫码访问 `/api/public/scan/{traceNumber}`，能看到完整溯源信息
- 链上验证：在 WeBASE 控制台查询合约调用历史，能看到本系统的 txHash
- 角色边界：consumer 角色尝试访问 `/api/admin/**` 必须 403
- 订单状态机：尝试从 COMPLETED 状态发起取消，必须拒绝
- 碳积分余额：以旧换新成功后，consumer 的 `nev_carbon_credit_account.balance` 必须增加

---

## 12. Non-Goals（非目标）

- 不做生产部署、不做 K8s、不做 CI/CD 流水线（本地能跑即可）
- 不做真实支付、不做真实物流对接
- 不做完整单元/集成测试覆盖
- 不做电池召回 / 梯次利用 / 再制造 / 决策引擎 / 合规监控（毕设阶段补）
- 不引入 ClickHouse / Kafka / Elasticsearch
- 不做小程序原生版（user-app uni-app H5 即可）
- 不实现合约创新点（多签 / 零知 / Merkle），但代码层面预留扩展位

---

## 13. Autonomy Mode（自治模式）

`interactive_governed` —— 重大决策点必须用户确认：
- 进入 plan_execute 前必须有 xl_plan 文档并经用户审阅
- 涉及数据库 schema 变更必须文档先行
- 涉及合约函数签名变更必须用户确认

---

## 14. Assumptions（假设）

- 用户本地已安装 / 可安装：JDK 21、Maven 3.9+、Node.js 18+、pnpm 8+、Docker、MySQL 客户端
- 用户保留对 老仓 NEV WeBASE 部署的访问，且可在其上部署新合约
- 用户拥有 RuoYi-Vue-Plus 和 plus-ui 的源代码访问权限（开源，无授权问题）
- 用户对 Sa-Token、MyBatis-Plus 有基础认知或愿意 1-2 天学习曲线
- demo seed 数据可由开发者自行编造，无需真实数据来源

---

## 15. Risks（风险登记）

| # | 风险 | 等级 | 缓解 |
|---|---|---|---|
| R1 | 1 个月单人完成 4 业务 + 7 角色 + 合约 + 双前端，工期紧 | 高 | 严格执行非目标边界；每周回看进度；如周末仍滞后，砍 retailer/distributor/recycler 专属页面只做权限 |
| R2 | 学生对 RuoYi-Plus 不熟，学习曲线 | 中 | 前 3 天专门跑通 RuoYi-Plus hello world + 看官方文档 |
| R3 | 链上交易延迟或失败影响演示 | 中 | 后端做异步上链 + 离线模式开关，链失败时仍能完成业务流程，链上数据延迟补偿 |
| R4 | 老仓和新仓共用 WeBASE，合约地址冲突 | 低 | 新合约部署到新地址，nev_contract_config 表记录，老仓继续用老地址 |
| R5 | uni-app 改 baseURL 后还有大量接口路径不对应 | 中 | 在 backend 加 URL 重写中间件做兼容层，或在 user-app 改接口文件 |
| R6 | RuoYi-Plus 自带的多租户/工作流误开启 | 低 | 配置文件统一关闭，不引用相关注解 |

---

## 16. Evidence Inputs（证据来源）

- 上一轮分析：`docs/analysis/2026-05-13-nev-vs-2026037462-comparison.md`
- 上一轮迁移计划：`docs/plans/2026-05-13-nev-ruoyi-migration-plan.md`（按渐进迁移路线写，部分仍可参考）
- 老仓代码：`E:/Study/IdeaProjects/NEV/`
- 参考项目：`E:/Study/IdeaProjects/NEV/2026037462/`
- RuoYi-Vue-Plus 文档：Context7 `/dromara/ruoyi-vue-plus` 5.X 分支
- 本次会话治理证据：`outputs/runtime/vibe-sessions/20260514T134803Z-053c7f3a/`

---

## 17. Next Step（下一步）

**本次 bounded stop = `requirement_doc`。**

进入下一阶段需要新开一轮：
- `$vibe-how` → 进入 `xl_plan` 阶段，产出 4 Wave 详细执行计划（按 1 个月工期排）
- 或 `$vibe` → 一气推到 `phase_cleanup`（但 vibe runtime 在 phase_cleanup 阶段有已知 bug，建议分轮跑）

**建议**：先用 `$vibe-how` 输出执行计划文档，让用户审阅后再开 `$vibe-do` 真正动手。

---

## 18. Completion Language Policy

- 本需求文档自身的"完成"指：用户审阅并确认冻结。
- 后续阶段任何"完成"声明必须有验证证据（测试通过、演示截图、合约 txHash 等），不能只是说"代码已写"。

---

## 19. 治理元数据（vibe runtime 必填）

| 项 | 值 |
|---|---|
| run_id | 20260514T134803Z-053c7f3a |
| governance_scope | root |
| entry_intent | vibe |
| requested_stop | requirement_doc |
| selected_pack | orchestration-core |
| runtime_selected_skill | vibe |
| autonomy_mode | interactive_governed |
| anti_drift_tier | C |
| validation_material_role | validation_only |
| completion_state | partial（仅 requirement_doc 冻结，后续 plan/execute/cleanup 待开展） |

---

> 本文档由 canonical vibe 治理流程冻结。下一轮启动 `xl_plan` 时回溯到本文档。
