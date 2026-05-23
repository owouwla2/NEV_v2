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
 * 购物车明细（nev_cart_item）
 *
 * @author NEV-v2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("nev_cart_item")
public class NevCartItemDO extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long cartId;
    private Long productId;
    private Integer quantity;

    /** 加入购物车时单价（避免后续价格变动追溯困难） */
    private BigDecimal unitPrice;

    /** 是否选中结算（'0' 未选 / '1' 已选，默认 '1'） */
    private String selected;

    private String tenantId;

    @TableLogic
    private String delFlag;

    private String remark;
}
