package com.ruoyi.workflow.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.util.Date;
import java.math.BigDecimal;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 业务_供应商信息对象 biz_supplier
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_supplier")
public class BizSupplier extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 供应商ID主键
     */
    @TableId(value = "id")
    private Long id;
    /**
     * 供应商名称
     */
    private String supplierName;
    /**
     * 联系人姓名
     */
    private String contactPerson;
    /**
     * 联系电话
     */
    private String contactPhone;
    /**
     * 对公银行账户
     */
    private String bankAccount;
    /**
     * 状态（1正常 0黑名单冻结）
     */
    private Integer isActive;
    /**
     * 删除标志（0代表存在 2代表删除）
     */
    @TableLogic
    private String delFlag;

}
