package com.nev.carbon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nev.battery.domain.NevBatteryDO;
import com.nev.battery.mapper.NevBatteryMapper;
import com.nev.carbon.domain.NevCarbonCreditAccountDO;
import com.nev.carbon.domain.NevCarbonCreditRecordDO;
import com.nev.carbon.domain.NevCarbonFootprintDO;
import com.nev.carbon.domain.NevCarbonStageDO;
import com.nev.carbon.mapper.NevCarbonCreditAccountMapper;
import com.nev.carbon.mapper.NevCarbonCreditRecordMapper;
import com.nev.carbon.mapper.NevCarbonFootprintMapper;
import com.nev.carbon.mapper.NevCarbonStageMapper;
import com.nev.common.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

/**
 * 碳积分服务
 *
 * 设计：每个用户一个 nev_carbon_credit_account（懒创建）+
 *      每次变动一条 nev_carbon_credit_record（流水）
 *
 * 积分单位：kgCO2eq（与排放因子一致）
 *
 * @author NEV-v2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CarbonCreditService {

    private final NevCarbonCreditAccountMapper accountMapper;
    private final NevCarbonCreditRecordMapper recordMapper;
    private final NevCarbonFootprintMapper footprintMapper;
    private final NevCarbonStageMapper stageMapper;
    private final NevBatteryMapper batteryMapper;

    /**
     * 商城订单完成时给 consumer 加碳积分
     *
     * 积分 = 该电池的 EOL 阶段减排量绝对值（即"通过回收避免的等量原材料生产排放"）
     * 设计动机：消费者购买 → 后续可能进入回收 → 相当于在产业链中"激活"了减排潜力
     *
     * @param userId 消费者用户ID
     * @param batteryId 关联电池ID
     * @param orderId 关联订单ID
     * @return 本次发放的积分（>0=增加；0=该电池未算碳足迹，跳过）
     */
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal awardFromOrder(Long userId, Long batteryId, Long orderId) {
        if (userId == null || batteryId == null) {
            throw new ServiceException("awardFromOrder 参数不全");
        }

        // 找电池碳足迹的 EOL 阶段，没有则返回 0
        LambdaQueryWrapper<NevCarbonFootprintDO> fq = new LambdaQueryWrapper<>();
        fq.eq(NevCarbonFootprintDO::getBatteryId, batteryId).last("limit 1");
        NevCarbonFootprintDO fp = footprintMapper.selectOne(fq);
        if (fp == null) {
            log.info("[carbon] battery_id={} 还没算碳足迹，跳过积分发放（admin 可调 /admin/carbon/calc 补算）", batteryId);
            return BigDecimal.ZERO;
        }
        LambdaQueryWrapper<NevCarbonStageDO> sq = new LambdaQueryWrapper<>();
        sq.eq(NevCarbonStageDO::getFootprintId, fp.getId())
            .eq(NevCarbonStageDO::getStage, "EOL").last("limit 1");
        NevCarbonStageDO eol = stageMapper.selectOne(sq);
        if (eol == null || eol.getCo2Kg() == null) {
            log.warn("[carbon] battery_id={} 碳足迹无 EOL 阶段数据，跳过", batteryId);
            return BigDecimal.ZERO;
        }
        // EOL 是负值（减排），积分取绝对值
        BigDecimal credit = eol.getCo2Kg().abs().setScale(4, RoundingMode.HALF_UP);
        if (credit.signum() == 0) {
            return BigDecimal.ZERO;
        }

        NevBatteryDO battery = batteryMapper.selectById(batteryId);
        String traceNumber = battery == null ? "unknown" : battery.getTraceNumber();

        // 懒创建账户
        NevCarbonCreditAccountDO account = ensureAccount(userId);

        // 累加余额 / 累计获得
        account.setBalance(safe(account.getBalance()).add(credit));
        account.setTotalEarned(safe(account.getTotalEarned()).add(credit));
        accountMapper.updateById(account);

        // 写流水
        NevCarbonCreditRecordDO rec = new NevCarbonCreditRecordDO();
        rec.setUserId(userId);
        rec.setChangeAmount(credit);
        rec.setBalanceAfter(account.getBalance());
        rec.setReason("ORDER_COMPLETE");
        rec.setRelatedId(orderId);
        rec.setRelatedType("ORDER");
        rec.setRemark("完成订单 #" + orderId + "，电池 " + traceNumber + " 的回收抵扣减排");
        rec.setTenantId("000000");
        rec.setDelFlag("0");
        recordMapper.insert(rec);

        log.info("[carbon] user={} 因订单 #{} 完成 获得 {} kgCO2eq 碳积分（电池 {}）",
            userId, orderId, credit, traceNumber);
        return credit;
    }

    public NevCarbonCreditAccountDO findOrCreateAccount(Long userId) {
        return ensureAccount(userId);
    }

    private NevCarbonCreditAccountDO ensureAccount(Long userId) {
        LambdaQueryWrapper<NevCarbonCreditAccountDO> q = new LambdaQueryWrapper<>();
        q.eq(NevCarbonCreditAccountDO::getUserId, userId).last("limit 1");
        NevCarbonCreditAccountDO acc = accountMapper.selectOne(q);
        if (acc != null) return acc;
        acc = new NevCarbonCreditAccountDO();
        acc.setUserId(userId);
        acc.setBalance(BigDecimal.ZERO);
        acc.setFrozen(BigDecimal.ZERO);
        acc.setTotalEarned(BigDecimal.ZERO);
        acc.setTotalSpent(BigDecimal.ZERO);
        acc.setTenantId("000000");
        acc.setDelFlag("0");
        accountMapper.insert(acc);
        // 重新查一次以获得自动填充的字段（create_time 等）
        return accountMapper.selectById(acc.getId());
    }

    private BigDecimal safe(BigDecimal x) {
        return x == null ? BigDecimal.ZERO : x;
    }
}
