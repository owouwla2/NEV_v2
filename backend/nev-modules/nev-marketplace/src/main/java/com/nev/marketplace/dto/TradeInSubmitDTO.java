package com.nev.marketplace.dto;

import com.nev.common.core.validate.encoding.NoReplacementChar;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class TradeInSubmitDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "老电池溯源编号不能为空")
    @NoReplacementChar
    private String oldBatteryTraceNumber;

    /** 可选：目标新商品（demo 阶段不强制）*/
    private Long newProductId;

    @NoReplacementChar
    private String remark;
}
