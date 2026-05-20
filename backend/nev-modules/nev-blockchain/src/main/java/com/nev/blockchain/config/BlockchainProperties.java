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
 *     defaultSignUserId: nev_admin
 *     timeout: 30000
 *     contracts:
 *       LifecycleTrace:
 *         address: 0x0000000000000000000000000000000000000000   # D11 部署后回填
 *         path: /
 *         signUserId: nev_admin
 * </pre>
 *
 * 优先级：
 * - 静态配置（本类，开发期占位）
 * - 数据库 nev_contract_config 表（D11 部署后由 ContractAddressResolver 优先读表）
 *
 * @author NEV-v2
 */
@Data
@ConfigurationProperties(prefix = "nev.blockchain")
public class BlockchainProperties {

    /**
     * WeBASE-Front HTTP 根地址（默认 http://localhost:5002）
     * 部署接口：${webaseFrontUrl}/WeBASE-Front/trans/handleWithSign
     * 查询接口：${webaseFrontUrl}/WeBASE-Front/trans/query-transaction
     */
    private String webaseFrontUrl = "http://localhost:5002";

    /**
     * FISCO BCOS 群组 ID（默认 1）
     */
    private Integer groupId = 1;

    /**
     * 默认 WeBASE 签名用户 ID（每个合约可以覆盖）
     * 通过 WeBASE-Sign 服务管理，业务侧无需接触私钥
     */
    private String defaultSignUserId;

    /**
     * HTTP 调用超时（毫秒）
     */
    private Integer timeout = 30000;

    /**
     * 合约名 → 静态配置（仅开发期占位用，正式数据走数据库 nev_contract_config）
     */
    private Map<String, ContractEntry> contracts = new HashMap<>();

    @Data
    public static class ContractEntry {
        /** 合约地址（0x 前缀 42 字符）*/
        private String address;
        /** 合约部署路径（WeBASE-Front 概念，默认 "/"）*/
        private String path = "/";
        /** 该合约的签名用户（覆盖 defaultSignUserId）*/
        private String signUserId;
    }
}
