package org.ocr.com.sdk.domain.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 通知消息领域模型
 * 表示需要推送的通知消息内容
 * 
 * @author SDK Team
 * @since 1.0
 */
public class NotificationMessage {
    
    /**
     * 消息标题
     */
    private final String title;
    
    /**
     * 消息内容
     */
    private final String content;
    
    /**
     * 消息摘要
     */
    private final String summary;
    
    /**
     * 消息链接（可选）
     */
    private final String linkUrl;
    
    /**
     * 消息链接文本（可选）
     */
    private final String linkText;
    
    /**
     * 消息元数据（用于扩展，如问题统计、提交信息等）
     */
    private final Map<String, String> metadata;
    
    /**
     * 消息时间
     */
    private final LocalDateTime timestamp;
    
    /**
     * 消息类型
     */
    private final MessageType messageType;
    
    /**
     * 消息优先级
     */
    private final Priority priority;
    
    /**
     * 消息类型枚举
     */
    public enum MessageType {
        CODE_REVIEW_COMPLETED("代码评审完成"),
        CODE_REVIEW_FAILED("代码评审失败"),
        SYSTEM_NOTIFICATION("系统通知");
        
        private final String description;
        
        MessageType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 消息优先级枚举
     */
    public enum Priority {
        HIGH("高"),
        MEDIUM("中"),
        LOW("低");
        
        private final String description;
        
