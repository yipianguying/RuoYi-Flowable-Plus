package com.ruoyi.workflow.service;


import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface FinanceReportExportService {
    /*
     * 核心导出方法
     */
    void exportFinanceReconciliationReport(HttpServletResponse response, String yearMonth, String currentOperator) throws IOException;

    /**
     * 银行账户名称
     */

}
