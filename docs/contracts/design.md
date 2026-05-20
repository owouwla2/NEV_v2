# NEV-v2 智能合约设计说明

> **状态**：Wave 2 D8 设计冻结
> **位置**：`contracts/RoleManager.sol` / `BatteryRegistry.sol` / `LifecycleTrace.sol`
> **创建日期**：2026-05-20
> **目标链**：FISCO BCOS 2.x（沿用老仓 WeBASE-Front 5000-5004 端口）
> **Solidity 版本**：0.6.10（与老仓 2026037462 一致，FISCO BCOS solc 默认支持）
> **部署形态**：三层继承，**只部署 LifecycleTrace 一份**（自带前两者全部功能）

---

## 1. 三层继承结构图

```
┌─────────────────────────────────────────┐
│   LifecycleTrace                        │  ← Wave 2 D11 实际部署的合约
│   - addEvent / verifyEvent              │     字节码包含下面两层所有方法
│   - getEvent / getLatestEvent           │
│   - getEventCount                       │
├─────────────────────────────────────────┤
│   BatteryRegistry                       │  ← 继承得到
│   - registerBattery / verifyBattery     │
│   - getBatteryInfo / isBatteryRegistered│
│   - getBatteryCount                     │
├─────────────────────────────────────────┤
│   RoleManager                           │  ← 继承得到
│   - grantRole / revokeRole              │
│   - hasRole / getRole                   │
│   - owner / roles mapping               │
└─────────────────────────────────────────┘
```

**为什么三层继承而不是单合约**：
1. 关注点分离：角色 / 数字身份 / 生命周期三层职责清晰
2. 老仓 2026037462 已验证可行（FISCO BCOS solc 0.6.10 编译产物 bytecodeBin 非空）
3. 后端 SDK 调用时按角色拆 service：`RoleContractService` / `BatteryContractService` / `LifecycleContractService`，但底层都指向同一个 `contract_address`

---

## 2. 角色映射表（链上 enum ↔ 后端 sys_role）

| `Role` 枚举值 | 整数 | 后端 `sys_role.role_key` | `role_id` | 链上方法权限 |
|---|---:|---|---:|---|
| `NONE`        | 0 | （未授权） | — | 无 |
| `ADMIN`       | 1 | `admin`（superadmin） | 1 | `grantRole` / `revokeRole` |
| `PRODUCER`    | 2 | `producer`     | 11 | `registerBattery` / `addEvent(PRODUCED)` |
| `DISTRIBUTOR` | 3 | `distributor`  | 12 | `addEvent(IN_USE)` |
| `RETAILER`    | 4 | `retailer`     | 13 | `addEvent(SOLD / REPAIRED)` |
| `MERCHANT`    | 5 | `merchant`     | 14 | 仅商城业务，**无链上事件触发权** |
| `CONSUMER`    | 6 | `consumer`     | 15 | 仅查询（`verify*` / `get*`） |
| `RECYCLER`    | 7 | `recycler`     | 16 | `addEvent(RECYCLED / DISMANTLED)` |

**后端映射约定**：在 Java 侧加一个 `RoleEnumMapper` 工具类，输入 `sys_role.role_key`，输出 `uint8 role`。

```java
public enum ChainRole {
    NONE(0), ADMIN(1), PRODUCER(2), DISTRIBUTOR(3),
    RETAILER(4), MERCHANT(5), CONSUMER(6), RECYCLER(7);

    public static ChainRole fromRoleKey(String roleKey) {
        // admin / producer / distributor / retailer / merchant / consumer / recycler
        return switch (roleKey) {
            case "admin" -> ADMIN;
            case "producer" -> PRODUCER;
            // ... 其他
        };
    }
}
```

---

## 3. 事件类型映射表（链上 enum ↔ 后端 nev_battery_lifecycle.event_type）

