package com.nev.marketplace.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class TradeInEvaluateDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "申请ID必填")
    private Long requestId;

    @NotNull(message = "SOH 必填")
    @Min(value = 0, message = "SOH >= 0")
    @Max(value = 100, message = "SOH <= 100")
    private Integer soh;

    @NotNull(message = "评估金额必填")
    private BigDecimal evaluatedAmount;

    private String summary;
}
