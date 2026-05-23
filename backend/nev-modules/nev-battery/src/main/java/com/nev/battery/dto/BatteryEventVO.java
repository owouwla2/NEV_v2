package com.nev.battery.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 通用电池事件响应 VO（IN_USE / SOLD / REPAIRED / RECYCLED / DISMANTLED 共用）
 *
 * @author NEV-v2
 */
@Data
@Builder
public class BatteryEventVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long batteryId;
    private String traceNumber;
    private String eventType;
    private Integer version;
    private String dataHash;
    private String txHash;
    private Long blockNumber;
    private Date eventTime;
    private String currentStatus;
    private Long currentOwnerId;
    private String currentRole;

    /** 链上状态: SUCCESS / FAILED */
    private String chainStatus;

    private String message;
}
