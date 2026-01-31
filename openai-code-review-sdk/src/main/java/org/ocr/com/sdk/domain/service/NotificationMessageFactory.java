package org.ocr.com.sdk.domain.service;

import org.ocr.com.sdk.config.CodeReviewConfig;
import org.ocr.com.sdk.domain.model.NotificationMessage;
import org.ocr.com.sdk.domain.model.ReviewResult;

/**
 * 通知消息工厂
 * 负责从领域对象构建通知消息
 * 
 * @author SDK Team
 * @since 1.0
 */
public class NotificationMessageFactory {
    
    private final CodeReviewConfig config;
    
    public NotificationMessageFactory(CodeReviewConfig config) {
        this.config = config;
    }
    
    /**
     * 从评审结果创建通知消息
     * 
     * @param reviewResult 评审结果
     * @return 通知消息
     */
    public NotificationMessage createFromReviewResult(ReviewResult reviewResult) {
        NotificationMessage message = NotificationMessage.fromReviewResult(reviewResult);
        
        // 构建报告链接（如果有GitHub配置）
        String reportUrl = buildReportUrl(reviewResult.getReportPath());
        if (reportUrl != null) {
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
        
        return message;
    }
    
    /**
     * 构建报告URL
     */
    private String buildReportUrl(String reportPath) {
        if (reportPath == null || reportPath.isEmpty()) {
            return null;
        }
        
        // 如果配置了GitHub仓库，构建GitHub链接
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
