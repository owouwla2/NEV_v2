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
import java.time.LocalDate;

/**
 * 排放因子库（nev_emission_factor）
 * D5-D6 已灌 15 条（RAW 5 / MFG 4 / TRANS 3 / USE 2 / EOL 1）
 *
 * @author NEV-v2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("nev_emission_factor")
public class NevEmissionFactorDO extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 因子编码（业务唯一，例如 RAW-LITHIUM / MFG-ELECTRICITY-CN） */
    private String factorCode;

    private String factorName;

    /** 单位（kgCO2eq/kg / kgCO2eq/kWh 等） */
    private String unit;

    /** 因子值 */
    private BigDecimal value;

    /** 数据来源（CLCD / Ecoinvent / IPCC / 生态环境部 等） */
    private String source;

    /** 适用阶段（RAW/MFG/TRANS/USE/EOL，可空 = 通用） */
    private String applicableStage;

    private LocalDate validFrom;
    private LocalDate validTo;

    private String tenantId;

    @TableLogic
    private String delFlag;

    private String remark;
}
