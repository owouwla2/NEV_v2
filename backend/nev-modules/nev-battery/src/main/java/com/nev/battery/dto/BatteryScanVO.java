package com.nev.battery.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 公开扫码 VO（消费者扫描二维码后看到的完整溯源时间线）
 *
 * @author NEV-v2
 */
@Data
@Builder
public class BatteryScanVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // ---- 电池基础信息（脱敏后） ----
    private String traceNumber;
    private String model;
    private BigDecimal capacityKwh;
    private BigDecimal voltage;
    private String currentStatus;
    private String currentRole;
    private Date producedAt;

    // ---- 链上整体校验状态 ----
    /** true = 所有事件链上验证通过 / false = 有事件被篡改或链上缺失 */
    private boolean overallVerified;

    /** 总事件数 / 校验通过数 */
    private int totalEvents;
    private int verifiedEvents;

    /** 链上记录的事件总数（应等于 totalEvents） */
    private Integer chainEventCount;

    // ---- 事件时间线 ----
    private List<EventItem> events;

    @Data
    @Builder
    public static class EventItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Integer version;
        private String eventType;
        private Long operatorId;
        private String operatorRole;
        private String dataHash;
        private String txHash;
        private Long blockNumber;
        private Date eventTime;
        private String payload;

        /** 该事件是否经链上 verifyEvent 校验通过 */
        private boolean chainVerified;
    }
}
