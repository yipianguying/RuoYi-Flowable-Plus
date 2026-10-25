package com.ruoyi.workflow.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.workflow.domain.BizSupplier;
import com.ruoyi.workflow.domain.vo.BizSupplierVo;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 业务_供应商信息Mapper接口
 *
 * @author ruoyi
 * @date 2026-07-23
 */
public interface BizSupplierMapper extends BaseMapperPlus<BizSupplierMapper, BizSupplier, BizSupplierVo> {

    @Select("select * from biz_supplier")
    List<BizSupplier> selectAll();
}
