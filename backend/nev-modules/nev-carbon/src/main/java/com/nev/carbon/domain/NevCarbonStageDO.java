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

/**
 * 碳足迹分阶段明细（nev_carbon_stage）
 * 固定 5 阶段：RAW 原材料 / MFG 制造 / TRANS 运输 / USE 使用 / EOL 报废
 *
 * @author NEV-v2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("nev_carbon_stage")
public class NevCarbonStageDO extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long footprintId;

    /** 阶段 RAW / MFG / TRANS / USE / EOL */
    private String stage;

    /** 本阶段碳排放（kgCO2eq） */
    private BigDecimal co2Kg;

    /** 细分明细（JSON：主要排放源 + 因子引用） */
    private String breakdown;

    private String tenantId;

    @TableLogic
    private String delFlag;

    private String remark;
}
