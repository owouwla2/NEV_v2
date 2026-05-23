package com.nev.marketplace.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 以旧换新申请视图（含状态机里里所有时间戳）
 */
@Data
@Builder
public class TradeInVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String requestNo;
    private Long consumerId;
    private Long oldBatteryId;
    private String oldBatteryTraceNumber;
    private Long newProductId;
    private BigDecimal evaluatedAmount;
    private Long recyclerId;
    private Long evaluatorId;
    private String status;
    private Integer soh;
    private String evaluationSummary;
    private String evaluationPayload;
    private Date submittedAt;
    private Date evaluatedAt;
    private Date acceptedAt;
    private Date recycledAt;
    private Date completedAt;
    /** 链上 RECYCLED 事件 txHash（接受时回填）*/
    private String chainTxHash;
}
