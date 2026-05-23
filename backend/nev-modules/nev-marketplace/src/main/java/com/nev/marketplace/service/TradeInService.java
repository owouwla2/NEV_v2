package com.nev.marketplace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nev.battery.domain.NevBatteryDO;
import com.nev.battery.domain.SysNevUserExtDO;
import com.nev.battery.dto.BatteryEventVO;
import com.nev.battery.mapper.NevBatteryMapper;
import com.nev.battery.mapper.SysNevUserExtMapper;
import com.nev.battery.service.BatteryService;
import com.nev.common.core.exception.ServiceException;
import com.nev.common.satoken.utils.LoginHelper;
import com.nev.marketplace.domain.NevTradeInRequestDO;
import com.nev.marketplace.dto.TradeInEvaluateDTO;
import com.nev.marketplace.dto.TradeInSubmitDTO;
import com.nev.marketplace.dto.TradeInVO;
import com.nev.marketplace.mapper.NevTradeInRequestMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 以旧换新业务
 *
 * 状态机:
 *   SUBMITTED  ← consumer.submit
 *   EVALUATED  ← recycler.evaluate
 *   ACCEPTED   ← consumer.accept （内部直接推进到 COMPLETED 并触发链上 RECYCLED）
 *   COMPLETED  ← 终态（accept 后）
 *   REJECTED   ← consumer.reject
 *
 * @author NEV-v2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeInService {

    private final NevTradeInRequestMapper requestMapper;
    private final NevBatteryMapper batteryMapper;
    private final SysNevUserExtMapper userExtMapper;
    private final BatteryService batteryService;
    private final ObjectMapper objectMapper;

    /** consumer 提交申请 */
    @Transactional(rollbackFor = Exception.class)
    public TradeInVO submit(TradeInSubmitDTO dto) {
        Long consumerId = requireUser();
        // 校验：老电池存在 + 属于该 consumer + 状态为 SOLD（已售给消费者）
        NevBatteryDO battery = findBatteryByTrace(dto.getOldBatteryTraceNumber());
        if (!consumerId.equals(battery.getCurrentOwnerId())) {
            throw new ServiceException("电池 [{}] 不属于您，无法申请换新", dto.getOldBatteryTraceNumber());
        }
        if ("RECYCLED".equals(battery.getCurrentStatus())
            || "DISMANTLED".equals(battery.getCurrentStatus())) {
            throw new ServiceException("电池 [{}] 已回收/报废，无法重复换新", dto.getOldBatteryTraceNumber());
        }

        // 同一电池不允许多个未完结的申请
        LambdaQueryWrapper<NevTradeInRequestDO> active = new LambdaQueryWrapper<>();
        active.eq(NevTradeInRequestDO::getOldBatteryId, battery.getId())
            .in(NevTradeInRequestDO::getStatus, List.of("SUBMITTED", "EVALUATED", "ACCEPTED"));
        if (requestMapper.selectCount(active) > 0) {
            throw new ServiceException("电池 [{}] 已有进行中的换新申请", battery.getTraceNumber());
        }

        Date now = new Date();
        NevTradeInRequestDO req = new NevTradeInRequestDO();
        req.setRequestNo(generateRequestNo());
        req.setConsumerId(consumerId);
        req.setOldBatteryId(battery.getId());
        req.setNewProductId(dto.getNewProductId());
        req.setStatus("SUBMITTED");
        req.setSubmittedAt(now);
        req.setTenantId("000000");
        req.setDelFlag("0");
        req.setRemark(dto.getRemark());
        requestMapper.insert(req);

        return toVo(req, battery, null);
    }

    /** recycler 评估 */
    @Transactional(rollbackFor = Exception.class)
    public TradeInVO evaluate(TradeInEvaluateDTO dto) {
        Long recyclerId = requireUser();
        SysNevUserExtDO ext = userExtMapper.selectById(recyclerId);
        if (ext == null || !"recycler".equals(ext.getUserType())) {
            throw new ServiceException("当前用户不是 recycler 角色");
        }
        NevTradeInRequestDO req = requestMapper.selectById(dto.getRequestId());
        if (req == null) throw new ServiceException("换新申请 [{}] 不存在", dto.getRequestId());
        if (!"SUBMITTED".equals(req.getStatus())) {
            throw new ServiceException("当前状态 [{}] 不可评估", req.getStatus());
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("soh", dto.getSoh());
        payload.put("summary", dto.getSummary() == null ? "" : dto.getSummary());

        req.setRecyclerId(recyclerId);
        req.setEvaluatorId(recyclerId);
        req.setEvaluatedAmount(dto.getEvaluatedAmount());
        req.setEvaluationPayload(toJson(payload));
        req.setEvaluatedAt(new Date());
        req.setStatus("EVALUATED");
        requestMapper.updateById(req);

        return toVo(req, null, null);
    }

    /** consumer 接受 → 自动触发链上 RECYCLED → COMPLETED */
    @Transactional(rollbackFor = Exception.class)
    public TradeInVO accept(Long requestId) {
        Long consumerId = requireUser();
        NevTradeInRequestDO req = requestMapper.selectById(requestId);
        if (req == null) throw new ServiceException("换新申请 [{}] 不存在", requestId);
        if (!consumerId.equals(req.getConsumerId())) {
            throw new ServiceException("无权操作他人的换新申请");
        }
        if (!"EVALUATED".equals(req.getStatus())) {
            throw new ServiceException("当前状态 [{}] 不可接受（应先评估）", req.getStatus());
        }

        NevBatteryDO battery = batteryMapper.selectById(req.getOldBatteryId());
        if (battery == null) throw new ServiceException("关联电池丢失");
        SysNevUserExtDO recyclerExt = userExtMapper.selectById(req.getRecyclerId());
        if (recyclerExt == null || !StringUtils.hasText(recyclerExt.getWalletAddress())) {
            throw new ServiceException("回收商钱包地址缺失，无法触发链上 RECYCLED");
        }

        Date now = new Date();
        req.setAcceptedAt(now);
        req.setStatus("ACCEPTED");
        requestMapper.updateById(req);

        // 链上 RECYCLED（system 用 recycler 钱包代签）
        Integer soh = parseSoh(req.getEvaluationPayload());
        BatteryEventVO chain = batteryService.recordRecycledByTradeIn(
            battery.getTraceNumber(), req.getRecyclerId(),
            recyclerExt.getWalletAddress(), soh, req.getRequestNo()
        );

        req.setRecycledAt(now);
        req.setCompletedAt(now);
        req.setStatus("COMPLETED");
        requestMapper.updateById(req);

        return toVo(req, battery, chain != null ? chain.getTxHash() : null);
    }

    /** consumer 拒绝 */
    @Transactional(rollbackFor = Exception.class)
    public TradeInVO reject(Long requestId, String reason) {
        Long consumerId = requireUser();
        NevTradeInRequestDO req = requestMapper.selectById(requestId);
        if (req == null) throw new ServiceException("换新申请 [{}] 不存在", requestId);
        if (!consumerId.equals(req.getConsumerId())) {
            throw new ServiceException("无权操作他人的换新申请");
        }
        if (!List.of("SUBMITTED", "EVALUATED").contains(req.getStatus())) {
            throw new ServiceException("当前状态 [{}] 不可拒绝", req.getStatus());
        }
        req.setStatus("REJECTED");
        req.setRemark((req.getRemark() == null ? "" : req.getRemark() + "; ") + "拒绝原因: " + (reason == null ? "无" : reason));
        requestMapper.updateById(req);
        return toVo(req, null, null);
    }

    public TradeInVO detail(Long requestId) {
        Long userId = requireUser();
        NevTradeInRequestDO req = requestMapper.selectById(requestId);
        if (req == null) throw new ServiceException("换新申请 [{}] 不存在", requestId);
        // 仅本人 / 关联 recycler 可看
        if (!userId.equals(req.getConsumerId()) && !userId.equals(req.getRecyclerId())) {
            throw new ServiceException("无权查看");
        }
        NevBatteryDO b = batteryMapper.selectById(req.getOldBatteryId());
        return toVo(req, b, null);
    }

    public List<TradeInVO> listMineConsumer() {
        Long consumerId = requireUser();
        LambdaQueryWrapper<NevTradeInRequestDO> q = new LambdaQueryWrapper<>();
        q.eq(NevTradeInRequestDO::getConsumerId, consumerId)
            .orderByDesc(NevTradeInRequestDO::getId);
        return requestMapper.selectList(q).stream().map(r -> toVo(r, null, null)).toList();
    }

    public List<TradeInVO> listPendingForRecycler() {
        // recycler 看 SUBMITTED 列表（待评估）
        LambdaQueryWrapper<NevTradeInRequestDO> q = new LambdaQueryWrapper<>();
        q.eq(NevTradeInRequestDO::getStatus, "SUBMITTED")
            .orderByDesc(NevTradeInRequestDO::getId);
        return requestMapper.selectList(q).stream().map(r -> toVo(r, null, null)).toList();
    }

    // -------- helpers --------

    private NevBatteryDO findBatteryByTrace(String traceNumber) {
        LambdaQueryWrapper<NevBatteryDO> q = new LambdaQueryWrapper<>();
        q.eq(NevBatteryDO::getTraceNumber, traceNumber).last("limit 1");
        NevBatteryDO b = batteryMapper.selectOne(q);
        if (b == null) throw new ServiceException("溯源编号 [{}] 不存在", traceNumber);
        return b;
    }

    private TradeInVO toVo(NevTradeInRequestDO r, NevBatteryDO b, String chainTx) {
        Integer soh = parseSoh(r.getEvaluationPayload());
        String summary = parseSummary(r.getEvaluationPayload());
        return TradeInVO.builder()
            .id(r.getId())
            .requestNo(r.getRequestNo())
            .consumerId(r.getConsumerId())
            .oldBatteryId(r.getOldBatteryId())
            .oldBatteryTraceNumber(b == null ? null : b.getTraceNumber())
            .newProductId(r.getNewProductId())
            .evaluatedAmount(r.getEvaluatedAmount())
            .recyclerId(r.getRecyclerId())
            .evaluatorId(r.getEvaluatorId())
            .status(r.getStatus())
            .soh(soh)
            .evaluationSummary(summary)
            .evaluationPayload(r.getEvaluationPayload())
            .submittedAt(r.getSubmittedAt())
            .evaluatedAt(r.getEvaluatedAt())
            .acceptedAt(r.getAcceptedAt())
            .recycledAt(r.getRecycledAt())
            .completedAt(r.getCompletedAt())
            .chainTxHash(chainTx)
            .build();
    }

    @SuppressWarnings("unchecked")
    private Integer parseSoh(String payload) {
        if (payload == null || payload.isBlank()) return null;
        try {
            Map<String, Object> m = objectMapper.readValue(payload, Map.class);
            Object v = m.get("soh");
            if (v instanceof Number n) return n.intValue();
            if (v != null) return Integer.valueOf(v.toString());
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String parseSummary(String payload) {
        if (payload == null || payload.isBlank()) return null;
        try {
            Map<String, Object> m = objectMapper.readValue(payload, Map.class);
            Object v = m.get("summary");
            return v == null ? null : v.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Long requireUser() {
        Long uid = LoginHelper.getUserId();
        if (uid == null) throw new ServiceException("未登录");
        return uid;
    }

    private String generateRequestNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "TI" + ts + ThreadLocalRandom.current().nextInt(100000, 999999);
    }
}
