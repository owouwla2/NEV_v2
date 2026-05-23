package com.nev.marketplace.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车（按商家分组）
 *
 * @author NEV-v2
 */
@Data
@Builder
public class CartVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 按 merchant 分组的子购物车列表 */
    private List<MerchantGroup> groups;

    /** 全部选中明细的总金额 */
    private BigDecimal grandSelectedTotal;

    @Data
    @Builder
    public static class MerchantGroup implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long cartId;
        private Long merchantId;
        private String merchantName;
        private List<Item> items;
        private BigDecimal selectedTotal;
    }

    @Data
    @Builder
    public static class Item implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long itemId;
        private Long productId;
        private String title;
        private String category;
        private List<String> images;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal subtotal;
        private String selected;
        private Integer stock;
        private String productStatus;
    }
}
