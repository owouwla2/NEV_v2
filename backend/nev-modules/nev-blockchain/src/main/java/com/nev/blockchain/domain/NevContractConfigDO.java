package com.nev.blockchain.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 合约配置表实体（nev_contract_config）
 *
 * @author NEV-v2
 */
@Data
@TableName("nev_contract_config")
public class NevContractConfigDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 合约名称（LifecycleTrace / BatteryRegistry / RoleManager） */
    private String contractName;

    /** 合约地址（0x 开头 42 字符） */
    private String contractAddress;

    /** 合约 ABI（JSON 字符串） */
    private String abi;

    /** 部署区块高度 */
    private Long deployBlock;

    /** 部署时间 */
    private LocalDateTime deployedAt;

    /** 链网络标识（fisco-bcos / ethereum 等） */
    private String network;

    /** FISCO BCOS 群组 ID */
    private String groupId;

    /** 是否启用（'0' 停用 / '1' 启用） */
    private String enabled;

    /** 租户编号 */
    private String tenantId;

    @TableLogic
    private String delFlag;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
    private String remark;
}
