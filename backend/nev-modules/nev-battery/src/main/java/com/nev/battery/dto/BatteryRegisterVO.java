package com.nev.battery.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 电池注册响应 VO
 *
 * @author NEV-v2
 */
@Data
@Builder
public class BatteryRegisterVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String traceNumber;
    private String dataHash;
    private String txHash;
    private Long blockNumber;
    private Integer version;
    private Date producedAt;

    /** 链上状态: SUCCESS / FAILED */
    private String chainStatus;

    private String message;
}
