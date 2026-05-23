package com.nev.battery.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.nev.battery.dto.BatteryEventVO;
import com.nev.battery.dto.TransferInDTO;
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
 * Distributor 端电池业务 API
 *
 * @author NEV-v2
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/distributor/battery")
@RequiredArgsConstructor
public class DistributorBatteryController {

    private final BatteryService batteryService;

    /**
     * 接收电池（IN_USE 事件）
     */
    @SaCheckLogin
    @SaCheckRole("distributor")
    @PostMapping("/transfer-in")
    public R<BatteryEventVO> transferIn(@Valid @RequestBody TransferInDTO dto) {
        return R.ok(batteryService.transferIn(dto));
    }
}
