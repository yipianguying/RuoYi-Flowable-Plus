package com.ruoyi.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.workflow.domain.BizSupplier;
import com.ruoyi.workflow.domain.bo.BizSupplierBo;
import com.ruoyi.workflow.domain.vo.BizSupplierVo;
import com.ruoyi.workflow.mapper.BizSupplierMapper;
import com.ruoyi.workflow.service.IBizSupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 业务_供应商信息Service业务层处理
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@RequiredArgsConstructor
@Service
public class BizSupplierServiceImpl implements IBizSupplierService {

    private final BizSupplierMapper baseMapper;

    /**
     * 查询业务_供应商信息
     */
    @Override
    public BizSupplierVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询业务_供应商信息列表
     */
    @Override
    public TableDataInfo<BizSupplierVo> queryPageList(BizSupplierBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<BizSupplier> lqw = buildQueryWrapper(bo);
        Page<BizSupplierVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询业务_供应商信息列表
     */
    @Override
    public List<BizSupplierVo> queryList(BizSupplierBo bo) {
        LambdaQueryWrapper<BizSupplier> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<BizSupplier> buildQueryWrapper(BizSupplierBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<BizSupplier> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getSupplierName()), BizSupplier::getSupplierName, bo.getSupplierName());
        lqw.eq(StringUtils.isNotBlank(bo.getContactPerson()), BizSupplier::getContactPerson, bo.getContactPerson());
        lqw.eq(StringUtils.isNotBlank(bo.getContactPhone()), BizSupplier::getContactPhone, bo.getContactPhone());
        lqw.eq(StringUtils.isNotBlank(bo.getBankAccount()), BizSupplier::getBankAccount, bo.getBankAccount());
        lqw.eq(bo.getIsActive() != null, BizSupplier::getIsActive, bo.getIsActive());
        return lqw;
    }

    /**
     * 新增业务_供应商信息
     */
    @Override
    public Boolean insertByBo(BizSupplierBo bo) {
        BizSupplier add = BeanUtil.toBean(bo, BizSupplier.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改业务_供应商信息
     */
    @Override
    public Boolean updateByBo(BizSupplierBo bo) {
        BizSupplier update = BeanUtil.toBean(bo, BizSupplier.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(BizSupplier entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除业务_供应商信息
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
