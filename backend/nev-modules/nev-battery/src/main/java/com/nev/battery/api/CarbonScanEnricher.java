package com.nev.battery.api;

import java.util.Optional;

/**
 * 给 BatteryScanService 提供"按 battery_id 取碳足迹摘要"的扩展点
 *
 * - 接口在 nev-battery 模块（基础设施）
 * - 实现在 nev-carbon 模块（CarbonScanEnricherImpl）
 * - BatteryScanService 用 ObjectProvider 注入，运行时若 nev-carbon 未加载则跳过
 *
 * @author NEV-v2
 */
public interface CarbonScanEnricher {

    /**
     * 取该电池的碳足迹摘要
     * @return empty 表示该电池尚未计算（admin 还没触发 /admin/carbon/calc）
     */
    Optional<CarbonSummary> getSummary(Long batteryId);
}
