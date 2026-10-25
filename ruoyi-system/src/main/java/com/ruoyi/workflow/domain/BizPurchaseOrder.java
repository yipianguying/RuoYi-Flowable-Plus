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
 * 业务_采购申请主对象 biz_purchase_order
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_purchase_order")
public class BizPurchaseOrder extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 采购单ID
     */
    @TableId(value = "id")
    private Long id;
    /**
     * 采购单号 (如 PUR-202410-001)
     */
    private String orderNo;
    /**
     * 关联供应商ID
     */
    private Long supplierId;
    /**
     * 预估总金额
     */
    private BigDecimal totalAmount;
    /**
     * 业务状态：0-草稿 1-审批中 2-已通过 3-已驳回
     */
    private Long status;
    /**
     * Flowable工作流实例ID (极其关键)
     */
    private String processInstanceId;
    /**
     * 删除标志（0代表存在 2代表删除）
     */
    @TableLogic
    private String delFlag;

}
