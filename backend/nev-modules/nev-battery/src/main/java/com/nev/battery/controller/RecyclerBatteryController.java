package com.nev.battery.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.nev.battery.dto.BatteryEventVO;
import com.nev.battery.dto.ReceiveDTO;
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
 * Recycler 端电池业务 API
 *
 * @author NEV-v2
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/recycler/battery")
@RequiredArgsConstructor
public class RecyclerBatteryController {

    private final BatteryService batteryService;

    /**
     * 接收回收电池（RECYCLED 事件）
     */
    @SaCheckLogin
    @SaCheckRole("recycler")
    @PostMapping("/receive")
    public R<BatteryEventVO> receive(@Valid @RequestBody ReceiveDTO dto) {
        return R.ok(batteryService.receive(dto));
    }
}
