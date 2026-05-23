package com.nev.battery.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nev.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.Date;

/**
 * 电池生命周期事件实体（nev_battery_lifecycle）
 *
 * @author NEV-v2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("nev_battery_lifecycle")
public class NevBatteryLifecycleDO extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long batteryId;

    /** 事件类型（PRODUCED/IN_USE/SOLD/REPAIRED/RECYCLED/DISMANTLED） */
    private String eventType;

    private Long operatorId;
    private String operatorRole;

    /** keccak256 hex 字符串（0x 开头 66 字符） */
    private String dataHash;

    /** 链上交易哈希（写链后回填） */
    private String txHash;

    /** 区块高度 */
    private Long blockNumber;

    /** 事件版本号（按 battery_id 单调递增） */
    private Integer version;

    /** 事件附加数据（json 字符串） */
    private String payload;

    private Date eventTime;

    private String tenantId;

    @TableLogic
    private String delFlag;

    private String remark;
}
