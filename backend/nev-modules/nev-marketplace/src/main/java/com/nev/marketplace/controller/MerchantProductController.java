package com.nev.marketplace.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.nev.common.core.domain.R;
import com.nev.marketplace.dto.ProductSaveDTO;
import com.nev.marketplace.dto.ProductVO;
import com.nev.marketplace.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Merchant 端商品管理 API
 *
 * @author NEV-v2
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/merchant/product")
@RequiredArgsConstructor
public class MerchantProductController {

    private final ProductService productService;

    /**
     * 上架 / 更新商品（id 为空 = 新增）
     */
    @SaCheckLogin
    @SaCheckRole("merchant")
    @PostMapping("/save")
    public R<ProductVO> save(@Valid @RequestBody ProductSaveDTO dto) {
        return R.ok(productService.save(dto));
    }

    /**
     * 我的商品列表
     */
    @SaCheckLogin
    @SaCheckRole("merchant")
    @GetMapping("/mine")
    public R<List<ProductVO>> mine() {
        return R.ok(productService.listMine());
    }
}
