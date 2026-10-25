package com.ruoyi.workflow.service.impl;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.ruoyi.common.config.DeepSeekApiClient;
import com.ruoyi.common.sensitive.SensitiveDataMasker;
import com.ruoyi.common.sensitive.SensitiveDataMasker.MaskingResult;
import com.ruoyi.workflow.domain.BizContract;
import com.ruoyi.workflow.mapper.BizContractMapper;
import com.ruoyi.workflow.service.AiRiskAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.flowable.engine.RuntimeService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.InputStream;

/**
 * 业务_合同风控审批Service业务层处理
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class AiRiskAnalysisServiceImpl implements AiRiskAnalysisService {

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private BizContractMapper contractMapper;

    // 伪代码：假设你封装的调用大模型（DeepSeek/千问）的工具类
    @Resource
    private DeepSeekApiClient deepSeekApiClient;

    @Resource
    private SensitiveDataMasker sensitiveDataMasker;

    /**
     * 异步解析合同，并将结果热更新到运行中的工作流实例里
     *
     * @param contractId        业务合同ID
     * @param fileUrl           合同附件 OSS 下载地址
     * @param processInstanceId Flowable 流程实例ID
     */
    @Override
    @Async("threadPoolTaskExecutor") // 使用 Spring 线程池异步执行，绝不阻塞主线程！
    public void asyncAnalyzeContract(Long contractId, String fileUrl, String processInstanceId) {
        log.info("开始异步解析合同附件，contractId: {}, 关联流程实例: {}",
            contractId, processInstanceId);

        try {
            // 🔥 改造点 2：将 OSS 文件地址读取并提取为纯文本字符串 (解决 URL 到 Text 的衔接问题)
            // 在实际工程中，这里可以通过 Apache PDFBox 读 PDF，或者用 Hutool Http 工具下载文本
            String contractText = extractTextFromFileUrl(fileUrl);

            // 发给外部大模型前必须脱敏，避免合同中的个人信息和联系信息离开内网
            MaskingResult maskingResult = sensitiveDataMasker.maskWithStats(contractText);
            String maskedContractText = maskingResult.getMaskedText();
            log.info("合同文本脱敏完成，命中统计: {}", maskingResult.getHitCounts());

            // 🔥 改造点 3：直接调用 DeepSeek 工具类！不需要再在这里写繁琐的 prompt 提示词
            // 提示词的法务人设已经封装在 analyzeContractRisk 内部，传入文字即可拿到 JSON 摘要
            String aiResult = deepSeekApiClient.analyzeContractRisk(maskedContractText);
            String maskedAiResult = sensitiveDataMasker.mask(aiResult);

            // 4. 【高阶核心操作】：将大模型解析结果直接热更新到 Flowable 的运行时流程变量中！
            // 当法务主管打开待办任务表单时，若依动态表单会直接展示最新的 aiRiskSummary
            runtimeService.setVariable(processInstanceId, "aiRiskSummary", maskedAiResult);

            // 5. 同步将结果持久化一份到 MySQL 业务表中，便于日后脱离流程引擎的单独历史审计
            BizContract updateEntity = new BizContract();
            updateEntity.setId(contractId);
            updateEntity.setAiRiskSummary(maskedAiResult);
            contractMapper.updateById(updateEntity);

            log.info("恭喜！DeepSeek 合同分析完成，已成功回写流程变量！流程ID: {}", processInstanceId);

        } catch (Exception e) {
            log.error("AI 异步解析合同失败, contractId: {}", contractId, e);
            // 容错兜底机制：即使 AI 报错或网络超时，流程必须能正常往下走，不能卡死业务！
            String fallbackMsg = "{\"riskLevel\":\"高\",\"penaltyClause\":\"【系统提示】：AI 大模型网络连接超时或解析异常，请法务人员手动审阅原件附件！\"}";
            runtimeService.setVariable(processInstanceId, "aiRiskSummary", fallbackMsg);
        }
    }

    /**
     * 辅助方法：模拟从 OSS 下载 PDF/Word 附件并解析出纯文本
     * （实战拓展：如果你项目里用到了阿里云 OSS 或 MinIO，可以在这里用 HttpUtil.downloadString 提取文字）
     */
    private String extractTextFromFileUrl(String fileUrl) {
        log.info("开始从 OSS 附件解析非结构化文本");

        // 1. 【防呆校验】 URL 非空判断
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("解析失败：合同附件下载链接不能为空！");
        }

        // 2. 【核心防 OOM】：设置 Tika 解析器的字符数上限！
        // 默认 Tika 只解析前 10万 字符。我们根据 DeepSeek 64k/128k 上下文窗口，
        // 将最大提取上限锁定在 30,000 个字符 (约合 2-30 页的买卖合同)，既保护 JVM 堆内存，又防止 Token 费用超标。
        int maxCharacterLimit = 30000;
        BodyContentHandler handler = new BodyContentHandler(maxCharacterLimit);

        // 3. 构建自动探测解析器 (AutoDetectParser 是 Tika 的灵魂，无需感知后缀名，底层依靠 Magic Number 识别文件真实类型)
        Parser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();

        // 4. 【网络防御】：利用 Hutool HttpUtil 发起流式请求，绝不一次性读取 byte[] 进内存！
        // 必须设置连接和读取超时，防止内网 MinIO 或阿里云 OSS 响应卡死导致工作流线程阻塞
        try (HttpResponse httpResponse = HttpUtil.createGet(fileUrl)
            .timeout(15000) // 15秒超时熔断
            .execute()) {

            // 检查 HTTP 状态码
            if (!httpResponse.isOk()) {
                throw new RuntimeException("无法从 OSS 下载合同附件，HTTP 响应码: " + httpResponse.getStatus());
            }

            // 5. 【流式读取与解析】：拿到输入流直接喂给 Tika，实现“边读边解析”，极致压榨堆内存
            try (InputStream inputStream = httpResponse.bodyStream()) {
                parser.parse(inputStream, handler, metadata, context);
            }

            // 6. 获取解析出来的原始纯文本
            String rawText = handler.toString();

            // 7. 【大模型降本增效核心】：文本洗净 (Sanitization)
            // 扫描件或 Word 排版提取出来的文字，往往带有大量的连续空行、制表符 \t 和特殊乱码。
            // 这种无意义的字符发给 DeepSeek 既浪费 Token 计费，又会干扰 LLM 的注意力机制 (Attention)！
            String cleanText = cleanAndCompressText(rawText);

            log.info("合同附件文本提取成功！原始字符数: {}, 洗净压缩后字符数: {}", rawText.length(), cleanText.length());
            return cleanText;

        } catch (TikaException e) {
            // Tika 解析非正常中文字符集或加密 PDF 时可能抛出的异常
            log.warn("Tika 遇到截断或超长文本，正在触发部分解析降级。具体原因: {}", e.getMessage());
            // 如果是因为超过了 maxCharacterLimit 限制， handler 里其实已经保存了前 30000 个有效字符，直接返回即可！
            if (e.getMessage() != null && e.getMessage().contains("Limit")) {
                return cleanAndCompressText(handler.toString());
            }
            throw new RuntimeException("附件文档内容解析格式错误，请确保是不加密的 PDF 或 Word 文档！", e);
        } catch (Exception e) {
            log.error("从远程 URL 解析合同附件正文引发系统级异常", e);
            throw new RuntimeException("文件解析流水线异常：" + e.getMessage());
        }
    }

    /**
     * 【面试超分点】：专为大模型 Prompt 打造的“洗字降本”算法
     * 过滤多余连续换行、特殊控制字符，降低 Token 成本，提升 LLM 推理精度
     */
    private String cleanAndCompressText(String rawText) {
        if (rawText == null) {
            return "";
        }
        return rawText
            // 1. 将所有换行符统一标准化为 \n
            .replaceAll("\r\n", "\n")
            .replaceAll("\r", "\n")
            // 2. 将连续的 3 个或以上换行符，压缩为双换行 (保留段落语义，去除大幅空白)
            .replaceAll("\n{3,}", "\n\n")
            // 3. 将制表符 \t 和连续的多个空格，压缩为单个空格 (节约 Token)
            .replaceAll("[ \\t]{2,}", " ")
            // 4. 剔除 ASCII 码中的非打印控制字符 (防止 LLM 解析 JSON 回报异常)
            .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "")
            // 5. 去除首尾留白
            .trim();
    }
}
