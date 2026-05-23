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
 * 碳积分账户（nev_carbon_credit_account）—— 每用户 1 条
 *
 * @author NEV-v2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("nev_carbon_credit_account")
public class NevCarbonCreditAccountDO extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    /** 可用余额（碳积分单位 kgCO2eq） */
    private BigDecimal balance;

    /** 冻结余额（待结算） */
    private BigDecimal frozen;

    /** 累计获得 */
    private BigDecimal totalEarned;

    /** 累计消耗 */
    private BigDecimal totalSpent;

    private String tenantId;

    @TableLogic
    private String delFlag;

    private String remark;
}
