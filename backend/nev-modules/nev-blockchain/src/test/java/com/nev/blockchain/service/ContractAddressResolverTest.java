package com.nev.blockchain.service;

import com.nev.blockchain.config.BlockchainProperties;
import com.nev.blockchain.config.BlockchainProperties.ContractEntry;
import com.nev.common.core.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("dev")
@DisplayName("ContractAddressResolver")
class ContractAddressResolverTest {

    @Test
    @DisplayName("运行时注册优先于静态配置，且 path 空值默认 /")
    void registeredContractWinsOverProperties() {
        BlockchainProperties props = propsWithEntry("LifecycleTrace", "0xSTATIC", "/static", "0xSTATIC_USER");
        props.setDefaultUserAddress("0xDEFAULT");
        ContractAddressResolver resolver = new ContractAddressResolver(props);

        resolver.register("LifecycleTrace", "0xRUNTIME", "[{\"type\":\"function\"}]", "0xRUNTIME_USER", "");

        assertThat(resolver.resolveAddress("LifecycleTrace")).isEqualTo("0xRUNTIME");
        assertThat(resolver.resolveAbi("LifecycleTrace")).contains("function");
        assertThat(resolver.resolveUserAddress("LifecycleTrace")).isEqualTo("0xRUNTIME_USER");
        assertThat(resolver.resolvePath("LifecycleTrace")).isEqualTo("/");
        assertThat(resolver.listRegistered()).containsKey("LifecycleTrace");
    }

    @Test
    @DisplayName("未注册 ABI 时，地址/path/userAddress 可从静态配置回退")
    void fallsBackToPropertiesForAddressUserAndPath() {
        BlockchainProperties props = propsWithEntry("LifecycleTrace", "0xSTATIC", "/contract", "0xSTATIC_USER");
        ContractAddressResolver resolver = new ContractAddressResolver(props);

        assertThat(resolver.resolveAddress("LifecycleTrace")).isEqualTo("0xSTATIC");
        assertThat(resolver.resolveUserAddress("LifecycleTrace")).isEqualTo("0xSTATIC_USER");
        assertThat(resolver.resolvePath("LifecycleTrace")).isEqualTo("/contract");
        assertThatThrownBy(() -> resolver.resolveAbi("LifecycleTrace"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("ABI");
    }

    @Test
    @DisplayName("合约未配置调用者时，回退 defaultUserAddress")
    void fallsBackToDefaultUserAddress() {
        BlockchainProperties props = propsWithEntry("LifecycleTrace", "0xSTATIC", null, null);
        props.setDefaultUserAddress("0xDEFAULT");
        ContractAddressResolver resolver = new ContractAddressResolver(props);

        assertThat(resolver.resolveUserAddress("LifecycleTrace")).isEqualTo("0xDEFAULT");
        assertThat(resolver.resolvePath("LifecycleTrace")).isEqualTo("/");
    }

    @Test
    @DisplayName("缺失配置、零地址和空注册字段均拒绝")
    void invalidConfigurationThrowsServiceException() {
        ContractAddressResolver empty = new ContractAddressResolver(new BlockchainProperties());
        assertThatThrownBy(() -> empty.resolveAddress("Missing"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("既未");

        ContractAddressResolver zero = new ContractAddressResolver(
            propsWithEntry("LifecycleTrace", "0x0000000000000000000000000000000000000000", "/", null));
        assertThatThrownBy(() -> zero.resolveAddress("LifecycleTrace"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("未部署");

        ContractAddressResolver resolver = new ContractAddressResolver(new BlockchainProperties());
        assertThatThrownBy(() -> resolver.register("", "0xRUNTIME", "[]", null, "/"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("contractName");
        assertThatThrownBy(() -> resolver.register("LifecycleTrace", "", "[]", null, "/"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("地址");
        assertThatThrownBy(() -> resolver.register("LifecycleTrace", "0xRUNTIME", "", null, "/"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("ABI");
    }

    @Test
    @DisplayName("registerAbi 使用静态配置里的地址、用户和路径")
    void registerAbiUsesPropertyEntry() {
        BlockchainProperties props = propsWithEntry("LifecycleTrace", "0xSTATIC", "/abi", "0xABI_USER");
        ContractAddressResolver resolver = new ContractAddressResolver(props);

        resolver.registerAbi("LifecycleTrace", "[{\"name\":\"getEventCount\"}]");

        assertThat(resolver.resolveAddress("LifecycleTrace")).isEqualTo("0xSTATIC");
        assertThat(resolver.resolveUserAddress("LifecycleTrace")).isEqualTo("0xABI_USER");
        assertThat(resolver.resolvePath("LifecycleTrace")).isEqualTo("/abi");
        assertThat(resolver.resolveAbi("LifecycleTrace")).contains("getEventCount");
    }

    private BlockchainProperties propsWithEntry(String name, String address, String path, String userAddress) {
        BlockchainProperties props = new BlockchainProperties();
        ContractEntry entry = new ContractEntry();
        entry.setAddress(address);
        entry.setPath(path);
        entry.setUserAddress(userAddress);
        props.getContracts().put(name, entry);
        return props;
    }
}
