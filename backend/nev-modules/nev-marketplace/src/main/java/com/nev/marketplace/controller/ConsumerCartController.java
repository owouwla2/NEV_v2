package com.nev.marketplace.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.nev.common.core.domain.R;
import com.nev.marketplace.dto.CartAddDTO;
import com.nev.marketplace.dto.CartItemUpdateDTO;
import com.nev.marketplace.dto.CartVO;
import com.nev.marketplace.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consumer 端购物车 API
 *
 * @author NEV-v2
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/consumer/cart")
@RequiredArgsConstructor
public class ConsumerCartController {

    private final CartService cartService;

    @SaCheckLogin @SaCheckRole("consumer")
    @PostMapping("/add")
    public R<Void> add(@Valid @RequestBody CartAddDTO dto) {
        cartService.add(dto);
        return R.ok();
    }

    @SaCheckLogin @SaCheckRole("consumer")
    @GetMapping("/list")
    public R<CartVO> list() {
        return R.ok(cartService.listMine());
    }

    @SaCheckLogin @SaCheckRole("consumer")
    @PostMapping("/update")
    public R<Void> update(@Valid @RequestBody CartItemUpdateDTO dto) {
        cartService.update(dto);
        return R.ok();
    }

    @SaCheckLogin @SaCheckRole("consumer")
    @DeleteMapping("/remove/{itemId}")
    public R<Void> remove(@PathVariable Long itemId) {
        cartService.remove(itemId);
        return R.ok();
    }

    @SaCheckLogin @SaCheckRole("consumer")
    @DeleteMapping("/clear")
    public R<Void> clear() {
        cartService.clear();
        return R.ok();
    }
}
