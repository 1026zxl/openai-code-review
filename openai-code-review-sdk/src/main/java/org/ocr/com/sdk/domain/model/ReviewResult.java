package org.ocr.com.sdk.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 代码评审结果聚合根（Aggregate Root）
 * 封装评审结果，提供领域行为
 * 
 * @author SDK Team
 * @since 1.0
 */
public class ReviewResult {
    
    private final CodeInfo codeInfo;
    private final String reviewContent;
    private final LocalDateTime reviewTime;
    private final String reportPath;
    
    /**
     * 构造函数
     */
    public ReviewResult(CodeInfo codeInfo, String reviewContent, 
                       LocalDateTime reviewTime, String reportPath) {
        this.codeInfo = Objects.requireNonNull(codeInfo, "代码信息不能为空");
        this.reviewContent = Objects.requireNonNull(reviewContent, "评审内容不能为空");
        this.reviewTime = Objects.requireNonNull(reviewTime, "评审时间不能为空");
        this.reportPath = reportPath; // 可为空
    }
    
    public CodeInfo getCodeInfo() {
        return codeInfo;
    }
    
    public String getReviewContent() {
        return reviewContent;
    }
    
    public LocalDateTime getReviewTime() {
        return reviewTime;
    }
    
    public String getReportPath() {
        return reportPath;
    }
    
    // 领域行为
    
    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return reviewContent == null || reviewContent.trim().isEmpty();
    }
    
    /**
     * 获取评审摘要（提取第一段）
     */
    public String getSummary() {
        if (isEmpty()) {
            return "";
        }
        String[] lines = reviewContent.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.length() > 20 && !line.startsWith("#") && !line.startsWith("*")) {
                return line.length() > 100 ? line.substring(0, 100) + "..." : line;
            }
        }
        return reviewContent.length() > 100 ? reviewContent.substring(0, 100) + "..." : reviewContent;
    }
    
    /**
     * 获取报告URL（如果配置了基础URL）
     */
    public String getReportUrl(String baseUrl) {
        if (reportPath == null || reportPath.isEmpty()) {
            return null;
        }
        if (baseUrl == null || baseUrl.isEmpty()) {
            return reportPath;
        }
        String normalizedBase = baseUrl.trim();
        if (normalizedBase.endsWith("/")) {
            normalizedBase = normalizedBase.substring(0, normalizedBase.length() - 1);
        }
        return normalizedBase + "/" + reportPath;
    }
    
    /**
     * 验证评审结果的有效性
     */
    public void validate() {
        if (isEmpty()) {
            throw new IllegalStateException("评审结果为空");
        }
        if (codeInfo == null) {
            throw new IllegalStateException("代码信息为空");
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReviewResult that = (ReviewResult) o;
        return Objects.equals(codeInfo, that.codeInfo) &&
               Objects.equals(reviewTime, that.reviewTime);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(codeInfo, reviewTime);
    }
    
    @Override
    public String toString() {
        return "ReviewResult{" +
               "codeInfo=" + codeInfo +
               ", reviewTime=" + reviewTime +
               ", reportPath='" + reportPath + '\'' +
               ", isEmpty=" + isEmpty() +
               '}';
    }
}