        Priority(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private NotificationMessage(Builder builder) {
        this.title = builder.title;
        this.content = builder.content;
        this.summary = builder.summary;
        this.linkUrl = builder.linkUrl;
        this.linkText = builder.linkText;
        this.metadata = new HashMap<>(builder.metadata);
        this.timestamp = builder.timestamp != null ? builder.timestamp : LocalDateTime.now();
        this.messageType = builder.messageType != null ? builder.messageType : MessageType.CODE_REVIEW_COMPLETED;
        this.priority = builder.priority != null ? builder.priority : Priority.MEDIUM;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getContent() {
        return content;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public String getLinkUrl() {
        return linkUrl;
    }
    
    public String getLinkText() {
        return linkText;
    }
    
    public Map<String, String> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    public String getMetadata(String key) {
        return metadata.get(key);
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public MessageType getMessageType() {
        return messageType;
    }
    
    public Priority getPriority() {
        return priority;
    }
    
    /**
     * 创建Builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * 从评审结果创建通知消息
     */
    public static NotificationMessage fromReviewResult(ReviewResult reviewResult) {
        CodeInfo codeInfo = reviewResult.getCodeInfo();
        
        // 提取摘要
        String summary = extractSummary(reviewResult.getReviewContent());
        
        // 提取问题统计
        String issueStats = extractIssueStats(reviewResult.getReviewContent());
        
        // 构建元数据
        Map<String, String> metadata = new HashMap<>();
        metadata.put("commitMessage", codeInfo.getCommitMessage());
        metadata.put("authorName", codeInfo.getAuthorName());
        metadata.put("commitHash", codeInfo.getCommitHash());
        metadata.put("issueStats", issueStats);
        metadata.put("reportPath", reviewResult.getReportPath());
        
        // 确定优先级
        Priority priority = determinePriority(issueStats);
        
        return builder()
                .title("代码评审完成通知")
                .content(reviewResult.getReviewContent())
                .summary(summary)
                .linkUrl(buildReportUrl(reviewResult.getReportPath()))
                .linkText("查看详细报告")
                .metadata(metadata)
                .timestamp(reviewResult.getReviewTime())
                .messageType(MessageType.CODE_REVIEW_COMPLETED)
                .priority(priority)
                .build();
    }
    
    /**
     * 提取评审内容摘要
     */
    private static String extractSummary(String reviewContent) {
        if (reviewContent == null || reviewContent.isEmpty()) {
            return "评审完成";
        }
        
        // 提取第一段作为摘要
        String[] lines = reviewContent.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.length() > 20 && !line.startsWith("#") && !line.startsWith("*")) {
                return line.length() > 100 ? line.substring(0, 100) + "..." : line;
            }
        }
        
        // 如果没有合适的行，返回前100个字符
        return reviewContent.length() > 100 ? reviewContent.substring(0, 100) + "..." : reviewContent;
    }
    
    /**
     * 提取问题统计
     */
    private static String extractIssueStats(String reviewContent) {
        if (reviewContent == null || reviewContent.isEmpty()) {
            return "无问题";
        }
        
        int highCount = 0, mediumCount = 0, lowCount = 0;
        
        String[] lines = reviewContent.split("\n");
        for (String line : lines) {
            if (line.contains("严重问题数量") || line.contains("问题数量")) {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                        "高[（(]\\s*(\\d+)\\s*[）)]|中[（(]\\s*(\\d+)\\s*[）)]|低[（(]\\s*(\\d+)\\s*[）)]");
                java.util.regex.Matcher matcher = pattern.matcher(line);
                while (matcher.find()) {
                    if (matcher.group(1) != null) {
                        highCount = Integer.parseInt(matcher.group(1));
                    } else if (matcher.group(2) != null) {
                        mediumCount = Integer.parseInt(matcher.group(2));
                    } else if (matcher.group(3) != null) {
                        lowCount = Integer.parseInt(matcher.group(3));
                    }
                }
                break;
            }
        }
        
        if (highCount == 0 && mediumCount == 0 && lowCount == 0) {
            return "查看详情";
        }
        
        return String.format("高:%d 中:%d 低:%d", highCount, mediumCount, lowCount);
    }
    
    /**
     * 根据问题统计确定优先级
     */
    private static Priority determinePriority(String issueStats) {
        if (issueStats == null || issueStats.isEmpty() || issueStats.equals("无问题") || issueStats.equals("查看详情")) {
            return Priority.LOW;
        }
        
        // 提取高危问题数量
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("高:(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(issueStats);
        if (matcher.find()) {
            int highCount = Integer.parseInt(matcher.group(1));
            if (highCount > 0) {
                return Priority.HIGH;
            }
        }
        
        return Priority.MEDIUM;
    }
    
    /**
     * 构建报告URL
     * 注意：此方法在领域层无法访问配置，返回null
     * 实际URL构建应在 NotificationMessageFactory 中完成
     */
    private static String buildReportUrl(String reportPath) {
        // 领域层无法访问配置，返回null
        // 实际URL构建在 NotificationMessageFactory 中完成
        return null;
    }
    
    /**
     * Builder类
     */
    public static class Builder {
        private String title;
        private String content;
        private String summary;
        private String linkUrl;
        private String linkText;
        private Map<String, String> metadata = new HashMap<>();
        private LocalDateTime timestamp;
        private MessageType messageType;
        private Priority priority;
        
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        
        public Builder content(String content) {
            this.content = content;
            return this;
        }
        
        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }
        
        public Builder linkUrl(String linkUrl) {
            this.linkUrl = linkUrl;
            return this;
        }
        
        public Builder linkText(String linkText) {
            this.linkText = linkText;
            return this;
        }
        
        public Builder metadata(String key, String value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public Builder metadata(Map<String, String> metadata) {
            this.metadata.putAll(metadata);
            return this;
        }
        
        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder messageType(MessageType messageType) {
            this.messageType = messageType;
            return this;
        }
        
        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }
        
        public NotificationMessage build() {
            if (title == null || title.isEmpty()) {
                throw new IllegalArgumentException("消息标题不能为空");
            }
            return new NotificationMessage(this);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationMessage that = (NotificationMessage) o;
        return Objects.equals(title, that.title) &&
               Objects.equals(timestamp, that.timestamp) &&
               messageType == that.messageType;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(title, timestamp, messageType);
    }
    
    @Override
    public String toString() {
        return "NotificationMessage{" +
               "title='" + title + '\'' +
               ", summary='" + summary + '\'' +
               ", messageType=" + messageType +
               ", priority=" + priority +
               ", timestamp=" + timestamp +
               '}';
    }
}
