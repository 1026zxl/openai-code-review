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
        System.out.println("========================================");
        System.out.println("OpenAI Code Review SDK");
        System.out.println("========================================");
        System.out.println("开始初始化代码评审客户端...");
        
        try {
            // 使用 CodeReviewClient 执行评审
            CodeReviewClient client = CodeReviewClient.create();
            System.out.println("✓ 代码评审客户端初始化成功");
            System.out.println();
            
            ReviewResult result = client.review();
            
            System.out.println();
            System.out.println("========================================");
            System.out.println("代码评审执行成功！");
            System.out.println("========================================");
            
        } catch (CodeReviewException e) {
            System.err.println("========================================");
            System.err.println("代码评审失败: [" + e.getErrorCode() + "] " + e.getMessage());
            System.err.println("========================================");
            if (e.getCause() != null) {
                e.getCause().printStackTrace();
            }
            logger.error("代码评审失败", e);
            System.exit(1);
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("代码评审过程发生未知异常: " + e.getMessage());
            System.err.println("========================================");
            logger.error("代码评审过程发生未知异常", e);
            e.printStackTrace();
            System.exit(1);
        }
    }
}