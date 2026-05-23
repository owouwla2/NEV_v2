package com.nev.battery.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.nev.battery.dto.BatteryEventVO;
import com.nev.battery.dto.SellDTO;
import com.nev.battery.service.BatteryService;
import com.nev.common.core.domain.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Retailer 端电池业务 API
 *
 * @author NEV-v2
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/retailer/battery")
@RequiredArgsConstructor
public class RetailerBatteryController {

    private final BatteryService batteryService;

    /**
     * 售出电池给消费者（SOLD 事件）
     */
    @SaCheckLogin
    @SaCheckRole("retailer")
    @PostMapping("/sell")
    public R<BatteryEventVO> sell(@Valid @RequestBody SellDTO dto) {
        return R.ok(batteryService.sell(dto));
    }
}
