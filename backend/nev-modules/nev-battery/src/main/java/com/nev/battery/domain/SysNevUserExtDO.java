package com.nev.battery.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nev.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * sys_user 业务扩展实体（sys_nev_user_ext）
 *
 * 关注字段：user_type、wallet_address（链上身份）
 *
 * @author NEV-v2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_nev_user_ext")
public class SysNevUserExtDO extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户ID（FK sys_user.user_id，1:1 关联）*/
    @TableId(value = "user_id")
    private Long userId;

    private String userType;
    private String walletAddress;
    private String wxOpenid;
    private String phoneVerified;
    private String realName;
    private String idCardNo;
    private String companyName;
    private String companyLicense;
    private String tenantId;

    @TableLogic
    private String delFlag;

    private String remark;
}
