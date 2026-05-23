package com.nev.battery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 电池注册请求 DTO
 *
 * @author NEV-v2
 */
@Data
public class BatteryRegisterDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 溯源编号（业务唯一，前端通常按规则生成） */
    @NotBlank(message = "溯源编号不能为空")
    private String traceNumber;

    @NotBlank(message = "电池型号不能为空")
    private String model;

    @NotBlank(message = "电芯序列号不能为空")
    private String serialNo;

    @NotNull(message = "电池容量不能为空")
    private BigDecimal capacityKwh;

    private BigDecimal voltage;

    // ---- 规格快照（用于 dataHash 计算） ----
    /** 电芯供应商 */
    private String cellSupplier;
    /** 电芯类型（LFP/NCM/NCA） */
    private String cellType;
    /** BMS 信息 */
    private String bmsInfo;
}
