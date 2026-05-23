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
 * 商家档案（nev_merchant）—— merchant 角色用户的扩展信息（1:1 sys_user）
 *
 * @author NEV-v2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("nev_merchant")
public class NevMerchantDO extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID（FK sys_user.user_id, role=merchant，唯一） */
    private Long userId;

    private String merchantName;

    private String businessLicense;

    private String contact;
    private String contactPhone;
    private String address;

    /** 状态（ACTIVE/SUSPENDED/CLOSED） */
    private String status;

    private String tenantId;

    @TableLogic
    private String delFlag;

    private String remark;
}
