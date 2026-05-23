package com.nev.marketplace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nev.common.core.exception.ServiceException;
import com.nev.common.satoken.utils.LoginHelper;
import com.nev.marketplace.domain.NevCartDO;
import com.nev.marketplace.domain.NevCartItemDO;
import com.nev.marketplace.domain.NevMerchantDO;
import com.nev.marketplace.domain.NevProductDO;
import com.nev.marketplace.dto.CartAddDTO;
import com.nev.marketplace.dto.CartItemUpdateDTO;
import com.nev.marketplace.dto.CartVO;
import com.nev.marketplace.mapper.NevCartItemMapper;
import com.nev.marketplace.mapper.NevCartMapper;
import com.nev.marketplace.mapper.NevMerchantMapper;
import com.nev.marketplace.mapper.NevProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 购物车服务
 *
 * 设计：每个用户在每个商家有一个 nev_cart（懒创建），cart 下挂多个 cart_item
 *
 * @author NEV-v2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final NevCartMapper cartMapper;
    private final NevCartItemMapper cartItemMapper;
    private final NevProductMapper productMapper;
    private final NevMerchantMapper merchantMapper;
    private final ObjectMapper objectMapper;

    /** 加入购物车（同商品累加数量） */
    @Transactional(rollbackFor = Exception.class)
    public void add(CartAddDTO dto) {
        Long userId = currentUserId();
        NevProductDO product = productMapper.selectById(dto.getProductId());
        if (product == null) {
            throw new ServiceException("商品 [{}] 不存在", dto.getProductId());
        }
        if (!"ON_SALE".equals(product.getStatus())) {
            throw new ServiceException("商品 [{}] 已下架", product.getTitle());
        }
        if (product.getStock() == null || product.getStock() < dto.getQuantity()) {
            throw new ServiceException("商品 [{}] 库存不足", product.getTitle());
        }

        NevCartDO cart = ensureCart(userId, product.getMerchantId());

        // 找已有 item
        LambdaQueryWrapper<NevCartItemDO> q = new LambdaQueryWrapper<>();
        q.eq(NevCartItemDO::getCartId, cart.getId())
            .eq(NevCartItemDO::getProductId, product.getId()).last("limit 1");
        NevCartItemDO existing = cartItemMapper.selectOne(q);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + dto.getQuantity());
            existing.setUnitPrice(product.getPrice());
            cartItemMapper.updateById(existing);
        } else {
            NevCartItemDO item = new NevCartItemDO();
            item.setCartId(cart.getId());
            item.setProductId(product.getId());
            item.setQuantity(dto.getQuantity());
            item.setUnitPrice(product.getPrice());
            item.setSelected("1");
            item.setTenantId("000000");
            item.setDelFlag("0");
            cartItemMapper.insert(item);
        }
    }

    /** 我的购物车（按 merchant 分组） */
    public CartVO listMine() {
        Long userId = currentUserId();
        LambdaQueryWrapper<NevCartDO> cq = new LambdaQueryWrapper<>();
        cq.eq(NevCartDO::getUserId, userId);
        List<NevCartDO> carts = cartMapper.selectList(cq);
        if (carts.isEmpty()) {
            return CartVO.builder().groups(Collections.emptyList()).grandSelectedTotal(BigDecimal.ZERO).build();
        }

        // 拉所有 item
        List<Long> cartIds = carts.stream().map(NevCartDO::getId).collect(Collectors.toList());
        LambdaQueryWrapper<NevCartItemDO> iq = new LambdaQueryWrapper<>();
        iq.in(NevCartItemDO::getCartId, cartIds).orderByDesc(NevCartItemDO::getId);
        List<NevCartItemDO> items = cartItemMapper.selectList(iq);

        // 拉商品
        List<Long> productIds = items.stream().map(NevCartItemDO::getProductId).distinct().collect(Collectors.toList());
        Map<Long, NevProductDO> productMap = productIds.isEmpty() ? new HashMap<>()
            : productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(NevProductDO::getId, p -> p));

        // 拉商家
        List<Long> merchantIds = carts.stream().map(NevCartDO::getMerchantId).distinct().collect(Collectors.toList());
        Map<Long, NevMerchantDO> merchantMap = merchantIds.isEmpty() ? new HashMap<>()
            : merchantMapper.selectBatchIds(merchantIds).stream()
                .collect(Collectors.toMap(NevMerchantDO::getId, m -> m));

        // 按 cart 分组
        Map<Long, List<NevCartItemDO>> itemsByCart = items.stream()
            .collect(Collectors.groupingBy(NevCartItemDO::getCartId, LinkedHashMap::new, Collectors.toList()));

        List<CartVO.MerchantGroup> groups = new ArrayList<>(carts.size());
        BigDecimal grand = BigDecimal.ZERO;
        for (NevCartDO cart : carts) {
            NevMerchantDO m = merchantMap.get(cart.getMerchantId());
            List<NevCartItemDO> grpItems = itemsByCart.getOrDefault(cart.getId(), Collections.emptyList());
            if (grpItems.isEmpty()) continue;

            BigDecimal selectedSum = BigDecimal.ZERO;
            List<CartVO.Item> voItems = new ArrayList<>(grpItems.size());
            for (NevCartItemDO ci : grpItems) {
                NevProductDO p = productMap.get(ci.getProductId());
                BigDecimal subtotal = ci.getUnitPrice().multiply(BigDecimal.valueOf(ci.getQuantity()));
                if ("1".equals(ci.getSelected())) {
                    selectedSum = selectedSum.add(subtotal);
                }
                voItems.add(CartVO.Item.builder()
                    .itemId(ci.getId())
                    .productId(ci.getProductId())
                    .title(p != null ? p.getTitle() : null)
                    .category(p != null ? p.getCategory() : null)
                    .images(parseImages(p != null ? p.getImages() : null))
                    .unitPrice(ci.getUnitPrice())
                    .quantity(ci.getQuantity())
                    .subtotal(subtotal)
                    .selected(ci.getSelected())
                    .stock(p != null ? p.getStock() : null)
                    .productStatus(p != null ? p.getStatus() : null)
                    .build());
            }
            grand = grand.add(selectedSum);
            groups.add(CartVO.MerchantGroup.builder()
                .cartId(cart.getId())
                .merchantId(cart.getMerchantId())
                .merchantName(m != null ? m.getMerchantName() : null)
                .items(voItems)
                .selectedTotal(selectedSum)
                .build());
        }

        return CartVO.builder().groups(groups).grandSelectedTotal(grand).build();
    }

    /** 改数量或选中状态 */
    @Transactional(rollbackFor = Exception.class)
    public void update(CartItemUpdateDTO dto) {
        NevCartItemDO item = requireMyItem(dto.getItemId());
        if (dto.getQuantity() != null) {
            item.setQuantity(dto.getQuantity());
        }
        if (StringUtils.hasText(dto.getSelected())) {
            item.setSelected(dto.getSelected());
        }
        cartItemMapper.updateById(item);
    }

    /** 删除一条 */
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long itemId) {
        NevCartItemDO item = requireMyItem(itemId);
        cartItemMapper.deleteById(item.getId());
    }

    /** 清空购物车（仅删 item，保留 cart 主表） */
    @Transactional(rollbackFor = Exception.class)
    public void clear() {
        Long userId = currentUserId();
        LambdaQueryWrapper<NevCartDO> cq = new LambdaQueryWrapper<>();
        cq.eq(NevCartDO::getUserId, userId);
        List<Long> cartIds = cartMapper.selectList(cq).stream()
            .map(NevCartDO::getId).collect(Collectors.toList());
        if (cartIds.isEmpty()) return;
        LambdaQueryWrapper<NevCartItemDO> iq = new LambdaQueryWrapper<>();
        iq.in(NevCartItemDO::getCartId, cartIds);
        cartItemMapper.delete(iq);
    }

    // ----------------- helpers -----------------

    private NevCartDO ensureCart(Long userId, Long merchantId) {
        LambdaQueryWrapper<NevCartDO> q = new LambdaQueryWrapper<>();
        q.eq(NevCartDO::getUserId, userId)
            .eq(NevCartDO::getMerchantId, merchantId).last("limit 1");
        NevCartDO existing = cartMapper.selectOne(q);
        if (existing != null) return existing;
        NevCartDO c = new NevCartDO();
        c.setUserId(userId);
        c.setMerchantId(merchantId);
        c.setTenantId("000000");
        c.setDelFlag("0");
        cartMapper.insert(c);
        return c;
    }

    /** 校验 item 存在且属于当前用户 */
    NevCartItemDO requireMyItem(Long itemId) {
        Long userId = currentUserId();
        NevCartItemDO item = cartItemMapper.selectById(itemId);
        if (item == null) {
            throw new ServiceException("购物车明细 [{}] 不存在", itemId);
        }
        NevCartDO cart = cartMapper.selectById(item.getCartId());
        if (cart == null || !userId.equals(cart.getUserId())) {
            throw new ServiceException("无权操作此购物车明细");
        }
        return item;
    }

    private List<String> parseImages(String json) {
        if (!StringUtils.hasText(json)) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Long currentUserId() {
        Long uid = LoginHelper.getUserId();
        if (uid == null) throw new ServiceException("未登录");
        return uid;
    }
}
