package com.nev.marketplace.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nev.battery.mapper.NevBatteryLifecycleMapper;
import com.nev.battery.mapper.NevBatteryMapper;
import com.nev.carbon.domain.NevCarbonCreditRecordDO;
import com.nev.carbon.mapper.NevCarbonCreditRecordMapper;
import com.nev.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 公开仪表盘统计 API（无需登录，对应 security.excludes 中的 /public/**）
 *
 * 用途：admin-web 仪表盘 4 张统计卡（商品总数 / 电池注册 / 碳积分发放 / 链上事件）。
 * 商品总数走 /public/product/list 的 total，其余三项由本接口提供：
 *
 * <ul>
 *   <li>batteryCount    —— nev_battery 总数（电池注册量）</li>
 *   <li>carbonIssued    —— nev_carbon_credit_record 正向变动求和（累计发放 kgCO2eq）</li>
 *   <li>chainEventCount —— nev_battery_lifecycle 总数（链上双写事件，含 PRODUCED）</li>
 * </ul>
 *
 * 说明：跨模块统计放在 marketplace（唯一同时依赖 nev-battery 与 nev-carbon 的业务模块），
 * 只读 count/sum，不写任何表。
 *
 * @author NEV-v2
 */
@Slf4j
@RestController
@RequestMapping("/public/dashboard")
@RequiredArgsConstructor
public class PublicDashboardController {

    private final NevBatteryMapper batteryMapper;
    private final NevBatteryLifecycleMapper lifecycleMapper;
    private final NevCarbonCreditRecordMapper creditRecordMapper;

    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        Long batteryCount = batteryMapper.selectCount(null);
        Long chainEventCount = lifecycleMapper.selectCount(null);

        QueryWrapper<NevCarbonCreditRecordDO> qw = new QueryWrapper<>();
        qw.select("IFNULL(SUM(change_amount), 0) AS total_issued").gt("change_amount", BigDecimal.ZERO);
        Object sum = creditRecordMapper.selectObjs(qw).stream().findFirst().orElse(null);
        BigDecimal issued = (sum == null) ? BigDecimal.ZERO : new BigDecimal(sum.toString());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("batteryCount", batteryCount);
        data.put("carbonIssued", issued.stripTrailingZeros().toPlainString());
        data.put("chainEventCount", chainEventCount);
        return R.ok(data);
    }
}
