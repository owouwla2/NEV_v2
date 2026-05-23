package com.nev.battery.controller;

import com.nev.battery.dto.BatteryScanVO;
import com.nev.battery.service.BatteryScanService;
import com.nev.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开溯源扫码 API（无需登录，对应 security.excludes 中的 /public/**）
 *
 * 使用场景：消费者扫描电池二维码 → 跳转 GET /public/scan/{traceNumber}
 *
 * @author NEV-v2
 */
@Slf4j
@RestController
@RequestMapping("/public/scan")
@RequiredArgsConstructor
public class PublicScanController {

    private final BatteryScanService batteryScanService;

    /**
     * 公开扫码：返回电池完整溯源 + 链上校验状态
     *
     * 注意：本接口对所有人开放，不做用户身份鉴权，仅做基础速率限制
     * （rate limit 由网关 / nginx 层兜底，本轮 demo 不集成 ratelimiter）
     */
    @GetMapping("/{traceNumber}")
    public R<BatteryScanVO> scan(@PathVariable String traceNumber) {
        return R.ok(batteryScanService.scan(traceNumber));
    }
}
