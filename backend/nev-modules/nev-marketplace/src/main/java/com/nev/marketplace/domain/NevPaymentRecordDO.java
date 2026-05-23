package com.nev.marketplace.domain;

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
 * 支付记录（nev_payment_record）
 * D17 当前仅支持 method=MOCK，正式接入支付宝/微信留 Wave 4+
 *
 * @author NEV-v2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("nev_payment_record")
public class NevPaymentRecordDO extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    /** 支付单号（业务唯一，PAY + yyyyMMddHHmmss + random6） */
    private String paymentNo;

    private BigDecimal amount;

    /** 支付方式（ALIPAY/WECHAT/CARBON_CREDIT/BALANCE/MOCK） */
    private String method;

    /** 状态（PENDING/SUCCESS/FAILED/REFUNDED） */
    private String status;

    /** 第三方交易号（mock 直接复用 paymentNo） */
    private String tradeNo;

    private Date paidAt;

    private String callbackPayload;

    private String tenantId;

    @TableLogic
    private String delFlag;

    private String remark;
}
