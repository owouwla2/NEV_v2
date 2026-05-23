package com.nev.carbon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nev.battery.api.CarbonScanEnricher;
import com.nev.battery.api.CarbonSummary;
import com.nev.carbon.domain.NevCarbonFootprintDO;
import com.nev.carbon.domain.NevCarbonStageDO;
import com.nev.carbon.mapper.NevCarbonFootprintMapper;
import com.nev.carbon.mapper.NevCarbonStageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * BatteryScanService 的碳足迹扩展点实现
 *
 * 让 nev-battery 模块的公开扫码接口能在不直接依赖 nev-carbon 的前提下，
 * 通过 ObjectProvider 注入获取碳足迹摘要
 *
 * @author NEV-v2
 */
@Component
@RequiredArgsConstructor
public class CarbonScanEnricherImpl implements CarbonScanEnricher {

    private final NevCarbonFootprintMapper footprintMapper;
    private final NevCarbonStageMapper stageMapper;

    @Override
    public Optional<CarbonSummary> getSummary(Long batteryId) {
        if (batteryId == null) return Optional.empty();
        LambdaQueryWrapper<NevCarbonFootprintDO> fq = new LambdaQueryWrapper<>();
        fq.eq(NevCarbonFootprintDO::getBatteryId, batteryId).last("limit 1");
        NevCarbonFootprintDO fp = footprintMapper.selectOne(fq);
        if (fp == null) return Optional.empty();

        LambdaQueryWrapper<NevCarbonStageDO> sq = new LambdaQueryWrapper<>();
        sq.eq(NevCarbonStageDO::getFootprintId, fp.getId());
        List<NevCarbonStageDO> stageRows = stageMapper.selectList(sq);
        stageRows.sort((a, b) -> orderOf(a.getStage()) - orderOf(b.getStage()));

        List<CarbonSummary.StageBrief> stages = stageRows.stream().map(s ->
            CarbonSummary.StageBrief.builder().stage(s.getStage()).co2Kg(s.getCo2Kg()).build()
        ).toList();

        return Optional.of(CarbonSummary.builder()
            .totalCo2Kg(fp.getTotalCo2Kg())
            .calcMethod(fp.getCalcMethod())
            .calcVersion(fp.getCalcVersion())
            .calcTime(fp.getCalcTime())
            .stages(stages)
            .build());
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
