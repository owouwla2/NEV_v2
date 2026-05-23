package com.nev.carbon.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * consumer 碳积分账户视图（含最近流水）
 *
 * @author NEV-v2
 */
@Data
@Builder
public class CarbonAccountVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private BigDecimal balance;
    private BigDecimal frozen;
    private BigDecimal totalEarned;
    private BigDecimal totalSpent;
    private List<RecordItem> recentRecords;

    @Data
    @Builder
    public static class RecordItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private BigDecimal changeAmount;
        private BigDecimal balanceAfter;
        private String reason;
        private Long relatedId;
        private String relatedType;
        private String remark;
        private Date createTime;
    }
}
