// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.6.10;

/**
 * @title RoleManager
 * @dev NEV-v2 角色管理合约（7 角色版）
 *
 * 设计参考：老仓 E:/Study/IdeaProjects/NEV/2026037462/ 的 RoleManager.sol
 * 主要差异：
 *   - 老仓 8 角色（MANUFACTURER/MAINTAINER/GRADIENT_UTILIZATION/DISMANTLER ...）
 *   - 新仓 7 角色（PRODUCER/DISTRIBUTOR/RETAILER/MERCHANT/CONSUMER/RECYCLER + ADMIN）
 *     对齐需求文档 §3.2 完整产业链定义
 *   - 用 enum 替代 uint8 constant（Solidity 0.6.10 已稳定支持 enum，
 *     ABI 自动暴露为 uint8，可读性更好）
 *
 * 角色值映射（与后端 sys_role.role_key 一一对应）：
 *   Role.NONE        = 0  无角色（未授权）
 *   Role.ADMIN       = 1  系统管理员（合约部署者默认）
 *   Role.PRODUCER    = 2  电池生产商（PRODUCED 事件触发者）
 *   Role.DISTRIBUTOR = 3  经销商（IN_USE 事件触发者）
 *   Role.RETAILER    = 4  零售商 / 4S 店（SOLD 事件触发者）
 *   Role.MERCHANT    = 5  商城商家（仅商城业务，不触发链上事件）
 *   Role.CONSUMER    = 6  终端消费者（仅查询）
 *   Role.RECYCLER    = 7  回收处理商（RECYCLED 事件触发者）
 *
 * 后端钱包绑定：参见 sys_nev_user_ext.wallet_address 字段。
 *   Wave 2 D11 部署完合约后，需把 6 个 demo 用户的占位地址
 *   （0x101..0101~0106）替换成 FISCO BCOS 真实 account。
 */
contract RoleManager {

    enum Role {
        NONE,         // 0
        ADMIN,        // 1
        PRODUCER,     // 2
        DISTRIBUTOR,  // 3
        RETAILER,     // 4
        MERCHANT,     // 5
        CONSUMER,     // 6
        RECYCLER      // 7
    }

    // 地址 => 角色
    mapping(address => Role) public roles;

    // 合约部署者（超级管理员，不可撤销自身角色）
    address public owner;

    event RoleGranted(address indexed account, Role role);
    event RoleRevoked(address indexed account);

    modifier onlyAdmin() {
        require(roles[msg.sender] == Role.ADMIN, "Caller is not admin");
        _;
    }

    modifier onlyProducer() {
        require(roles[msg.sender] == Role.PRODUCER, "Caller is not producer");
        _;
    }

    modifier onlyDistributor() {
        require(roles[msg.sender] == Role.DISTRIBUTOR, "Caller is not distributor");
        _;
    }

    modifier onlyRetailer() {
        require(roles[msg.sender] == Role.RETAILER, "Caller is not retailer");
        _;
    }

    modifier onlyRecycler() {
        require(roles[msg.sender] == Role.RECYCLER, "Caller is not recycler");
        _;
    }

    /// @dev 限定指定角色（业务层多角色判断更通用）
    modifier onlyRole(Role _role) {
        require(roles[msg.sender] == _role, "Caller does not have required role");
        _;
    }

    constructor() public {
        owner = msg.sender;
        roles[msg.sender] = Role.ADMIN;
        emit RoleGranted(msg.sender, Role.ADMIN);
    }

    /**
     * @dev 管理员授予角色
     * @param account 目标地址
     * @param role 角色枚举值（ADMIN..RECYCLER）
     */
    function grantRole(address account, Role role) public onlyAdmin {
        require(role >= Role.ADMIN && role <= Role.RECYCLER, "Invalid role");
        require(account != address(0), "Account cannot be zero address");
        roles[account] = role;
        emit RoleGranted(account, role);
    }

    /**
     * @dev 管理员撤销角色（owner 自身不可撤销，避免合约失去管理员）
     */
    function revokeRole(address account) public onlyAdmin {
        require(account != owner, "Cannot revoke owner role");
        roles[account] = Role.NONE;
        emit RoleRevoked(account);
    }

    /// @dev 查询账户角色
    function getRole(address account) public view returns (Role) {
        return roles[account];
    }

    /// @dev 检查是否拥有指定角色
    function hasRole(address account, Role role) public view returns (bool) {
        return roles[account] == role;
    }

    // ---------------------------------------------------------------
    // 毕设阶段预留扩展位（本轮不实现，仅注释）
    // ---------------------------------------------------------------
    // function grantRoleMultiSig(address account, Role role, bytes[] calldata signatures) external;
    //   多签：N 个 ADMIN 签名后才能授予新管理员，防止单点失误
    // function rotateOwner(address newOwner) external;
    //   合约 owner 轮换（合约升级前置）
    // ---------------------------------------------------------------
}
