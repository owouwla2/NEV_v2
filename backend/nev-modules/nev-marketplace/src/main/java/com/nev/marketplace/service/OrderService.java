package com.nev.marketplace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nev.common.core.exception.ServiceException;
import com.nev.common.satoken.utils.LoginHelper;
import com.nev.marketplace.domain.NevCartItemDO;
import com.nev.marketplace.domain.NevMerchantDO;
import com.nev.marketplace.domain.NevOrderDO;
import com.nev.marketplace.domain.NevOrderItemDO;
import com.nev.marketplace.domain.NevProductDO;
import com.nev.marketplace.dto.AddressDTO;
import com.nev.marketplace.dto.OrderCreateDTO;
import com.nev.marketplace.dto.OrderQueryDTO;
import com.nev.marketplace.dto.OrderVO;
import com.nev.marketplace.mapper.NevCartItemMapper;
import com.nev.marketplace.mapper.NevMerchantMapper;
import com.nev.marketplace.mapper.NevOrderItemMapper;
import com.nev.marketplace.mapper.NevOrderMapper;
import com.nev.marketplace.mapper.NevProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 订单服务
 *
 * @author NEV-v2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final NevOrderMapper orderMapper;
    private final NevOrderItemMapper orderItemMapper;
    private final NevCartItemMapper cartItemMapper;
    private final NevProductMapper productMapper;
    private final NevMerchantMapper merchantMapper;
    private final ObjectMapper objectMapper;

    /** 从购物车选项创建订单（同一订单必须来自同一商家） */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createFromCart(OrderCreateDTO dto) {
        Long userId = currentUserId();

        // 1. 拉所有 cart_item
        List<NevCartItemDO> items = cartItemMapper.selectBatchIds(dto.getCartItemIds());
        if (items.size() != dto.getCartItemIds().size()) {
            throw new ServiceException("购物车明细已变化，请刷新购物车");
        }

        // 2. 拉商品 + 校验
        List<Long> productIds = items.stream().map(NevCartItemDO::getProductId).distinct().collect(Collectors.toList());
        Map<Long, NevProductDO> productMap = productMapper.selectBatchIds(productIds).stream()
            .collect(Collectors.toMap(NevProductDO::getId, p -> p));

        Set<Long> merchantIds = new java.util.HashSet<>();
        for (NevCartItemDO ci : items) {
            NevProductDO p = productMap.get(ci.getProductId());
            if (p == null) throw new ServiceException("商品 [{}] 不存在", ci.getProductId());
            if (!"ON_SALE".equals(p.getStatus())) throw new ServiceException("商品 [{}] 已下架", p.getTitle());
            if (p.getStock() == null || p.getStock() < ci.getQuantity()) {
                throw new ServiceException("商品 [{}] 库存不足（剩余 {}）", p.getTitle(), p.getStock());
            }
            merchantIds.add(p.getMerchantId());
        }
        if (merchantIds.size() != 1) {
            throw new ServiceException("一次下单的商品必须来自同一商家");
        }
        Long merchantId = merchantIds.iterator().next();

        // 3. 扣库存
        BigDecimal total = BigDecimal.ZERO;
        for (NevCartItemDO ci : items) {
            NevProductDO p = productMap.get(ci.getProductId());
            p.setStock(p.getStock() - ci.getQuantity());
            p.setSalesCount((p.getSalesCount() == null ? 0 : p.getSalesCount()) + ci.getQuantity());
            if (p.getStock() == 0) {
                p.setStatus("SOLD_OUT");
            }
            productMapper.updateById(p);
            total = total.add(ci.getUnitPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
        }

        // 4. 写订单主表
        Date now = new Date();
        NevOrderDO order = new NevOrderDO();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setMerchantId(merchantId);
        order.setTotalAmount(total);
        order.setStatus("PENDING");
        order.setAddressSnapshot(toJson(dto.getAddress()));
        order.setRemark(dto.getRemark());
        order.setTenantId("000000");
        order.setDelFlag("0");
        orderMapper.insert(order);

        // 5. 写订单明细
        List<NevOrderItemDO> orderItems = new ArrayList<>(items.size());
        for (NevCartItemDO ci : items) {
            NevProductDO p = productMap.get(ci.getProductId());
            NevOrderItemDO oi = new NevOrderItemDO();
            oi.setOrderId(order.getId());
            oi.setProductId(p.getId());
            oi.setProductSnapshot(productSnapshot(p));
            oi.setQuantity(ci.getQuantity());
            oi.setUnitPrice(ci.getUnitPrice());
            oi.setSubtotal(ci.getUnitPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
            oi.setTenantId("000000");
            oi.setDelFlag("0");
            orderItemMapper.insert(oi);
            orderItems.add(oi);
        }

        // 6. 删除已下单的 cart_item
        cartItemMapper.deleteByIds(items.stream().map(NevCartItemDO::getId).collect(Collectors.toList()));

        return toVo(order, orderItems, productMap, merchantMapper.selectById(merchantId));
    }

    /** consumer 看自己的订单 */
    public Page<OrderVO> listMineConsumer(OrderQueryDTO q) {
        Long userId = currentUserId();
        LambdaQueryWrapper<NevOrderDO> w = new LambdaQueryWrapper<>();
        w.eq(NevOrderDO::getUserId, userId);
        if (StringUtils.hasText(q.getStatus())) w.eq(NevOrderDO::getStatus, q.getStatus());
        w.orderByDesc(NevOrderDO::getId);
        return paginate(w, q.getPageNum(), q.getPageSize());
    }

    /** merchant 看自家订单 */
    public Page<OrderVO> listMineMerchant(OrderQueryDTO q) {
        Long userId = currentUserId();
        NevMerchantDO m = findMerchantByUserId(userId);
        if (m == null) {
            return new Page<>(q.getPageNum(), q.getPageSize(), 0);
        }
        LambdaQueryWrapper<NevOrderDO> w = new LambdaQueryWrapper<>();
        w.eq(NevOrderDO::getMerchantId, m.getId());
        if (StringUtils.hasText(q.getStatus())) w.eq(NevOrderDO::getStatus, q.getStatus());
        w.orderByDesc(NevOrderDO::getId);
        return paginate(w, q.getPageNum(), q.getPageSize());
    }

    /** 详情（consumer 看自己的或 merchant 看自家的） */
    public OrderVO detail(Long orderId) {
        NevOrderDO order = orderMapper.selectById(orderId);
        if (order == null) throw new ServiceException("订单 [{}] 不存在", orderId);
        Long userId = currentUserId();
        boolean isOwner = userId.equals(order.getUserId());
        boolean isMerchant = false;
        NevMerchantDO m = findMerchantByUserId(userId);
        if (m != null && m.getId().equals(order.getMerchantId())) isMerchant = true;
        if (!isOwner && !isMerchant) {
            throw new ServiceException("无权查看该订单");
        }
        return buildVo(order);
    }

    /** consumer 取消未支付订单 */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId, String reason) {
        Long userId = currentUserId();
        NevOrderDO order = orderMapper.selectById(orderId);
        if (order == null) throw new ServiceException("订单 [{}] 不存在", orderId);
        if (!userId.equals(order.getUserId())) {
            throw new ServiceException("无权取消他人订单");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new ServiceException("订单当前状态 [{}] 不可取消", order.getStatus());
        }
        order.setStatus("CANCELLED");
        order.setCancelledAt(new Date());
        order.setCancelReason(reason);
        orderMapper.updateById(order);

        // 还原库存
        LambdaQueryWrapper<NevOrderItemDO> iq = new LambdaQueryWrapper<>();
        iq.eq(NevOrderItemDO::getOrderId, orderId);
        List<NevOrderItemDO> items = orderItemMapper.selectList(iq);
        for (NevOrderItemDO it : items) {
            NevProductDO p = productMapper.selectById(it.getProductId());
            if (p != null) {
                p.setStock(p.getStock() + it.getQuantity());
                p.setSalesCount(Math.max(0, (p.getSalesCount() == null ? 0 : p.getSalesCount()) - it.getQuantity()));
                if ("SOLD_OUT".equals(p.getStatus()) && p.getStock() > 0) {
                    p.setStatus("ON_SALE");
                }
                productMapper.updateById(p);
            }
        }
    }

    // ----------------- helpers -----------------

    private Page<OrderVO> paginate(LambdaQueryWrapper<NevOrderDO> w, int pageNum, int pageSize) {
        Page<NevOrderDO> page = new Page<>(pageNum, pageSize);
        Page<NevOrderDO> rows = orderMapper.selectPage(page, w);

        // 批量取明细
        List<Long> orderIds = rows.getRecords().stream().map(NevOrderDO::getId).collect(Collectors.toList());
        Map<Long, List<NevOrderItemDO>> itemsMap;
        if (orderIds.isEmpty()) {
            itemsMap = new java.util.HashMap<>();
        } else {
            LambdaQueryWrapper<NevOrderItemDO> iq = new LambdaQueryWrapper<>();
            iq.in(NevOrderItemDO::getOrderId, orderIds);
            itemsMap = orderItemMapper.selectList(iq).stream()
                .collect(Collectors.groupingBy(NevOrderItemDO::getOrderId, LinkedHashMap::new, Collectors.toList()));
        }

        // 批量取商家
        List<Long> merchantIds = rows.getRecords().stream().map(NevOrderDO::getMerchantId).distinct().collect(Collectors.toList());
        Map<Long, NevMerchantDO> mMap = merchantIds.isEmpty() ? new java.util.HashMap<>()
            : merchantMapper.selectBatchIds(merchantIds).stream()
                .collect(Collectors.toMap(NevMerchantDO::getId, m -> m));

        Page<OrderVO> voPage = new Page<>(rows.getCurrent(), rows.getSize(), rows.getTotal());
        voPage.setRecords(rows.getRecords().stream()
            .map(o -> toVo(o, itemsMap.getOrDefault(o.getId(), Collections.emptyList()), null, mMap.get(o.getMerchantId())))
            .collect(Collectors.toList()));
        return voPage;
    }

    private OrderVO buildVo(NevOrderDO order) {
        LambdaQueryWrapper<NevOrderItemDO> iq = new LambdaQueryWrapper<>();
        iq.eq(NevOrderItemDO::getOrderId, order.getId());
        List<NevOrderItemDO> items = orderItemMapper.selectList(iq);
        NevMerchantDO m = merchantMapper.selectById(order.getMerchantId());
        return toVo(order, items, null, m);
    }

    private OrderVO toVo(NevOrderDO order, List<NevOrderItemDO> items,
                         Map<Long, NevProductDO> productMap, NevMerchantDO merchant) {
        AddressDTO addr = null;
        try {
            if (StringUtils.hasText(order.getAddressSnapshot())) {
                addr = objectMapper.readValue(order.getAddressSnapshot(), AddressDTO.class);
            }
        } catch (Exception ignored) {
        }
        List<OrderVO.Item> voItems = items.stream().map(it -> {
            Map<String, Object> snap = parseSnapshot(it.getProductSnapshot());
            return OrderVO.Item.builder()
                .id(it.getId())
                .productId(it.getProductId())
                .title(asString(snap, "title"))
                .category(asString(snap, "category"))
                .batteryId(asLong(snap, "batteryId"))
                .images(parseImagesFromSnapshot(snap))
                .quantity(it.getQuantity())
                .unitPrice(it.getUnitPrice())
                .subtotal(it.getSubtotal())
                .build();
        }).collect(Collectors.toList());

        return OrderVO.builder()
            .id(order.getId())
            .orderNo(order.getOrderNo())
            .userId(order.getUserId())
            .merchantId(order.getMerchantId())
            .merchantName(merchant != null ? merchant.getMerchantName() : null)
            .totalAmount(order.getTotalAmount())
            .payAmount(order.getPayAmount())
            .status(order.getStatus())
            .address(addr)
            .items(voItems)
            .paidAt(order.getPaidAt())
            .shippedAt(order.getShippedAt())
            .deliveredAt(order.getDeliveredAt())
            .completedAt(order.getCompletedAt())
            .cancelledAt(order.getCancelledAt())
            .cancelReason(order.getCancelReason())
            .createTime(order.getCreateTime())
            .build();
    }

    private String generateOrderNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rnd = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "ORD" + ts + rnd;
    }

    private String productSnapshot(NevProductDO p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("productId", p.getId());
        m.put("title", p.getTitle());
        m.put("category", p.getCategory());
        m.put("price", p.getPrice());
        m.put("batteryId", p.getBatteryId());
        m.put("images", p.getImages()); // 保留原 JSON 字符串
        return toJson(m);
    }

    private Map<String, Object> parseSnapshot(String json) {
        if (!StringUtils.hasText(json)) return new java.util.HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new java.util.HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> parseImagesFromSnapshot(Map<String, Object> snap) {
        Object imgs = snap.get("images");
        if (imgs == null) return Collections.emptyList();
        if (imgs instanceof List) return (List<String>) imgs;
        if (imgs instanceof String s && StringUtils.hasText(s)) {
            try {
                return objectMapper.readValue(s, new TypeReference<>() {});
            } catch (Exception ignored) {
            }
        }
        return Collections.emptyList();
    }

    private String asString(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : v.toString();
    }

    private Long asLong(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new ServiceException("序列化失败: " + e.getMessage());
        }
    }

    private NevMerchantDO findMerchantByUserId(Long userId) {
        LambdaQueryWrapper<NevMerchantDO> q = new LambdaQueryWrapper<>();
        q.eq(NevMerchantDO::getUserId, userId).last("limit 1");
        return merchantMapper.selectOne(q);
    }

    private Long currentUserId() {
        Long uid = LoginHelper.getUserId();
        if (uid == null) throw new ServiceException("未登录");
        return uid;
    }
}
