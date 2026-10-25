package com.ruoyi.common.sensitive;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 外发文本脱敏器。
 *
 * <p>使用占位符替换敏感字段，同一文本中的相同敏感值复用同一编号，
 * 从而在隐藏原始值的同时保留大模型分析所需的上下文关系。</p>
 */
@Component
public class SensitiveDataMasker {

    private static final String EMAIL = "email";
    private static final String ID_CARD = "idCard";
    private static final String PHONE = "phone";
    private static final String CREDIT_CODE = "creditCode";
    private static final String BANK_CARD = "bankCard";
    private static final String LANDLINE = "landline";
    private static final String ADDRESS = "address";
    private static final String PERSON_NAME = "personName";

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"
    );

    private static final Pattern ID_CARD_PATTERN = Pattern.compile(
        "(?<![0-9A-Za-z])(?:"
            + "[1-9]\\d{5}(?:18|19|20)\\d{2}(?:0[1-9]|1[0-2])"
            + "(?:0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]"
            + "|[1-9]\\d{7}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}"
            + ")(?![0-9A-Za-z])"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "(?<![0-9])(?:\\+?86[\\s-]?)?1[3-9]\\d{9}(?![0-9])"
    );

    private static final Pattern CREDIT_CODE_PATTERN = Pattern.compile(
        "(?:统一社会信用代码|纳税人识别号|税号)\\s*[:：]\\s*"
            + "(?!\\[)([0-9A-HJ-NPQRTUWXY]{15,20})",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern BANK_CARD_PATTERN = Pattern.compile(
        "(?:银行卡号|银行账号|收款账号|开户账号|账号|卡号)\\s*[:：]\\s*"
            + "(?!\\[)([A-Za-z0-9](?:[A-Za-z0-9 -]{6,40})[A-Za-z0-9])",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern LANDLINE_PATTERN = Pattern.compile(
        "(?:联系电话|办公电话|固定电话|座机|电话|传真)\\s*[:：]\\s*"
            + "(?!\\[)((?:\\+?86[\\s-]?)?(?:0\\d{2,3}[\\s-]?)?\\d{7,8}(?:[-转]\\d{1,6})?)"
    );

    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
        "(?:联系地址|注册地址|通讯地址|住所地|住所|地址)\\s*[:：]\\s*"
            + "(?!\\[)([^，,；;。\\n\\r]{4,80})"
    );

    private static final Pattern PERSON_NAME_PATTERN = Pattern.compile(
        "(?:联系人姓名|联系人|法定代表人|委托代理人|授权代表|签约代表|经办人|负责人)"
            + "\\s*[:：]\\s*(?!\\[)([\\u4e00-\\u9fa5·]{2,8})"
    );

    private static final List<MaskRule> MASK_RULES = Collections.unmodifiableList(Arrays.asList(
        new MaskRule(EMAIL, "邮箱", EMAIL_PATTERN, 0),
        new MaskRule(ID_CARD, "身份证号", ID_CARD_PATTERN, 0),
        new MaskRule(PHONE, "手机号", PHONE_PATTERN, 0),
        new MaskRule(CREDIT_CODE, "统一社会信用代码", CREDIT_CODE_PATTERN, 1),
        new MaskRule(BANK_CARD, "银行卡号", BANK_CARD_PATTERN, 1),
        new MaskRule(LANDLINE, "座机", LANDLINE_PATTERN, 1),
        new MaskRule(ADDRESS, "地址", ADDRESS_PATTERN, 1),
        new MaskRule(PERSON_NAME, "姓名", PERSON_NAME_PATTERN, 1)
    ));

    /**
     * 脱敏文本并返回结果。
     *
     * @param text 原始文本
     * @return 脱敏后的文本
     */
    public String mask(String text) {
        return maskWithStats(text).getMaskedText();
    }

    /**
     * 脱敏文本并返回命中统计。统计信息不包含任何原始敏感值。
     *
     * @param text 原始文本
     * @return 脱敏结果
     */
    public MaskingResult maskWithStats(String text) {
        if (text == null || text.isEmpty()) {
            return new MaskingResult(text, Collections.<String, Integer>emptyMap());
        }

        Map<String, String> placeholders = new LinkedHashMap<String, String>();
        Map<String, Integer> counters = new LinkedHashMap<String, Integer>();
        Map<String, Integer> hitCounts = new LinkedHashMap<String, Integer>();
        String maskedText = text;

        for (MaskRule rule : MASK_RULES) {
            maskedText = applyRule(maskedText, rule, placeholders, counters, hitCounts);
        }
        return new MaskingResult(maskedText, hitCounts);
    }

    private String applyRule(String text, MaskRule rule, Map<String, String> placeholders,
                             Map<String, Integer> counters, Map<String, Integer> hitCounts) {
        Matcher matcher = rule.pattern.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String sensitiveValue = matcher.group(rule.captureGroup);
            String placeholderKey = rule.type + '\u0000' + sensitiveValue;
            String placeholder = placeholders.get(placeholderKey);
            if (placeholder == null) {
                Integer currentIndex = counters.get(rule.type);
                int nextIndex = currentIndex == null ? 1 : currentIndex + 1;
                counters.put(rule.type, nextIndex);
                placeholder = "[" + rule.placeholderName + "_" + nextIndex + "]";
                placeholders.put(placeholderKey, placeholder);
            }

            int localStart = matcher.start(rule.captureGroup) - matcher.start();
            int localEnd = matcher.end(rule.captureGroup) - matcher.start();
            String matchedText = matcher.group();
            String replacement = matchedText.substring(0, localStart)
                + placeholder
                + matchedText.substring(localEnd);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));

            Integer currentHitCount = hitCounts.get(rule.type);
            hitCounts.put(rule.type, currentHitCount == null ? 1 : currentHitCount + 1);
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static final class MaskRule {

        private final String type;
        private final String placeholderName;
        private final Pattern pattern;
        private final int captureGroup;

        private MaskRule(String type, String placeholderName, Pattern pattern, int captureGroup) {
            this.type = type;
            this.placeholderName = placeholderName;
            this.pattern = pattern;
            this.captureGroup = captureGroup;
        }
    }

    /**
     * 脱敏结果。
     */
    public static final class MaskingResult {

        private final String maskedText;
        private final Map<String, Integer> hitCounts;

        private MaskingResult(String maskedText, Map<String, Integer> hitCounts) {
            this.maskedText = maskedText;
            this.hitCounts = Collections.unmodifiableMap(
                new LinkedHashMap<String, Integer>(hitCounts)
            );
        }

        public String getMaskedText() {
            return maskedText;
        }

        public Map<String, Integer> getHitCounts() {
            return hitCounts;
        }
    }
}
