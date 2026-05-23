package com.nev.marketplace.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nev.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 购物车（nev_cart）—— 每用户每商家 1 行
 *
 * @author NEV-v2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("nev_cart")
public class NevCartDO extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private Long merchantId;

    private String tenantId;

    @TableLogic
    private String delFlag;

    private String remark;
}
