package org.ocr.com.sdk.application;

import org.ocr.com.sdk.config.CodeReviewConfig;
import org.ocr.com.sdk.domain.model.NotificationMessage;
import org.ocr.com.sdk.domain.model.ReviewResult;

/**
 * 默认的评审结果通知构建器（应用层）
 * 基于领域模型构建通知消息，并注入报告链接等与配置相关的内容
 *
 * @author SDK Team
 * @since 1.0
 */
public class DefaultReviewResultNotificationBuilder implements ReviewResultNotificationBuilder {

    private final CodeReviewConfig config;

    public DefaultReviewResultNotificationBuilder(CodeReviewConfig config) {
        this.config = config;
    }

    @Override
    public NotificationMessage build(ReviewResult reviewResult) {
        NotificationMessage message = NotificationMessage.fromReviewResult(reviewResult);
        String reportUrl = buildReportUrl(reviewResult.getReportPath());
        if (reportUrl == null || reportUrl.isEmpty()) {
            return message;
        }
        return NotificationMessage.builder()
                .title(message.getTitle())
                .content(message.getContent())
                .summary(message.getSummary())
                .linkUrl(reportUrl)
                .linkText(message.getLinkText())
                .metadata(message.getMetadata())
                .timestamp(message.getTimestamp())
                .messageType(message.getMessageType())
                .priority(message.getPriority())
                .build();
    }

    private String buildReportUrl(String reportPath) {
        if (reportPath == null || reportPath.isEmpty()) {
            return null;
        }
        if (config.getGithubRepoUrl() != null && !config.getGithubRepoUrl().isEmpty()) {
            String repoUrl = config.getGithubRepoUrl();
            if (repoUrl.endsWith(".git")) {
                repoUrl = repoUrl.substring(0, repoUrl.length() - 4);
            }
            return repoUrl + "/blob/main/" + reportPath;
        }
        return null;
    }
}
