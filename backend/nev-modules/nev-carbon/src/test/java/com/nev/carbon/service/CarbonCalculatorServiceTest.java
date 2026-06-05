package com.nev.carbon.service;

import com.nev.carbon.domain.NevEmissionFactorDO;
import com.nev.carbon.dto.CarbonFootprintVO;
import com.nev.carbon.mapper.NevEmissionFactorMapper;
import com.nev.common.core.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * NEV-v2 round-4 M-B · P1-002 业务单测
 *
 * 目标：CarbonCalculatorService 是 GB-T 24067 5 阶段计算引擎，毕设核心得分点。
 * 测试覆盖：
 *   1) 入参校验（null / 0 / 负 capacity）
 *   2) cellType 分流（LFP 不含 Co/Ni；NCM 含 Co + Ni）
 *   3) cellType=null 默认 LFP
 *   4) 5 阶段全部产出（RAW/MFG/TRANS/USE/EOL）
 *   5) 排放因子缺失 → stage 存在但 breakdown 为空、co2=0（容错）
 *   6) **公式正确性**：用真实因子值 + 已知 capacity（75 kWh）反推 RAW/TRANS/USE 关键中间量
 *
 * 全部 Mockito 单测，无 Spring Context；@Tag("dev") 让默认 profile 跑得到。
 *
 * @author NEV-v2 round-4
 */
@Tag("dev")
@DisplayName("CarbonCalculatorService.calcAllStages")
@ExtendWith(MockitoExtension.class)
class CarbonCalculatorServiceTest {

    @Mock private NevEmissionFactorMapper factorMapper;

    private CarbonCalculatorService service;

    @BeforeEach
    void setUp() {
        service = new CarbonCalculatorService(factorMapper);
    }

    // ============ 入参校验 ============

