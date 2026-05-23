package com.nev.marketplace.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 订单分页查询（consumer / merchant 共用，差别在 service 注入的 owner 条件）
 *
 * @author NEV-v2
 */
@Data
public class OrderQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 状态过滤（PENDING/PAID/SHIPPED/...，null = 全部） */
    private String status;

    @Min(value = 1) private Integer pageNum = 1;
    @Min(value = 1) @Max(value = 100) private Integer pageSize = 20;
}
