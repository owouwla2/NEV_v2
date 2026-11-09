package com.nev.blockchain.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nev.blockchain.domain.NevContractConfigDO;
import com.nev.blockchain.mapper.NevContractConfigMapper;
import com.nev.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 公开链上配置查询（无需登录，对应 security.excludes 中的 /public/**）
 *
 * 用途：admin-web「区块链 → 合约配置」页面。只读展示合约登记信息；
 * ABI 全文不外发（链上浏览器可见，这里只给长度）。
 *
 * @author NEV-v2
 */
@RestController
@RequestMapping("/public/chain")
@RequiredArgsConstructor
public class PublicChainController {

    private final NevContractConfigMapper contractConfigMapper;

    /** 已启用的合约登记列表（LifecycleTrace 等） */
    @GetMapping("/config")
    public R<List<Map<String, Object>>> config() {
        LambdaQueryWrapper<NevContractConfigDO> q = new LambdaQueryWrapper<>();
        q.eq(NevContractConfigDO::getEnabled, "1").orderByAsc(NevContractConfigDO::getId);
        List<Map<String, Object>> list = contractConfigMapper.selectList(q).stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("contractName", c.getContractName());
            m.put("contractAddress", c.getContractAddress());
            m.put("network", c.getNetwork());
            m.put("groupId", c.getGroupId());
            m.put("deployBlock", c.getDeployBlock());
            m.put("deployedAt", c.getDeployedAt());
            m.put("abiLength", c.getAbi() == null ? 0 : c.getAbi().length());
            return m;
        }).toList();
        return R.ok(list);
    }
}
