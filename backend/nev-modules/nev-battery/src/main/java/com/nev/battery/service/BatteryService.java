package com.nev.battery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nev.battery.domain.NevBatteryDO;
import com.nev.battery.domain.NevBatteryLifecycleDO;
import com.nev.battery.domain.SysNevUserExtDO;
import com.nev.battery.dto.BatteryEventVO;
import com.nev.battery.dto.BatteryRegisterDTO;
import com.nev.battery.dto.BatteryRegisterVO;
import com.nev.battery.dto.ReceiveDTO;
import com.nev.battery.dto.SellDTO;
import com.nev.battery.dto.TransferInDTO;
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
import java.util.function.Function;

/**
 * 电池业务服务（producer 注册 + distributor/retailer/recycler 事件上报）
 *
 * 事件 EventType 与链上 enum 对齐：
 *   PRODUCED=0 / IN_USE=1 / SOLD=2 / REPAIRED=3 / RECYCLED=4 / DISMANTLED=5
 *
 * @author NEV-v2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatteryService {

    private static final String CONTRACT_NAME = "LifecycleTrace";

    private static final int EVT_PRODUCED   = 0;
    private static final int EVT_IN_USE     = 1;
    private static final int EVT_SOLD       = 2;
    private static final int EVT_REPAIRED   = 3;
    private static final int EVT_RECYCLED   = 4;
    private static final int EVT_DISMANTLED = 5;

    private final NevBatteryMapper batteryMapper;
    private final NevBatteryLifecycleMapper lifecycleMapper;
    private final SysNevUserExtMapper userExtMapper;
    private final ContractInvoker contractInvoker;
    private final ObjectMapper objectMapper;

    // ==========================================================================
    // PRODUCED — Producer 注册新电池
    // ==========================================================================

    @Transactional(rollbackFor = Exception.class)
    public BatteryRegisterVO register(BatteryRegisterDTO dto) {
        OperatorContext op = requireOperator("producer");

        // 唯一性
        LambdaQueryWrapper<NevBatteryDO> exists = new LambdaQueryWrapper<>();
        exists.eq(NevBatteryDO::getTraceNumber, dto.getTraceNumber()).last("limit 1");
        if (batteryMapper.selectOne(exists) != null) {
            throw new ServiceException("溯源编号 [{}] 已存在", dto.getTraceNumber());
        }

        // 落 nev_battery
        Date now = new Date();
        NevBatteryDO entity = new NevBatteryDO();
        entity.setTraceNumber(dto.getTraceNumber());
        entity.setModel(dto.getModel());
        entity.setSerialNo(dto.getSerialNo());
        entity.setCapacityKwh(dto.getCapacityKwh());
        entity.setVoltage(dto.getVoltage());
        entity.setProducerId(op.userId());
        entity.setCurrentOwnerId(op.userId());
        entity.setCurrentRole("producer");
        entity.setCurrentStatus("PRODUCED");
        entity.setProducedAt(now);
        entity.setTenantId("000000");
        entity.setDelFlag("0");
        batteryMapper.insert(entity);

        // dataHash
        LocalDateTime producedAtLdt = toLdt(now);
        String specJson = canonicalSpec(dto);
        byte[] hashBytes = DataHashCalculator.produced(
            dto.getTraceNumber(), op.userId(), producedAtLdt, specJson);
        String dataHashHex = DataHashCalculator.toHex(hashBytes);

        // 链上 registerBattery（写入 BatteryRegistry.batteries mapping + emit BatteryRegistered）
        ChainCallResult chain = contractInvoker.invokeAs(
            op.walletAddress(),
            CONTRACT_NAME,
            "registerBattery",
            List.of(dto.getTraceNumber(), dataHashHex)
        );
        if (!chain.isSuccess()) {
            log.error("[battery] 链上 registerBattery 失败: trace={} msg={}",
                dto.getTraceNumber(), chain.getMessage());
            throw new ServiceException("链上注册失败: " + chain.getMessage());
        }

        // 链上 addEvent(PRODUCED) —— 同步写入 LifecycleTrace.lifecycleEvents 数组
        // 不然后续 IN_USE/SOLD/... 链上 version 与 backend version 错位（链上 v=1 对应 IN_USE 而非 PRODUCED）
        ChainCallResult lcChain = contractInvoker.invokeAs(
            op.walletAddress(),
            CONTRACT_NAME,
            "addEvent",
            List.of(dto.getTraceNumber(), EVT_PRODUCED, dataHashHex)
        );
        if (!lcChain.isSuccess()) {
            log.error("[battery] 链上 addEvent(PRODUCED) 失败: trace={} msg={}",
                dto.getTraceNumber(), lcChain.getMessage());
            throw new ServiceException("链上 PRODUCED 事件写入失败: " + lcChain.getMessage());
        }

        // lifecycle PRODUCED v1（用 addEvent 的 txHash，更能代表链上 lifecycle 行）
        insertLifecycle(entity.getId(), "PRODUCED", op, dataHashHex, lcChain, 1, specJson, now);

        return BatteryRegisterVO.builder()
            .id(entity.getId())
            .traceNumber(entity.getTraceNumber())
            .dataHash(dataHashHex)
            .txHash(lcChain.getTxHash())
            .blockNumber(lcChain.getBlockNumber())
            .version(1)
            .producedAt(now)
            .chainStatus("SUCCESS")
            .message("注册成功")
            .build();
    }

    // ==========================================================================
    // IN_USE — Distributor 接收电池
    // ==========================================================================

    @Transactional(rollbackFor = Exception.class)
    public BatteryEventVO transferIn(TransferInDTO dto) {
        OperatorContext op = requireOperator("distributor");
        return appendEvent(
            "IN_USE", EVT_IN_USE, op, dto.getTraceNumber(),
            (now) -> DataHashCalculator.inUse(
                dto.getTraceNumber(), dto.getFromOwnerId(), op.userId(), toLdt(now)),
            (now) -> serializePayload(Map.of(
                "fromOwnerId", dto.getFromOwnerId(),
                "toOwnerId",   op.userId(),
                "handoverAt",  toLdt(now).toString(),
                "remark",      dto.getRemark() == null ? "" : dto.getRemark()
            )),
            "distributor"
        );
    }

    // ==========================================================================
    // SOLD — Retailer 售出
    // ==========================================================================

    @Transactional(rollbackFor = Exception.class)
    public BatteryEventVO sell(SellDTO dto) {
        OperatorContext op = requireOperator("retailer");
        // 校验 consumer 存在且角色匹配
        SysNevUserExtDO consumer = userExtMapper.selectById(dto.getConsumerId());
        if (consumer == null || !"consumer".equals(consumer.getUserType())) {
            throw new ServiceException("consumer_id [{}] 不存在或角色不是 consumer", dto.getConsumerId());
        }
        return appendEvent(
            "SOLD", EVT_SOLD, op, dto.getTraceNumber(),
            (now) -> DataHashCalculator.sold(
                dto.getTraceNumber(), dto.getOrderNo(), dto.getConsumerId(), toLdt(now)),
            (now) -> serializePayload(Map.of(
                "orderNo",    dto.getOrderNo(),
                "consumerId", dto.getConsumerId(),
                "soldAt",     toLdt(now).toString(),
                "remark",     dto.getRemark() == null ? "" : dto.getRemark()
            )),
            // SOLD 后所有权转给 consumer
            "consumer",
            dto.getConsumerId()
        );
    }

    // ==========================================================================
    // RECYCLED — Recycler 接收回收
    // ==========================================================================

    @Transactional(rollbackFor = Exception.class)
    public BatteryEventVO receive(ReceiveDTO dto) {
        OperatorContext op = requireOperator("recycler");
        return appendEvent(
            "RECYCLED", EVT_RECYCLED, op, dto.getTraceNumber(),
            (now) -> DataHashCalculator.recycled(
                dto.getTraceNumber(), op.userId(), dto.getSoh(), toLdt(now)),
            (now) -> serializePayload(Map.of(
                "recyclerId", op.userId(),
                "soh",        dto.getSoh(),
                "receivedAt", toLdt(now).toString(),
                "remark",     dto.getRemark() == null ? "" : dto.getRemark()
            )),
            "recycler"
        );
    }

    // ==========================================================================
    // SOLD by Marketplace — 跨模块入口（nev-marketplace 订单完成时调用）
    // 不走 LoginHelper / requireOperator，由调用方传入 merchant 上下文
    // ==========================================================================

    /**
     * 商城订单完成时触发 SOLD 事件
     * @param traceNumber 电池业务编号
     * @param merchantUserId 商家用户ID（链上 operator）
     * @param merchantWalletAddress 商家钱包地址（Front 本地私钥库必须已导入）
     * @param orderNo 商城订单号（计入 dataHash）
     * @param consumerId 购买消费者用户ID（计入 dataHash + 转移 owner）
     */
    @Transactional(rollbackFor = Exception.class)
    public BatteryEventVO recordSoldByMerchant(
        String traceNumber, Long merchantUserId, String merchantWalletAddress,
        String orderNo, Long consumerId
    ) {
        if (!StringUtils.hasText(traceNumber) || merchantUserId == null
            || !StringUtils.hasText(merchantWalletAddress) || consumerId == null) {
            throw new ServiceException("recordSoldByMerchant 参数不全");
        }
        OperatorContext op = new OperatorContext(merchantUserId, merchantWalletAddress, "merchant");
        return appendEvent(
            "SOLD", EVT_SOLD, op, traceNumber,
            (now) -> DataHashCalculator.sold(
                traceNumber, orderNo, consumerId, toLdt(now)),
            (now) -> serializePayload(Map.of(
                "orderNo",    orderNo,
                "consumerId", consumerId,
                "merchantUserId", merchantUserId,
                "soldAt",     toLdt(now).toString(),
                "via",        "marketplace"
            )),
            "consumer",
            consumerId
        );
    }

    /**
     * 以旧换新场景：consumer accept 后 system 用 recycler 钱包代签触发链上 RECYCLED
     * @param traceNumber 老电池业务编号
     * @param recyclerUserId 接收回收的回收商用户ID
     * @param recyclerWalletAddress 回收商钱包地址（Front 本地私钥库已导入）
     * @param soh 评估的 State of Health（0-100）
     * @param requestNo 以旧换新申请单号（计入 payload，便于审计）
     */
    @Transactional(rollbackFor = Exception.class)
    public BatteryEventVO recordRecycledByTradeIn(
        String traceNumber, Long recyclerUserId, String recyclerWalletAddress,
        Integer soh, String requestNo
    ) {
        if (!StringUtils.hasText(traceNumber) || recyclerUserId == null
            || !StringUtils.hasText(recyclerWalletAddress)) {
            throw new ServiceException("recordRecycledByTradeIn 参数不全");
        }
        OperatorContext op = new OperatorContext(recyclerUserId, recyclerWalletAddress, "recycler");
        Integer soundSoh = soh == null ? 0 : soh;
        return appendEvent(
            "RECYCLED", EVT_RECYCLED, op, traceNumber,
            (now) -> DataHashCalculator.recycled(
                traceNumber, recyclerUserId, soundSoh, toLdt(now)),
            (now) -> serializePayload(Map.of(
                "recyclerId", recyclerUserId,
                "soh",        soundSoh,
                "receivedAt", toLdt(now).toString(),
                "via",        "trade-in",
                "requestNo",  requestNo == null ? "" : requestNo
            )),
            "recycler"
        );
    }

    // ==========================================================================
    // Helpers
    // ==========================================================================

    /** 通用事件追加：电池存在性 → 计算 hash → 链上 addEvent → 写 lifecycle + 更新 nev_battery */
    private BatteryEventVO appendEvent(
        String eventType,
        int chainEventTypeInt,
        OperatorContext op,
        String traceNumber,
        Function<Date, byte[]> hashFn,
        Function<Date, String> payloadFn,
        String newCurrentRole
    ) {
        return appendEvent(eventType, chainEventTypeInt, op, traceNumber, hashFn, payloadFn, newCurrentRole, op.userId());
    }

    private BatteryEventVO appendEvent(
        String eventType,
        int chainEventTypeInt,
        OperatorContext op,
        String traceNumber,
        Function<Date, byte[]> hashFn,
        Function<Date, String> payloadFn,
        String newCurrentRole,
        Long newCurrentOwnerId
    ) {
        // 1. 校验电池存在
        NevBatteryDO battery = findBattery(traceNumber);
        if ("DISMANTLED".equals(battery.getCurrentStatus())) {
            throw new ServiceException("电池 [{}] 已报废拆解，不能继续操作", traceNumber);
        }

        Date now = new Date();

        // 2. 计算 dataHash
        byte[] hashBytes = hashFn.apply(now);
        String dataHashHex = DataHashCalculator.toHex(hashBytes);
        String payload = payloadFn.apply(now);

        // 3. 链上 addEvent
        ChainCallResult chain = contractInvoker.invokeAs(
            op.walletAddress(),
            CONTRACT_NAME,
            "addEvent",
            List.of(traceNumber, chainEventTypeInt, dataHashHex)
        );
        if (!chain.isSuccess()) {
            log.error("[battery] 链上 addEvent({}) 失败: trace={} msg={}",
                eventType, traceNumber, chain.getMessage());
            throw new ServiceException("链上 {} 失败: {}", eventType, chain.getMessage());
        }

        // 4. 计算 version（按 battery_id 单调递增）
        LambdaQueryWrapper<NevBatteryLifecycleDO> maxQ = new LambdaQueryWrapper<>();
        maxQ.eq(NevBatteryLifecycleDO::getBatteryId, battery.getId())
            .orderByDesc(NevBatteryLifecycleDO::getVersion).last("limit 1");
        NevBatteryLifecycleDO latest = lifecycleMapper.selectOne(maxQ);
        int nextVersion = (latest == null ? 0 : latest.getVersion()) + 1;

        // 5. 写 lifecycle
        insertLifecycle(battery.getId(), eventType, op, dataHashHex, chain, nextVersion, payload, now);

        // 6. 更新 nev_battery 当前状态 + 持有者
        battery.setCurrentStatus(eventType);
        battery.setCurrentRole(newCurrentRole);
        battery.setCurrentOwnerId(newCurrentOwnerId);
        batteryMapper.updateById(battery);

        return BatteryEventVO.builder()
            .batteryId(battery.getId())
            .traceNumber(traceNumber)
            .eventType(eventType)
            .version(nextVersion)
            .dataHash(dataHashHex)
            .txHash(chain.getTxHash())
            .blockNumber(chain.getBlockNumber())
            .eventTime(now)
            .currentStatus(eventType)
            .currentOwnerId(newCurrentOwnerId)
            .currentRole(newCurrentRole)
            .chainStatus("SUCCESS")
            .message(eventType + " 事件已上报")
            .build();
    }

    private NevBatteryDO findBattery(String traceNumber) {
        LambdaQueryWrapper<NevBatteryDO> q = new LambdaQueryWrapper<>();
        q.eq(NevBatteryDO::getTraceNumber, traceNumber).last("limit 1");
        NevBatteryDO b = batteryMapper.selectOne(q);
        if (b == null) {
            throw new ServiceException("溯源编号 [{}] 不存在", traceNumber);
        }
        return b;
    }

    private OperatorContext requireOperator(String requiredRole) {
        Long uid = LoginHelper.getUserId();
        if (uid == null) {
            throw new ServiceException("未登录");
        }
        SysNevUserExtDO ext = userExtMapper.selectById(uid);
        if (ext == null || !StringUtils.hasText(ext.getWalletAddress())) {
            throw new ServiceException("当前用户 [{}] 未绑定区块链钱包地址", uid);
        }
        if (!requiredRole.equals(ext.getUserType())) {
            throw new ServiceException("当前用户角色 [{}] 不是 {}，无权操作",
                ext.getUserType(), requiredRole);
        }
        return new OperatorContext(uid, ext.getWalletAddress(), ext.getUserType());
    }

    private void insertLifecycle(Long batteryId, String eventType, OperatorContext op,
                                 String dataHashHex, ChainCallResult chain,
                                 int version, String payload, Date eventTime) {
        NevBatteryLifecycleDO lc = new NevBatteryLifecycleDO();
        lc.setBatteryId(batteryId);
        lc.setEventType(eventType);
        lc.setOperatorId(op.userId());
        lc.setOperatorRole(op.role());
        lc.setDataHash(dataHashHex);
        lc.setTxHash(chain.getTxHash());
        lc.setBlockNumber(chain.getBlockNumber());
        lc.setVersion(version);
        lc.setPayload(payload);
        lc.setEventTime(eventTime);
        lc.setTenantId("000000");
        lc.setDelFlag("0");
        lifecycleMapper.insert(lc);
    }

    private LocalDateTime toLdt(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
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
        return serializePayload(spec);
    }

    private String serializePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new ServiceException("payload 序列化失败: " + e.getMessage());
        }
    }

    /** 当前操作者上下文 */
    private record OperatorContext(Long userId, String walletAddress, String role) {
    }
}
