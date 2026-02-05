package org.ocr.com.sdk.domain.service;

import org.ocr.com.sdk.domain.model.NotificationMessage;
import org.ocr.com.sdk.domain.model.ReviewResult;

/**
 * 通知消息工厂（领域层，纯领域逻辑，不依赖配置）
 * 负责从评审结果构建通知消息内容；报告链接等由应用层通过 {@link org.ocr.com.sdk.application.ReviewResultNotificationBuilder} 注入。
 *
 * @author SDK Team
 * @since 1.0
 */
public class NotificationMessageFactory {

    /**
     * 从评审结果创建通知消息（不含报告链接，链接由应用层构建器补充）
     *
     * @param reviewResult 评审结果
     * @return 通知消息
     */
    public static NotificationMessage createFromReviewResult(ReviewResult reviewResult) {
        return NotificationMessage.fromReviewResult(reviewResult);
    }
}
