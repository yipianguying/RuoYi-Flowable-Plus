package com.ruoyi.workflow.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.ruoyi.common.annotation.ExcelDictFormat;
import com.ruoyi.common.convert.ExcelDictConvert;
import lombok.Data;
import java.util.Date;



/**
 * 业务_供应商信息视图对象 biz_supplier
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Data
@ExcelIgnoreUnannotated
public class BizSupplierVo {

    private static final long serialVersionUID = 1L;

    /**
     * 供应商ID主键
     */
    @ExcelProperty(value = "供应商ID主键")
    private Long id;

    /**
     * 供应商名称
     */
    @ExcelProperty(value = "供应商名称")
    private String supplierName;

    /**
     * 联系人姓名
     */
    @ExcelProperty(value = "联系人姓名")
    private String contactPerson;

    /**
     * 联系电话
     */
    @ExcelProperty(value = "联系电话")
    private String contactPhone;

    /**
     * 对公银行账户
     */
    @ExcelProperty(value = "对公银行账户")
    private String bankAccount;

    /**
     * 状态（1正常 0黑名单冻结）
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "1=正常,0=黑名单冻结")
    private Integer isActive;


}
