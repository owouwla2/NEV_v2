package com.nev.carbon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nev.battery.domain.NevBatteryDO;
import com.nev.battery.mapper.NevBatteryMapper;
import com.nev.carbon.domain.NevCarbonFootprintDO;
import com.nev.carbon.domain.NevCarbonStageDO;
import com.nev.carbon.dto.CarbonFootprintVO;
import com.nev.carbon.mapper.NevCarbonFootprintMapper;
import com.nev.carbon.mapper.NevCarbonStageMapper;
import com.nev.common.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 碳足迹业务服务：调 calculator + 写库 + 查询
 *
 * @author NEV-v2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CarbonFootprintService {

    private static final String CALC_METHOD  = "GB-T-24067";
    private static final String CALC_VERSION = "v1";

    private final NevBatteryMapper batteryMapper;
    private final NevCarbonFootprintMapper footprintMapper;
    private final NevCarbonStageMapper stageMapper;
    private final CarbonCalculatorService calculator;
    private final ObjectMapper objectMapper;

    /** admin 触发：按 traceNumber 计算碳足迹（已有则覆盖） */
    @Transactional(rollbackFor = Exception.class)
    public CarbonFootprintVO calcAndSave(String traceNumber) {
        NevBatteryDO battery = findByTraceNumber(traceNumber);
        String cellType = resolveCellType(battery.getId());

        Date now = new Date();
        List<CarbonFootprintVO.StageDetail> stages = calculator.calcAllStages(battery.getCapacityKwh(), cellType);
        BigDecimal total = stages.stream().map(CarbonFootprintVO.StageDetail::getCo2Kg)
            .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP);

        // upsert footprint
        LambdaQueryWrapper<NevCarbonFootprintDO> fq = new LambdaQueryWrapper<>();
        fq.eq(NevCarbonFootprintDO::getBatteryId, battery.getId()).last("limit 1");
        NevCarbonFootprintDO fp = footprintMapper.selectOne(fq);
        if (fp == null) {
            fp = new NevCarbonFootprintDO();
            fp.setBatteryId(battery.getId());
        }
        fp.setTotalCo2Kg(total);
        fp.setCalcMethod(CALC_METHOD);
        fp.setCalcVersion(CALC_VERSION);
        fp.setCalcTime(now);
        fp.setTenantId("000000");
        fp.setDelFlag("0");
        if (fp.getId() == null) {
            footprintMapper.insert(fp);
        } else {
            footprintMapper.updateById(fp);
            // 删旧 stages
            LambdaQueryWrapper<NevCarbonStageDO> dq = new LambdaQueryWrapper<>();
            dq.eq(NevCarbonStageDO::getFootprintId, fp.getId());
            stageMapper.delete(dq);
        }

        // insert 5 stages
        for (CarbonFootprintVO.StageDetail st : stages) {
            NevCarbonStageDO sdo = new NevCarbonStageDO();
            sdo.setFootprintId(fp.getId());
            sdo.setStage(st.getStage());
            sdo.setCo2Kg(st.getCo2Kg());
            sdo.setBreakdown(toJson(st.getBreakdown()));
            sdo.setTenantId("000000");
            sdo.setDelFlag("0");
            stageMapper.insert(sdo);
        }

        return buildVo(battery, cellType, fp, stages);
    }

    /** 公开查询：返回碳足迹（含 5 阶段 breakdown） */
    public CarbonFootprintVO getByTraceNumber(String traceNumber) {
        NevBatteryDO battery = findByTraceNumber(traceNumber);
        return getByBattery(battery);
    }

    public CarbonFootprintVO getByBattery(NevBatteryDO battery) {
        LambdaQueryWrapper<NevCarbonFootprintDO> fq = new LambdaQueryWrapper<>();
        fq.eq(NevCarbonFootprintDO::getBatteryId, battery.getId()).last("limit 1");
        NevCarbonFootprintDO fp = footprintMapper.selectOne(fq);
        if (fp == null) {
            return CarbonFootprintVO.builder()
                .batteryId(battery.getId())
                .traceNumber(battery.getTraceNumber())
                .batteryModel(battery.getModel())
                .capacityKwh(battery.getCapacityKwh())
                .totalCo2Kg(BigDecimal.ZERO)
                .calcMethod(CALC_METHOD)
                .calcVersion(CALC_VERSION)
                .stages(new ArrayList<>())
                .build();
        }
        LambdaQueryWrapper<NevCarbonStageDO> sq = new LambdaQueryWrapper<>();
        sq.eq(NevCarbonStageDO::getFootprintId, fp.getId());
        List<NevCarbonStageDO> stageRows = stageMapper.selectList(sq);

        List<CarbonFootprintVO.StageDetail> details = new ArrayList<>(stageRows.size());
        for (NevCarbonStageDO row : stageRows) {
            details.add(CarbonFootprintVO.StageDetail.builder()
                .stage(row.getStage())
                .co2Kg(row.getCo2Kg())
                .breakdown(fromJsonList(row.getBreakdown()))
                .build());
        }
        // 排序：RAW MFG TRANS USE EOL
        details.sort((a, b) -> orderOf(a.getStage()) - orderOf(b.getStage()));

        return buildVo(battery, resolveCellType(battery.getId()), fp, details);
    }

    private CarbonFootprintVO buildVo(NevBatteryDO battery, String cellType,
                                      NevCarbonFootprintDO fp, List<CarbonFootprintVO.StageDetail> stages) {
        return CarbonFootprintVO.builder()
            .batteryId(battery.getId())
            .traceNumber(battery.getTraceNumber())
            .batteryModel(battery.getModel())
            .capacityKwh(battery.getCapacityKwh())
            .cellType(cellType)
            .totalCo2Kg(fp.getTotalCo2Kg())
            .calcMethod(fp.getCalcMethod())
            .calcVersion(fp.getCalcVersion())
            .calcTime(fp.getCalcTime())
            .stages(stages)
            .build();
    }

    private NevBatteryDO findByTraceNumber(String traceNumber) {
        LambdaQueryWrapper<NevBatteryDO> q = new LambdaQueryWrapper<>();
        q.eq(NevBatteryDO::getTraceNumber, traceNumber).last("limit 1");
        NevBatteryDO b = batteryMapper.selectOne(q);
        if (b == null) throw new ServiceException("溯源编号 [{}] 不存在", traceNumber);
        return b;
    }

    private String resolveCellType(Long batteryId) {
        NevBatteryDO b = batteryMapper.selectById(batteryId);
        if (b == null || b.getModel() == null) return "LFP";
        String m = b.getModel().toUpperCase();
        if (m.contains("NCM")) return "NCM";
        if (m.contains("NCA")) return "NCA";
        return "LFP";
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<CarbonFootprintVO.LineItem> fromJsonList(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json,
                objectMapper.getTypeFactory().constructCollectionType(List.class, CarbonFootprintVO.LineItem.class));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private int orderOf(String stage) {
        return switch (stage) {
            case "RAW"   -> 0;
            case "MFG"   -> 1;
            case "TRANS" -> 2;
            case "USE"   -> 3;
            case "EOL"   -> 4;
            default      -> 99;
        };
    }
}
