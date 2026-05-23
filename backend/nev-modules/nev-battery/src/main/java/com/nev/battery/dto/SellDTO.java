package com.nev.battery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * retailer 售出电池（SOLD 事件）
 * dataHash 输入：trace_number | order_no | consumer_id | sold_at
 *
 * @author NEV-v2
 */
@Data
public class SellDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "溯源编号不能为空")
    private String traceNumber;

    /** 订单号（业务订单 nev_order.order_no；本轮 demo 可填任意编号） */
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /** 购买的消费者 user_id（必须存在且角色=consumer） */
    @NotNull(message = "消费者 user_id 不能为空")
    private Long consumerId;

    private String remark;
}
