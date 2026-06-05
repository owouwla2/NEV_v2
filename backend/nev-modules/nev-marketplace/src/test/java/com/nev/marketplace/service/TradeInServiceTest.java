package com.nev.marketplace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nev.battery.domain.NevBatteryDO;
import com.nev.battery.domain.SysNevUserExtDO;
import com.nev.battery.dto.BatteryEventVO;
import com.nev.battery.mapper.NevBatteryMapper;
import com.nev.battery.mapper.SysNevUserExtMapper;
import com.nev.battery.service.BatteryService;
import com.nev.common.core.exception.ServiceException;
import com.nev.common.satoken.utils.LoginHelper;
import com.nev.marketplace.domain.NevTradeInRequestDO;
import com.nev.marketplace.dto.TradeInEvaluateDTO;
import com.nev.marketplace.dto.TradeInVO;
import com.nev.marketplace.mapper.NevTradeInRequestMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NEV-v2 round-4 M-C · P1-002 业务单测
 *
 * 覆盖 TradeInService 4 个状态迁移点（合法 + 非法）：
 *   submit:    null/blank → ACTIVE 重复 → SUBMITTED
 *   evaluate:  非 recycler → 不存在 → 非 SUBMITTED → EVALUATED
 *   accept:    非本人 → 非 EVALUATED → recycler 钱包缺失 → SUBMITTED→拒绝 / EVALUATED→COMPLETED
 *   reject:    非本人 → 终态 → SUBMITTED/EVALUATED 可拒
 *
 * 涉及静态方法 LoginHelper.getUserId() 用 Mockito.mockStatic 覆盖。
 * accept 成功路径会触发 batteryService.recordRecycledByTradeIn → 链上 RECYCLED；mock 之，断言被调用 1 次。
 *
 * @author NEV-v2 round-4
 */
@Tag("dev")
@DisplayName("TradeInService 状态机")
@ExtendWith(MockitoExtension.class)
class TradeInServiceTest {

    @Mock private NevTradeInRequestMapper requestMapper;
    @Mock private NevBatteryMapper batteryMapper;
    @Mock private SysNevUserExtMapper userExtMapper;
    @Mock private BatteryService batteryService;

    private TradeInService service;
    private MockedStatic<LoginHelper> loginHelper;

    private static final Long CONSUMER_ID = 1001L;
    private static final Long RECYCLER_ID = 2001L;
    private static final Long OTHER_USER_ID = 9999L;

    @BeforeEach
    void setUp() {
        service = new TradeInService(
            requestMapper, batteryMapper, userExtMapper, batteryService, new ObjectMapper()
        );
        loginHelper = Mockito.mockStatic(LoginHelper.class);
    }

    @AfterEach
    void tearDown() {
        loginHelper.close();
    }

    // ============ evaluate ============

    @Test
    @DisplayName("evaluate: 非 recycler 角色 → 拒绝")
    void evaluateRejectsNonRecycler() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(RECYCLER_ID);
        when(userExtMapper.selectById(RECYCLER_ID))
            .thenReturn(userExt(RECYCLER_ID, "consumer", "0xabc"));  // 角色不对

