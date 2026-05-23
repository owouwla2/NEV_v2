package com.nev.blockchain.service;

import com.nev.blockchain.config.BlockchainProperties;
import com.nev.blockchain.config.BlockchainProperties.ContractEntry;
import com.nev.common.core.exception.ServiceException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 合约地址与 ABI 解析服务
 *
 * 优先级：
 *   1. 内存注册表（ContractRegistryBootstrap 启动期从 nev_contract_config 表注入）
 *   2. BlockchainProperties.contracts 静态配置（dev/test 占位用）
 *
 * 线程安全：register / resolve 方法都使用 ConcurrentHashMap
 *
 * @author NEV-v2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractAddressResolver {

    private final BlockchainProperties properties;

    /** 合约名 → 完整注册信息（运行时优先） */
    private final Map<String, Registration> registry = new ConcurrentHashMap<>();

    @Data
    public static class Registration {
        private final String contractName;
        private final String address;
        private final String abi;
        private final String path;
        /** null 表示用全局 defaultUserAddress */
        private final String userAddress;
    }

    /**
     * 注册合约（启动期由 ContractRegistryBootstrap 从 nev_contract_config 表写入）
     */
    public void register(String contractName, String address, String abi, String userAddress, String path) {
        if (!StringUtils.hasText(contractName)) {
            throw new ServiceException("contractName 不能为空");
        }
        if (!StringUtils.hasText(address)) {
            throw new ServiceException("合约 [{}] 地址不能为空", contractName);
        }
        if (!StringUtils.hasText(abi)) {
            throw new ServiceException("合约 [{}] ABI 不能为空", contractName);
        }
        registry.put(contractName, new Registration(contractName, address, abi,
            StringUtils.hasText(path) ? path : "/", userAddress));
        log.info("[blockchain] registered contract: name={} address={} abiLen={} userAddress={}",
            contractName, address, abi.length(), userAddress);
    }

    /** 兼容老用法：只注册 ABI（D10 时本来打算让调用方启动期注入） */
    public void registerAbi(String contractName, String abiJson) {
        ContractEntry entry = properties.getContracts().get(contractName);
        String address = entry != null ? entry.getAddress() : null;
        String userAddress = entry != null ? entry.getUserAddress() : null;
        String path = entry != null ? entry.getPath() : "/";
        register(contractName, address, abiJson, userAddress, path);
    }

    public String resolveAddress(String contractName) {
        Registration reg = registry.get(contractName);
        if (reg != null) {
            return reg.getAddress();
        }
        ContractEntry entry = requireEntry(contractName);
        String address = entry.getAddress();
        if (!StringUtils.hasText(address) || address.equals("0x0000000000000000000000000000000000000000")) {
            throw new ServiceException("合约 [{}] 未部署或地址未配置（请检查 nev_contract_config 表或 nev.blockchain.contracts.{}.address）",
                contractName, contractName);
        }
        return address;
    }

    public String resolveAbi(String contractName) {
        Registration reg = registry.get(contractName);
        if (reg != null) {
            return reg.getAbi();
        }
        throw new ServiceException("合约 [{}] 的 ABI 未注册，请确认启动期 ContractRegistryBootstrap 已从 nev_contract_config 表加载，"
            + "或在 application.yml 中提供占位再手工 registerAbi()", contractName);
    }

    public String resolveUserAddress(String contractName) {
        Registration reg = registry.get(contractName);
        String addr = reg != null && StringUtils.hasText(reg.getUserAddress())
            ? reg.getUserAddress()
            : null;
        if (!StringUtils.hasText(addr)) {
            ContractEntry entry = properties.getContracts().get(contractName);
            if (entry != null && StringUtils.hasText(entry.getUserAddress())) {
                addr = entry.getUserAddress();
            }
        }
        if (!StringUtils.hasText(addr)) {
            addr = properties.getDefaultUserAddress();
        }
        if (!StringUtils.hasText(addr)) {
            throw new ServiceException("合约 [{}] 没有调用者地址，且未配置 nev.blockchain.defaultUserAddress", contractName);
        }
        return addr;
    }

    public String resolvePath(String contractName) {
        Registration reg = registry.get(contractName);
        if (reg != null && StringUtils.hasText(reg.getPath())) {
            return reg.getPath();
        }
        ContractEntry entry = properties.getContracts().get(contractName);
        return entry != null && StringUtils.hasText(entry.getPath()) ? entry.getPath() : "/";
    }

    /** 列出所有已注册的合约（监控/管理端点用） */
    public Map<String, Registration> listRegistered() {
        return Map.copyOf(registry);
    }

    private ContractEntry requireEntry(String contractName) {
        ContractEntry entry = properties.getContracts().get(contractName);
        if (entry == null) {
            throw new ServiceException("合约 [{}] 既未在数据库 nev_contract_config 中注册，也未在 nev.blockchain.contracts 配置", contractName);
        }
        return entry;
    }
}
