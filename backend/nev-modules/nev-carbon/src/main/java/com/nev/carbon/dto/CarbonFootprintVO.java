package com.nev.carbon.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 电池碳足迹完整视图（含 5 阶段明细 + 因子级别 breakdown）
 *
 * @author NEV-v2
 */
@Data
@Builder
public class CarbonFootprintVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long batteryId;
    private String traceNumber;
    private String batteryModel;
    private String cellType;
    private BigDecimal capacityKwh;

    private BigDecimal totalCo2Kg;
    private String calcMethod;
    private String calcVersion;
    private Date calcTime;

    /** 5 阶段明细 */
    private List<StageDetail> stages;

    @Data
    @Builder
    public static class StageDetail implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String stage;          // RAW / MFG / TRANS / USE / EOL
        private BigDecimal co2Kg;
        private List<LineItem> breakdown;
    }

    @Data
    @Builder
    public static class LineItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String factorCode;
        private String factorName;
        private BigDecimal input;
        private String inputUnit;
        private BigDecimal factorValue;
        private String factorUnit;
        private BigDecimal co2Kg;
        private String note;
    }
}
