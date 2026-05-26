package com.nev.battery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nev.battery.domain.NevBatteryDO;
import com.nev.battery.domain.NevBatteryLifecycleDO;
import com.nev.battery.dto.BatteryScanVO;
import com.nev.battery.mapper.NevBatteryLifecycleMapper;
import com.nev.battery.mapper.NevBatteryMapper;
import com.nev.blockchain.client.ContractInvoker;
import com.nev.blockchain.dto.ChainCallResult;
import com.nev.common.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 公开溯源扫码服务（无鉴权）
 *
 * 设计目标：
 *   1. 任何人扫码即可看到电池完整溯源历史
 *   2. 每条事件都附带链上校验状态（chainVerified=true/false），证明数据未被篡改
 *   3. 不暴露敏感字段（producerId / 钱包地址 / 内部 id 等）
 *
 * @author NEV-v2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatteryScanService {

    private static final String CONTRACT_NAME = "LifecycleTrace";

    private final NevBatteryMapper batteryMapper;
    private final NevBatteryLifecycleMapper lifecycleMapper;
    private final ContractInvoker contractInvoker;
    private final ObjectMapper objectMapper;
    /** 碳足迹扩展点：nev-carbon 实现 CarbonScanEnricher 后自动注入；未启用碳模块时为 empty */
    private final org.springframework.beans.factory.ObjectProvider<com.nev.battery.api.CarbonScanEnricher> carbonEnricherProvider;

    public BatteryScanVO scan(String traceNumber) {
        if (traceNumber == null || traceNumber.isBlank()) {
            throw new ServiceException("溯源编号不能为空");
        }

        // 1. 查电池
        LambdaQueryWrapper<NevBatteryDO> q = new LambdaQueryWrapper<>();
        q.eq(NevBatteryDO::getTraceNumber, traceNumber).last("limit 1");
        NevBatteryDO battery = batteryMapper.selectOne(q);
        if (battery == null) {
            throw new ServiceException("溯源编号 [{}] 不存在", traceNumber);
        }

        // 2. 查链下完整 lifecycle
        LambdaQueryWrapper<NevBatteryLifecycleDO> lcQ = new LambdaQueryWrapper<>();
        lcQ.eq(NevBatteryLifecycleDO::getBatteryId, battery.getId())
            .orderByAsc(NevBatteryLifecycleDO::getVersion);
        List<NevBatteryLifecycleDO> lcList = lifecycleMapper.selectList(lcQ);

        // 3. 取链上事件总数（不含 PRODUCED，PRODUCED 走 registerBattery 单独存）
        Integer chainEventCount = queryChainEventCount(traceNumber);

        // 4. 每条事件链上校验
        //    NEV-v2 fix P1-001 / round-2 M-1: verifyOnChain 根据 event_type 分流到 verifyBattery / verifyEvent
        List<BatteryScanVO.EventItem> items = new ArrayList<>(lcList.size());
        int verified = 0;
        for (NevBatteryLifecycleDO lc : lcList) {
            boolean ok = verifyOnChain(traceNumber, lc.getEventType(), lc.getVersion(), lc.getDataHash());
            if (ok) verified++;
            items.add(BatteryScanVO.EventItem.builder()
                .version(lc.getVersion())
                .eventType(lc.getEventType())
                .operatorId(lc.getOperatorId())
                .operatorRole(lc.getOperatorRole())
                .dataHash(lc.getDataHash())
                .txHash(lc.getTxHash())
                .blockNumber(lc.getBlockNumber())
                .eventTime(lc.getEventTime())
                .payload(lc.getPayload())
                .chainVerified(ok)
                .build());
        }

        // 5. 全局校验
        //    NEV-v2 fix P1-001 / round-2 M-1: 链上 events 数组不含 PRODUCED，所以期望 chainEventCount + 1 = lcList.size()
        //    （前提：第一条事件必须是 PRODUCED；若无 PRODUCED 则按 chainEventCount == lcList.size() 兜底）
        boolean hasProduced = !lcList.isEmpty() && "PRODUCED".equalsIgnoreCase(lcList.get(0).getEventType());
        int expectedChainCount = hasProduced ? lcList.size() - 1 : lcList.size();
        boolean overallOk = (verified == lcList.size())
            && (chainEventCount == null || chainEventCount == expectedChainCount);

        // 碳足迹（如果 nev-carbon 模块加载且电池已计算）
        com.nev.battery.api.CarbonSummary carbon = null;
        com.nev.battery.api.CarbonScanEnricher enricher = carbonEnricherProvider.getIfAvailable();
        if (enricher != null) {
            carbon = enricher.getSummary(battery.getId()).orElse(null);
        }

        return BatteryScanVO.builder()
            .traceNumber(battery.getTraceNumber())
            .model(battery.getModel())
            .capacityKwh(battery.getCapacityKwh())
            .voltage(battery.getVoltage())
            .currentStatus(battery.getCurrentStatus())
            .currentRole(battery.getCurrentRole())
            .producedAt(battery.getProducedAt())
            .overallVerified(overallOk)
            .totalEvents(lcList.size())
            .verifiedEvents(verified)
            .chainEventCount(chainEventCount)
            .events(items)
            .carbonFootprint(carbon)
            .build();
    }

    private Integer queryChainEventCount(String traceNumber) {
        try {
            ChainCallResult r = contractInvoker.query(CONTRACT_NAME, "getEventCount", List.of(traceNumber));
            if (!r.isSuccess() || r.getOutput() == null) {
                return null;
            }
            return parseInt(r.getOutput());
        } catch (Exception e) {
            log.warn("[scan] queryChainEventCount 异常 trace={}: {}", traceNumber, e.getMessage());
            return null;
        }
    }

    /**
     * 链上事件校验（NEV-v2 fix P1-001 / round-2 M-1）
     *
     * 业务事实：MySQL lifecycle.version 从 1 起包含 PRODUCED；链上 LifecycleTrace.events 数组只记录 addEvent
     * 调用（IN_USE / SOLD / REPAIRED / RECYCLED / DISMANTLED），PRODUCED 走 registerBattery 单独存。
     * 所以：
     *   - PRODUCED → 调 verifyBattery(traceNumber, dataHash)
     *   - 其它    → 调 verifyEvent(traceNumber, mysql_version - 1, dataHash)
     *
     * 详见 docs/qa/first-round/defects.md P1-001 + docs/qa/round-2/fix-log.md
     */
    private boolean verifyOnChain(String traceNumber, String eventType, Integer mysqlVersion, String dataHash) {
        try {
            boolean isProduced = "PRODUCED".equalsIgnoreCase(eventType);
            String func = isProduced ? "verifyBattery" : "verifyEvent";
            List<Object> params = isProduced
                ? List.of(traceNumber, dataHash)
                : List.of(traceNumber, mysqlVersion - 1, dataHash);
            ChainCallResult r = contractInvoker.query(CONTRACT_NAME, func, params);
            if (!r.isSuccess() || r.getOutput() == null) {
                return false;
            }
            return parseBool(r.getOutput());
        } catch (Exception e) {
            log.warn("[scan] verifyOnChain 异常 trace={} type={} v={}: {}", traceNumber, eventType, mysqlVersion, e.getMessage());
            return false;
        }
    }

    /** WeBASE 返回值通常是 ["xxx"] 形式的 List，解析第一个元素 */
    private Integer parseInt(Object output) {
        String s = firstString(output);
        if (s == null) return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean parseBool(Object output) {
        String s = firstString(output);
        return "true".equalsIgnoreCase(s);
    }

    private String firstString(Object output) {
        JsonNode node = objectMapper.valueToTree(output);
        if (node.isArray() && !node.isEmpty()) {
            return node.get(0).asText();
        }
        if (node.isTextual()) {
            return node.asText();
        }
        return node.toString();
    }
}
