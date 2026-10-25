package com.ruoyi.workflow.service;


import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.workflow.domain.bo.BizSupplierBo;
import com.ruoyi.workflow.domain.vo.BizSupplierVo;

import java.util.Collection;
import java.util.List;

/**
 * 业务_供应商信息Service接口
 *
 * @author ruoyi
 * @date 2026-07-23
 */
public interface IBizSupplierService {

    /**
     * 查询业务_供应商信息
     */
    BizSupplierVo queryById(Long id);

    /**
     * 查询业务_供应商信息列表
     */
    TableDataInfo<BizSupplierVo> queryPageList(BizSupplierBo bo, PageQuery pageQuery);

    /**
     * 查询业务_供应商信息列表
     */
    List<BizSupplierVo> queryList(BizSupplierBo bo);

    /**
     * 新增业务_供应商信息
     */
    Boolean insertByBo(BizSupplierBo bo);

    /**
     * 修改业务_供应商信息
     */
    Boolean updateByBo(BizSupplierBo bo);

    /**
     * 校验并批量删除业务_供应商信息信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
