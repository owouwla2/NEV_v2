package com.nev.battery.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nev.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 电池主表实体（nev_battery）
 *
 * @author NEV-v2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("nev_battery")
public class NevBatteryDO extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 溯源编号（业务唯一，对应链上 traceNumber） */
    private String traceNumber;

    /** 电池型号 */
    private String model;

    /** 电芯序列号 */
    private String serialNo;

    /** 电池容量（kWh） */
    private BigDecimal capacityKwh;

    /** 额定电压（V） */
    private BigDecimal voltage;

    /** 生产商用户ID */
    private Long producerId;

    /** 当前持有者用户ID */
    private Long currentOwnerId;

    /** 当前持有者角色 */
    private String currentRole;

    /** 当前生命周期状态 */
    private String currentStatus;

    /** 二维码 OSS 路径 */
    private String qrCodePath;

    /** 链上合约地址（BatteryRegistry 注册后回填） */
    private String chainAddress;

    /** 出厂时间 */
    private Date producedAt;

    /** 租户编号 */
    private String tenantId;

    @TableLogic
    private String delFlag;

    private String remark;
}
