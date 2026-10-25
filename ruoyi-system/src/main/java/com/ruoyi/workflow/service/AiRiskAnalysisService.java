package com.ruoyi.workflow.service;

public interface AiRiskAnalysisService {
     /*
     * 异步解析合同，并将结果热更新到运行中的工作流实例里
     * */
    void asyncAnalyzeContract(Long contractId, String fileUrl, String processInstanceId);
}
