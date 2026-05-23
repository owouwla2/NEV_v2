package com.nev.marketplace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nev.marketplace.domain.NevCartItemDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;

@Mapper
public interface NevCartItemMapper extends BaseMapper<NevCartItemDO> {

    /**
     * 物理删除（绕过 @TableLogic）—— 订单结算后清理 cart_item 必须物理删除，
     * 否则 (cart_id, product_id) 唯一索引会阻止用户再次添加同商品
     */
    @Delete("<script>DELETE FROM nev_cart_item WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    int physicalDeleteByIds(@Param("ids") Collection<Long> ids);
}
