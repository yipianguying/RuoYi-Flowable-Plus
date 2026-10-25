package com.ruoyi.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.workflow.domain.BizPurchaseOrder;
import com.ruoyi.workflow.domain.bo.BizPurchaseOrderBo;
import com.ruoyi.workflow.domain.vo.BizPurchaseOrderVo;
import com.ruoyi.workflow.mapper.BizPurchaseOrderMapper;
import com.ruoyi.workflow.service.IBizPurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 业务_采购申请主Service业务层处理
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@RequiredArgsConstructor
@Service
public class BizPurchaseOrderServiceImpl implements IBizPurchaseOrderService {

    private final BizPurchaseOrderMapper baseMapper;

    /**
     * 查询业务_采购申请主
     */
    @Override
    public BizPurchaseOrderVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询业务_采购申请主列表
     */
    @Override
    public TableDataInfo<BizPurchaseOrderVo> queryPageList(BizPurchaseOrderBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<BizPurchaseOrder> lqw = buildQueryWrapper(bo);
        Page<BizPurchaseOrderVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询业务_采购申请主列表
     */
    @Override
    public List<BizPurchaseOrderVo> queryList(BizPurchaseOrderBo bo) {
        LambdaQueryWrapper<BizPurchaseOrder> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<BizPurchaseOrder> buildQueryWrapper(BizPurchaseOrderBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<BizPurchaseOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getOrderNo()), BizPurchaseOrder::getOrderNo, bo.getOrderNo());
        lqw.eq(bo.getSupplierId() != null, BizPurchaseOrder::getSupplierId, bo.getSupplierId());
        lqw.eq(bo.getTotalAmount() != null, BizPurchaseOrder::getTotalAmount, bo.getTotalAmount());
        lqw.eq(bo.getStatus() != null, BizPurchaseOrder::getStatus, bo.getStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getProcessInstanceId()), BizPurchaseOrder::getProcessInstanceId, bo.getProcessInstanceId());
        return lqw;
    }

    /**
     * 新增业务_采购申请主
     */
    @Override
    public Boolean insertByBo(BizPurchaseOrderBo bo) {
        BizPurchaseOrder add = BeanUtil.toBean(bo, BizPurchaseOrder.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改业务_采购申请主
     */
    @Override
    public Boolean updateByBo(BizPurchaseOrderBo bo) {
        BizPurchaseOrder update = BeanUtil.toBean(bo, BizPurchaseOrder.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(BizPurchaseOrder entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除业务_采购申请主
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
