package com.nev.battery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * distributor 接收电池（IN_USE 事件）
 * dataHash 输入：trace_number | from_owner | to_owner | handover_at
 *
 * @author NEV-v2
 */
@Data
public class TransferInDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "溯源编号不能为空")
    private String traceNumber;

    /** 上一持有者 user_id（distributor 从谁手里接到的，通常 = 该电池的 producer_id 或 上一 distributor） */
    @NotNull(message = "前持有者 user_id 不能为空")
    private Long fromOwnerId;

    /** 备注 */
    private String remark;
}
