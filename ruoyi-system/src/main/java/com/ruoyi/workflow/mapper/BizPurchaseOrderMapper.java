package com.ruoyi.workflow.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.workflow.domain.BizPurchaseOrder;
import com.ruoyi.workflow.domain.vo.BizPurchaseOrderVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 业务_采购申请主Mapper接口
 *
 * @author ruoyi
 * @date 2026-07-23
 */
public interface BizPurchaseOrderMapper extends BaseMapperPlus<BizPurchaseOrderMapper, BizPurchaseOrder, BizPurchaseOrderVo> {

    int countByYearMonth(@Param("yearMonth") String yearMonth);

    List<BizPurchaseOrder> selectPageByYearMonth(@Param("yearMonth") String yearMonth,
                                                 @Param("offset") int offset,
                                                 @Param("pageSize") int pageSize);
}
