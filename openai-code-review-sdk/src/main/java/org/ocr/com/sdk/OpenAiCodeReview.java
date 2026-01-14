package org.ocr.com.sdk;

import org.ocr.com.sdk.api.CodeReviewClient;
import org.ocr.com.sdk.domain.model.ReviewResult;
import org.ocr.com.sdk.exception.CodeReviewException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenAI代码自动评审组件（命令行入口）
 * 
 * <p>此类作为命令行入口，适合在 GitHub Actions 等 CI/CD 环境中使用</p>
 * <p>通过 {@code java -jar} 方式执行时会调用此类的 main 方法</p>
 * 
 * <p>使用示例：</p>
 * <pre>{@code
 * // 命令行方式（推荐用于CI/CD）
 * java -jar openai-code-review-sdk-1.0-SNAPSHOT.jar
 * 
 * // 编程式调用（推荐用于其他项目集成）
 * CodeReviewClient client = CodeReviewClient.create();
 * ReviewResult result = client.review();
 * }</pre>
 * 
 * @author zhengxiaolong
 * @see CodeReviewClient
 */
public class OpenAiCodeReview {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAiCodeReview.class);
    
    /**
     * 主入口方法
     * 
     * @param args 命令行参数（当前未使用，保留用于未来扩展）
     */
    public static void main(String[] args) {
        // 测试日志输出
        System.out.println("=== 开始执行代码评审 ===");
        System.out.println("日志系统检查: " + (logger != null ? "正常" : "异常"));
        
        try {
            logger.info("=== OpenAI代码自动评审开始 ===");
            logger.debug("调试信息：日志系统已初始化");
            
            // 使用 CodeReviewClient 执行评审
            CodeReviewClient client = CodeReviewClient.create();
            ReviewResult result = client.review();
            
            logger.info("=== OpenAI代码自动评审完成 ===");
            logger.info("评审报告路径: {}", result.getReportPath());
            
        } catch (CodeReviewException e) {
            logger.error("代码评审失败: [{}] {}", e.getErrorCode(), e.getMessage(), e);
            System.err.println("代码评审失败: [" + e.getErrorCode() + "] " + e.getMessage());
            if (e.getCause() != null) {
                e.getCause().printStackTrace();
            }
            System.exit(1);
        } catch (Exception e) {
            logger.error("代码评审过程发生未知异常", e);
            System.err.println("代码评审失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}