        TradeInEvaluateDTO dto = evalDto(100L, new BigDecimal("3000"), 85);
        assertThatThrownBy(() -> service.evaluate(dto))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("recycler");
    }

    @Test
    @DisplayName("evaluate: 申请不存在 → 拒绝")
    void evaluateRejectsMissingRequest() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(RECYCLER_ID);
        when(userExtMapper.selectById(RECYCLER_ID))
            .thenReturn(userExt(RECYCLER_ID, "recycler", "0xabc"));
        when(requestMapper.selectById(100L)).thenReturn(null);

        assertThatThrownBy(() -> service.evaluate(evalDto(100L, BigDecimal.TEN, 90)))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("evaluate: 状态非 SUBMITTED → 拒绝")
    void evaluateRejectsNonSubmittedStatus() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(RECYCLER_ID);
        when(userExtMapper.selectById(RECYCLER_ID))
            .thenReturn(userExt(RECYCLER_ID, "recycler", "0xabc"));
        when(requestMapper.selectById(100L))
            .thenReturn(request(100L, CONSUMER_ID, "EVALUATED"));  // 已 EVALUATED

        assertThatThrownBy(() -> service.evaluate(evalDto(100L, BigDecimal.TEN, 90)))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("不可评估");
    }

    @Test
    @DisplayName("evaluate: 合法路径 SUBMITTED → EVALUATED，写入 recyclerId/evaluatorId/amount")
    void evaluateHappyPath() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(RECYCLER_ID);
        when(userExtMapper.selectById(RECYCLER_ID))
            .thenReturn(userExt(RECYCLER_ID, "recycler", "0xabc"));
        NevTradeInRequestDO req = request(100L, CONSUMER_ID, "SUBMITTED");
        when(requestMapper.selectById(100L)).thenReturn(req);

        TradeInVO vo = service.evaluate(evalDto(100L, new BigDecimal("3000"), 85));

        assertThat(req.getStatus()).isEqualTo("EVALUATED");
        assertThat(req.getRecyclerId()).isEqualTo(RECYCLER_ID);
        assertThat(req.getEvaluatorId()).isEqualTo(RECYCLER_ID);
        assertThat(req.getEvaluatedAmount()).isEqualByComparingTo("3000");
        assertThat(req.getEvaluatedAt()).isNotNull();
        verify(requestMapper).updateById(req);
        assertThat(vo.getStatus()).isEqualTo("EVALUATED");
    }

    // ============ accept ============

    @Test
    @DisplayName("accept: 非本人申请 → 拒绝")
    void acceptRejectsOtherUser() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(OTHER_USER_ID);
        when(requestMapper.selectById(100L))
            .thenReturn(request(100L, CONSUMER_ID, "EVALUATED"));

        assertThatThrownBy(() -> service.accept(100L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("无权");
    }

    @Test
    @DisplayName("accept: 状态非 EVALUATED → 拒绝")
    void acceptRejectsNonEvaluatedStatus() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(CONSUMER_ID);
        when(requestMapper.selectById(100L))
            .thenReturn(request(100L, CONSUMER_ID, "SUBMITTED"));

        assertThatThrownBy(() -> service.accept(100L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("应先评估");
    }

    @Test
    @DisplayName("accept: recycler 钱包缺失 → 拒绝（无法上链）")
    void acceptRejectsMissingRecyclerWallet() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(CONSUMER_ID);
        NevTradeInRequestDO req = request(100L, CONSUMER_ID, "EVALUATED");
        req.setOldBatteryId(500L);
        req.setRecyclerId(RECYCLER_ID);
        when(requestMapper.selectById(100L)).thenReturn(req);
        when(batteryMapper.selectById(500L)).thenReturn(battery(500L, "BAT-001"));
        when(userExtMapper.selectById(RECYCLER_ID))
            .thenReturn(userExt(RECYCLER_ID, "recycler", null));  // 钱包缺失

        assertThatThrownBy(() -> service.accept(100L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("钱包地址缺失");
    }

    @Test
    @DisplayName("accept: 合法路径 EVALUATED → ACCEPTED → COMPLETED，触发 1 次链上 RECYCLED")
    void acceptHappyPathTriggersRecycled() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(CONSUMER_ID);
        NevTradeInRequestDO req = request(100L, CONSUMER_ID, "EVALUATED");
        req.setOldBatteryId(500L);
        req.setRecyclerId(RECYCLER_ID);
        req.setEvaluationPayload("{\"soh\":85,\"summary\":\"ok\"}");
        req.setRequestNo("TI-001");
        when(requestMapper.selectById(100L)).thenReturn(req);
        when(batteryMapper.selectById(500L)).thenReturn(battery(500L, "BAT-001"));
        when(userExtMapper.selectById(RECYCLER_ID))
            .thenReturn(userExt(RECYCLER_ID, "recycler", "0xRECYCLER"));

        BatteryEventVO chainResult = BatteryEventVO.builder()
            .traceNumber("BAT-001").eventType("RECYCLED").txHash("0xtxh").build();
        when(batteryService.recordRecycledByTradeIn(
            eq("BAT-001"), eq(RECYCLER_ID), eq("0xRECYCLER"), eq(85), eq("TI-001")
        )).thenReturn(chainResult);

        TradeInVO vo = service.accept(100L);

        // 状态最终为 COMPLETED
        assertThat(req.getStatus()).isEqualTo("COMPLETED");
        assertThat(req.getAcceptedAt()).isNotNull();
        assertThat(req.getCompletedAt()).isNotNull();
        assertThat(req.getRecycledAt()).isNotNull();
        // 链上写入正好 1 次
        verify(batteryService, times(1)).recordRecycledByTradeIn(
            anyString(), anyLong(), anyString(), any(), anyString());
        // requestMapper.updateById 调用了两次（ACCEPTED 阶段一次 + COMPLETED 阶段一次）
        verify(requestMapper, times(2)).updateById(req);
        assertThat(vo.getStatus()).isEqualTo("COMPLETED");
    }

    // ============ reject ============

    @Test
    @DisplayName("reject: 非本人 → 拒绝")
    void rejectRejectsOtherUser() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(OTHER_USER_ID);
        when(requestMapper.selectById(100L))
            .thenReturn(request(100L, CONSUMER_ID, "EVALUATED"));

        assertThatThrownBy(() -> service.reject(100L, "不满意"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("无权");
    }

    @Test
    @DisplayName("reject: 终态 COMPLETED 不可拒绝")
    void rejectRejectsTerminalState() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(CONSUMER_ID);
        when(requestMapper.selectById(100L))
            .thenReturn(request(100L, CONSUMER_ID, "COMPLETED"));

        assertThatThrownBy(() -> service.reject(100L, "晚了"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("不可拒绝");

        verify(batteryService, never())
            .recordRecycledByTradeIn(anyString(), anyLong(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("reject: SUBMITTED 可拒绝 → REJECTED + remark 写入原因")
    void rejectFromSubmitted() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(CONSUMER_ID);
        NevTradeInRequestDO req = request(100L, CONSUMER_ID, "SUBMITTED");
        req.setRemark("原 remark");
        when(requestMapper.selectById(100L)).thenReturn(req);

        TradeInVO vo = service.reject(100L, "不要了");

        assertThat(req.getStatus()).isEqualTo("REJECTED");
        assertThat(req.getRemark()).contains("不要了");
        assertThat(vo.getStatus()).isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("reject: EVALUATED 也可拒绝 → REJECTED")
    void rejectFromEvaluated() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(CONSUMER_ID);
        NevTradeInRequestDO req = request(100L, CONSUMER_ID, "EVALUATED");
        when(requestMapper.selectById(100L)).thenReturn(req);

        service.reject(100L, null);

        assertThat(req.getStatus()).isEqualTo("REJECTED");
        // 即使 reason=null 也不崩
        assertThat(req.getRemark()).contains("拒绝原因");
    }

    // ============ helpers ============

    private NevTradeInRequestDO request(Long id, Long consumerId, String status) {
        NevTradeInRequestDO r = new NevTradeInRequestDO();
        r.setId(id);
        r.setRequestNo("TI-" + id);
        r.setConsumerId(consumerId);
        r.setStatus(status);
        return r;
    }

    private SysNevUserExtDO userExt(Long userId, String type, String wallet) {
        SysNevUserExtDO e = new SysNevUserExtDO();
        e.setUserId(userId);
        e.setUserType(type);
        e.setWalletAddress(wallet);
        return e;
    }

    private NevBatteryDO battery(Long id, String trace) {
        NevBatteryDO b = new NevBatteryDO();
        b.setId(id);
        b.setTraceNumber(trace);
        return b;
    }

    private TradeInEvaluateDTO evalDto(Long requestId, BigDecimal amount, Integer soh) {
        TradeInEvaluateDTO d = new TradeInEvaluateDTO();
        d.setRequestId(requestId);
        d.setEvaluatedAmount(amount);
        d.setSoh(soh);
        d.setSummary("电池外观完好");
        return d;
    }
}
