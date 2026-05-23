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
 * 碳积分流水（nev_carbon_credit_record）
 *
 * @author NEV-v2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("nev_carbon_credit_record")
public class NevCarbonCreditRecordDO extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    /** 变更金额（正数=增加，负数=扣减） */
    private BigDecimal changeAmount;

    /** 变更后余额 */
    private BigDecimal balanceAfter;

    /** 业务原因（TRADE_IN / RECYCLE / PURCHASE_DEDUCT / ADMIN_ADJUST 等） */
    private String reason;

    /** 关联业务ID（订单ID / 换新单ID 等） */
    private Long relatedId;

    /** 关联业务类型（ORDER / TRADE_IN / MANUAL） */
    private String relatedType;

    private String tenantId;

    @TableLogic
    private String delFlag;

    private String remark;
}
