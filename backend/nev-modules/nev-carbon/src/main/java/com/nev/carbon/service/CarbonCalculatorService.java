package com.nev.carbon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nev.carbon.domain.NevEmissionFactorDO;
import com.nev.carbon.dto.CarbonFootprintVO;
import com.nev.carbon.mapper.NevEmissionFactorMapper;
import com.nev.common.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 碳计算引擎（GB-T 24067 简化版）
 *
 * 5 阶段公式：CF_total = CF_raw + CF_mfg + CF_trans + CF_use + CF_eol
 *   每阶段 CF_stage = Σ(activity_data × emission_factor)
 *
 * 关键假设（写在 design 注释里，可通过 D20 增加 overrides 调整）：
 *   - LFP 每 kWh 用料：锂 0.10 kg + 铜 0.50 kg + 铝 0.40 kg（无钴无镍）
 *   - NCM 每 kWh 用料：锂 0.12 + 钴 0.20 + 镍 0.45 + 铜 0.50 + 铝 0.40 kg
 *   - 电池重量 5 kg/kWh
 *   - 运输 1000 km 公路（柴油重卡）
 *   - 使用阶段：循环 2000 次 × DOD 0.8 × CN 电网充电
 *   - 报废 EOL：按重量给回收抵扣（负值）
 *
 * @author NEV-v2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CarbonCalculatorService {

    private static final BigDecimal WEIGHT_PER_KWH_KG    = new BigDecimal("5.00");
    private static final BigDecimal TRANS_DISTANCE_KM    = new BigDecimal("1000");
    private static final BigDecimal CYCLE_COUNT          = new BigDecimal("2000");
    private static final BigDecimal DEPTH_OF_DISCHARGE   = new BigDecimal("0.80");

    private final NevEmissionFactorMapper factorMapper;

    public List<CarbonFootprintVO.StageDetail> calcAllStages(
        BigDecimal capacityKwh, String cellType
    ) {
        if (capacityKwh == null || capacityKwh.signum() <= 0) {
            throw new ServiceException("电池容量必须 > 0");
        }
        String ct = cellType == null ? "LFP" : cellType.toUpperCase();
        Map<String, NevEmissionFactorDO> fm = loadFactorMap();

        List<CarbonFootprintVO.StageDetail> stages = new ArrayList<>(5);
        stages.add(calcRaw(capacityKwh, ct, fm));
        stages.add(calcMfg(capacityKwh, fm));
        stages.add(calcTrans(capacityKwh, fm));
        stages.add(calcUse(capacityKwh, fm));
        stages.add(calcEol(capacityKwh, fm));
        return stages;
    }

    // ============ RAW 原材料 ============
    private CarbonFootprintVO.StageDetail calcRaw(BigDecimal capacity, String cellType, Map<String, NevEmissionFactorDO> fm) {
        List<CarbonFootprintVO.LineItem> items = new ArrayList<>();
        // 单位用量（kg / kWh）
        addRawLine(items, "RAW-LITHIUM",  isNcm(cellType) ? "0.12" : "0.10", capacity, fm);
        if (isNcm(cellType)) {
            addRawLine(items, "RAW-COBALT", "0.20", capacity, fm);
            addRawLine(items, "RAW-NICKEL", "0.45", capacity, fm);
        }
        addRawLine(items, "RAW-COPPER",   "0.50", capacity, fm);
        addRawLine(items, "RAW-ALUMINUM", "0.40", capacity, fm);
        return stage("RAW", items);
    }

    private void addRawLine(List<CarbonFootprintVO.LineItem> items, String code, String kgPerKwh,
                            BigDecimal capacity, Map<String, NevEmissionFactorDO> fm) {
        NevEmissionFactorDO f = fm.get(code);
        if (f == null) {
            log.warn("[carbon] 排放因子 {} 缺失，跳过", code);
            return;
        }
        BigDecimal totalKg = new BigDecimal(kgPerKwh).multiply(capacity).setScale(4, RoundingMode.HALF_UP);
        BigDecimal co2 = totalKg.multiply(f.getValue()).setScale(4, RoundingMode.HALF_UP);
        items.add(CarbonFootprintVO.LineItem.builder()
            .factorCode(code).factorName(f.getFactorName())
            .input(totalKg).inputUnit("kg")
            .factorValue(f.getValue()).factorUnit(f.getUnit())
            .co2Kg(co2)
            .note(kgPerKwh + " kg/kWh × " + capacity + " kWh")
            .build());
    }

    // ============ MFG 制造 ============
    private CarbonFootprintVO.StageDetail calcMfg(BigDecimal capacity, Map<String, NevEmissionFactorDO> fm) {
        List<CarbonFootprintVO.LineItem> items = new ArrayList<>();
        addEnergyLine(items, "MFG-CELL-ASSEMBLY",   capacity, fm, "电芯组装");
        addEnergyLine(items, "MFG-MODULE-ASSEMBLY", capacity, fm, "模组组装");
        addEnergyLine(items, "MFG-PACK-ASSEMBLY",   capacity, fm, "PACK 组装");
        return stage("MFG", items);
    }

    private void addEnergyLine(List<CarbonFootprintVO.LineItem> items, String code,
                               BigDecimal capacity, Map<String, NevEmissionFactorDO> fm, String note) {
        NevEmissionFactorDO f = fm.get(code);
        if (f == null) return;
        BigDecimal co2 = capacity.multiply(f.getValue()).setScale(4, RoundingMode.HALF_UP);
        items.add(CarbonFootprintVO.LineItem.builder()
            .factorCode(code).factorName(f.getFactorName())
            .input(capacity).inputUnit("kWh")
            .factorValue(f.getValue()).factorUnit(f.getUnit())
            .co2Kg(co2).note(note)
            .build());
    }

    // ============ TRANS 运输 ============
    private CarbonFootprintVO.StageDetail calcTrans(BigDecimal capacity, Map<String, NevEmissionFactorDO> fm) {
        List<CarbonFootprintVO.LineItem> items = new ArrayList<>();
        NevEmissionFactorDO f = fm.get("TRANS-TRUCK-DIESEL");
        if (f != null) {
            BigDecimal weightT = capacity.multiply(WEIGHT_PER_KWH_KG).divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);
            BigDecimal tkm = weightT.multiply(TRANS_DISTANCE_KM).setScale(4, RoundingMode.HALF_UP);
            BigDecimal co2 = tkm.multiply(f.getValue()).setScale(4, RoundingMode.HALF_UP);
            items.add(CarbonFootprintVO.LineItem.builder()
                .factorCode(f.getFactorCode()).factorName(f.getFactorName())
                .input(tkm).inputUnit("t·km")
                .factorValue(f.getValue()).factorUnit(f.getUnit())
                .co2Kg(co2)
                .note("重 " + weightT + " t × " + TRANS_DISTANCE_KM + " km（公路重卡）")
                .build());
        }
        return stage("TRANS", items);
    }

    // ============ USE 使用 ============
    private CarbonFootprintVO.StageDetail calcUse(BigDecimal capacity, Map<String, NevEmissionFactorDO> fm) {
        List<CarbonFootprintVO.LineItem> items = new ArrayList<>();
        NevEmissionFactorDO f = fm.get("USE-CHARGE-CN-GRID");
        if (f != null) {
            BigDecimal totalKwh = capacity.multiply(CYCLE_COUNT).multiply(DEPTH_OF_DISCHARGE)
                .setScale(2, RoundingMode.HALF_UP);
            BigDecimal co2 = totalKwh.multiply(f.getValue()).setScale(4, RoundingMode.HALF_UP);
            items.add(CarbonFootprintVO.LineItem.builder()
                .factorCode(f.getFactorCode()).factorName(f.getFactorName())
                .input(totalKwh).inputUnit("kWh")
                .factorValue(f.getValue()).factorUnit(f.getUnit())
                .co2Kg(co2)
                .note(capacity + " kWh × " + CYCLE_COUNT + " 次 × DOD " + DEPTH_OF_DISCHARGE + "（中国电网）")
                .build());
        }
        return stage("USE", items);
    }

    // ============ EOL 报废回收 ============
    private CarbonFootprintVO.StageDetail calcEol(BigDecimal capacity, Map<String, NevEmissionFactorDO> fm) {
        List<CarbonFootprintVO.LineItem> items = new ArrayList<>();
        NevEmissionFactorDO f = fm.get("EOL-RECYCLE-CREDIT");
        if (f != null) {
            BigDecimal weightKg = capacity.multiply(WEIGHT_PER_KWH_KG).setScale(2, RoundingMode.HALF_UP);
            BigDecimal co2 = weightKg.multiply(f.getValue()).setScale(4, RoundingMode.HALF_UP);
            items.add(CarbonFootprintVO.LineItem.builder()
                .factorCode(f.getFactorCode()).factorName(f.getFactorName())
                .input(weightKg).inputUnit("kg")
                .factorValue(f.getValue()).factorUnit(f.getUnit())
                .co2Kg(co2)
                .note("电池总重 " + weightKg + " kg × 回收抵扣（负值表示减排）")
                .build());
        }
        return stage("EOL", items);
    }

    // ============ helpers ============
    private CarbonFootprintVO.StageDetail stage(String name, List<CarbonFootprintVO.LineItem> items) {
        BigDecimal sum = items.stream().map(CarbonFootprintVO.LineItem::getCo2Kg)
            .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP);
        return CarbonFootprintVO.StageDetail.builder().stage(name).co2Kg(sum).breakdown(items).build();
    }

    private boolean isNcm(String cellType) {
        return cellType.startsWith("NCM") || cellType.startsWith("NCA");
    }

    private Map<String, NevEmissionFactorDO> loadFactorMap() {
        LambdaQueryWrapper<NevEmissionFactorDO> q = new LambdaQueryWrapper<>();
        List<NevEmissionFactorDO> all = factorMapper.selectList(q);
        Map<String, NevEmissionFactorDO> m = new HashMap<>(all.size());
        for (NevEmissionFactorDO f : all) {
            m.put(f.getFactorCode(), f);
        }
        return m;
    }
}
