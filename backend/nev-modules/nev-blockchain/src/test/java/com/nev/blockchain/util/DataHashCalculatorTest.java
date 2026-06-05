package com.nev.blockchain.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
@DisplayName("DataHashCalculator")
class DataHashCalculatorTest {

    private final LocalDateTime at = LocalDateTime.of(2026, 5, 28, 12, 30, 45);

    @Test
    @DisplayName("所有生命周期事件 hash 均输出 0x + 64 位 hex")
    void eventHashesAreHexEncodedKeccak256() {
        Set<String> hashes = Set.of(
            DataHashCalculator.toHex(DataHashCalculator.produced("BAT-001", 101L, at, "{\"model\":\"LFP\"}")),
            DataHashCalculator.toHex(DataHashCalculator.inUse("BAT-001", 101L, 102L, at)),
            DataHashCalculator.toHex(DataHashCalculator.sold("BAT-001", "ORD-1", 105L, at)),
            DataHashCalculator.toHex(DataHashCalculator.repaired("BAT-001", "REP-1", "ok", at)),
            DataHashCalculator.toHex(DataHashCalculator.recycled("BAT-001", 106L, 78, at)),
            DataHashCalculator.toHex(DataHashCalculator.dismantled("BAT-001", 106L, "done", at))
        );

        assertThat(hashes).hasSize(6);
        assertThat(hashes).allSatisfy(hash -> assertThat(hash).matches("0x[0-9a-f]{64}"));
    }

    @Test
    @DisplayName("null 字段按空串处理，保证链上链下拼接规则一致")
    void nullFieldIsSerializedAsEmptyString() {
        String withNull = DataHashCalculator.keccak256Hex((Object) null, "x", at);
        String withEmpty = DataHashCalculator.keccak256Hex("", "x", at);

        assertThat(withNull).isEqualTo(withEmpty);
    }

    @Test
    @DisplayName("toHex 保留 0x 前缀和低位补零")
    void toHexKeepsPrefixAndZeroPadding() {
        assertThat(DataHashCalculator.toHex(new byte[] {0, 1, 15, 16, -1}))
            .isEqualTo("0x00010f10ff");
    }
}
