package com.nev.battery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nev.battery.domain.NevBatteryDO;
import com.nev.battery.domain.NevBatteryLifecycleDO;
import com.nev.battery.domain.SysNevUserExtDO;
import com.nev.battery.dto.BatteryRegisterDTO;
import com.nev.battery.dto.BatteryRegisterVO;
import com.nev.battery.mapper.NevBatteryLifecycleMapper;
import com.nev.battery.mapper.NevBatteryMapper;
import com.nev.battery.mapper.SysNevUserExtMapper;
import com.nev.blockchain.client.ContractInvoker;
import com.nev.blockchain.dto.ChainCallResult;
import com.nev.blockchain.util.DataHashCalculator;
import com.nev.common.core.exception.ServiceException;
import com.nev.common.satoken.utils.LoginHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 电池业务服务
 *
 * 当前实现：注册（PRODUCED 事件），D13 起加 IN_USE/SOLD/RECYCLED 等
 *
 * @author NEV-v2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatteryService {

    private static final String CONTRACT_NAME = "LifecycleTrace";

    private final NevBatteryMapper batteryMapper;
    private final NevBatteryLifecycleMapper lifecycleMapper;
    private final SysNevUserExtMapper userExtMapper;
    private final ContractInvoker contractInvoker;
    private final ObjectMapper objectMapper;

    /**
     * Producer 注册新电池：MySQL 落库 → keccak256 → 链上 registerBattery → 写 PRODUCED lifecycle
     */
    @Transactional(rollbackFor = Exception.class)
    public BatteryRegisterVO register(BatteryRegisterDTO dto) {
        // 0. 当前登录用户 = producer
        Long producerId = LoginHelper.getUserId();
        if (producerId == null) {
            throw new ServiceException("未登录");
        }
        SysNevUserExtDO ext = userExtMapper.selectById(producerId);
        if (ext == null || !StringUtils.hasText(ext.getWalletAddress())) {
            throw new ServiceException("当前用户 [{}] 未绑定区块链钱包地址", producerId);
        }
        if (!"producer".equals(ext.getUserType())) {
            throw new ServiceException("当前用户角色 [{}] 不是 producer，无权注册电池", ext.getUserType());
        }

        // 1. 校验 traceNumber 唯一
        LambdaQueryWrapper<NevBatteryDO> exists = new LambdaQueryWrapper<>();
        exists.eq(NevBatteryDO::getTraceNumber, dto.getTraceNumber()).last("limit 1");
        if (batteryMapper.selectOne(exists) != null) {
            throw new ServiceException("溯源编号 [{}] 已存在", dto.getTraceNumber());
        }

        // 2. 落库 nev_battery
        Date now = new Date();
        NevBatteryDO entity = new NevBatteryDO();
        entity.setTraceNumber(dto.getTraceNumber());
        entity.setModel(dto.getModel());
        entity.setSerialNo(dto.getSerialNo());
        entity.setCapacityKwh(dto.getCapacityKwh());
        entity.setVoltage(dto.getVoltage());
        entity.setProducerId(producerId);
        entity.setCurrentOwnerId(producerId);
        entity.setCurrentRole("producer");
        entity.setCurrentStatus("PRODUCED");
        entity.setProducedAt(now);
        entity.setTenantId("000000");
        entity.setDelFlag("0");
        batteryMapper.insert(entity);

        // 3. 计算 dataHash（按 design.md §5 规约）
        LocalDateTime producedAtLdt = LocalDateTime.ofInstant(now.toInstant(), ZoneId.systemDefault());
        String specJson = canonicalSpec(dto);
        byte[] hashBytes = DataHashCalculator.produced(
            dto.getTraceNumber(), producerId, producedAtLdt, specJson);
        String dataHashHex = DataHashCalculator.toHex(hashBytes);

        // 4. 调用链上 registerBattery
        ChainCallResult chain = contractInvoker.invokeAs(
            ext.getWalletAddress(),
            CONTRACT_NAME,
            "registerBattery",
            List.of(dto.getTraceNumber(), dataHashHex)
        );
        if (!chain.isSuccess()) {
            log.error("[battery] 链上 registerBattery 失败: trace={} msg={}",
                dto.getTraceNumber(), chain.getMessage());
            throw new ServiceException("链上注册失败: " + chain.getMessage());
        }

        // 5. 写 nev_battery_lifecycle（PRODUCED, version=1）
        NevBatteryLifecycleDO lc = new NevBatteryLifecycleDO();
        lc.setBatteryId(entity.getId());
        lc.setEventType("PRODUCED");
        lc.setOperatorId(producerId);
        lc.setOperatorRole("producer");
        lc.setDataHash(dataHashHex);
        lc.setTxHash(chain.getTxHash());
        lc.setBlockNumber(chain.getBlockNumber());
        lc.setVersion(1);
        lc.setPayload(specJson);
        lc.setEventTime(now);
        lc.setTenantId("000000");
        lc.setDelFlag("0");
        lifecycleMapper.insert(lc);

        return BatteryRegisterVO.builder()
            .id(entity.getId())
            .traceNumber(entity.getTraceNumber())
            .dataHash(dataHashHex)
            .txHash(chain.getTxHash())
            .blockNumber(chain.getBlockNumber())
            .version(1)
            .producedAt(now)
            .chainStatus("SUCCESS")
            .message("注册成功")
            .build();
    }

    private String canonicalSpec(BatteryRegisterDTO dto) {
        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("model", dto.getModel());
        spec.put("serialNo", dto.getSerialNo());
        spec.put("capacityKwh", dto.getCapacityKwh() == null ? null : dto.getCapacityKwh().toPlainString());
        spec.put("voltage", dto.getVoltage() == null ? null : dto.getVoltage().toPlainString());
        spec.put("cellSupplier", dto.getCellSupplier());
        spec.put("cellType", dto.getCellType());
        spec.put("bmsInfo", dto.getBmsInfo());
        try {
            return objectMapper.writeValueAsString(spec);
        } catch (JsonProcessingException e) {
            throw new ServiceException("电池规格序列化失败: " + e.getMessage());
        }
    }
}
