package com.ruoyi.workflow.domain.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
public class BizContractDto implements Serializable {
    /**
     * 合同名称
     */
    private String contractName;

    /**
     * 合同附件OSS下载地址 (PDF/Word)
     */
    private String fileUrl;

    /**
     * 合同总金额
     */
    private BigDecimal contractAmount;

    /**
     * 状态：0-草稿 1-法务财务会签中 2-已生效 3-驳回
     */
    private int status;
}
