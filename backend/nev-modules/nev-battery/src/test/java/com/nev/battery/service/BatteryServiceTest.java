package com.nev.battery.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nev.battery.domain.NevBatteryDO;
import com.nev.battery.domain.NevBatteryLifecycleDO;
import com.nev.battery.dto.BatteryEventVO;
import com.nev.battery.mapper.NevBatteryLifecycleMapper;
import com.nev.battery.mapper.NevBatteryMapper;
import com.nev.battery.mapper.SysNevUserExtMapper;
import com.nev.blockchain.client.ContractInvoker;
import com.nev.blockchain.dto.ChainCallResult;
import com.nev.common.core.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
@DisplayName("BatteryService 跨模块事件入口")
@ExtendWith(MockitoExtension.class)
class BatteryServiceTest {

    @Mock private NevBatteryMapper batteryMapper;
    @Mock private NevBatteryLifecycleMapper lifecycleMapper;
    @Mock private SysNevUserExtMapper userExtMapper;
    @Mock private ContractInvoker contractInvoker;

    private BatteryService service;

    @BeforeEach
    void setUp() {
        service = new BatteryService(
            batteryMapper, lifecycleMapper, userExtMapper, contractInvoker, new ObjectMapper());
    }

    @Test
    @DisplayName("recordSoldByMerchant: SOLD 事件写链成功后追加 lifecycle 并转移给 consumer")
    void recordSoldByMerchantAppendsSoldEvent() {
        NevBatteryDO battery = battery("BAT-001", "IN_USE");
        when(batteryMapper.selectOne(any())).thenReturn(battery);
        when(lifecycleMapper.selectOne(any())).thenReturn(lifecycle(1));
        when(contractInvoker.invokeAs(eq("0xMERCHANT"), eq("LifecycleTrace"), eq("addEvent"), any()))
            .thenReturn(chain("0xTX", 88L));

        BatteryEventVO vo = service.recordSoldByMerchant(
            "BAT-001", 201L, "0xMERCHANT", "ORD-001", 105L);

        assertThat(vo.getEventType()).isEqualTo("SOLD");
        assertThat(vo.getVersion()).isEqualTo(2);
        assertThat(vo.getCurrentOwnerId()).isEqualTo(105L);
        assertThat(vo.getCurrentRole()).isEqualTo("consumer");
        assertThat(battery.getCurrentStatus()).isEqualTo("SOLD");

        ArgumentCaptor<NevBatteryLifecycleDO> lcCaptor = ArgumentCaptor.forClass(NevBatteryLifecycleDO.class);
        verify(lifecycleMapper).insert(lcCaptor.capture());
        NevBatteryLifecycleDO lc = lcCaptor.getValue();
        assertThat(lc.getEventType()).isEqualTo("SOLD");
        assertThat(lc.getOperatorId()).isEqualTo(201L);
        assertThat(lc.getVersion()).isEqualTo(2);
        assertThat(lc.getPayload()).contains("ORD-001", "marketplace", "merchantUserId");

        verify(batteryMapper).updateById(battery);
    }

    @Test
    @DisplayName("recordSoldByMerchant: 已售出电池拒绝重复销售")
    void soldBatteryRejectsDuplicateMarketplaceSale() {
        when(batteryMapper.selectOne(any())).thenReturn(battery("BAT-001", "SOLD"));

        assertThatThrownBy(() -> service.recordSoldByMerchant(
            "BAT-001", 201L, "0xMERCHANT", "ORD-002", 105L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("已售出");

        verify(contractInvoker, never()).invokeAs(any(), any(), any(), any());
        verify(lifecycleMapper, never()).insert(any(NevBatteryLifecycleDO.class));
        verify(batteryMapper, never()).updateById(any(NevBatteryDO.class));
    }

    @Test
    @DisplayName("recordRecycledByTradeIn: soh=null 时按 0 上链并写 RECYCLED")
    @SuppressWarnings("unchecked")
    void recordRecycledByTradeInUsesZeroWhenSohMissing() {
        NevBatteryDO battery = battery("BAT-001", "SOLD");
        when(batteryMapper.selectOne(any())).thenReturn(battery);
        when(lifecycleMapper.selectOne(any())).thenReturn(null);
        when(contractInvoker.invokeAs(eq("0xRECYCLER"), eq("LifecycleTrace"), eq("addEvent"), any()))
            .thenReturn(chain("0xREC", 99L));

        BatteryEventVO vo = service.recordRecycledByTradeIn(
            "BAT-001", 301L, "0xRECYCLER", null, "TI-001");

        assertThat(vo.getEventType()).isEqualTo("RECYCLED");
        assertThat(vo.getVersion()).isEqualTo(1);
        assertThat(vo.getCurrentOwnerId()).isEqualTo(301L);
        assertThat(vo.getCurrentRole()).isEqualTo("recycler");

        ArgumentCaptor<List<Object>> argsCaptor = ArgumentCaptor.forClass(List.class);
        verify(contractInvoker).invokeAs(
            eq("0xRECYCLER"), eq("LifecycleTrace"), eq("addEvent"), argsCaptor.capture());
        assertThat(argsCaptor.getValue()).containsExactly("BAT-001", 4, argsCaptor.getValue().get(2));

        ArgumentCaptor<NevBatteryLifecycleDO> lcCaptor = ArgumentCaptor.forClass(NevBatteryLifecycleDO.class);
        verify(lifecycleMapper).insert(lcCaptor.capture());
        assertThat(lcCaptor.getValue().getPayload()).contains("\"soh\":0", "trade-in", "TI-001");
    }

    @Test
    @DisplayName("电池已 DISMANTLED 时拒绝继续追加事件")
    void dismantledBatteryRejectsFurtherEvents() {
        when(batteryMapper.selectOne(any())).thenReturn(battery("BAT-001", "DISMANTLED"));

        assertThatThrownBy(() -> service.recordSoldByMerchant(
            "BAT-001", 201L, "0xMERCHANT", "ORD-001", 105L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("已报废拆解");

        verify(contractInvoker, never()).invokeAs(any(), any(), any(), any());
        verify(lifecycleMapper, never()).insert(any(NevBatteryLifecycleDO.class));
    }

    @Test
    @DisplayName("链上 addEvent 失败时不写 lifecycle")
    void chainFailureStopsPersistence() {
        when(batteryMapper.selectOne(any())).thenReturn(battery("BAT-001", "IN_USE"));
        when(contractInvoker.invokeAs(eq("0xMERCHANT"), eq("LifecycleTrace"), eq("addEvent"), any()))
            .thenReturn(ChainCallResult.builder().success(false).message("front down").build());

        assertThatThrownBy(() -> service.recordSoldByMerchant(
            "BAT-001", 201L, "0xMERCHANT", "ORD-001", 105L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("front down");

        verify(lifecycleMapper, never()).insert(any(NevBatteryLifecycleDO.class));
        verify(batteryMapper, never()).updateById(any(NevBatteryDO.class));
    }

    private NevBatteryDO battery(String traceNumber, String status) {
        NevBatteryDO battery = new NevBatteryDO();
        battery.setId(10L);
        battery.setTraceNumber(traceNumber);
        battery.setCurrentStatus(status);
        return battery;
    }

    private NevBatteryLifecycleDO lifecycle(int version) {
        NevBatteryLifecycleDO lc = new NevBatteryLifecycleDO();
        lc.setVersion(version);
        return lc;
    }

    private ChainCallResult chain(String txHash, Long blockNumber) {
        return ChainCallResult.builder()
            .success(true)
            .txHash(txHash)
            .blockNumber(blockNumber)
            .build();
    }
}
