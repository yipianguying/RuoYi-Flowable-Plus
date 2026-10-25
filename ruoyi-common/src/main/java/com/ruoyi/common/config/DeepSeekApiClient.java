package com.ruoyi.common.config;


import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.sensitive.SensitiveDataMasker;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 针对 DeepSeek API 深度定制的大模型客户端工具类
 */
@Slf4j
@Component
public class DeepSeekApiClient {
    @Value("${deepseek.api-key:sk-YOUR_DEEPSEEK_KEY_HERE}")
    private String apiKey;

    @Resource
    private SensitiveDataMasker sensitiveDataMasker;

    private static final String API_URL = "https://api.deepseek.com/chat/completions";

    private OkHttpClient okHttpClient;

    @PostConstruct
    public void init() {
        this.okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS) // 连接超时
            .readTimeout(60, TimeUnit.SECONDS)    // 读取超时 (大模型生成文字慢，一定要设长)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
            .build();
    }

    /**
     * 调用 DeepSeek 分析合同文本
     */
    public String analyzeContractRisk(String contractText) {
        String systemPrompt = "你是一位精通中国《民法典》和供应链风控的资深企业法务大律师。" +
            "请阅读用户提供的采购合同文本，精准提取以下三点，并严格按照 JSON 格式返回，不要包含任何多余的 Markdown 格式：\n" +
            "1. totalAmount: 合同总金额\n" +
            "2. penaltyClause: 违约金条款及严苛程度\n" +
            "3. courtJurisdiction: 争议解决管辖法院(是否存在异地管辖诉讼风险)\n" +
            "4. riskLevel: 风险评级(低/中/高)";

        String maskedContractText = sensitiveDataMasker.mask(contractText);
        String safeText = maskedContractText.length() > 8000
            ? maskedContractText.substring(0, 8000) + "..."
            : maskedContractText;

        // ================= 1. 使用 Hutool JSONObject 构建请求 =================
        JSONObject requestBodyJson = new JSONObject();
        requestBodyJson.set("model", "deepseek-chat");
        requestBodyJson.set("temperature", 0.1); // 低温度保障风控结论的严谨性、唯一性
        requestBodyJson.set("max_tokens", 1500);

        // Hutool 的 .set() 支持完美的链式调用，代码比 Fastjson 更简洁
        JSONArray messages = new JSONArray();
        messages.add(new JSONObject().set("role", "system").set("content", systemPrompt));
        messages.add(new JSONObject().set("role", "user").set("content", "请分析以下合同：\n" + safeText));
        requestBodyJson.set("messages", messages);

        // 使用 Hutool 的 JSONUtil.toJsonStr() 转为字符串
        RequestBody body = RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"),
            JSONUtil.toJsonStr(requestBodyJson)
        );

        Request request = new Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build();

        // ================= 2. 发起请求与 Hutool 极简解析 =================
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.error("DeepSeek 接口调用失败，HTTP状态码: {}", response.code());
                return "{\"riskLevel\":\"未知\",\"penaltyClause\":\"调用AI大模型服务超时或异常，请法务人工审查\"}";
            }

            String responseString = response.body().string();

            // 使用 Hutool 解析返回的 JSON 字符串
            JSONObject responseJson = JSONUtil.parseObj(responseString);

            // 🔥 【高光亮点】：使用 Hutool 的 getByPath 语法提取深层嵌套字段！
            // 传统写法需要：responseJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getStr("content");
            // 只要中间有一层是 null 就会报 NullPointerException。
            // 而 getByPath("choices[0].message.content", String.class) 一行搞定，自带空指针防呆！
            String aiContent = responseJson.getByPath("choices[0].message.content", String.class);

            log.info("DeepSeek 合同风控解析成功，返回字符数: {}",
                aiContent == null ? 0 : aiContent.length());
            return aiContent;

        } catch (IOException e) {
            log.error("网络请求 DeepSeek 发生 IO 异常", e);
            throw new RuntimeException("AI 风控系统连接异常: " + e.getMessage());
        }
    }
}
