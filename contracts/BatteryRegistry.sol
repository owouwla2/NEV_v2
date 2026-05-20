// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.6.10;

import "./RoleManager.sol";

/**
 * @title BatteryRegistry
 * @dev NEV-v2 电池数字身份合约（继承 RoleManager）
 *
 * 设计参考：老仓 BatteryRegistry.sol
 * 主要差异：
 *   - 老仓用 uint256 batteryId 作 mapping key + 单独存 uniqueCode 字符串
 *   - 新仓直接用 string traceNumber 作 key（对齐需求文档 §5.2 nev_battery.trace_number）
 *     虽然 string key 比 uint256 略贵 gas，但溯源场景 traceNumber 本身就是业务唯一编号，
 *     直接用 string key 让链上链下 ID 完全对齐，减少后端映射心智负担
 *   - 链上只存最关键 4 字段：traceNumber / dataHash / producer / producedAt
 *     完整电池规格（容量/电压/型号/BMS 信息）在 MySQL nev_battery + nev_battery_spec
 *
 * dataHash 计算规约（后端必须严格遵守）：
 *   keccak256(abi.encodePacked(
 *       trace_number || model || serial_no || capacity_kwh || voltage ||
 *       producer_id || produced_at(ISO8601)
 *   ))
 *   后端 Java 侧用 web3j 的 Hash.sha3() 实现，输入字段顺序 + 编码必须固定。
 *
 * 后端调用链（参见 docs/contracts/design.md §4 "后端契约"）：
 *   producer 注册电池 → BatteryService.create()
 *     → 写入 nev_battery + nev_battery_spec
 *     → 计算 dataHash
 *     → 调用 registerBattery(traceNumber, dataHash)
 *     → 链上回执 tx_hash + block_number 写回 nev_battery_lifecycle
 */
contract BatteryRegistry is RoleManager {

    /// @dev 链式构造父合约，确保 RoleManager 先初始化
    constructor() public RoleManager() {}

    struct Battery {
        string  traceNumber;     // 业务唯一溯源编号（对应 nev_battery.trace_number）
        bytes32 dataHash;        // 电池规格快照 keccak256（防篡改）
        address producer;        // 注册者地址（必为 PRODUCER 角色）
        uint256 producedAt;      // 链上时间戳（block.timestamp，不可篡改）
        bool    exists;          // 存在标记（区分未注册和已注册）
    }

    // traceNumber => Battery
    mapping(string => Battery) public batteries;

    // 所有 traceNumber 列表（用于遍历 / 分页）
    string[] public traceNumbers;

    event BatteryRegistered(
        string  indexed traceNumberIndexed,  // indexed 后会被 keccak256 哈希（不可还原），仅用于事件过滤
        string  traceNumber,                  // 完整 traceNumber 字符串（明文，供后端解析）
        bytes32 dataHash,
        address indexed producer,
        uint256 producedAt
    );

    /**
     * @dev PRODUCER 注册新电池（核心方法）
     * @param traceNumber 业务唯一溯源编号
     * @param dataHash 电池规格 keccak256（由后端按规约计算）
     *
     * 设计说明：
     * 1. 仅 PRODUCER 角色可调用（onlyProducer 修饰符来自父合约 RoleManager）
     * 2. traceNumber 不可重复注册（同一电池只能上链一次）
     * 3. producedAt 由链上自动生成（block.timestamp），不接受参数防止时间作假
     * 4. emit 事件后，后端通过 webase-app-sdk 监听写回 MySQL 的 tx_hash/block_number
     */
    function registerBattery(string calldata traceNumber, bytes32 dataHash)
        external
        onlyProducer
        returns (bool)
    {
        require(bytes(traceNumber).length > 0, "Trace number cannot be empty");
        require(dataHash != bytes32(0), "Data hash cannot be zero");
        require(!batteries[traceNumber].exists, "Battery already registered");

        batteries[traceNumber] = Battery({
            traceNumber: traceNumber,
            dataHash: dataHash,
            producer: msg.sender,
            producedAt: block.timestamp,
            exists: true
        });
        traceNumbers.push(traceNumber);

        emit BatteryRegistered(traceNumber, traceNumber, dataHash, msg.sender, block.timestamp);
        return true;
    }

    /**
     * @dev 验证电池数据是否被篡改
     * @param traceNumber 业务唯一编号
     * @param expectedHash 当前 MySQL 数据重算的哈希
     * @return true = 一致未篡改 / false = 已篡改或不存在
     *
     * 使用场景：
     *   消费者扫码 → 后端读 MySQL → keccak256 重算 → 调用 verifyBattery 校验链上一致性
     */
    function verifyBattery(string calldata traceNumber, bytes32 expectedHash)
        external
        view
        returns (bool)
    {
        if (!batteries[traceNumber].exists) {
            return false;
        }
        return batteries[traceNumber].dataHash == expectedHash;
    }

    /// @dev 查询电池基础信息
    function getBatteryInfo(string calldata traceNumber)
        external
        view
        returns (
            string memory _traceNumber,
            bytes32 dataHash,
            address producer,
            uint256 producedAt
        )
    {
        require(batteries[traceNumber].exists, "Battery not found");
        Battery storage b = batteries[traceNumber];
        return (b.traceNumber, b.dataHash, b.producer, b.producedAt);
    }

    /// @dev 检查电池是否已注册（轻量版，不抛异常）
    function isBatteryRegistered(string calldata traceNumber) external view returns (bool) {
        return batteries[traceNumber].exists;
    }

    /// @dev 已注册电池总数
    function getBatteryCount() external view returns (uint256) {
        return traceNumbers.length;
    }

    // ---------------------------------------------------------------
    // 毕设阶段预留扩展位（本轮不实现，仅注释）
    // ---------------------------------------------------------------
    // function verifyOwnershipZk(string calldata traceNumber, bytes calldata proof) external view returns (bool);
    //   零知识证明：消费者证明拥有电池但不暴露身份
    // function registerBatteryBatch(string[] calldata traceNumbers, bytes32 merkleRoot) external onlyProducer;
    //   批量注册（Merkle 根 + 链下证明），适合 1k+ 电池/批
    // ---------------------------------------------------------------
}
