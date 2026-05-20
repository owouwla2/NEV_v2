// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.6.10;

import "./BatteryRegistry.sol";

/**
 * @title LifecycleTrace
 * @dev NEV-v2 电池生命周期事件合约（继承 BatteryRegistry → RoleManager）
 *
 * 设计参考：老仓 LifecycleTrace.sol
 * 主要差异：
 *   - 老仓 6 事件：PRODUCED/IN_USE/REPAIRED/RECYCLED/GRADIENT_UTILIZED/DISMANTLED
 *   - 新仓 6 事件：PRODUCED/IN_USE/SOLD/REPAIRED/RECYCLED/DISMANTLED
 *     去 GRADIENT_UTILIZED（梯次利用归到毕设阶段），加 SOLD（新仓"零售商→消费者"环节强需求）
 *   - 用 string traceNumber 作 key（同 BatteryRegistry，链上链下 ID 对齐）
 *
 * 事件触发权限（链上仅做基础校验，业务侧权限在后端 RuoYi 的 Sa-Token SaCheckRole 注解处理）：
 *   PRODUCED  ← PRODUCER     注册电池时自动触发
 *   IN_USE    ← DISTRIBUTOR  经销商接收 / 转运
 *   SOLD      ← RETAILER     零售商售出给消费者
 *   REPAIRED  ← (扩展角色)   本轮 demo 用 RETAILER 代演
 *   RECYCLED  ← RECYCLER     回收商接收旧电池
 *   DISMANTLED← RECYCLER     回收商拆解 / 报废
 *
 * AppendOnly + 版本号：
 *   每次 addEvent 自动 version+1，链上数据本身不可改，天然实现"修改 = 新增记录"。
 *
 * dataHash 计算规约（按 EventType 不同，输入字段不同；详见 docs/contracts/design.md §5）：
 *   PRODUCED: keccak256(trace_number || producer_id || produced_at || spec_snapshot_json)
 *   IN_USE  : keccak256(trace_number || from_owner || to_owner || handover_at)
 *   SOLD    : keccak256(trace_number || order_no || consumer_id || sold_at)
 *   REPAIRED: keccak256(trace_number || repair_no || repair_summary || repaired_at)
 *   RECYCLED: keccak256(trace_number || recycler_id || soh || received_at)
 *   DISMANTLED: keccak256(trace_number || recycler_id || dismantle_summary || dismantled_at)
 */
contract LifecycleTrace is BatteryRegistry {

    /// @dev 链式构造父合约链 RoleManager → BatteryRegistry → LifecycleTrace
    constructor() public BatteryRegistry() {}

    enum EventType {
        PRODUCED,    // 0  生产入库（PRODUCER）
        IN_USE,      // 1  流通中（DISTRIBUTOR）
        SOLD,        // 2  零售售出（RETAILER）
        REPAIRED,    // 3  维修/检测
        RECYCLED,    // 4  回收接收（RECYCLER）
        DISMANTLED   // 5  拆解报废（RECYCLER）
    }

    struct LifecycleEvent {
        string    traceNumber;
        EventType eventType;
        uint256   version;       // 单调递增，从 1 开始
        bytes32   dataHash;
        address   operator;
        uint256   timestamp;     // block.timestamp
    }

    // traceNumber => 生命周期事件数组
    mapping(string => LifecycleEvent[]) private lifecycleEvents;

    event LifecycleEventAdded(
        string  indexed traceNumberIndexed,  // 仅索引用
        string  traceNumber,
        EventType eventType,
        uint256 version,
        bytes32 dataHash,
        address indexed operator,
        uint256 timestamp
    );

    /**
     * @dev 添加生命周期事件
     * @param traceNumber 电池业务编号（必须已在 BatteryRegistry 注册）
     * @param eventType 事件类型枚举（0-5）
     * @param dataHash 事件数据 keccak256（按 EventType 分别有不同输入规约）
     * @return version 本次事件版本号
     *
     * 权限策略：
     *   链上仅校验"调用者持有有效角色"+"电池已注册"+"哈希非空"
     *   "哪个角色能触发哪个事件"的精细规则在后端 RuoYi 业务层做（更易迭代）
     */
    function addEvent(
        string calldata traceNumber,
        EventType eventType,
        bytes32 dataHash
    ) external returns (uint256) {
        require(roles[msg.sender] != Role.NONE, "Caller has no role");
        require(batteries[traceNumber].exists, "Battery not registered");
        require(dataHash != bytes32(0), "Data hash cannot be zero");

        uint256 version = lifecycleEvents[traceNumber].length + 1;
        uint256 ts = block.timestamp;

        lifecycleEvents[traceNumber].push(LifecycleEvent({
            traceNumber: traceNumber,
            eventType: eventType,
            version: version,
            dataHash: dataHash,
            operator: msg.sender,
            timestamp: ts
        }));

        emit LifecycleEventAdded(
            traceNumber, traceNumber, eventType, version, dataHash, msg.sender, ts
        );
        return version;
    }

    /// @dev 验证某版本事件数据是否被篡改
    function verifyEvent(string calldata traceNumber, uint256 version, bytes32 expectedHash)
        external
        view
        returns (bool)
    {
        uint256 total = lifecycleEvents[traceNumber].length;
        if (version == 0 || version > total) {
            return false;
        }
        return lifecycleEvents[traceNumber][version - 1].dataHash == expectedHash;
    }

    /// @dev 获取事件数量（= 最新版本号）
    function getEventCount(string calldata traceNumber) external view returns (uint256) {
        return lifecycleEvents[traceNumber].length;
    }

    /// @dev 获取指定版本事件详情
    function getEvent(string calldata traceNumber, uint256 version)
        external
        view
        returns (
            string memory _traceNumber,
            EventType eventType,
            uint256 _version,
            bytes32 dataHash,
            address operator,
            uint256 timestamp
        )
    {
        uint256 total = lifecycleEvents[traceNumber].length;
        require(version >= 1 && version <= total, "Invalid version");
        LifecycleEvent storage e = lifecycleEvents[traceNumber][version - 1];
        return (e.traceNumber, e.eventType, e.version, e.dataHash, e.operator, e.timestamp);
    }

    /// @dev 获取最新事件（消费者扫码主用接口）
    function getLatestEvent(string calldata traceNumber)
        external
        view
        returns (
            string memory _traceNumber,
            EventType eventType,
            uint256 version,
            bytes32 dataHash,
            address operator,
            uint256 timestamp
        )
    {
        uint256 total = lifecycleEvents[traceNumber].length;
        require(total > 0, "No events found");
        LifecycleEvent storage e = lifecycleEvents[traceNumber][total - 1];
        return (e.traceNumber, e.eventType, e.version, e.dataHash, e.operator, e.timestamp);
    }

    // ---------------------------------------------------------------
    // 毕设阶段预留扩展位（本轮不实现，仅注释）
    // ---------------------------------------------------------------
    // function addEventMultiSig(string calldata traceNumber, EventType eventType, bytes32 dataHash, bytes[] calldata signatures) external;
    //   多签：高价值事件（如 DISMANTLED）需 N 个 RECYCLER 联签
    // function addEventBatch(string[] calldata traceNumbers, EventType eventType, bytes32 merkleRoot) external;
    //   Merkle 根批量上链（场景：经销商一次接收 100 块电池）
    // function rollbackEvent(string calldata traceNumber, uint256 version) external onlyAdmin;
    //   事件回滚（不删除，而是 emit 一个 ROLLBACK 标记事件）
    // ---------------------------------------------------------------
}
