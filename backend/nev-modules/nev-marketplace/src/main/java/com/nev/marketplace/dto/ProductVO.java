package com.nev.marketplace.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 商品响应 VO（含 merchant 名称、images 数组）
 *
 * @author NEV-v2
 */
@Data
@Builder
public class ProductVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long merchantId;
    private String merchantName;
    private String category;
    private String title;
    private String subtitle;
    private BigDecimal price;
    private Integer stock;
    private Integer salesCount;
    private Long batteryId;
    private List<String> images;
    private String description;
    private String status;
    private Date createTime;
    private Date updateTime;
}
