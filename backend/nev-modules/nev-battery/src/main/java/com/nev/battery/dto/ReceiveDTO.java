package com.nev.battery.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * recycler 接收回收电池（RECYCLED 事件）
 * dataHash 输入：trace_number | recycler_id | soh | received_at
 *
 * @author NEV-v2
 */
@Data
public class ReceiveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "溯源编号不能为空")
    private String traceNumber;

    /** SOH（State of Health）健康度 0-100% */
    @NotNull(message = "SOH 不能为空")
    @Min(value = 0, message = "SOH 不能小于 0")
    @Max(value = 100, message = "SOH 不能大于 100")
    private Integer soh;

    private String remark;
}
