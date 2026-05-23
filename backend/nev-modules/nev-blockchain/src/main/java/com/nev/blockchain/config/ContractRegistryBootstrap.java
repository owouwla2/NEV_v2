package com.nev.blockchain.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nev.blockchain.domain.NevContractConfigDO;
import com.nev.blockchain.mapper.NevContractConfigMapper;
import com.nev.blockchain.service.ContractAddressResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动期从 nev_contract_config 表把所有 enabled='1' 的合约注册到 ContractAddressResolver
 *
 * 触发时机：ApplicationReadyEvent —— 所有 Bean 已就绪 + DataSource + MyBatis 已可用
 *
 * @author NEV-v2
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContractRegistryBootstrap {

    private final NevContractConfigMapper mapper;
    private final ContractAddressResolver resolver;

    @EventListener(ApplicationReadyEvent.class)
    public void loadFromDatabase() {
        LambdaQueryWrapper<NevContractConfigDO> q = new LambdaQueryWrapper<>();
        q.eq(NevContractConfigDO::getEnabled, "1");
        List<NevContractConfigDO> rows = mapper.selectList(q);
        if (rows.isEmpty()) {
            log.warn("[blockchain] nev_contract_config 表为空（或全部停用），ContractAddressResolver 仅依赖 application.yml 配置");
            return;
        }
        for (NevContractConfigDO row : rows) {
            try {
                resolver.register(
                    row.getContractName(),
                    row.getContractAddress(),
                    row.getAbi(),
                    null, // 默认走 defaultUserAddress
                    "/"
                );
            } catch (Exception e) {
                log.error("[blockchain] 注册合约失败 contractName={} error={}",
                    row.getContractName(), e.getMessage(), e);
            }
        }
        log.info("[blockchain] ContractRegistryBootstrap: 已从数据库注册 {} 个合约", rows.size());
    }
}
