package com.ruoyi.workflow.service;


import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.workflow.domain.bo.BizPurchaseOrderBo;
import com.ruoyi.workflow.domain.vo.BizPurchaseOrderVo;

import java.util.Collection;
import java.util.List;

/**
 * 业务_采购申请主Service接口
 *
 * @author ruoyi
 * @date 2026-07-23
 */
public interface IBizPurchaseOrderService {

    /**
     * 查询业务_采购申请主
     */
    BizPurchaseOrderVo queryById(Long id);

    /**
     * 查询业务_采购申请主列表
     */
    TableDataInfo<BizPurchaseOrderVo> queryPageList(BizPurchaseOrderBo bo, PageQuery pageQuery);

    /**
     * 查询业务_采购申请主列表
     */
    List<BizPurchaseOrderVo> queryList(BizPurchaseOrderBo bo);

    /**
     * 新增业务_采购申请主
     */
    Boolean insertByBo(BizPurchaseOrderBo bo);

    /**
     * 修改业务_采购申请主
     */
    Boolean updateByBo(BizPurchaseOrderBo bo);

    /**
     * 校验并批量删除业务_采购申请主信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
