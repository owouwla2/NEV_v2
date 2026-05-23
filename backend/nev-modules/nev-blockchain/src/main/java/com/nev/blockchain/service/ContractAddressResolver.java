package com.nev.blockchain.service;

import com.nev.blockchain.config.BlockchainProperties;
import com.nev.blockchain.config.BlockchainProperties.ContractEntry;
import com.nev.common.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 合约地址与 ABI 解析服务
 *
 * D10 阶段：从 BlockchainProperties.contracts 静态配置读取（占位）
 * D11 阶段：扩展为从 nev_contract_config 表查询 → 优先级 表 > 配置
 * Wave 3+：可加 Redis 缓存 + ttl 自动刷新
 *
 * 当前实现：纯内存缓存，进程重启重新加载配置
 *
 * @author NEV-v2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractAddressResolver {

    private final BlockchainProperties properties;

    /** 合约名 → ABI JSON 字符串（来自 contracts/build/*.abi，由调用方注入或外部读取） */
    private final Map<String, String> abiCache = new ConcurrentHashMap<>();

    /**
     * 注册一个合约的 ABI（启动期初始化时调用，或由 nev-admin 接口动态更新）
     */
    public void registerAbi(String contractName, String abiJson) {
        if (!StringUtils.hasText(contractName) || !StringUtils.hasText(abiJson)) {
            throw new ServiceException("contractName / abiJson 不能为空");
        }
        abiCache.put(contractName, abiJson);
        log.info("[blockchain] registered ABI for contract: {} ({} chars)", contractName, abiJson.length());
    }

    /**
     * 获取合约地址（D11 起会先查 nev_contract_config 表）
     */
    public String resolveAddress(String contractName) {
        ContractEntry entry = requireEntry(contractName);
        String address = entry.getAddress();
        if (!StringUtils.hasText(address) || address.equals("0x0000000000000000000000000000000000000000")) {
            throw new ServiceException("合约 [{}] 未部署或地址未配置（请检查 nev_contract_config 表或 nev.blockchain.contracts.{}.address）",
                contractName, contractName);
        }
        return address;
    }

    /**
     * 获取合约 ABI（要求先 registerAbi）
     */
    public String resolveAbi(String contractName) {
        String abi = abiCache.get(contractName);
        if (!StringUtils.hasText(abi)) {
            throw new ServiceException("合约 [{}] 的 ABI 未注册，请在启动期调用 registerAbi() 或检查 nev_contract_config", contractName);
        }
        return abi;
    }

    /**
     * 获取合约调用者钱包地址（合约级覆盖 > 全局默认）
     * 用于 /trans/handle 接口的 user 字段
     */
    public String resolveUserAddress(String contractName) {
        ContractEntry entry = requireEntry(contractName);
        String addr = StringUtils.hasText(entry.getUserAddress())
            ? entry.getUserAddress()
            : properties.getDefaultUserAddress();
        if (!StringUtils.hasText(addr)) {
            throw new ServiceException("合约 [{}] 没有调用者地址，且未配置 nev.blockchain.defaultUserAddress", contractName);
        }
        return addr;
    }

    /**
     * 获取合约部署路径（WeBASE-Front 概念，默认 "/"）
     */
    public String resolvePath(String contractName) {
        ContractEntry entry = requireEntry(contractName);
        return StringUtils.hasText(entry.getPath()) ? entry.getPath() : "/";
    }

    private ContractEntry requireEntry(String contractName) {
        ContractEntry entry = properties.getContracts().get(contractName);
        if (entry == null) {
            throw new ServiceException("合约 [{}] 未在 nev.blockchain.contracts 中配置", contractName);
        }
        return entry;
    }
}
