package com.nev.blockchain.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * NEV-v2 round-4 M-A · P1-002 示范单测之一
 *
 * 目标：覆盖 ContractInvoker.longOrNull 的 4 种输入形态。
 * 背景：round-2 O-2 修复时新增此方法，处理 WeBASE-Front 返回的 blockNumber
 *      可能是 NumericNode、TextNode（十进制）、TextNode（0x 前缀十六进制）三种形态。
 *      此前 22/22 条 lifecycle 全 NULL 即因解析路径未兼容造成。
 *
 * 测试策略：纯单元测试，不启动 Spring Context；
 *      ContractInvoker 用 (null, null, ObjectMapper) 构造（仅 ObjectMapper 用于建 JsonNode 树）。
 * 同包 package-private 访问 longOrNull，无需反射。
 *
 * @Tag("dev") 让默认 profile（dev）的 mvn test 能跑到。
 *
 * @author NEV-v2 round-4
 */
@Tag("dev")
@DisplayName("ContractInvoker.longOrNull")
class ContractInvokerLongOrNullTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ContractInvoker invoker = new ContractInvoker(null, null, mapper);

    @Test
    @DisplayName("NumericNode 直接转 long")
    void numericNode() throws Exception {
        JsonNode node = mapper.readTree("12345");
        assertEquals(12345L, invoker.longOrNull(node));
    }

    @Test
    @DisplayName("TextNode 十进制字符串")
    void textNodeDecimal() throws Exception {
        JsonNode node = mapper.readTree("\"678\"");
        assertEquals(678L, invoker.longOrNull(node));
    }

    @Test
    @DisplayName("TextNode 0x 前缀十六进制")
    void textNodeHexLowerPrefix() throws Exception {
        JsonNode node = mapper.readTree("\"0x1a\"");
        assertEquals(26L, invoker.longOrNull(node));
    }

    @Test
    @DisplayName("TextNode 0X 大写前缀十六进制")
    void textNodeHexUpperPrefix() throws Exception {
        JsonNode node = mapper.readTree("\"0XFF\"");
        assertEquals(255L, invoker.longOrNull(node));
    }

    @Test
    @DisplayName("null JsonNode 返回 null")
    void nullNode() {
        assertNull(invoker.longOrNull(null));
    }

    @Test
    @DisplayName("NullNode（jackson 的 NullNode）返回 null")
    void jacksonNullNode() throws Exception {
        JsonNode node = mapper.readTree("null");
        assertNull(invoker.longOrNull(node));
    }

    @Test
    @DisplayName("空字符串 TextNode 返回 null")
    void blankText() throws Exception {
        JsonNode node = mapper.readTree("\"\"");
        assertNull(invoker.longOrNull(node));
    }

    @Test
    @DisplayName("纯空白 TextNode 返回 null")
    void whitespaceText() throws Exception {
        JsonNode node = mapper.readTree("\"   \"");
        assertNull(invoker.longOrNull(node));
    }

    @Test
    @DisplayName("无法解析的非数字 TextNode 返回 null（吞 NumberFormatException）")
    void nonNumericText() throws Exception {
        JsonNode node = mapper.readTree("\"abc\"");
        assertNull(invoker.longOrNull(node));
    }
}