| `EventType` | 整数 | 后端 `event_type` 字符串 | 触发角色 | dataHash 输入字段 |
|---|---:|---|---|---|
| `PRODUCED`   | 0 | `PRODUCED`   | PRODUCER     | trace_number ‖ producer_id ‖ produced_at ‖ spec_snapshot_json |
| `IN_USE`     | 1 | `IN_USE`     | DISTRIBUTOR  | trace_number ‖ from_owner ‖ to_owner ‖ handover_at |
| `SOLD`       | 2 | `SOLD`       | RETAILER     | trace_number ‖ order_no ‖ consumer_id ‖ sold_at |
| `REPAIRED`   | 3 | `REPAIRED`   | RETAILER（demo） | trace_number ‖ repair_no ‖ repair_summary ‖ repaired_at |
| `RECYCLED`   | 4 | `RECYCLED`   | RECYCLER     | trace_number ‖ recycler_id ‖ soh ‖ received_at |
| `DISMANTLED` | 5 | `DISMANTLED` | RECYCLER     | trace_number ‖ recycler_id ‖ dismantle_summary ‖ dismantled_at |

---

## 4. 后端调用契约

### 4.1 写链（producer 注册电池）

```
ProducerController.registerBattery(BatteryRegisterDTO)
  → BatteryService.create(dto)                          // 业务校验
      → 写入 nev_battery + nev_battery_spec             // MySQL 事务
      → dataHash = keccak256(规约字段)                  // web3j Hash.sha3()
      → BatteryContractService.registerBattery(traceNumber, dataHash)
          → webase-app-sdk 调用 LifecycleTrace 合约
              registerBattery(traceNumber, dataHash)
      → 接收回执 → 写 nev_battery_lifecycle(event_type=PRODUCED,
                                              tx_hash, block_number, version=1)
```

### 4.2 写链（distributor / retailer / recycler 写事件）

```
XxxController.markEvent(eventType, dto)
  → XxxService.recordEvent(dto)
      → 校验 battery 已在 MySQL nev_battery 中
      → dataHash = keccak256(按 EventType 输入规约)
      → LifecycleContractService.addEvent(traceNumber, eventType, dataHash)
      → 接收回执 → 写 nev_battery_lifecycle(event_type, tx_hash, block_number, version=链上回填)
```

### 4.3 读链（消费者扫码验证）

```
PublicController.scan(traceNumber)
  → 读 MySQL nev_battery + nev_battery_lifecycle 完整历史
  → 对每条事件：
      dataHash_recalc = keccak256(MySQL 数据按 EventType 规约重算)
      LifecycleContractService.verifyEvent(traceNumber, version, dataHash_recalc)
      → true = 链上数据一致（绿色✓） / false = 篡改告警（红色✗）
  → 返回带"链上校验通过"标记的完整溯源时间线
```

---

## 5. keccak256 输入规约（必须固定）

### 5.1 Java 侧 Hash 计算示例

```java
import org.web3j.crypto.Hash;
import java.nio.charset.StandardCharsets;

public class DataHashCalculator {

    /** PRODUCED 事件哈希 */
    public static byte[] producedHash(String traceNumber, Long producerId,
                                      LocalDateTime producedAt, String specJson) {
        String input = String.join("|",
            traceNumber,
            producerId.toString(),
            producedAt.format(DateTimeFormatter.ISO_DATE_TIME),
            specJson  // 必须用 Jackson 排序后的 canonical JSON
        );
        return Hash.sha3(input.getBytes(StandardCharsets.UTF_8));
    }
    // 其他 EventType 同模板...
}
```

**注意点**：
1. **字段顺序固定**：每个 EventType 字段顺序写死在文档中，不可变（变了 verify 就全部失效）
2. **JSON 必须 canonical**：用 Jackson `ObjectMapper#configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)`
3. **分隔符 `|`**：避免字段拼接歧义（避免 "ab"+"c" 和 "a"+"bc" 哈希相同）
4. **时间格式 ISO-8601**：`LocalDateTime.format(DateTimeFormatter.ISO_DATE_TIME)`

### 5.2 链上 `dataHash` 类型

合约中 `dataHash` 类型为 `bytes32`，对应 Java 的 `byte[32]`。`Hash.sha3()` 返回的就是 `byte[32]`，可直接传入 web3j 生成的合约 wrapper 方法。

---

## 6. 与老仓 2026037462 合约的差异

