package com.nev.blockchain.util;

import org.bouncycastle.jcajce.provider.digest.Keccak;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 链上 dataHash 计算工具
 *
 * 规约：所有 keccak256 输入按字段顺序用 "|" 分隔拼接成 UTF-8 字符串
 * 详见 docs/contracts/design.md §5
 *
 * 重要约定：
 *   - 时间统一用 ISO-8601 DateTimeFormatter.ISO_DATE_TIME 序列化
 *   - 数字 / 枚举 / 字符串以 toString() 拼接（null 用空串）
 *   - 字段顺序写死，**不可调整**（一旦调整链上历史 verify 全部失效）
 *
 * @author NEV-v2
 */
public final class DataHashCalculator {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_DATE_TIME;
    private static final String SEP = "|";

    private DataHashCalculator() {
    }

    /**
     * 通用入口：把任意字段列表拼接后 keccak256
     */
    public static byte[] keccak256(Object... fields) {
        List<String> parts = new ArrayList<>(fields.length);
        for (Object f : fields) {
            parts.add(stringify(f));
        }
        String joined = String.join(SEP, parts);
        Keccak.Digest256 digest = new Keccak.Digest256();
        return digest.digest(joined.getBytes(StandardCharsets.UTF_8));
    }

    /** keccak256 hex 字符串（0x 开头 66 字符） */
    public static String keccak256Hex(Object... fields) {
        return toHex(keccak256(fields));
    }

    // ---------------------------------------------------------------
    // 6 种事件的 dataHash 计算（与 LifecycleTrace EventType 一一对应）
    // ---------------------------------------------------------------

    /**
     * PRODUCED 事件：trace_number | producer_id | produced_at | spec_snapshot_json
     */
    public static byte[] produced(String traceNumber, Long producerId,
                                   LocalDateTime producedAt, String specJson) {
        return keccak256(traceNumber, producerId, producedAt, specJson);
    }

    /**
     * IN_USE 事件：trace_number | from_owner | to_owner | handover_at
     */
    public static byte[] inUse(String traceNumber, Long fromOwner, Long toOwner,
                                LocalDateTime handoverAt) {
        return keccak256(traceNumber, fromOwner, toOwner, handoverAt);
    }

    /**
     * SOLD 事件：trace_number | order_no | consumer_id | sold_at
     */
    public static byte[] sold(String traceNumber, String orderNo, Long consumerId,
                               LocalDateTime soldAt) {
        return keccak256(traceNumber, orderNo, consumerId, soldAt);
    }

    /**
     * REPAIRED 事件：trace_number | repair_no | repair_summary | repaired_at
     */
    public static byte[] repaired(String traceNumber, String repairNo, String repairSummary,
                                   LocalDateTime repairedAt) {
        return keccak256(traceNumber, repairNo, repairSummary, repairedAt);
    }

    /**
     * RECYCLED 事件：trace_number | recycler_id | soh | received_at
     */
    public static byte[] recycled(String traceNumber, Long recyclerId, Integer soh,
                                   LocalDateTime receivedAt) {
        return keccak256(traceNumber, recyclerId, soh, receivedAt);
    }

    /**
     * DISMANTLED 事件：trace_number | recycler_id | dismantle_summary | dismantled_at
     */
    public static byte[] dismantled(String traceNumber, Long recyclerId, String dismantleSummary,
                                     LocalDateTime dismantledAt) {
        return keccak256(traceNumber, recyclerId, dismantleSummary, dismantledAt);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private static String stringify(Object f) {
        if (f == null) {
            return "";
        }
        if (f instanceof LocalDateTime ldt) {
            return ldt.format(ISO);
        }
        return f.toString();
    }

    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder("0x");
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
