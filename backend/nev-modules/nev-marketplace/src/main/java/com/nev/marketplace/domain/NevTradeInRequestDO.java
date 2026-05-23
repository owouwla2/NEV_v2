package com.nev.marketplace.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nev.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 以旧换新申请
 *
 * 状态机：SUBMITTED → EVALUATED → ACCEPTED → COMPLETED
 *                                    ↘ REJECTED
 *
 * @author NEV-v2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("nev_trade_in_request")
public class NevTradeInRequestDO extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String requestNo;
    private Long consumerId;
    private Long oldBatteryId;
    private Long newProductId;
    private BigDecimal evaluatedAmount;
    private Long recyclerId;
    private Long evaluatorId;
    private String status;
    /** 评估详情 JSON */
    private String evaluationPayload;
    private Long linkedOrderId;
    private Date submittedAt;
    private Date evaluatedAt;
    private Date acceptedAt;
    private Date recycledAt;
    private Date completedAt;
    private String tenantId;
    @TableLogic
    private String delFlag;
    private String remark;
}
