# NEV-v2 API 接口索引

> **状态**：Wave 4 D28 首版交付后整理
> **基础 URL**：`http://localhost:9280/nev`（开发环境）
> **OpenAPI**：SpringDoc Swagger UI 入口 `http://localhost:9280/doc.html`（注解补全留答辩前）
> **响应格式**：所有业务接口统一返回 `R<T> = { code: 200, msg: string, data: T }`，axios 拦截器自动 unwrap

---

## 1. 鉴权（Sa-Token 双 header）

所有非公开接口必须带：

```
Authorization: Bearer <satoken>
clientid: e5cd7e4891bf95d1d19206ce24a7b32e
```

| 端点 | 方法 | 用途 | 鉴权 |
|---|---|---|---|
| `/auth/login` | POST | 登录拿 satoken | 否（仅需 clientid） |
| `/auth/logout` | POST | 退出登录 | 是 |
| `/system/user/getInfo` | GET | 加载当前用户信息（含 roles） | 是 |
| `/system/menu/getRouters` | GET | 拉服务端动态路由 | 是 |

**登录请求体**：

```json
{
  "username": "consumer1",
  "password": "admin123",
  "clientId": "e5cd7e4891bf95d1d19206ce24a7b32e",
  "grantType": "password"
}
```

**登录返回**：

```json
{
  "code": 200,
  "data": {
    "access_token": "satoken-...",
    "expire_in": 43200,
    "client_id": "e5cd7e4891bf95d1d19206ce24a7b32e"
  }
}
```

---

## 2. 公开接口（免鉴权）

| 端点 | 方法 | 用途 |
|---|---|---|
| `/scan/{trace}` | GET | 公开扫码查询：返回 `BatteryScanVO`（电池基础 + 事件时间线 + 链上校验状态 + 5 阶段碳足迹） |
| `/carbon/public/{trace}` | GET | 公开碳足迹明细查询：返回 `CarbonFootprintVO`（5 阶段 + 因子明细） |

**`BatteryScanVO` 关键字段**：

```ts
{
  traceNumber: string         // 业务唯一编号
  model: string               // 电池型号
  capacityKwh: number
  voltage: number
  producedAt: string
  currentStatus: string       // IN_USE / SOLD / RECYCLED ...
  currentRole: string         // 当前持有角色

  totalEvents: number         // MySQL 计数
  verifiedEvents: number      // verifyEvent=true 计数
  chainEventCount: number     // 链上 getEventCount()
  overallVerified: boolean    // 整体一致性

  events: [{
    version: number           // 链上版本号
    eventType: string         // PRODUCED/IN_USE/SOLD/...
    operatorRole: string
    eventTime: string
    txHash: string
    chainVerified: boolean    // 链上 verifyEvent 返回值
  }]

  carbonFootprint: {          // 可选（nev-carbon 模块可单独撤掉）
    totalCo2Kg: number
    calcMethod: string        // GB-T 24067
    calcVersion: string
    stages: [{
      stage: 'RAW' | 'MFG' | 'TRANS' | 'USE' | 'EOL'
      co2Kg: number
    }]
  }
}
```

---

## 3. 电池模块（nev-battery）

| 端点 | 方法 | 角色 | 用途 |
|---|---|---|---|
| `/battery/register` | POST | producer | 注册电池 + 计算 dataHash + 写链 PRODUCED |
| `/battery/transfer` | POST | distributor / retailer | 流转交付 + 写链 IN_USE |
| `/battery/query` | GET | 任意登录角色 | 链上链下对比查询（含 verifyEvent 明细） |
| `/battery/page` | GET | 任意登录角色 | 按角色过滤的电池分页列表 |

**注册请求体**：

```json
{
  "traceNumber": "BAT-DEMO-001",
  "model": "BYD LFP-85",
  "capacityKwh": 85,
  "voltage": 400,
  "producerWalletAddress": "0x501d..."
}
```

---

## 4. 商城模块（nev-marketplace）

| 端点 | 方法 | 角色 | 用途 |
|---|---|---|---|
| `/marketplace/merchant/product` | POST | merchant | 上架商品（绑定一块电池） |
| `/marketplace/merchant/product/{id}/offline` | POST | merchant | 下架 |
| `/marketplace/merchant/product/page` | GET | merchant | 商家商品列表 |
| `/marketplace/merchant/order/page` | GET | merchant | 商家订单列表 |
| `/marketplace/merchant/order/{id}/ship` | POST | merchant | 发货 |
| `/marketplace/consumer/product/page` | GET | consumer | 消费者商品浏览（仅 ON_SALE） |
| `/marketplace/consumer/cart` | GET / POST / DELETE | consumer | 购物车增删查 |
| `/marketplace/consumer/order` | POST | consumer | 下单（cartItemIds → order）|
| `/marketplace/consumer/order/{id}/pay` | POST | consumer | 模拟支付（PENDING → PAID） |
| `/marketplace/consumer/order/{id}/confirm` | POST | consumer | 确认收货 → **触发 SOLD 上链 + 发碳积分** |

