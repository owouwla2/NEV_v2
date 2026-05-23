package com.nev.marketplace.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 公开商品列表查询参数
 *
 * @author NEV-v2
 */
@Data
public class ProductQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 商品类目过滤（可空） */
    private String category;

    /** 标题模糊（可空） */
    private String keyword;

    /** 商家 ID 过滤（可空） */
    private Long merchantId;

    /** 是否只看在售（默认 true） */
    private Boolean onSaleOnly = Boolean.TRUE;

    @Min(value = 1, message = "页码必须 >= 1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "页大小必须 >= 1")
    @Max(value = 100, message = "页大小不能超过 100")
    private Integer pageSize = 20;
}
