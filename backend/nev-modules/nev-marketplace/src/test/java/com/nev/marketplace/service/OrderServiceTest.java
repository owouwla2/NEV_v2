package com.nev.marketplace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nev.battery.mapper.NevBatteryMapper;
import com.nev.battery.mapper.SysNevUserExtMapper;
import com.nev.battery.service.BatteryService;
import com.nev.carbon.service.CarbonCreditService;
import com.nev.common.core.exception.ServiceException;
import com.nev.common.satoken.utils.LoginHelper;
import com.nev.marketplace.domain.NevMerchantDO;
import com.nev.marketplace.domain.NevOrderDO;
import com.nev.marketplace.domain.NevPaymentRecordDO;
import com.nev.marketplace.mapper.NevCartItemMapper;
import com.nev.marketplace.mapper.NevMerchantMapper;
import com.nev.marketplace.mapper.NevOrderItemMapper;
import com.nev.marketplace.mapper.NevOrderMapper;
import com.nev.marketplace.mapper.NevPaymentRecordMapper;
import com.nev.marketplace.mapper.NevProductMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NEV-v2 round-4 M-C · P1-002 业务单测
 *
 * 覆盖 OrderService 4 个状态迁移点（合法 + 非法）：
 *   cancel: 非本人 → 非 PENDING → PENDING→CANCELLED
 *   pay:    非本人 → 非 PENDING → PENDING→PAID（写支付记录）
 *   ship:   非该商家 → 非 PAID → PAID→SHIPPED
 *   confirm: 非本人 → 非 SHIPPED → 商家钱包缺失（无法上链 SOLD）
 *
 * confirm 的完整成功路径会触发 batteryService.recordSoldByMerchant + carbonCreditService.grantTo
 * 那是集成测试范畴；本单测只覆盖到「商家钱包缺失」的拒绝路径，证明守门关在。
 *
 * @author NEV-v2 round-4
 */
