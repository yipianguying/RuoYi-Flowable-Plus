package com.ruoyi.workflow.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.validation.constraints.*;

import java.util.Date;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 业务_供应商信息业务对象 biz_supplier
 *
 * @author ruoyi
 * @date 2026-07-23
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class BizSupplierBo extends BaseEntity {

    /**
     * 供应商ID主键
     */
    @NotNull(message = "供应商ID主键不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 供应商名称
     */
    @NotBlank(message = "供应商名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String supplierName;

    /**
     * 联系人姓名
     */
    @NotBlank(message = "联系人姓名不能为空", groups = { AddGroup.class, EditGroup.class })
    private String contactPerson;

    /**
     * 联系电话
     */
    @NotBlank(message = "联系电话不能为空", groups = { AddGroup.class, EditGroup.class })
    private String contactPhone;

    /**
     * 对公银行账户
     */
    @NotBlank(message = "对公银行账户不能为空", groups = { AddGroup.class, EditGroup.class })
    private String bankAccount;

    /**
     * 状态（1正常 0黑名单冻结）
     */
    @NotNull(message = "状态（1正常 0黑名单冻结）不能为空", groups = { AddGroup.class, EditGroup.class })
    private Integer isActive;


}
