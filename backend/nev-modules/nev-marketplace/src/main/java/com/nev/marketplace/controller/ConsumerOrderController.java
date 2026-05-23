package com.nev.marketplace.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nev.common.core.domain.R;
import com.nev.marketplace.dto.OrderCreateDTO;
import com.nev.marketplace.dto.OrderQueryDTO;
import com.nev.marketplace.dto.OrderVO;
import com.nev.marketplace.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consumer 端订单 API
 *
 * @author NEV-v2
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/consumer/order")
@RequiredArgsConstructor
public class ConsumerOrderController {

    private final OrderService orderService;

    @SaCheckLogin @SaCheckRole("consumer")
    @PostMapping("/create")
    public R<OrderVO> create(@Valid @RequestBody OrderCreateDTO dto) {
        return R.ok(orderService.createFromCart(dto));
    }

    @SaCheckLogin @SaCheckRole("consumer")
    @GetMapping("/list")
    public R<Page<OrderVO>> list(@ModelAttribute OrderQueryDTO q) {
        return R.ok(orderService.listMineConsumer(q));
    }

    @SaCheckLogin @SaCheckRole("consumer")
    @GetMapping("/detail/{orderId}")
    public R<OrderVO> detail(@PathVariable Long orderId) {
        return R.ok(orderService.detail(orderId));
    }

    @SaCheckLogin @SaCheckRole("consumer")
    @PostMapping("/cancel/{orderId}")
    public R<Void> cancel(@PathVariable Long orderId,
                          @RequestParam(required = false) String reason) {
        orderService.cancel(orderId, reason);
        return R.ok();
    }
}
