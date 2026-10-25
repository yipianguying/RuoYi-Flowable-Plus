package com.ruoyi.workflow.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.validation.constraints.*;

import java.util.Date;

import java.math.BigDecimal;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 业务_合同风控审批业务对象 biz_contract
 *
 * @author ruoyi
 * @date 2026-07-23
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class BizContractBo extends BaseEntity {

    /**
     * 合同ID
     */
    @NotNull(message = "合同ID不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 合同编号
     */
    @NotBlank(message = "合同编号不能为空", groups = { AddGroup.class, EditGroup.class })
    private String contractNo;

    /**
     * 合同名称
     */
    @NotBlank(message = "合同名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String contractName;

    /**
     * 关联供应商ID
     */
    @NotNull(message = "关联供应商ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long supplierId;

    /**
     * 合同总金额
     */
    @NotNull(message = "合同总金额不能为空", groups = { AddGroup.class, EditGroup.class })
    private BigDecimal contractAmount;

    /**
     * 合同附件OSS下载地址 (PDF/Word)
     */
    @NotBlank(message = "合同附件OSS下载地址 (PDF/Word)不能为空", groups = { AddGroup.class, EditGroup.class })
    private String fileUrl;

    /**
     * AI大模型提取的风险条款摘要 (JSON或纯文本)
     */
    @NotBlank(message = "AI大模型提取的风险条款摘要 (JSON或纯文本)不能为空", groups = { AddGroup.class, EditGroup.class })
    private String aiRiskSummary;

    /**
     * 状态：0-草稿 1-法务财务会签中 2-已生效 3-驳回
     */
    @NotNull(message = "状态：0-草稿 1-法务财务会签中 2-已生效 3-驳回不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long status;

    /**
     * Flowable工作流实例ID
     */
    @NotBlank(message = "Flowable工作流实例ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private String processInstanceId;


}