    @Test
    @DisplayName("capacity=null 抛 ServiceException")
    void nullCapacityThrows() {
        assertThatThrownBy(() -> service.calcAllStages(null, "LFP"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("电池容量");
    }

    @Test
    @DisplayName("capacity=0 抛 ServiceException")
    void zeroCapacityThrows() {
        assertThatThrownBy(() -> service.calcAllStages(BigDecimal.ZERO, "LFP"))
            .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("capacity<0 抛 ServiceException")
    void negativeCapacityThrows() {
        assertThatThrownBy(() -> service.calcAllStages(new BigDecimal("-1"), "LFP"))
            .isInstanceOf(ServiceException.class);
    }

    // ============ cellType 分流 ============

    @Test
    @DisplayName("LFP 类型：RAW 阶段仅 锂/铜/铝，不含 钴/镍")
    void lfpRawHasNoCobaltOrNickel() {
        when(factorMapper.selectList(any())).thenReturn(allFactors());

        List<CarbonFootprintVO.StageDetail> stages =
            service.calcAllStages(new BigDecimal("75"), "LFP");

        List<String> rawCodes = breakdownCodes(stages, "RAW");
        assertThat(rawCodes).contains("RAW-LITHIUM", "RAW-COPPER", "RAW-ALUMINUM");
        assertThat(rawCodes).doesNotContain("RAW-COBALT", "RAW-NICKEL");
    }

    @Test
    @DisplayName("NCM 类型：RAW 阶段含 锂/钴/镍/铜/铝 共 5 项")
    void ncmRawHasCobaltAndNickel() {
        when(factorMapper.selectList(any())).thenReturn(allFactors());

        List<CarbonFootprintVO.StageDetail> stages =
            service.calcAllStages(new BigDecimal("75"), "NCM811");

        List<String> rawCodes = breakdownCodes(stages, "RAW");
        assertThat(rawCodes).containsExactlyInAnyOrder(
            "RAW-LITHIUM", "RAW-COBALT", "RAW-NICKEL", "RAW-COPPER", "RAW-ALUMINUM");
    }

    @Test
    @DisplayName("cellType=null 默认按 LFP 处理（不含钴镍）")
    void nullCellTypeDefaultsToLfp() {
        when(factorMapper.selectList(any())).thenReturn(allFactors());

        List<CarbonFootprintVO.StageDetail> stages =
            service.calcAllStages(new BigDecimal("75"), null);

        List<String> rawCodes = breakdownCodes(stages, "RAW");
        assertThat(rawCodes).doesNotContain("RAW-COBALT", "RAW-NICKEL");
    }

    // ============ 5 阶段全产出 ============

    @Test
    @DisplayName("无论 cellType，必须返回 5 个 stage：RAW/MFG/TRANS/USE/EOL")
    void alwaysReturnFiveStages() {
        when(factorMapper.selectList(any())).thenReturn(allFactors());

        List<CarbonFootprintVO.StageDetail> stages =
            service.calcAllStages(new BigDecimal("75"), "LFP");

        assertThat(stages).hasSize(5);
        assertThat(stages.stream().map(CarbonFootprintVO.StageDetail::getStage).toList())
            .containsExactly("RAW", "MFG", "TRANS", "USE", "EOL");
    }

    // ============ 容错 ============

    @Test
    @DisplayName("排放因子全部缺失：5 stage 仍存在，breakdown 为空，co2=0")
    void missingFactorsYieldEmptyBreakdown() {
        when(factorMapper.selectList(any())).thenReturn(List.of()); // 数据库空表

        List<CarbonFootprintVO.StageDetail> stages =
            service.calcAllStages(new BigDecimal("75"), "LFP");

        assertThat(stages).hasSize(5);
        for (CarbonFootprintVO.StageDetail s : stages) {
            assertThat(s.getBreakdown()).isEmpty();
            assertThat(s.getCo2Kg()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ============ 公式正确性（毕设核心） ============

    @Test
    @DisplayName("RAW LFP 锂用量 = 0.1 kg/kWh × 75 kWh = 7.5 kg；co2 = 7.5 × 14.3 = 107.25")
    void rawLithiumFormulaForLfp() {
        when(factorMapper.selectList(any())).thenReturn(allFactors());

        List<CarbonFootprintVO.StageDetail> stages =
            service.calcAllStages(new BigDecimal("75"), "LFP");

        CarbonFootprintVO.LineItem lithium =
            breakdownItem(stages, "RAW", "RAW-LITHIUM");
        assertThat(lithium.getInput()).isEqualByComparingTo("7.5");
        assertThat(lithium.getInputUnit()).isEqualTo("kg");
        // 7.5 × 14.3 = 107.25
        assertThat(lithium.getCo2Kg()).isEqualByComparingTo("107.25");
    }

    @Test
    @DisplayName("RAW NCM 锂用量 = 0.12 × 75 = 9 kg（高于 LFP）")
    void rawLithiumFormulaForNcm() {
        when(factorMapper.selectList(any())).thenReturn(allFactors());

        List<CarbonFootprintVO.StageDetail> stages =
            service.calcAllStages(new BigDecimal("75"), "NCM811");

        CarbonFootprintVO.LineItem lithium = breakdownItem(stages, "RAW", "RAW-LITHIUM");
        assertThat(lithium.getInput()).isEqualByComparingTo("9.0000");
    }

    @Test
    @DisplayName("TRANS：weight=5kg/kWh × 75kWh = 375 kg = 0.375 t；t·km=0.375×1000=375")
    void transTkmFormula() {
        when(factorMapper.selectList(any())).thenReturn(allFactors());

        List<CarbonFootprintVO.StageDetail> stages =
            service.calcAllStages(new BigDecimal("75"), "LFP");

        CarbonFootprintVO.LineItem truck =
            breakdownItem(stages, "TRANS", "TRANS-TRUCK-DIESEL");
        assertThat(truck.getInput()).isEqualByComparingTo("375.0000");
        assertThat(truck.getInputUnit()).isEqualTo("t·km");
        // 375 × 0.062 = 23.25
        assertThat(truck.getCo2Kg().doubleValue())
            .isCloseTo(23.25, within(0.01));
    }

    @Test
    @DisplayName("USE：充电总量 = 75 kWh × 2000 cycle × 0.8 DOD = 120000 kWh")
    void useTotalChargeFormula() {
        when(factorMapper.selectList(any())).thenReturn(allFactors());

        List<CarbonFootprintVO.StageDetail> stages =
            service.calcAllStages(new BigDecimal("75"), "LFP");

        CarbonFootprintVO.LineItem charge =
            breakdownItem(stages, "USE", "USE-CHARGE-CN-GRID");
        assertThat(charge.getInput()).isEqualByComparingTo("120000.00");
        assertThat(charge.getInputUnit()).isEqualTo("kWh");
        // 120000 × 0.581 = 69720
        assertThat(charge.getCo2Kg().doubleValue())
            .isCloseTo(69720.0, within(0.5));
    }

    @Test
    @DisplayName("EOL：电池总重 = 5 kg/kWh × 75 = 375 kg；回收抵扣为负")
    void eolWeightFormulaIsNegative() {
        when(factorMapper.selectList(any())).thenReturn(allFactors());

        List<CarbonFootprintVO.StageDetail> stages =
            service.calcAllStages(new BigDecimal("75"), "LFP");

        CarbonFootprintVO.LineItem eol =
            breakdownItem(stages, "EOL", "EOL-RECYCLE-CREDIT");
        assertThat(eol.getInput()).isEqualByComparingTo("375.00");
        // factorValue = -0.5（回收抵扣），co2 = 375 × -0.5 = -187.5
        assertThat(eol.getCo2Kg().signum()).isNegative();
        assertThat(eol.getCo2Kg().doubleValue())
            .isCloseTo(-187.5, within(0.01));
    }

    @Test
    @DisplayName("Stage co2 = breakdown 各 LineItem co2 之和")
    void stageCo2EqualsSumOfBreakdown() {
        when(factorMapper.selectList(any())).thenReturn(allFactors());

        List<CarbonFootprintVO.StageDetail> stages =
            service.calcAllStages(new BigDecimal("75"), "LFP");

        for (CarbonFootprintVO.StageDetail s : stages) {
            BigDecimal expected = s.getBreakdown().stream()
                .map(CarbonFootprintVO.LineItem::getCo2Kg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            // 允许 setScale 误差 0.01
            assertThat(s.getCo2Kg().doubleValue())
                .as("stage=%s", s.getStage())
                .isCloseTo(expected.doubleValue(), within(0.01));
        }
    }

    // ============ helpers ============

    /** 全因子表：覆盖 RAW/MFG/TRANS/USE/EOL 所有因子 code */
    private List<NevEmissionFactorDO> allFactors() {
        List<NevEmissionFactorDO> list = new ArrayList<>();
        // RAW kg CO2/kg
        list.add(factor("RAW-LITHIUM",   "锂", "kgCO2e/kg",  new BigDecimal("14.30")));
        list.add(factor("RAW-COBALT",    "钴", "kgCO2e/kg",  new BigDecimal("8.50")));
        list.add(factor("RAW-NICKEL",    "镍", "kgCO2e/kg",  new BigDecimal("6.50")));
        list.add(factor("RAW-COPPER",    "铜", "kgCO2e/kg",  new BigDecimal("3.83")));
        list.add(factor("RAW-ALUMINUM",  "铝", "kgCO2e/kg",  new BigDecimal("11.46")));
        // MFG kg CO2/kWh
        list.add(factor("MFG-CELL-ASSEMBLY",   "电芯组装", "kgCO2e/kWh", new BigDecimal("25.00")));
        list.add(factor("MFG-MODULE-ASSEMBLY", "模组组装", "kgCO2e/kWh", new BigDecimal("8.00")));
        list.add(factor("MFG-PACK-ASSEMBLY",   "PACK 组装", "kgCO2e/kWh", new BigDecimal("5.00")));
        // TRANS kg CO2/t·km
        list.add(factor("TRANS-TRUCK-DIESEL", "公路重卡柴油", "kgCO2e/t·km", new BigDecimal("0.062")));
        // USE kg CO2/kWh
        list.add(factor("USE-CHARGE-CN-GRID", "中国电网充电", "kgCO2e/kWh", new BigDecimal("0.581")));
        // EOL kg CO2/kg（负值=抵扣）
        list.add(factor("EOL-RECYCLE-CREDIT", "拆解回收抵扣", "kgCO2e/kg",   new BigDecimal("-0.50")));
        return list;
    }

    private NevEmissionFactorDO factor(String code, String name, String unit, BigDecimal value) {
        NevEmissionFactorDO f = new NevEmissionFactorDO();
        f.setFactorCode(code);
        f.setFactorName(name);
        f.setUnit(unit);
        f.setValue(value);
        return f;
    }

    private List<String> breakdownCodes(List<CarbonFootprintVO.StageDetail> stages, String stage) {
        return stageOf(stages, stage).getBreakdown().stream()
            .map(CarbonFootprintVO.LineItem::getFactorCode)
            .collect(Collectors.toList());
    }

    private CarbonFootprintVO.LineItem breakdownItem(
        List<CarbonFootprintVO.StageDetail> stages, String stage, String code
    ) {
        return stageOf(stages, stage).getBreakdown().stream()
            .filter(li -> code.equals(li.getFactorCode()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("breakdown item not found: " + stage + "/" + code));
    }

    private CarbonFootprintVO.StageDetail stageOf(
        List<CarbonFootprintVO.StageDetail> stages, String stage
    ) {
        return stages.stream()
            .filter(s -> stage.equals(s.getStage()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("stage not found: " + stage));
    }
}
