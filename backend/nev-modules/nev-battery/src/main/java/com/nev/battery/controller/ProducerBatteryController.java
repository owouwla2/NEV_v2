package com.nev.battery.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.nev.battery.dto.BatteryRegisterDTO;
import com.nev.battery.dto.BatteryRegisterVO;
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
 * Producer 端电池业务 API
 *
 * @author NEV-v2
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/producer/battery")
@RequiredArgsConstructor
public class ProducerBatteryController {

    private final BatteryService batteryService;

    /**
     * 注册新电池（producer 角色专用）
     */
    @SaCheckLogin
    @SaCheckRole("producer")
    @PostMapping("/register")
    public R<BatteryRegisterVO> register(@Valid @RequestBody BatteryRegisterDTO dto) {
        BatteryRegisterVO vo = batteryService.register(dto);
        return R.ok(vo);
    }
}
