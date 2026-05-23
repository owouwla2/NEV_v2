package com.nev.marketplace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nev.battery.domain.SysNevUserExtDO;
import com.nev.battery.mapper.SysNevUserExtMapper;
import com.nev.battery.service.BatteryService;
import com.nev.common.core.exception.ServiceException;
import com.nev.common.satoken.utils.LoginHelper;
import com.nev.marketplace.domain.NevCartItemDO;
import com.nev.marketplace.domain.NevMerchantDO;
import com.nev.marketplace.domain.NevOrderDO;
import com.nev.marketplace.domain.NevOrderItemDO;
import com.nev.marketplace.domain.NevPaymentRecordDO;
import com.nev.marketplace.domain.NevProductDO;
import com.nev.marketplace.dto.AddressDTO;
import com.nev.marketplace.dto.OrderCreateDTO;
import com.nev.marketplace.dto.OrderQueryDTO;
import com.nev.marketplace.dto.OrderVO;
import com.nev.marketplace.mapper.NevCartItemMapper;
import com.nev.marketplace.mapper.NevMerchantMapper;
import com.nev.marketplace.mapper.NevOrderItemMapper;
import com.nev.marketplace.mapper.NevOrderMapper;
import com.nev.marketplace.mapper.NevPaymentRecordMapper;
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
    private final NevPaymentRecordMapper paymentMapper;
    private final SysNevUserExtMapper userExtMapper;
    private final BatteryService batteryService;
    private final com.nev.carbon.service.CarbonCreditService carbonCreditService;
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

        // 6. 物理删除已下单的 cart_item（绕过 @TableLogic，避免唯一索引复用冲突）
        cartItemMapper.physicalDeleteByIds(items.stream().map(NevCartItemDO::getId).collect(Collectors.toList()));

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

    /** consumer mock 支付：PENDING -> PAID + 写 nev_payment_record */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO pay(Long orderId) {
        Long userId = currentUserId();
        NevOrderDO order = orderMapper.selectById(orderId);
        if (order == null) throw new ServiceException("订单 [{}] 不存在", orderId);
        if (!userId.equals(order.getUserId())) {
            throw new ServiceException("无权支付他人订单");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new ServiceException("订单当前状态 [{}] 不可支付", order.getStatus());
        }
        Date now = new Date();
        // 1. 写支付记录
        NevPaymentRecordDO pay = new NevPaymentRecordDO();
        pay.setOrderId(order.getId());
        pay.setPaymentNo(generatePaymentNo());
        pay.setAmount(order.getTotalAmount());
        pay.setMethod("MOCK");
        pay.setStatus("SUCCESS");
        pay.setTradeNo(pay.getPaymentNo());
        pay.setPaidAt(now);
        pay.setTenantId("000000");
        pay.setDelFlag("0");
        paymentMapper.insert(pay);

        // 2. 改订单状态
        order.setStatus("PAID");
        order.setPayAmount(order.getTotalAmount());
        order.setPaidAt(now);
        orderMapper.updateById(order);

        return buildVo(order);
    }

    /** merchant 发货：PAID -> SHIPPED；校验是当前 merchant 自家订单 */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO ship(Long orderId) {
        Long userId = currentUserId();
        NevOrderDO order = orderMapper.selectById(orderId);
        if (order == null) throw new ServiceException("订单 [{}] 不存在", orderId);
        NevMerchantDO m = findMerchantByUserId(userId);
        if (m == null || !m.getId().equals(order.getMerchantId())) {
            throw new ServiceException("无权对该订单发货");
        }
        if (!"PAID".equals(order.getStatus())) {
            throw new ServiceException("订单当前状态 [{}] 不可发货", order.getStatus());
        }
        order.setStatus("SHIPPED");
        order.setShippedAt(new Date());
        orderMapper.updateById(order);
        return buildVo(order);
    }

    /** consumer 确认收货：SHIPPED -> COMPLETED；含 batteryId 商品自动触发链上 SOLD */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO confirm(Long orderId) {
        Long userId = currentUserId();
        NevOrderDO order = orderMapper.selectById(orderId);
        if (order == null) throw new ServiceException("订单 [{}] 不存在", orderId);
        if (!userId.equals(order.getUserId())) {
            throw new ServiceException("无权确认他人订单");
        }
        if (!"SHIPPED".equals(order.getStatus())) {
            throw new ServiceException("订单当前状态 [{}] 不可确认收货", order.getStatus());
        }

        // 1. 找商家 wallet
        NevMerchantDO merchant = merchantMapper.selectById(order.getMerchantId());
        if (merchant == null) {
            throw new ServiceException("订单商家档案丢失");
        }
        SysNevUserExtDO merchantExt = userExtMapper.selectById(merchant.getUserId());
        if (merchantExt == null || !StringUtils.hasText(merchantExt.getWalletAddress())) {
            throw new ServiceException("商家用户 [{}] 未绑定钱包地址，无法触发链上 SOLD", merchant.getUserId());
        }

        // 2. 推进订单状态
        Date now = new Date();
        order.setStatus("COMPLETED");
        order.setDeliveredAt(now);
        order.setCompletedAt(now);
        orderMapper.updateById(order);

        // 3. 找所有含 batteryId 的订单明细，逐个触发链上 SOLD
        LambdaQueryWrapper<NevOrderItemDO> iq = new LambdaQueryWrapper<>();
        iq.eq(NevOrderItemDO::getOrderId, order.getId());
        List<NevOrderItemDO> items = orderItemMapper.selectList(iq);

        // 从 snapshot 中提取 batteryId（最稳）
        for (NevOrderItemDO it : items) {
            Long batteryId = asLong(parseSnapshot(it.getProductSnapshot()), "batteryId");
            if (batteryId == null) {
                continue;
            }
            // 从 nev_battery 拿 traceNumber 由 batteryService 在内部完成；这里我们查 product 拿
            // 简化：直接从 productMapper 拿当前商品的 batteryId（如商品改过 batteryId 优先用快照）
            // 这里取 snapshot.batteryId 即可；然后调链
            String traceNumber = findTraceNumberByBatteryId(batteryId);
            if (traceNumber == null) {
                log.warn("[order] battery_id={} 找不到 trace_number，跳过链上 SOLD", batteryId);
                continue;
            }
            log.info("[order] order#{} -> recordSoldByMerchant trace={} merchantUser={} consumer={}",
                order.getId(), traceNumber, merchant.getUserId(), order.getUserId());
            batteryService.recordSoldByMerchant(
                traceNumber, merchant.getUserId(), merchantExt.getWalletAddress(),
                order.getOrderNo(), order.getUserId()
            );
            // 给 consumer 发碳积分（按电池碳足迹 EOL 减排量）
            try {
                java.math.BigDecimal credit = carbonCreditService.awardFromOrder(
                    order.getUserId(), batteryId, order.getId());
                if (credit.signum() > 0) {
                    log.info("[order] order#{} -> consumer#{} +{} kgCO2eq 碳积分",
                        order.getId(), order.getUserId(), credit);
                }
            } catch (Exception ex) {
                log.error("[order] 碳积分发放失败 order#{} battery#{} err={}",
                    order.getId(), batteryId, ex.getMessage());
                // 积分发放失败不回滚订单（订单已完成 + 链上 SOLD 已写入），但记录日志
            }
        }

        return buildVo(order);
    }

    private String findTraceNumberByBatteryId(Long batteryId) {
        // 在 nev_battery 表里查（用 nev-battery 模块的 mapper，跨模块的简单办法是再用 sql 注入）
        // 这里用最简单方式：通过 BatteryService 暴露的入口反查。
        // 实际我们已经把 batteryId 存到 product.battery_id，订单 snapshot 也存了 batteryId，
        // 但 trace_number 是电池业务编号。需要从 nev_battery 表反查。
        // 简化：直接执行原生 SQL（通过 productMapper 的 sqlSession？）
        // 为了避免额外依赖，复用 jdbc：在 helper 里做。
        // 这里通过 productMapper 拿不到 trace_number，需要 nev-battery 的 NevBatteryMapper。
        // 但 nev-marketplace 已 depend on nev-battery，可以注入 NevBatteryMapper。
        return traceNumberCache.computeIfAbsent(batteryId, this::lookupTraceNumber);
    }

    private final java.util.concurrent.ConcurrentHashMap<Long, String> traceNumberCache = new java.util.concurrent.ConcurrentHashMap<>();

    private String lookupTraceNumber(Long batteryId) {
        // 用 BatteryService 没有暴露 trace 查询接口；这里直接走 nev_battery 表的 mapper
        com.nev.battery.domain.NevBatteryDO b = nevBatteryMapper.selectById(batteryId);
        return b == null ? null : b.getTraceNumber();
    }

    private final com.nev.battery.mapper.NevBatteryMapper nevBatteryMapper;

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

    private String generatePaymentNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rnd = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "PAY" + ts + rnd;
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
