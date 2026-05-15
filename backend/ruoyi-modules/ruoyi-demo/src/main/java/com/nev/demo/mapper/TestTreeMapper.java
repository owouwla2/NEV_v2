package com.nev.demo.mapper;

import com.nev.common.mybatis.annotation.DataColumn;
import com.nev.common.mybatis.annotation.DataPermission;
import com.nev.common.mybatis.core.mapper.BaseMapperPlus;
import com.nev.demo.domain.TestTree;
import com.nev.demo.domain.vo.TestTreeVo;

/**
 * 测试树表Mapper接口
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@DataPermission({
    @DataColumn(key = "deptName", value = "dept_id"),
    @DataColumn(key = "userName", value = "user_id")
})
public interface TestTreeMapper extends BaseMapperPlus<TestTree, TestTreeVo> {

}