| 维度 | 老仓 | 新仓 | 调整原因 |
|---|---|---|---|
| **角色枚举** | 8 个：ADMIN/MANUFACTURER/CONSUMER/MAINTAINER/RECYCLER/GRADIENT_UTILIZATION/DISMANTLER（外加 NONE） | 7 个：ADMIN/PRODUCER/DISTRIBUTOR/RETAILER/MERCHANT/CONSUMER/RECYCLER | 新仓覆盖"生产→流通→零售→消费→回收"完整产业链，梯次利用 / 拆解归到 RECYCLER 业务范围 |
| **角色定义方式** | `uint8 constant` | `enum Role` | Solidity 0.6.10 enum 稳定支持，ABI 自动 uint8，代码可读性高 |
| **事件枚举** | 6 个：PRODUCED/IN_USE/REPAIRED/RECYCLED/GRADIENT_UTILIZED/DISMANTLED | 6 个：PRODUCED/IN_USE/SOLD/REPAIRED/RECYCLED/DISMANTLED | 去 GRADIENT_UTILIZED（毕设阶段），加 SOLD（新仓零售→消费强需求） |
| **电池 key 类型** | `uint256 batteryId`（后端生成）+ 额外存 `uniqueCode` 字符串 | `string traceNumber` 直接作 key | 减少链上链下 ID 映射；trace_number 本身已是业务唯一编号 |
| **哈希算法** | SHA256（链下计算，链上仅存 `bytes32`） | keccak256（EVM 原生 + web3j 原生支持） | EVM/Solidity 原生 keccak256，避免引入额外 SHA256 库 |
| **时间戳** | `now`（已弃用别名） | `block.timestamp`（推荐写法） | 0.6.x 后 `now` 是 `block.timestamp` 别名，二者等价；用新写法对编译告警友好 |
| **事件 indexed** | `string` 字段不能直接 indexed | 同时 emit `string indexed`（哈希后）+ `string`（明文） | 既保留过滤能力又保留可读性，后端订阅事件可拿明文 |

---

## 7. 毕设阶段预留扩展位（本轮不实现）

每个合约末尾都加了注释占位，方便毕设阶段直接补：

| 合约 | 扩展点 | 适用场景 |
|---|---|---|
| `RoleManager`     | `grantRoleMultiSig(account, role, signatures[])` | N 个 ADMIN 多签授权新管理员，防单点失误 |
| `RoleManager`     | `rotateOwner(newOwner)`                          | owner 轮换（合约升级前置） |
| `BatteryRegistry` | `verifyOwnershipZk(traceNumber, proof)`          | 消费者证明拥有但不暴露身份（ZK） |
| `BatteryRegistry` | `registerBatteryBatch(traceNumbers[], merkleRoot)` | Merkle 根批量注册（1k+ 电池/批） |
| `LifecycleTrace`  | `addEventMultiSig(...)`                          | 高价值事件（DISMANTLED）多签 |
| `LifecycleTrace`  | `addEventBatch(traceNumbers[], type, merkleRoot)` | 一次接收 100 块电池批量上链 |
| `LifecycleTrace`  | `rollbackEvent(traceNumber, version)`            | 事件回滚（不删除，仅 emit ROLLBACK 标记） |

---

## 8. 部署 / 测试 / 后端接入路径（D9-D11 预告）

- **D9**：用 `solc 0.6.10` 编译三合约 → 输出 `contracts/build/LifecycleTrace.{bin,abi}`
  - solc 安装：FISCO BCOS WeBASE 自带 solcJS；或本地 `npm i -g solc@0.6.10`
- **D10**：用 web3j 0.6.10 plugin 生成 Java wrapper → 输出到 `nev-modules/nev-blockchain/src/.../contracts/`
- **D11**：通过 WeBASE-Front HTTP API 部署到 FISCO BCOS group 1，回写 `nev_contract_config(contract_name='LifecycleTrace', contract_address='0x...', abi=...)`
  - 部署后，把 6 个 demo 用户的占位 wallet_address (`0x101..0101~0106`) 换成真实 FISCO BCOS account（通过 WeBASE-Sign 服务生成）
  - 然后用 ADMIN account 调用 `grantRole(producer1_addr, PRODUCER)` 等 6 次完成链上授权
