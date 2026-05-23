package com.nev.carbon.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.nev.carbon.dto.CarbonFootprintVO;
import com.nev.carbon.service.CarbonFootprintService;
import com.nev.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin 触发碳足迹计算
 *
 * @author NEV-v2
 */
@RestController
@RequestMapping("/admin/carbon")
@RequiredArgsConstructor
@SaCheckRole("superadmin")
public class AdminCarbonController {

    private final CarbonFootprintService footprintService;

    /** 按 traceNumber 计算并保存碳足迹（已有则覆盖） */
    @PostMapping("/calc/{traceNumber}")
    public R<CarbonFootprintVO> calc(@PathVariable String traceNumber) {
        return R.ok(footprintService.calcAndSave(traceNumber));
    }
}
