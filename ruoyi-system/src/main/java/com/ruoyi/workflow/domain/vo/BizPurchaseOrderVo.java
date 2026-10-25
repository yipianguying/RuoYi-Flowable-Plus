package com.ruoyi.workflow.domain.vo;

import java.math.BigDecimal;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.ruoyi.common.annotation.ExcelDictFormat;
import com.ruoyi.common.convert.ExcelDictConvert;
import lombok.Data;
import java.util.Date;



/**
 * 业务_采购申请主视图对象 biz_purchase_order
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Data
@ExcelIgnoreUnannotated
public class BizPurchaseOrderVo {

    private static final long serialVersionUID = 1L;

    /**
     * 采购单ID
     */
    @ExcelProperty(value = "采购单ID")
    private Long id;

    /**
     * 采购单号 (如 PUR-202410-001)
     */
    @ExcelProperty(value = "采购单号 (如 PUR-202410-001)")
    private String orderNo;

    /**
     * 关联供应商ID
     */
    @ExcelProperty(value = "关联供应商ID")
    private Long supplierId;

    /**
     * 预估总金额
     */
    @ExcelProperty(value = "预估总金额")
    private BigDecimal totalAmount;

    /**
     * 业务状态：0-草稿 1-审批中 2-已通过 3-已驳回
     */
    @ExcelProperty(value = "业务状态：0-草稿 1-审批中 2-已通过 3-已驳回")
    private Long status;

    /**
     * Flowable工作流实例ID (极其关键)
     */
    @ExcelProperty(value = "Flowable工作流实例ID (极其关键)")
    private String processInstanceId;


}
