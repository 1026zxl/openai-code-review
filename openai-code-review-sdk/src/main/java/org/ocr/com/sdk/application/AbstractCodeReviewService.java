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
            System.out.println("========================================");
            System.out.println("=== 开始代码评审 ===");
            System.out.println("========================================");
            logger.info("=== 开始代码评审 ===");
            
            // 1. 获取代码变更
            System.out.println("[步骤 1/5] 正在获取代码变更...");
            CodeInfo codeInfo = getCodeChanges();
            codeInfo.validate();
            System.out.println("✓ 代码变更获取成功");
            System.out.println("  - 提交信息: " + codeInfo.getCommitMessage());
            System.out.println("  - 提交作者: " + codeInfo.getAuthorName());
            System.out.println("  - 变更统计: " + codeInfo.getChangeSummary());
            
            // 2. 评审代码
            System.out.println("[步骤 2/5] 正在调用AI进行代码评审...");
            String reviewContent = reviewCode(codeInfo);
            System.out.println("✓ AI评审完成");
            System.out.println("  - 评审内容长度: " + reviewContent.length() + " 字符");
            
            // 3. 保存报告
            System.out.println("[步骤 3/5] 正在保存评审报告...");
            String reportPath = saveReport(codeInfo, reviewContent);
            System.out.println("✓ 评审报告保存成功");
            System.out.println("  - 报告路径: " + (reportPath != null ? reportPath : "未保存"));
            
            // 4. 构建评审结果
            System.out.println("[步骤 4/5] 正在构建评审结果...");
            ReviewResult result = new ReviewResult(
                codeInfo,
                reviewContent,
                LocalDateTime.now(),
                reportPath
            );
            System.out.println("✓ 评审结果构建完成");
            
            // 5. 发送通知
            System.out.println("[步骤 5/5] 正在发送通知...");
            sendNotification(result);
            System.out.println("✓ 通知发送完成");
            
            System.out.println("========================================");
            System.out.println("=== 代码评审完成 ===");
            System.out.println("========================================");
            logger.info("=== 代码评审完成 ===");
            logger.info("评审报告路径: {}", reportPath != null ? reportPath : "未保存");
            
            return result;
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("=== 代码评审失败 ===");
            System.err.println("========================================");
            System.err.println("错误信息: " + e.getMessage());
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
