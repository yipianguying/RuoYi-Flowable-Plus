package com.ruoyi.workflow.service;


import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.workflow.domain.bo.BizContractBo;
import com.ruoyi.workflow.domain.dto.BizContractDto;
import com.ruoyi.workflow.domain.vo.BizContractVo;

import java.util.Collection;
import java.util.List;

/**
 * 业务_合同风控审批Service接口
 *
 * @author ruoyi
 * @date 2026-07-23
 */
public interface IBizContractService {

    void startContractProcess(BizContractDto dto);

    /**
     * 查询业务_合同风控审批
     */
    BizContractVo queryById(Long id);

    /**
     * 查询业务_合同风控审批列表
     */
    TableDataInfo<BizContractVo> queryPageList(BizContractBo bo, PageQuery pageQuery);

    /**
     * 查询业务_合同风控审批列表
     */
    List<BizContractVo> queryList(BizContractBo bo);

    /**
     * 新增业务_合同风控审批
     */
    Boolean insertByBo(BizContractBo bo);

    /**
     * 修改业务_合同风控审批
     */
    Boolean updateByBo(BizContractBo bo);

    /**
     * 校验并批量删除业务_合同风控审批信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
