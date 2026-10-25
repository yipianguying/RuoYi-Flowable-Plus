package com.ruoyi.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.workflow.domain.BizContract;
import com.ruoyi.workflow.domain.bo.BizContractBo;
import com.ruoyi.workflow.domain.dto.BizContractDto;
import com.ruoyi.workflow.domain.vo.BizContractVo;
import com.ruoyi.workflow.mapper.BizContractMapper;
import com.ruoyi.workflow.service.AiRiskAnalysisService;
import com.ruoyi.workflow.service.IBizContractService;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 业务_合同风控审批Service业务层处理
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@RequiredArgsConstructor
@Service
public class BizContractServiceImpl implements IBizContractService {

    @Resource
    private BizContractMapper contractMapper;

    @Resource
    private RuntimeService runtimeService; // Flowable 核心服务

    @Resource
    private AiRiskAnalysisService aiRiskAnalysisService; // 你的异步AI服务

    /**
     * 发起合同风控审批流
     */
    @Transactional(rollbackFor = Exception.class)
    public void startContractProcess(BizContractDto dto) {
        // 1. 保存业务数据到 biz_contract 表 (状态设为审批中)
        BizContract contract = new BizContract();
        contract.setContractName(dto.getContractName());
        contract.setFileUrl(dto.getFileUrl());
        contract.setContractAmount(dto.getContractAmount());
        contract.setStatus(1L); // 1-审批中
        contractMapper.insert(contract);

        // 2. 组装 Flowable 流程变量 (传入网关和表单需要的基础数据)
        Map<String, Object> variables = new HashMap<>();
        variables.put("contractAmount", contract.getContractAmount());
        // 【关键防呆设计】：先给 AI 摘要塞一个默认值，防止前端渲染报错或空白
        variables.put("aiRiskSummary", "【系统正在异步调用 AI 大模型解析合同附件，请稍后...】");

        // 3. 生成 BusinessKey (业务流与工作流解耦的桥梁)
        String businessKey = "CONTRACT:" + contract.getId();

        // 4. 启动 Flowable 流程实例
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
            "contract_risk_process", businessKey, variables);

        // 5. 将生成的流程实例 ID 反写回业务表
        contract.setProcessInstanceId(instance.getId());
        contractMapper.updateById(contract);

        // 6. 【核心高光】：触发异步线程去调用大模型解析文件，绝不阻塞当前 HTTP 请求！
        // 传入流程实例ID，方便 AI 解析完成后反向注入变量
        aiRiskAnalysisService.asyncAnalyzeContract(contract.getId(), dto.getFileUrl(), instance.getId());

        // 方法结束，事务提交，前端瞬间收到“发起成功”的提示。
    }

    /**
     * 查询业务_合同风控审批
     */
    @Override
    public BizContractVo queryById(Long id){
        return contractMapper.selectVoById(id);
    }

    /**
     * 查询业务_合同风控审批列表
     */
    @Override
    public TableDataInfo<BizContractVo> queryPageList(BizContractBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<BizContract> lqw = buildQueryWrapper(bo);
        Page<BizContractVo> result = contractMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询业务_合同风控审批列表
     */
    @Override
    public List<BizContractVo> queryList(BizContractBo bo) {
        LambdaQueryWrapper<BizContract> lqw = buildQueryWrapper(bo);
        return contractMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<BizContract> buildQueryWrapper(BizContractBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<BizContract> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getContractNo()), BizContract::getContractNo, bo.getContractNo());
        lqw.like(StringUtils.isNotBlank(bo.getContractName()), BizContract::getContractName, bo.getContractName());
        lqw.eq(bo.getSupplierId() != null, BizContract::getSupplierId, bo.getSupplierId());
        lqw.eq(bo.getContractAmount() != null, BizContract::getContractAmount, bo.getContractAmount());
        lqw.eq(StringUtils.isNotBlank(bo.getFileUrl()), BizContract::getFileUrl, bo.getFileUrl());
        lqw.eq(StringUtils.isNotBlank(bo.getAiRiskSummary()), BizContract::getAiRiskSummary, bo.getAiRiskSummary());
        lqw.eq(bo.getStatus() != null, BizContract::getStatus, bo.getStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getProcessInstanceId()), BizContract::getProcessInstanceId, bo.getProcessInstanceId());
        return lqw;
    }

    /**
     * 新增业务_合同风控审批
     */
    @Override
    public Boolean insertByBo(BizContractBo bo) {
        BizContract add = BeanUtil.toBean(bo, BizContract.class);
        validEntityBeforeSave(add);
        boolean flag = contractMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改业务_合同风控审批
     */
    @Override
    public Boolean updateByBo(BizContractBo bo) {
        BizContract update = BeanUtil.toBean(bo, BizContract.class);
        validEntityBeforeSave(update);
        return contractMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(BizContract entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除业务_合同风控审批
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return contractMapper.deleteBatchIds(ids) > 0;
    }
}
