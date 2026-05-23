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
 * 订单主表（nev_order）
 *
 * 状态机：PENDING → PAID → SHIPPED → DELIVERED → COMPLETED
 *                 ↘ CANCELLED   ↘ REFUNDED
 *
 * @author NEV-v2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("nev_order")
public class NevOrderDO extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;
    private Long userId;
    private Long merchantId;

    private BigDecimal totalAmount;
    private BigDecimal payAmount;

    private String status;

    /** 收货地址快照（JSON 字符串：recipient/phone/province/city/district/detail） */
    private String addressSnapshot;

    private Date paidAt;
    private Date shippedAt;
    private Date deliveredAt;
    private Date completedAt;
    private Date cancelledAt;
    private String cancelReason;

    private String tenantId;

    @TableLogic
    private String delFlag;

    private String remark;
}
