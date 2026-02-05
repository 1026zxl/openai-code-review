package org.ocr.com.sdk.domain.port;

/**
 * 代码评审 API 端口（DDD 端口）
 * 由领域定义，由基础设施实现（如 OpenAI/HTTP 适配器）
 *
 * @author SDK Team
 * @since 1.0
 */
public interface CodeReviewApi {

    /**
     * 调用 AI 进行代码评审
     *
     * @param prompt 评审提示词（完整 prompt）
     * @return 评审结果文本，不为 null 且非空
     */
    String reviewByPrompt(String prompt);
}