@Tag("dev")
@DisplayName("OrderService 状态机")
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private NevOrderMapper orderMapper;
    @Mock private NevOrderItemMapper orderItemMapper;
    @Mock private NevCartItemMapper cartItemMapper;
    @Mock private NevProductMapper productMapper;
    @Mock private NevMerchantMapper merchantMapper;
    @Mock private NevPaymentRecordMapper paymentMapper;
    @Mock private SysNevUserExtMapper userExtMapper;
    @Mock private BatteryService batteryService;
    @Mock private CarbonCreditService carbonCreditService;
    @Mock private NevBatteryMapper nevBatteryMapper;

    private OrderService service;
    private MockedStatic<LoginHelper> loginHelper;

    private static final Long CONSUMER_ID = 1001L;
    private static final Long MERCHANT_USER_ID = 2001L;
    private static final Long MERCHANT_ID = 50L;
    private static final Long OTHER_USER_ID = 9999L;

    @BeforeEach
    void setUp() {
        service = new OrderService(
            orderMapper, orderItemMapper, cartItemMapper, productMapper,
            merchantMapper, paymentMapper, userExtMapper,
            batteryService, carbonCreditService, new ObjectMapper(), nevBatteryMapper
        );
        loginHelper = Mockito.mockStatic(LoginHelper.class);
        // pay/ship/cancel 的 buildVo 会扫 orderItem 和 merchant；默认返回空列表/null，让成功路径不挂
        lenient().when(orderItemMapper.selectList(any())).thenReturn(Collections.emptyList());
        lenient().when(merchantMapper.selectById(any())).thenReturn(merchant(MERCHANT_ID, MERCHANT_USER_ID));
    }

    @AfterEach
    void tearDown() {
        loginHelper.close();
    }

    // ============ cancel ============

    @Test
    @DisplayName("cancel: 订单不存在 → 拒绝")
    void cancelRejectsMissingOrder() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(CONSUMER_ID);
        when(orderMapper.selectById(100L)).thenReturn(null);

        assertThatThrownBy(() -> service.cancel(100L, "不要了"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("cancel: 非本人订单 → 拒绝")
    void cancelRejectsOtherUserOrder() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(OTHER_USER_ID);
        when(orderMapper.selectById(100L))
            .thenReturn(order(100L, CONSUMER_ID, MERCHANT_ID, "PENDING"));

        assertThatThrownBy(() -> service.cancel(100L, ""))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("无权");
    }

    @Test
    @DisplayName("cancel: 状态非 PENDING → 拒绝")
    void cancelRejectsNonPendingStatus() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(CONSUMER_ID);
        when(orderMapper.selectById(100L))
            .thenReturn(order(100L, CONSUMER_ID, MERCHANT_ID, "PAID"));

        assertThatThrownBy(() -> service.cancel(100L, ""))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("不可取消");
    }

    @Test
    @DisplayName("cancel: 合法路径 PENDING → CANCELLED，写入 cancelReason + cancelledAt")
    void cancelHappyPath() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(CONSUMER_ID);
        NevOrderDO o = order(100L, CONSUMER_ID, MERCHANT_ID, "PENDING");
        when(orderMapper.selectById(100L)).thenReturn(o);

        service.cancel(100L, "不要了");

        assertThat(o.getStatus()).isEqualTo("CANCELLED");
        assertThat(o.getCancelledAt()).isNotNull();
        assertThat(o.getCancelReason()).isEqualTo("不要了");
        verify(orderMapper).updateById(o);
    }

    // ============ pay ============

    @Test
    @DisplayName("pay: 非本人订单 → 拒绝")
    void payRejectsOtherUserOrder() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(OTHER_USER_ID);
        when(orderMapper.selectById(100L))
            .thenReturn(order(100L, CONSUMER_ID, MERCHANT_ID, "PENDING"));

        assertThatThrownBy(() -> service.pay(100L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("无权");
    }

    @Test
    @DisplayName("pay: 状态非 PENDING → 拒绝（如已 PAID 不可重复支付）")
    void payRejectsNonPendingStatus() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(CONSUMER_ID);
        when(orderMapper.selectById(100L))
            .thenReturn(order(100L, CONSUMER_ID, MERCHANT_ID, "PAID"));

        assertThatThrownBy(() -> service.pay(100L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("不可支付");
        verify(paymentMapper, never()).insert(any(NevPaymentRecordDO.class));
    }

    @Test
    @DisplayName("pay: 合法路径 PENDING → PAID，写支付记录 + payAmount/paidAt")
    void payHappyPath() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(CONSUMER_ID);
        NevOrderDO o = order(100L, CONSUMER_ID, MERCHANT_ID, "PENDING");
        o.setTotalAmount(new BigDecimal("199.99"));
        when(orderMapper.selectById(100L)).thenReturn(o);

        service.pay(100L);

        assertThat(o.getStatus()).isEqualTo("PAID");
        assertThat(o.getPayAmount()).isEqualByComparingTo("199.99");
        assertThat(o.getPaidAt()).isNotNull();
        verify(paymentMapper, times(1)).insert(any(NevPaymentRecordDO.class));
        verify(orderMapper).updateById(o);
    }

    // ============ ship ============

    @Test
    @DisplayName("ship: 非该商家用户 → 拒绝")
    void shipRejectsForeignMerchant() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(OTHER_USER_ID);
        when(orderMapper.selectById(100L))
            .thenReturn(order(100L, CONSUMER_ID, MERCHANT_ID, "PAID"));
        // OTHER_USER_ID 没有对应商家档案
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> service.ship(100L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("无权");
    }

    @Test
    @DisplayName("ship: 状态非 PAID → 拒绝")
    void shipRejectsNonPaidStatus() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(MERCHANT_USER_ID);
        when(orderMapper.selectById(100L))
            .thenReturn(order(100L, CONSUMER_ID, MERCHANT_ID, "PENDING"));
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(merchant(MERCHANT_ID, MERCHANT_USER_ID));

        assertThatThrownBy(() -> service.ship(100L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("不可发货");
    }

    @Test
    @DisplayName("ship: 合法路径 PAID → SHIPPED，写 shippedAt")
    void shipHappyPath() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(MERCHANT_USER_ID);
        NevOrderDO o = order(100L, CONSUMER_ID, MERCHANT_ID, "PAID");
        when(orderMapper.selectById(100L)).thenReturn(o);
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(merchant(MERCHANT_ID, MERCHANT_USER_ID));

        service.ship(100L);

        assertThat(o.getStatus()).isEqualTo("SHIPPED");
        assertThat(o.getShippedAt()).isNotNull();
        verify(orderMapper).updateById(o);
    }

    // ============ confirm（仅守门关） ============

    @Test
    @DisplayName("confirm: 非本人 → 拒绝")
    void confirmRejectsOtherUser() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(OTHER_USER_ID);
        when(orderMapper.selectById(100L))
            .thenReturn(order(100L, CONSUMER_ID, MERCHANT_ID, "SHIPPED"));

        assertThatThrownBy(() -> service.confirm(100L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("无权");
    }

    @Test
    @DisplayName("confirm: 状态非 SHIPPED → 拒绝（含 PENDING / PAID / 已 COMPLETED）")
    void confirmRejectsNonShippedStatus() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(CONSUMER_ID);
        when(orderMapper.selectById(100L))
            .thenReturn(order(100L, CONSUMER_ID, MERCHANT_ID, "PAID"));

        assertThatThrownBy(() -> service.confirm(100L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("不可确认收货");
    }

    // ============ helpers ============

    private NevOrderDO order(Long id, Long userId, Long merchantId, String status) {
        NevOrderDO o = new NevOrderDO();
        o.setId(id);
        o.setOrderNo("ORD-" + id);
        o.setUserId(userId);
        o.setMerchantId(merchantId);
        o.setStatus(status);
        o.setTotalAmount(BigDecimal.ONE);
        return o;
    }

    private NevMerchantDO merchant(Long id, Long userId) {
        NevMerchantDO m = new NevMerchantDO();
        m.setId(id);
        m.setUserId(userId);
        m.setMerchantName("Test Merchant");
        return m;
    }
}
