package com.ruoyi.workflow.domain.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class FinanceExportDTO implements Serializable {
    /**
     * 采购单号 (如 PUR-202410-001)
     */
    private String orderNo;

    /**
     * 预估总金额
     */
    private BigDecimal totalAmount;

    /**
     * 供应商名称
     */
    private String supplierName;

    /**
     * 银行账户名称
     */
    private String bankAccount;

    /*
     * 空构造方法
     */
    public FinanceExportDTO(){

    }
}
