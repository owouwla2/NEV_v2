package com.nev.carbon.controller;

 import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
 import com.nev.carbon.domain.NevEmissionFactorDO;
 import com.nev.carbon.dto.CarbonFootprintVO;
 import com.nev.carbon.mapper.NevEmissionFactorMapper;
import com.nev.carbon.service.CarbonFootprintService;
import com.nev.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 import java.util.List;


/**
 * 公开查询电池碳足迹（消费者扫码用）
 *
 * 路径 /public/** 已在 application.yml security.excludes 配置免鉴权
 *
 * @author NEV-v2
 */
@RestController
@RequestMapping("/public/carbon")
@RequiredArgsConstructor
public class PublicCarbonController {

    private final CarbonFootprintService footprintService;
    private final NevEmissionFactorMapper emissionFactorMapper;

    /** 查询某电池的碳足迹（含 5 阶段 breakdown） */
    @GetMapping("/{traceNumber}")
    public R<CarbonFootprintVO> get(@PathVariable String traceNumber) {
        return R.ok(footprintService.getByTraceNumber(traceNumber));
    }

    /** 排放因子库（公开只读：GB-T 24067 五阶段因子，RAW/MFG/TRANS/USE/EOL） */
    @GetMapping("/factors")
    public R<List<NevEmissionFactorDO>> factors() {
        LambdaQueryWrapper<NevEmissionFactorDO> q = new LambdaQueryWrapper<>();
        q.orderByAsc(NevEmissionFactorDO::getId);
        return R.ok(emissionFactorMapper.selectList(q));
    }
}
