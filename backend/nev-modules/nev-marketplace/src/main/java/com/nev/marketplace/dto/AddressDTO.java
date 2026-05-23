package com.nev.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 收货地址（下单时定格存入 nev_order.address_snapshot）
 *
 * @author NEV-v2
 */
@Data
public class AddressDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "收件人不能为空")
    private String recipient;

    @NotBlank(message = "电话不能为空")
    private String phone;

    @NotBlank(message = "省不能为空")
    private String province;

    @NotBlank(message = "市不能为空")
    private String city;

    @NotBlank(message = "区/县不能为空")
    private String district;

    @NotBlank(message = "详细地址不能为空")
    private String detail;
}
