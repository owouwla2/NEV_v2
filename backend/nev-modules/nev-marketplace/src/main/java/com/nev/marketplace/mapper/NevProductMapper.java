package com.nev.marketplace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nev.marketplace.domain.NevProductDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NevProductMapper extends BaseMapper<NevProductDO> {
}
