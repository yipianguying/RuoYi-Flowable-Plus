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
 * 业务_采购申请主业务对象 biz_purchase_order
 *
 * @author ruoyi
 * @date 2026-07-23
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class BizPurchaseOrderBo extends BaseEntity {

    /**
     * 采购单ID
     */
    @NotNull(message = "采购单ID不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 采购单号 (如 PUR-202410-001)
     */
    @NotBlank(message = "采购单号 (如 PUR-202410-001)不能为空", groups = { AddGroup.class, EditGroup.class })
    private String orderNo;

    /**
     * 关联供应商ID
     */
    @NotNull(message = "关联供应商ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long supplierId;

    /**
     * 预估总金额
     */
    @NotNull(message = "预估总金额不能为空", groups = { AddGroup.class, EditGroup.class })
    private BigDecimal totalAmount;

    /**
     * 业务状态：0-草稿 1-审批中 2-已通过 3-已驳回
     */
    @NotNull(message = "业务状态：0-草稿 1-审批中 2-已通过 3-已驳回不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long status;

    /**
     * Flowable工作流实例ID (极其关键)
     */
    @NotBlank(message = "Flowable工作流实例ID (极其关键)不能为空", groups = { AddGroup.class, EditGroup.class })
    private String processInstanceId;


}
