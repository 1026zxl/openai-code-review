package org.ocr.com.sdk.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 代码评审结果领域模型
 * 
 * @author SDK Team
 * @since 1.0
 */
public class ReviewResult {
    
    private final CodeInfo codeInfo;
    private final String reviewContent;
    private final LocalDateTime reviewTime;
    private final String reportPath;
    
    public ReviewResult(CodeInfo codeInfo, String reviewContent, LocalDateTime reviewTime, String reportPath) {
        this.codeInfo = codeInfo;
        this.reviewContent = reviewContent;
        this.reviewTime = reviewTime;
        this.reportPath = reportPath;
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
    
    public boolean isEmpty() {
        return reviewContent == null || reviewContent.isEmpty();
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

