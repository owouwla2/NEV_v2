package com.nev.marketplace.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.nev.common.core.domain.R;
import com.nev.marketplace.dto.TradeInEvaluateDTO;
import com.nev.marketplace.dto.TradeInVO;
import com.nev.marketplace.service.TradeInService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recycler/trade-in")
@RequiredArgsConstructor
@SaCheckRole("recycler")
public class RecyclerTradeInController {

    private final TradeInService tradeInService;

    @PostMapping("/evaluate")
    public R<TradeInVO> evaluate(@Valid @RequestBody TradeInEvaluateDTO dto) {
        return R.ok(tradeInService.evaluate(dto));
    }

    @GetMapping("/pending")
    public R<List<TradeInVO>> pending() {
        return R.ok(tradeInService.listPendingForRecycler());
    }

    @GetMapping("/detail/{id}")
    public R<TradeInVO> detail(@PathVariable Long id) {
        return R.ok(tradeInService.detail(id));
    }
}
