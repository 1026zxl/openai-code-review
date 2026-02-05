package org.ocr.com.sdk.application;

import org.ocr.com.sdk.domain.model.NotificationMessage;
import org.ocr.com.sdk.domain.model.ReviewResult;

/**
 * 从评审结果构建通知消息的构建器（应用层端口）
 * 由应用层实现，可注入报告链接等与配置相关的内容
 *
 * @author SDK Team
 * @since 1.0
 */
public interface ReviewResultNotificationBuilder {

    /**
     * 根据评审结果构建通知消息
     *
     * @param reviewResult 评审结果
     * @return 通知消息
     */
    NotificationMessage build(ReviewResult reviewResult);
}
