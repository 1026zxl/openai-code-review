package org.ocr.com.sdk.application;

import org.ocr.com.sdk.domain.model.CodeInfo;
import org.ocr.com.sdk.domain.model.ReviewResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * 代码评审服务抽象类（模板方法模式）
 * 定义代码评审的主流程骨架，子类实现具体步骤
 * 
 * @author SDK Team
 * @since 1.0
 */
public abstract class AbstractCodeReviewService {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 执行代码评审（模板方法）
     * 主流程：获取变更 → 评审代码 → 保存报告 → 发送通知
     * 
     * @return 评审结果
     */
    public ReviewResult execute() {
        try {
            logger.info("=== 开始代码评审 ===");
            
            // 1. 获取代码变更
            CodeInfo codeInfo = getCodeChanges();
            codeInfo.validate();
            
            // 2. 评审代码
            String reviewContent = reviewCode(codeInfo);
            
            // 3. 保存报告
            String reportPath = saveReport(codeInfo, reviewContent);
            
            // 4. 构建评审结果
            ReviewResult result = new ReviewResult(
                codeInfo,
                reviewContent,
                LocalDateTime.now(),
                reportPath
            );
            
            // 5. 发送通知
            sendNotification(result);
            
            logger.info("=== 代码评审完成 ===");
            logger.info("评审报告路径: {}", reportPath != null ? reportPath : "未保存");
            
            return result;
        } catch (Exception e) {
            logger.error("代码评审失败", e);
            throw e;
        }
    }

    /**
     * 获取代码变更（抽象方法）
     * 
     * @return 代码信息
     */
    protected abstract CodeInfo getCodeChanges();

    /**
     * 评审代码（抽象方法）
     * 
     * @param codeInfo 代码信息
     * @return 评审内容
     */
    protected abstract String reviewCode(CodeInfo codeInfo);

    /**
     * 保存报告（抽象方法）
     * 
     * @param codeInfo 代码信息
     * @param reviewContent 评审内容
     * @return 报告路径
     */
    protected abstract String saveReport(CodeInfo codeInfo, String reviewContent);

    /**
     * 发送通知（抽象方法）
     * 
     * @param result 评审结果
     */
    protected abstract void sendNotification(ReviewResult result);
}
