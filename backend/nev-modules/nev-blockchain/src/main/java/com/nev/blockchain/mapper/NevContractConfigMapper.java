package com.nev.blockchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nev.blockchain.domain.NevContractConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 合约配置表 Mapper
 *
 * @author NEV-v2
 */
@Mapper
public interface NevContractConfigMapper extends BaseMapper<NevContractConfigDO> {
}
