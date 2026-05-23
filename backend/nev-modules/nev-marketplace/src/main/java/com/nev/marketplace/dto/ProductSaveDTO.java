package com.nev.marketplace.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * merchant 上架 / 更新商品的请求 DTO
 *
 * @author NEV-v2
 */
@Data
public class ProductSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 商品 ID（null=新增，非 null=更新） */
    private Long id;

    @NotBlank(message = "商品标题不能为空")
    @Size(max = 200, message = "商品标题不能超过 200 字")
    private String title;

    @Size(max = 255, message = "副标题不能超过 255 字")
    private String subtitle;

    @NotBlank(message = "商品类目不能为空")
    private String category;

    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0.00", inclusive = false, message = "单价必须大于 0")
    private BigDecimal price;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负")
    private Integer stock;

    /** 关联电池 ID（可选） */
    private Long batteryId;

    /** 图片 URL 列表，后端用 JSON 数组字符串存入 nev_product.images */
    private List<String> images;

    /** 商品详情富文本 */
    private String description;

    /** 状态（默认 ON_SALE，可传 OFF_SHELF 直接下架） */
    private String status;
}
