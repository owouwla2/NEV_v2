package com.nev.battery.api;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 碳足迹轻量摘要（跨模块用）
 *
 * 放在 nev-battery 的 api 包里：
 *   - nev-battery 不依赖 nev-carbon，避免循环
 *   - 实际实现在 nev-carbon 中（CarbonScanEnricherImpl）
 *
 * @author NEV-v2
 */
@Data
@Builder
public class CarbonSummary implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private BigDecimal totalCo2Kg;
    private String calcMethod;
    private String calcVersion;
    private Date calcTime;
    private List<StageBrief> stages;

    @Data
    @Builder
    public static class StageBrief implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String stage;
        private BigDecimal co2Kg;
    }
}