**`OrderService.confirm()` 内部链路**：

```
PaidOrderRow → SHIPPED check → COMPLETED
            └→ batteryService.recordSoldByMerchant(...)  // 链上 SOLD
            └→ carbonCreditService.awardFromOrder(...)   // +X kgCO2eq
```

---

## 5. 以旧换新（nev-marketplace.tradein）

| 端点 | 方法 | 角色 | 用途 |
|---|---|---|---|
| `/tradein/consumer/submit` | POST | consumer | 提交申请（含老电池 traceNumber） |
| `/tradein/consumer/list` | GET | consumer | 我的换新申请 |
| `/tradein/consumer/accept/{id}` | POST | consumer | 接受评估 → **触发 RECYCLED 上链** |
| `/tradein/consumer/reject/{id}` | POST | consumer | 拒绝评估 |
| `/tradein/recycler/pending` | GET | recycler | 待评估列表（SUBMITTED） |
| `/tradein/recycler/evaluate` | POST | recycler | 提交评估（SOH + 报价 + 摘要） |

**状态机**：

```
SUBMITTED ──recycler.evaluate──▶ EVALUATED ──consumer.accept──▶ ACCEPTED → COMPLETED
                                            └──consumer.reject──▶ REJECTED
```

---

## 6. 碳模块（nev-carbon）

| 端点 | 方法 | 角色 | 用途 |
|---|---|---|---|
| `/carbon/public/{trace}` | GET | 免鉴权 | 公开碳足迹查询 |
| `/carbon/admin/calc/{trace}` | POST | superadmin | 手动触发重算（覆盖已有结果） |
| `/carbon/credit/me` | GET | consumer | 我的碳积分流水 |
| `/carbon/credit/me/total` | GET | consumer | 我的碳积分总额 |

**5 阶段定义**（GB-T 24067）：

| stage | 含义 | 备注 |
|---|---|---|
| RAW | 原材料获取 | 锂矿 / 石墨 / 电解液 等因子 |
| MFG | 制造 | 电芯组装 + 模组 + Pack |
| TRANS | 运输 | 出厂 → 经销商 → 零售商 |
| USE | 使用 | 充放电次数 × 电网 emission factor |
| EOL | 报废 / 回收 | **负值**（拆解抵扣再生材料排放） |

参考数据：85 kWh LFP 全周期 ≈ **80744.05 kgCO2eq**。

---

## 7. 区块链查询（nev-blockchain，主要给前端 blockchain/trace 页用）

| 端点 | 方法 | 用途 |
|---|---|---|
| `/blockchain/trace/{trace}` | GET | 拉全部链上事件 + 每条 verifyEvent 结果 |
| `/blockchain/contract/info` | GET | 当前部署的合约地址 + ABI 摘要 |

---

## 8. 通用约定

### 8.1 ID 序列化

雪花 ID 是 19 位 Long，超出 JS Number 安全范围。**所有接口的 ID 字段以 String 返回**：

```json
{ "id": "1856392917401239552", "batteryId": "1856392917401239553" }
```

前端约定：ID 字段比较一律用 `===` 字符串。

### 8.2 错误码

| code | 含义 | 前端处理 |
|---|---|---|
| 200 | 成功 | 正常处理 data |
| 401 | 未鉴权 | 重定向 `/login` |
| 403 | 权限不足 | toast 提示 |
| 500 | 业务异常 | toast 显示 msg |

### 8.3 时间格式

所有时间字段统一 `yyyy-MM-dd HH:mm:ss`（北京时区，TZ=Asia/Shanghai）。

### 8.4 分页参数

`pageNum`（从 1 开始）+ `pageSize`，返回 `{ records, total, pages, current }`。

---

## 9. 待补全（答辩前）

- [ ] 全部 controller 方法补 SpringDoc `@Operation(summary=..., description=...)` 注解
- [ ] 全部 DTO 补 `@Schema(description=...)` 注解
- [ ] `/doc.html` Swagger UI 截图 + 在线版部署
- [ ] 时序图（mermaid）：注册电池上链 / 确认收货发碳积分 / 接受换新写 RECYCLED 三条
