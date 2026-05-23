package com.nev.marketplace.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 下单请求（从购物车选项）
 *
 * @author NEV-v2
 */
@Data
public class OrderCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 要结算的购物车明细 ID 列表（同一订单必须来自同一商家） */
    @NotEmpty(message = "至少选一项")
    private List<Long> cartItemIds;

    @NotNull(message = "收货地址不能为空")
    @Valid
    private AddressDTO address;

    private String remark;
}
