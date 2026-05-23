package com.nev.marketplace.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 修改购物车明细（quantity 或 selected）
 *
 * @author NEV-v2
 */
@Data
public class CartItemUpdateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "明细ID不能为空")
    private Long itemId;

    /** 新数量（null = 不改） */
    @Min(value = 1, message = "数量必须 >= 1")
    private Integer quantity;

    /** 是否选中结算（'0'/'1'，null = 不改） */
    private String selected;
}
