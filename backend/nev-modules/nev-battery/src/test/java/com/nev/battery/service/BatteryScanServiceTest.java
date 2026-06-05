package com.nev.battery.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nev.battery.api.CarbonScanEnricher;
import com.nev.battery.domain.NevBatteryDO;
import com.nev.battery.domain.NevBatteryLifecycleDO;
import com.nev.battery.dto.BatteryScanVO;
import com.nev.battery.mapper.NevBatteryLifecycleMapper;
import com.nev.battery.mapper.NevBatteryMapper;
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
import org.springframework.beans.factory.ObjectProvider;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NEV-v2 round-4 M-A · P1-002 示范单测之二
 *
 * 覆盖 BatteryScanService.scan() 的关键路径：
 *   1. trace 入参空白 → ServiceException
 *   2. 电池不存在 → ServiceException
 *   3. 正常路径：lifecycle 含 PRODUCED + IN_USE，verifyOnChain 应分流到 verifyBattery / verifyEvent
 *      —— 这是 round-2 M-1 的关键修复点，必须有断言保护
 *   4. overallVerified 在所有 chainVerified=true 且 chainEventCount 对得上时为 true
 *
 * 全部用 Mockito，无 Spring Context；@Tag("dev") 让默认 profile 跑得到。
 *
 * @author NEV-v2 round-4
 */
@Tag("dev")
@DisplayName("BatteryScanService.scan")
@ExtendWith(MockitoExtension.class)
class BatteryScanServiceTest {

    @Mock private NevBatteryMapper batteryMapper;
    @Mock private NevBatteryLifecycleMapper lifecycleMapper;
    @Mock private ContractInvoker contractInvoker;
    @Mock private ObjectProvider<CarbonScanEnricher> carbonEnricherProvider;

    private BatteryScanService service;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        service = new BatteryScanService(
            batteryMapper, lifecycleMapper, contractInvoker, mapper, carbonEnricherProvider);
        // 默认不启用碳模块
        lenient().when(carbonEnricherProvider.getIfAvailable()).thenReturn(null);
    }

    @Test
    @DisplayName("空白 traceNumber 抛 ServiceException")
    void blankTraceThrows() {
        assertThatThrownBy(() -> service.scan("  "))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("溯源编号");
    }

    @Test
    @DisplayName("null traceNumber 抛 ServiceException")
    void nullTraceThrows() {
        assertThatThrownBy(() -> service.scan(null))
            .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("电池不存在抛 ServiceException")
    void batteryNotFoundThrows() {
        when(batteryMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.scan("BAT-NOPE"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("BAT-NOPE");
    }

    /**
     * round-2 M-1 关键回归测试：PRODUCED 必须走 verifyBattery，其它走 verifyEvent。
     */
    @Test
    @DisplayName("verifyOnChain 分流：PRODUCED→verifyBattery / IN_USE→verifyEvent (round-2 M-1)")
    void verifyOnChainDispatchByEventType() {
        NevBatteryDO battery = sampleBattery();
        when(batteryMapper.selectOne(any())).thenReturn(battery);

        NevBatteryLifecycleDO produced = lifecycle(1, "PRODUCED", "hash-prod");
        NevBatteryLifecycleDO inUse = lifecycle(2, "IN_USE", "hash-inuse");
        when(lifecycleMapper.selectList(any())).thenReturn(List.of(produced, inUse));

        // 链上 events 不含 PRODUCED，所以期望 chainEventCount = 1
        when(contractInvoker.query(eq("LifecycleTrace"), eq("getEventCount"), any()))
            .thenReturn(ChainCallResult.builder().success(true).output(List.of("1")).build());
        when(contractInvoker.query(eq("LifecycleTrace"), eq("verifyBattery"), any()))
            .thenReturn(ChainCallResult.builder().success(true).output(List.of("true")).build());
        when(contractInvoker.query(eq("LifecycleTrace"), eq("verifyEvent"), any()))
            .thenReturn(ChainCallResult.builder().success(true).output(List.of("true")).build());

        BatteryScanVO vo = service.scan("BAT-001");

        assertThat(vo.getTotalEvents()).isEqualTo(2);
        assertThat(vo.getVerifiedEvents()).isEqualTo(2);
        assertThat(vo.getChainEventCount()).isEqualTo(1);
        assertThat(vo.isOverallVerified()).isTrue();
        assertThat(vo.getEvents()).hasSize(2);
        assertThat(vo.getEvents().get(0).isChainVerified()).isTrue();
        assertThat(vo.getEvents().get(1).isChainVerified()).isTrue();

        // 关键断言：分流真的发生了
        ArgumentCaptor<String> funcs = ArgumentCaptor.forClass(String.class);
        verify(contractInvoker, atLeast(3))
            .query(eq("LifecycleTrace"), funcs.capture(), any());
        assertThat(funcs.getAllValues())
            .contains("getEventCount", "verifyBattery", "verifyEvent");
    }

    @Test
    @DisplayName("verifyEvent 返回 false 时 overallVerified=false 且单条 chainVerified=false")
    void verifyEventFalseFlipsOverall() {
        NevBatteryDO battery = sampleBattery();
        when(batteryMapper.selectOne(any())).thenReturn(battery);

        NevBatteryLifecycleDO produced = lifecycle(1, "PRODUCED", "hash-prod");
        NevBatteryLifecycleDO inUse = lifecycle(2, "IN_USE", "hash-inuse");
        when(lifecycleMapper.selectList(any())).thenReturn(List.of(produced, inUse));

        when(contractInvoker.query(eq("LifecycleTrace"), eq("getEventCount"), any()))
            .thenReturn(ChainCallResult.builder().success(true).output(List.of("1")).build());
        when(contractInvoker.query(eq("LifecycleTrace"), eq("verifyBattery"), any()))
            .thenReturn(ChainCallResult.builder().success(true).output(List.of("true")).build());
        // 关键：verifyEvent 返回 false（模拟数据被篡改）
        when(contractInvoker.query(eq("LifecycleTrace"), eq("verifyEvent"), any()))
            .thenReturn(ChainCallResult.builder().success(true).output(List.of("false")).build());

        BatteryScanVO vo = service.scan("BAT-001");

        assertThat(vo.getVerifiedEvents()).isEqualTo(1);
        assertThat(vo.isOverallVerified()).isFalse();
        assertThat(vo.getEvents().get(0).isChainVerified()).isTrue();
        assertThat(vo.getEvents().get(1).isChainVerified()).isFalse();
    }

    // ---------- helpers ----------

    private NevBatteryDO sampleBattery() {
        NevBatteryDO b = new NevBatteryDO();
        b.setId(100L);
        b.setTraceNumber("BAT-001");
        b.setModel("CTL-NCM-75");
        b.setCurrentStatus("IN_USE");
        b.setProducedAt(new Date());
        return b;
    }

    private NevBatteryLifecycleDO lifecycle(int version, String type, String hash) {
        NevBatteryLifecycleDO lc = new NevBatteryLifecycleDO();
        lc.setBatteryId(100L);
        lc.setVersion(version);
        lc.setEventType(type);
        lc.setDataHash(hash);
        lc.setTxHash("0xtx" + version);
        lc.setBlockNumber(1000L + version);
        lc.setEventTime(new Date());
        return lc;
    }
}
