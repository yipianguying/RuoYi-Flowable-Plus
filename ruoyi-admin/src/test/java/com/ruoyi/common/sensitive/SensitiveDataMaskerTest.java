package com.ruoyi.common.sensitive;

import com.ruoyi.common.sensitive.SensitiveDataMasker.MaskingResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
public class SensitiveDataMaskerTest {

    private final SensitiveDataMasker masker = new SensitiveDataMasker();

    @Test
    void shouldMaskSensitiveFieldsAndPreserveContractTerms() {
        String source = "甲方联系人：张三，手机：13812345678，"
            + "身份证号：110101199001011234，邮箱：zhangsan@example.com，"
            + "银行卡号：6222021234567890123，"
            + "统一社会信用代码：91350211M000100Y43，"
            + "联系地址：北京市朝阳区测试路1号。"
            + "合同金额：100000元，签订日期：2026-10-25，"
            + "违约金为合同金额的10%。";

        MaskingResult result = masker.maskWithStats(source);
        String masked = result.getMaskedText();

        assertFalse(masked.contains("张三"));
        assertFalse(masked.contains("13812345678"));
        assertFalse(masked.contains("110101199001011234"));
        assertFalse(masked.contains("zhangsan@example.com"));
        assertFalse(masked.contains("6222021234567890123"));
        assertFalse(masked.contains("91350211M000100Y43"));
        assertFalse(masked.contains("北京市朝阳区测试路1号"));

        assertTrue(masked.contains("[姓名_1]"));
        assertTrue(masked.contains("[手机号_1]"));
        assertTrue(masked.contains("[身份证号_1]"));
        assertTrue(masked.contains("[邮箱_1]"));
        assertTrue(masked.contains("[银行卡号_1]"));
        assertTrue(masked.contains("[统一社会信用代码_1]"));
        assertTrue(masked.contains("[地址_1]"));

        assertTrue(masked.contains("合同金额：100000元"));
        assertTrue(masked.contains("签订日期：2026-10-25"));
        assertTrue(masked.contains("违约金为合同金额的10%"));

        assertEquals(Integer.valueOf(1), result.getHitCounts().get("phone"));
        assertEquals(Integer.valueOf(1), result.getHitCounts().get("idCard"));
        assertEquals(Integer.valueOf(1), result.getHitCounts().get("email"));
        assertEquals(Integer.valueOf(1), result.getHitCounts().get("bankCard"));
        assertEquals(Integer.valueOf(1), result.getHitCounts().get("creditCode"));
        assertEquals(Integer.valueOf(1), result.getHitCounts().get("address"));
        assertEquals(Integer.valueOf(1), result.getHitCounts().get("personName"));
    }

    @Test
    void shouldReusePlaceholderForRepeatedSensitiveValue() {
        String source = "手机：13812345678；备用手机：13812345678。";

        MaskingResult result = masker.maskWithStats(source);
        String masked = result.getMaskedText();

        assertEquals(2, countOccurrences(masked, "[手机号_1]"));
        assertFalse(masked.contains("[手机号_2]"));
        assertEquals(Integer.valueOf(2), result.getHitCounts().get("phone"));
    }

    @Test
    void shouldBeIdempotent() {
        String source = "联系人：张三，手机：13812345678，"
            + "联系地址：北京市朝阳区测试路1号。";

        String maskedOnce = masker.mask(source);
        String maskedTwice = masker.mask(maskedOnce);

        assertEquals(maskedOnce, maskedTwice);
    }

    @Test
    void shouldKeepNullOrEmptyTextUnchanged() {
        assertNull(masker.mask(null));
        assertEquals("", masker.mask(""));
    }

    private int countOccurrences(String text, String target) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(target, index)) >= 0) {
            count++;
            index += target.length();
        }
        return count;
    }
}
