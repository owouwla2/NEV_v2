package com.nev.marketplace.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.nev.common.core.domain.R;
import com.nev.marketplace.dto.TradeInSubmitDTO;
import com.nev.marketplace.dto.TradeInVO;
import com.nev.marketplace.service.TradeInService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/consumer/trade-in")
@RequiredArgsConstructor
@SaCheckRole("consumer")
public class ConsumerTradeInController {

    private final TradeInService tradeInService;

    @PostMapping("/submit")
    public R<TradeInVO> submit(@Valid @RequestBody TradeInSubmitDTO dto) {
        return R.ok(tradeInService.submit(dto));
    }

    @PostMapping("/accept/{id}")
    public R<TradeInVO> accept(@PathVariable Long id) {
        return R.ok(tradeInService.accept(id));
    }

    @PostMapping("/reject/{id}")
    public R<TradeInVO> reject(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return R.ok(tradeInService.reject(id, reason));
    }

    @GetMapping("/list")
    public R<List<TradeInVO>> list() {
        return R.ok(tradeInService.listMineConsumer());
    }

    @GetMapping("/detail/{id}")
    public R<TradeInVO> detail(@PathVariable Long id) {
        return R.ok(tradeInService.detail(id));
    }
}
