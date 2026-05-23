package com.nev.blockchain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * NEV-v2 区块链配置（application.yml 中 nev.blockchain.* 段读取）
 *
 * 用法示例（application-dev.yml）：
 * <pre>
 * nev:
 *   blockchain:
 *     webaseFrontUrl: http://localhost:5002
 *     groupId: 1
 *     defaultUserAddress: 0x6933f6d76d71b7ca66f70f3faf6b108a10697aa2   # admin1 wallet address（已通过 importP12 导入 Front 5002 本地私钥）
 *     timeout: 30000
 *     contracts:
 *       LifecycleTrace:
 *         address: 0xf71701365b8b35d4a03a12ecc51edf5fd5797b08
 *         path: /
 *         userAddress: 0x6933f6d76d71b7ca66f70f3faf6b108a10697aa2     # 可选，单合约覆盖
 * </pre>
 *
 * 当前使用 WeBASE-Front 本地私钥签名 / 调用（{@code /trans/handle}，user 字段传 wallet address）。
 * 如果将来切换到 WeBASE-Sign 远程签名服务，把字段语义改为 signUserId 并把 ContractInvoker 切到 {@code /trans/handleWithSign} 即可。
 *
 * @author NEV-v2
 */
@Data
@ConfigurationProperties(prefix = "nev.blockchain")
public class BlockchainProperties {

    /**
     * WeBASE-Front HTTP 根地址（默认 http://localhost:5002）
     * 调用接口：${webaseFrontUrl}/WeBASE-Front/trans/handle
     */
    private String webaseFrontUrl = "http://localhost:5002";

    /**
     * FISCO BCOS 群组 ID（默认 1）
     */
    private Integer groupId = 1;

    /**
     * 默认调用者钱包地址（Front 5002 本地私钥库 /privateKey/localKeyStores 中已有的 address）
     * 每个合约可在 contracts.xxx.userAddress 覆盖
     */
    private String defaultUserAddress;

    /**
     * HTTP 调用超时（毫秒）
     */
    private Integer timeout = 30000;

    /**
     * 合约名 → 静态配置（开发期占位用，正式数据走数据库 nev_contract_config）
     */
    private Map<String, ContractEntry> contracts = new HashMap<>();

    @Data
    public static class ContractEntry {
        /** 合约地址（0x 前缀 42 字符）*/
        private String address;
        /** 合约部署路径（WeBASE-Front 概念，默认 "/"）*/
        private String path = "/";
        /** 该合约的调用者钱包地址（覆盖 defaultUserAddress）*/
        private String userAddress;
    }
}
