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
 * 商品表（nev_product）
 *
 * @author NEV-v2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("nev_product")
public class NevProductDO extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long merchantId;

    /** 商品类目（BATTERY/ACCESSORY/SERVICE，对应字典 nev_product_category） */
    private String category;

    private String title;
    private String subtitle;

    private BigDecimal price;
    private Integer stock;
    private Integer salesCount;

    /** 关联电池 ID（可选，电池即商品场景） */
    private Long batteryId;

    /** 商品图片列表（JSON 数组字符串，前端展示用） */
    private String images;

    /** 商品详情富文本 */
    private String description;

    /** 状态（ON_SALE/OFF_SHELF/SOLD_OUT） */
    private String status;

    private String tenantId;

    @TableLogic
    private String delFlag;

    private String remark;
}
