package com.nev.marketplace.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nev.common.core.domain.R;
import com.nev.marketplace.dto.ProductQueryDTO;
import com.nev.marketplace.dto.ProductVO;
import com.nev.marketplace.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开商品列表 API（无需登录，对应 security.excludes /public/**）
 *
 * @author NEV-v2
 */
@Slf4j
@RestController
@RequestMapping("/public/product")
@RequiredArgsConstructor
public class PublicProductController {

    private final ProductService productService;

    @GetMapping("/list")
    public R<Page<ProductVO>> list(@ModelAttribute ProductQueryDTO q) {
        return R.ok(productService.publicSearch(q));
    }
}
