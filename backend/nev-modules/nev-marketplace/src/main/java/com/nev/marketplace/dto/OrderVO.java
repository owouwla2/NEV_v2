package com.nev.marketplace.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 订单详情 VO
 *
 * @author NEV-v2
 */
@Data
@Builder
public class OrderVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private String merchantName;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private String status;
    private AddressDTO address;
    private List<Item> items;

    private Date paidAt;
    private Date shippedAt;
    private Date deliveredAt;
    private Date completedAt;
    private Date cancelledAt;
    private String cancelReason;
    private Date createTime;

    @Data
    @Builder
    public static class Item implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private Long productId;
        private String title;
        private String category;
        private Long batteryId;
        private List<String> images;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}
