package com.ruoyi.workflow.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.util.Date;
import java.math.BigDecimal;

import java.math.BigDecimal;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 业务_合同风控审批对象 biz_contract
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_contract")
public class BizContract extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 合同ID
     */
    @TableId(value = "id")
    private Long id;
    /**
     * 合同编号
     */
    private String contractNo;
    /**
     * 合同名称
     */
    private String contractName;
    /**
     * 关联供应商ID
     */
    private Long supplierId;
    /**
     * 合同总金额
     */
    private BigDecimal contractAmount;
    /**
     * 合同附件OSS下载地址 (PDF/Word)
     */
    private String fileUrl;
    /**
     * AI大模型提取的风险条款摘要 (JSON或纯文本)
     */
    private String aiRiskSummary;
    /**
     * 状态：0-草稿 1-法务财务会签中 2-已生效 3-驳回
     */
    private Long status;
    /**
     * Flowable工作流实例ID
     */
    private String processInstanceId;
    /**
     * 删除标志
     */
    @TableLogic
    private String delFlag;

}
