package com.nev.marketplace.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nev.common.core.domain.R;
import com.nev.marketplace.dto.OrderQueryDTO;
import com.nev.marketplace.dto.OrderVO;
import com.nev.marketplace.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Merchant 端订单查询 API（只读，D17 后会加发货等操作）
 *
 * @author NEV-v2
 */
@Slf4j
@RestController
@RequestMapping("/merchant/order")
@RequiredArgsConstructor
public class MerchantOrderController {

    private final OrderService orderService;

    @SaCheckLogin @SaCheckRole("merchant")
    @GetMapping("/list")
    public R<Page<OrderVO>> list(@ModelAttribute OrderQueryDTO q) {
        return R.ok(orderService.listMineMerchant(q));
    }

    @SaCheckLogin @SaCheckRole("merchant")
    @GetMapping("/detail/{orderId}")
    public R<OrderVO> detail(@PathVariable Long orderId) {
        return R.ok(orderService.detail(orderId));
    }
}
