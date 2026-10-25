package com.ruoyi.workflow.domain.vo;

import java.math.BigDecimal;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.ruoyi.common.annotation.ExcelDictFormat;
import com.ruoyi.common.convert.ExcelDictConvert;
import lombok.Data;
import java.util.Date;



/**
 * 业务_合同风控审批视图对象 biz_contract
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Data
@ExcelIgnoreUnannotated
public class BizContractVo {

    private static final long serialVersionUID = 1L;

    /**
     * 合同ID
     */
    @ExcelProperty(value = "合同ID")
    private Long id;

    /**
     * 合同编号
     */
    @ExcelProperty(value = "合同编号")
    private String contractNo;

    /**
     * 合同名称
     */
    @ExcelProperty(value = "合同名称")
    private String contractName;

    /**
     * 关联供应商ID
     */
    @ExcelProperty(value = "关联供应商ID")
    private Long supplierId;

    /**
     * 合同总金额
     */
    @ExcelProperty(value = "合同总金额")
    private BigDecimal contractAmount;

    /**
     * 合同附件OSS下载地址 (PDF/Word)
     */
    @ExcelProperty(value = "合同附件OSS下载地址 (PDF/Word)")
    private String fileUrl;

    /**
     * AI大模型提取的风险条款摘要 (JSON或纯文本)
     */
    @ExcelProperty(value = "AI大模型提取的风险条款摘要 (JSON或纯文本)")
    private String aiRiskSummary;

    /**
     * 状态：0-草稿 1-法务财务会签中 2-已生效 3-驳回
     */
    @ExcelProperty(value = "状态：0-草稿 1-法务财务会签中 2-已生效 3-驳回")
    private Long status;

    /**
     * Flowable工作流实例ID
     */
    @ExcelProperty(value = "Flowable工作流实例ID")
    private String processInstanceId;


}
