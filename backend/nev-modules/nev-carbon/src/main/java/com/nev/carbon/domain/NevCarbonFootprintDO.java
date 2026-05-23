package com.nev.carbon.domain;

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
 * 电池碳足迹主表（nev_carbon_footprint）—— 1:1 nev_battery
 *
 * @author NEV-v2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("nev_carbon_footprint")
public class NevCarbonFootprintDO extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long batteryId;

    /** 全生命周期总碳排放（kgCO2eq） */
    private BigDecimal totalCo2Kg;

    /** 核算方法（GB-T-24067 / ISO-14067 / GHG-Protocol） */
    private String calcMethod;

    /** 核算版本号（公式或因子库更新时递增） */
    private String calcVersion;

    /** 核算时间 */
    private Date calcTime;

    private String tenantId;

    @TableLogic
    private String delFlag;

    private String remark;
}
