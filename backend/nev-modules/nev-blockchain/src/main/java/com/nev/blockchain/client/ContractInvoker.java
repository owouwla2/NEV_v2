package com.nev.blockchain.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nev.blockchain.config.BlockchainProperties;
import com.nev.blockchain.dto.ChainCallResult;
import com.nev.blockchain.service.ContractAddressResolver;
import com.nev.common.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * WeBASE-Front 合约调用器
 *
 * 设计参考：老仓 NEV/2026037462 的 ContractInvoker.java
 * 关键差异：
 * - 用 Spring 6 RestClient 替代 OkHttpClient（零额外依赖，与 RuoYi 全局保持一致）
 * - 用 Jackson 替代 fastjson2
 * - 支持运行时多合约动态切换（通过 ContractAddressResolver 解析）
 * - 写链 invoke / 读链 query 分两个方法（视图函数走 query-transaction 不消耗 gas）
 *
 * 不在本类做的事：
 * - 不管私钥（交给 WeBASE-Sign）
 * - 不做业务校验（交给上层 Service）
 * - 不解析具体返回字段（返回 ChainCallResult，调用方自行解析 output）
 *
 * @author NEV-v2
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContractInvoker {

    private final BlockchainProperties properties;
    private final ContractAddressResolver addressResolver;
    private final ObjectMapper objectMapper;

    private RestClient restClient;

    /**
     * 懒初始化 RestClient（构造期还没有 properties 时不能直接 .build()）
     */
    private RestClient client() {
        if (restClient == null) {
            restClient = RestClient.builder()
                .baseUrl(properties.getWebaseFrontUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        }
        return restClient;
    }

    /**
     * 写链调用（onlyXxx 修饰符的函数：registerBattery / addEvent / grantRole 等）
     * 走 /WeBASE-Front/trans/handleWithSign，WeBASE-Sign 自动签名
     *
     * @param contractName 合约名（必须已在 ContractAddressResolver 中注册 ABI + 配置地址）
     * @param funcName     合约方法名
     * @param funcParams   方法参数列表（按 Solidity 函数签名顺序）
     */
    public ChainCallResult invoke(String contractName, String funcName, List<Object> funcParams) {
        Map<String, Object> body = buildBaseBody(contractName, funcName, funcParams);
        String url = "/WeBASE-Front/trans/handleWithSign";
        return doRequest(url, body, true);
    }

    /**
     * 读链调用（view / pure 函数：verifyBattery / hasRole / getEvent 等）
     * 走 /WeBASE-Front/trans/query-transaction，不消耗 gas，无 txHash
     */
    public ChainCallResult query(String contractName, String funcName, List<Object> funcParams) {
        Map<String, Object> body = buildBaseBody(contractName, funcName, funcParams);
        String url = "/WeBASE-Front/trans/query-transaction";
        return doRequest(url, body, false);
    }

    // ---------------------------------------------------------------

    private Map<String, Object> buildBaseBody(String contractName, String funcName, List<Object> funcParams) {
        String address = addressResolver.resolveAddress(contractName);
        String abiJson = addressResolver.resolveAbi(contractName);
        String signUserId = addressResolver.resolveSignUserId(contractName);
        String contractPath = addressResolver.resolvePath(contractName);

        List<Map<String, Object>> abiList = parseAbi(abiJson);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("groupId", properties.getGroupId());
        body.put("signUserId", signUserId);
        body.put("contractName", contractName);
        body.put("contractPath", contractPath);
        body.put("version", "");
        body.put("funcName", funcName);
        body.put("funcParam", funcParams == null ? List.of() : funcParams);
        body.put("contractAddress", address);
        body.put("contractAbi", abiList);
        body.put("useAes", false);
        body.put("useCns", false);
        return body;
    }

    private List<Map<String, Object>> parseAbi(String abiJson) {
        try {
            return objectMapper.readValue(abiJson, new TypeReference<>() {});
        } catch (Exception e) {
            throw new ServiceException("解析 ABI 失败: " + e.getMessage());
        }
    }

    private ChainCallResult doRequest(String path, Map<String, Object> body, boolean writeCall) {
        String response;
        try {
            response = client().post()
                .uri(path)
                .body(body)
                .retrieve()
                .body(String.class);
        } catch (RestClientException e) {
            log.error("[blockchain] WeBASE-Front 调用失败 path={} body.funcName={}", path, body.get("funcName"), e);
            throw new ServiceException("WeBASE-Front 调用失败: " + e.getMessage());
        }
        if (response == null) {
            throw new ServiceException("WeBASE-Front 返回空响应");
        }

        log.debug("[blockchain] {} resp ({}): {}", body.get("funcName"), writeCall ? "WRITE" : "READ ", response);
        return parseResponse(response, writeCall);
    }

    private ChainCallResult parseResponse(String response, boolean writeCall) {
        ChainCallResult.ChainCallResultBuilder builder = ChainCallResult.builder().rawResponse(response);
        try {
            JsonNode root = objectMapper.readTree(response);
            // 1. 成功判定：宽松解析 code/statusOK/error
            boolean success = isSuccess(root);
            builder.success(success);

            // 2. txHash / blockNumber（写链调用才有）
            if (writeCall) {
                builder.txHash(textOrNull(root, "transactionHash", "txHash"));
                JsonNode bn = firstNode(root, "blockNumber");
                builder.blockNumber(bn != null && bn.canConvertToLong() ? bn.asLong() : null);
            }

            // 3. message
            builder.message(textOrNull(root, "message", "error"));

            // 4. output（query 返回数据）
            if (!writeCall) {
                JsonNode out = firstNode(root, "output", "data", "result");
                builder.output(out == null ? null : objectMapper.convertValue(out, Object.class));
            }
        } catch (Exception e) {
            log.warn("[blockchain] 响应解析失败（非 JSON 格式或字段不规整），fallback 返回 raw: {}", e.getMessage());
            builder.success(!response.toLowerCase().contains("error"))
                .message(response);
        }
        return builder.build();
    }

    private boolean isSuccess(JsonNode root) {
        JsonNode codeNode = root.get("code");
        if (codeNode != null && codeNode.canConvertToInt()) {
            return codeNode.asInt() == 0;
        }
        JsonNode statusOk = root.get("statusOK");
        if (statusOk != null && statusOk.isBoolean()) {
            return statusOk.asBoolean();
        }
        return !root.has("error");
    }

    private String textOrNull(JsonNode root, String... keys) {
        for (String key : keys) {
            JsonNode node = root.get(key);
            if (node != null && !node.isNull()) {
                return node.asText();
            }
        }
        return null;
    }

    private JsonNode firstNode(JsonNode root, String... keys) {
        for (String key : keys) {
            JsonNode node = root.get(key);
            if (node != null && !node.isNull()) {
                return node;
            }
        }
        return null;
    }

    /** 测试钩子：手工指定 RestClient（单测注入 mock） */
    void setRestClient(RestClient client) {
        this.restClient = client;
    }

    /** 配置化超时（暂时未启用，预留 D11 调优） */
    Duration getTimeout() {
        return Duration.ofMillis(properties.getTimeout());
    }
}
