package com.nev.marketplace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nev.common.core.exception.ServiceException;
import com.nev.common.satoken.utils.LoginHelper;
import com.nev.marketplace.domain.NevMerchantDO;
import com.nev.marketplace.domain.NevProductDO;
import com.nev.marketplace.dto.ProductQueryDTO;
import com.nev.marketplace.dto.ProductSaveDTO;
import com.nev.marketplace.dto.ProductVO;
import com.nev.marketplace.mapper.NevMerchantMapper;
import com.nev.marketplace.mapper.NevProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商城商品服务（merchant CRUD + 公开列表）
 *
 * @author NEV-v2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final NevMerchantMapper merchantMapper;
    private final NevProductMapper productMapper;
    private final ObjectMapper objectMapper;

    // ==========================================================================
    // merchant 端
    // ==========================================================================

    /**
     * merchant 上架 / 更新商品
     * 如果当前用户还没有 nev_merchant 档案，自动创建一个最小档案
     */
    @Transactional(rollbackFor = Exception.class)
    public ProductVO save(ProductSaveDTO dto) {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            throw new ServiceException("未登录");
        }
        NevMerchantDO merchant = ensureMerchantProfile(userId);

        NevProductDO entity;
        if (dto.getId() != null) {
            entity = productMapper.selectById(dto.getId());
            if (entity == null) {
                throw new ServiceException("商品 [{}] 不存在", dto.getId());
            }
            if (!entity.getMerchantId().equals(merchant.getId())) {
                throw new ServiceException("无权修改其他商家的商品");
            }
        } else {
            entity = new NevProductDO();
            entity.setMerchantId(merchant.getId());
            entity.setSalesCount(0);
            entity.setTenantId("000000");
            entity.setDelFlag("0");
        }

        entity.setTitle(dto.getTitle());
        entity.setSubtitle(dto.getSubtitle());
        entity.setCategory(dto.getCategory());
        entity.setPrice(dto.getPrice());
        entity.setStock(dto.getStock());
        entity.setBatteryId(dto.getBatteryId());
        entity.setImages(serializeImages(dto.getImages()));
        entity.setDescription(dto.getDescription());
        entity.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "ON_SALE");

        if (entity.getId() == null) {
            productMapper.insert(entity);
        } else {
            productMapper.updateById(entity);
        }

        return toVo(entity, merchant);
    }

    /** 我的商品列表（merchant 后台用） */
    public List<ProductVO> listMine() {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            throw new ServiceException("未登录");
        }
        NevMerchantDO merchant = findMerchantByUserId(userId);
        if (merchant == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<NevProductDO> q = new LambdaQueryWrapper<>();
        q.eq(NevProductDO::getMerchantId, merchant.getId())
            .orderByDesc(NevProductDO::getId);
        List<NevProductDO> rows = productMapper.selectList(q);
        return rows.stream().map(r -> toVo(r, merchant)).collect(Collectors.toList());
    }

    // ==========================================================================
    // 公开端
    // ==========================================================================

    /** 公开分页查询商品（默认只看 ON_SALE） */
    public Page<ProductVO> publicSearch(ProductQueryDTO q) {
        LambdaQueryWrapper<NevProductDO> w = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(q.getOnSaleOnly())) {
            w.eq(NevProductDO::getStatus, "ON_SALE");
        }
        if (StringUtils.hasText(q.getCategory())) {
            w.eq(NevProductDO::getCategory, q.getCategory());
        }
        if (StringUtils.hasText(q.getKeyword())) {
            w.like(NevProductDO::getTitle, q.getKeyword());
        }
        if (q.getMerchantId() != null) {
            w.eq(NevProductDO::getMerchantId, q.getMerchantId());
        }
        w.orderByDesc(NevProductDO::getId);

        Page<NevProductDO> page = new Page<>(q.getPageNum(), q.getPageSize());
        Page<NevProductDO> result = productMapper.selectPage(page, w);

        // 批量取 merchant
        List<Long> merchantIds = result.getRecords().stream()
            .map(NevProductDO::getMerchantId).distinct().collect(Collectors.toList());
        Map<Long, NevMerchantDO> merchantMap = new HashMap<>();
        if (!merchantIds.isEmpty()) {
            List<NevMerchantDO> merchants = merchantMapper.selectBatchIds(merchantIds);
            merchants.forEach(m -> merchantMap.put(m.getId(), m));
        }

        Page<ProductVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
            .map(r -> toVo(r, merchantMap.get(r.getMerchantId())))
            .collect(Collectors.toList()));
        return voPage;
    }

    // ==========================================================================
    // helpers
    // ==========================================================================

    private NevMerchantDO ensureMerchantProfile(Long userId) {
        NevMerchantDO existing = findMerchantByUserId(userId);
        if (existing != null) {
            return existing;
        }
        // 自动建一个最小档案
        NevMerchantDO m = new NevMerchantDO();
        m.setUserId(userId);
        m.setMerchantName("merchant-" + userId);
        m.setStatus("ACTIVE");
        m.setTenantId("000000");
        m.setDelFlag("0");
        merchantMapper.insert(m);
        log.info("[marketplace] 自动创建 merchant 档案 userId={} merchantId={}", userId, m.getId());
        return m;
    }

    private NevMerchantDO findMerchantByUserId(Long userId) {
        LambdaQueryWrapper<NevMerchantDO> q = new LambdaQueryWrapper<>();
        q.eq(NevMerchantDO::getUserId, userId).last("limit 1");
        return merchantMapper.selectOne(q);
    }

    private String serializeImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(images);
        } catch (JsonProcessingException e) {
            throw new ServiceException("商品图片序列化失败: " + e.getMessage());
        }
    }

    private List<String> deserializeImages(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("[marketplace] 商品图片反序列化失败 raw={}", json);
            return Collections.emptyList();
        }
    }

    private ProductVO toVo(NevProductDO p, NevMerchantDO m) {
        return ProductVO.builder()
            .id(p.getId())
            .merchantId(p.getMerchantId())
            .merchantName(m != null ? m.getMerchantName() : null)
            .category(p.getCategory())
            .title(p.getTitle())
            .subtitle(p.getSubtitle())
            .price(p.getPrice())
            .stock(p.getStock())
            .salesCount(p.getSalesCount())
            .batteryId(p.getBatteryId())
            .images(deserializeImages(p.getImages()))
            .description(p.getDescription())
            .status(p.getStatus())
            .createTime(p.getCreateTime())
            .updateTime(p.getUpdateTime())
            .build();
    }
}
