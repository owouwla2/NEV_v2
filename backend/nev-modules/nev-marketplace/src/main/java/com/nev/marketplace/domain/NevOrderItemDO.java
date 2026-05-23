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

/**
 * 订单明细（nev_order_item）
 *
 * @author NEV-v2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("nev_order_item")
public class NevOrderItemDO extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;
    private Long productId;

    /** 商品快照（JSON：title/category/images/batteryId 等下单时定格） */
    private String productSnapshot;

    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    private String tenantId;

    @TableLogic
    private String delFlag;

    private String remark;
}
