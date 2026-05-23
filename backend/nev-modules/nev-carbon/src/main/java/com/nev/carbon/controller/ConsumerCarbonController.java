package com.nev.carbon.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nev.carbon.domain.NevCarbonCreditAccountDO;
import com.nev.carbon.domain.NevCarbonCreditRecordDO;
import com.nev.carbon.dto.CarbonAccountVO;
import com.nev.carbon.mapper.NevCarbonCreditRecordMapper;
import com.nev.carbon.service.CarbonCreditService;
import com.nev.common.core.domain.R;
import com.nev.common.core.exception.ServiceException;
import com.nev.common.satoken.utils.LoginHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * consumer 碳积分账户查询
 *
 * @author NEV-v2
 */
@RestController
@RequestMapping("/consumer/carbon")
@RequiredArgsConstructor
@SaCheckRole("consumer")
public class ConsumerCarbonController {

    private final CarbonCreditService creditService;
    private final NevCarbonCreditRecordMapper recordMapper;

    /** 查我的碳积分账户 + 最近 20 条流水 */
    @GetMapping("/account")
    public R<CarbonAccountVO> myAccount() {
        Long uid = LoginHelper.getUserId();
        if (uid == null) throw new ServiceException("未登录");
        NevCarbonCreditAccountDO acc = creditService.findOrCreateAccount(uid);

        LambdaQueryWrapper<NevCarbonCreditRecordDO> rq = new LambdaQueryWrapper<>();
        rq.eq(NevCarbonCreditRecordDO::getUserId, uid)
            .orderByDesc(NevCarbonCreditRecordDO::getId)
            .last("limit 20");
        List<NevCarbonCreditRecordDO> rows = recordMapper.selectList(rq);

        List<CarbonAccountVO.RecordItem> items = rows.stream().map(r ->
            CarbonAccountVO.RecordItem.builder()
                .id(r.getId())
                .changeAmount(r.getChangeAmount())
                .balanceAfter(r.getBalanceAfter())
                .reason(r.getReason())
                .relatedId(r.getRelatedId())
                .relatedType(r.getRelatedType())
                .remark(r.getRemark())
                .createTime(r.getCreateTime())
                .build()
        ).toList();

        return R.ok(CarbonAccountVO.builder()
            .userId(acc.getUserId())
            .balance(acc.getBalance())
            .frozen(acc.getFrozen())
            .totalEarned(acc.getTotalEarned())
            .totalSpent(acc.getTotalSpent())
            .recentRecords(items)
            .build());
    }
}
