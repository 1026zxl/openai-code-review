package org.ocr.com.sdk.application;

import org.ocr.com.sdk.domain.model.CodeInfo;
import org.ocr.com.sdk.domain.model.NotificationMessage;
import org.ocr.com.sdk.domain.model.ReviewResult;
import org.ocr.com.sdk.domain.port.CodeChangeSource;
import org.ocr.com.sdk.domain.port.CodeReviewApi;
import org.ocr.com.sdk.domain.port.ReviewReportRepository;
import org.ocr.com.sdk.domain.service.NotificationService;
import org.ocr.com.sdk.exception.CodeReviewException;
import org.ocr.com.sdk.exception.ErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 默认代码评审服务实现
 * 实现具体的代码评审步骤
 * 
 * @author SDK Team
 * @since 1.0
 */
public class DefaultCodeReviewService extends AbstractCodeReviewService {

    private static final int NOTIFICATION_TIMEOUT_SECONDS = 5;
    
    private static final String PROMPT_TEMPLATE = 
        "角色设定：\n" +
        "请你担任一名经验丰富的资深技术专家，负责对提供的代码进行严格、细致、富有建设性的评审。你的目标是发现潜在问题、提升代码质量、确保最佳实践，并提供具体、可操作的改进建议。\n" +
        "\n" +
        "我的请求：\n" +
        "请对以下代码进行全面的代码评审。\n" +
        "\n" +
        "代码信息：\n" +
        "%s\n" +
        "\n" +
        "评审方向与重点：\n" +
        "请从以下维度进行评审，并对发现的问题按严重程度（高/中/低）进行分级：\n" +
        "\n" +
        "技术正确性与逻辑：\n" +
        "- 代码是否能正确实现所述功能？是否存在边界条件、极端情况或并发场景下的逻辑错误？\n" +
        "- 算法或逻辑是否高效、清晰？是否有更优的实现方式？\n" +
        "- 输入验证、错误处理是否完备？（例如：空值、无效参数、异常捕获与处理）\n" +
        "\n" +
        "安全性与可靠性：\n" +
        "- 是否存在安全漏洞？（例如：SQL注入、XSS、CSRF、命令注入、不安全的反序列化、敏感信息泄露）\n" +
        "- 资源管理是否得当？（例如：数据库连接、文件句柄、网络连接是否确保被释放？是否存在内存泄漏风险？）\n" +
        "- 是否有适当的日志记录和监控点？\n" +
        "\n" +
        "性能与可扩展性：\n" +
        "- 是否存在明显的性能瓶颈？（例如：循环内的重复计算、N+1查询、未使用索引、低效的数据结构）\n" +
        "- 代码是否能良好地应对数据量或负载的增长？\n" +
        "\n" +
        "代码风格与可维护性：\n" +
        "- 是否符合该语言的通用编码规范？\n" +
        "  命名（变量、函数、类）是否清晰、达意？\n" +
        "- 函数/方法是否过于庞大，职责是否单一？是否需要重构（如提取函数、简化条件表达式）？\n" +
        "- 代码注释是否充分且有意义？是否解释了\"为什么\"而不是\"是什么\"？（对于复杂的业务逻辑或算法尤其重要）\n" +
        "  魔术数字是否被提取为常量或配置？\n" +
        "\n" +
        "可测试性：\n" +
        "- 代码是否便于编写单元测试或集成测试？（例如：是否依赖全局状态、难以Mock的硬编码、函数副作用过多）\n" +
        "\n" +
        "输出格式要求：\n" +
        "请按照以下结构化格式输出你的评审报告：\n" +
        "\n" +
        "## 代码评审报告\n" +
        "\n" +
        "### 一、总结\n" +
        "*   **整体评价：** （简要概述代码质量，指出最突出的优点和最重要的问题）\n" +
        "*   **严重问题数量：** 高（x） 中（y） 低（z）\n" +
        "\n" +
        "### 二、详细问题与建议\n" +
        "请使用以下模板对每个发现的问题进行描述：\n" +
        "\n" +
        "**【严重等级】** - **【问题类别】**： 简短的问题标题\n" +
        "*   **位置：** `文件名:行号` （或函数名）\n" +
        "*   **问题描述：** 具体解释哪里有问题，以及可能导致什么后果。\n" +
        "*   **改进建议：** 提供具体的修改方案、示例代码或最佳实践参考。\n" +
        "*   **理由：** 解释为何这样修改更好。\n" +
        "\n" +
        "（示例）\n" +
        "**【高】** - **安全性**： 存在潜在的SQL注入风险\n" +
        "*   **位置：** `user_service.py:45` （`get_user`函数）\n" +
        "*   **问题描述：** 直接使用字符串拼接构造SQL查询语句，如果`user_id`来自不可信输入，可能导致SQL注入攻击。\n" +
        "*   **改进建议：** 使用参数化查询或ORM提供的方法。例如，将`cursor.execute(f\"SELECT * FROM users WHERE id = {user_id}\")` 改为 `cursor.execute(\"SELECT * FROM users WHERE id = %s\", (user_id,))`。\n" +
        "*   **理由：** 参数化查询能确保输入被安全地转义，是防止SQL注入的标准做法。\n" +
        "\n" +
        "### 三、正面评价与优点\n" +
        "*   （列出代码中做得好的地方，例如：清晰的模块划分、巧妙的算法设计、完善的错误处理等）\n" +
        "\n" +
        "### 四、综合建议与后续步骤\n" +
        "1.  **必须优先修复：** （列出所有\"高\"等级问题）\n" +
        "2.  **建议近期优化：** （列出所有\"中\"等级问题）\n" +
        "3.  **可考虑重构：** （列出所有\"低\"等级问题或代码异味）";

    private final CodeChangeSource codeChangeSource;
    private final CodeReviewApi codeReviewApi;
    private final ReviewReportRepository reviewReportRepository;
    private final List<NotificationService> notificationServices;

    public DefaultCodeReviewService(
            CodeChangeSource codeChangeSource,
            CodeReviewApi codeReviewApi,
            ReviewReportRepository reviewReportRepository,
            List<NotificationService> notificationServices) {
        this.codeChangeSource = codeChangeSource;
        this.codeReviewApi = codeReviewApi;
        this.reviewReportRepository = reviewReportRepository;
        this.notificationServices = notificationServices != null ? notificationServices : new ArrayList<>();
    }

    @Override
    protected CodeInfo getCodeChanges() {
        System.out.println("  正在从Git仓库获取最新代码变更...");
        CodeInfo codeInfo = codeChangeSource.getLatestDiff();
        return codeInfo;
    }

    @Override
    protected String reviewCode(CodeInfo codeInfo) {
        // 生成提示词
        System.out.println("  正在生成AI评审提示词...");
        String prompt = generatePrompt(codeInfo.getDiffContent());
        System.out.println("  提示词生成完成，长度: " + prompt.length() + " 字符");
        logger.debug("生成提示词，长度: {}", prompt.length());
        
        // 调用AI进行评审
        System.out.println("  正在调用AI接口进行评审（可能需要一些时间）...");
        String reviewContent = codeReviewApi.reviewByPrompt(prompt);
        if (reviewContent == null || reviewContent.trim().isEmpty()) {
            System.err.println("  ✗ AI返回的评审内容为空");
            throw new CodeReviewException(ErrorCode.AI_API_RESPONSE_EMPTY.getCode(),
                    ErrorCode.AI_API_RESPONSE_EMPTY.getMessage());
        }
        
        return reviewContent;
    }

    @Override
    protected String saveReport(CodeInfo codeInfo, String reviewContent) {
        System.out.println("  正在保存评审报告到GitHub仓库...");
        String reportPath = reviewReportRepository.save(codeInfo, reviewContent);
        return reportPath;
    }

    @Override
    protected void sendNotification(ReviewResult result) {
        if (notificationServices.isEmpty()) {
            System.out.println("  未配置通知服务，跳过通知");
            logger.debug("未配置通知服务，跳过通知");
            return;
        }
        
        System.out.println("  正在构建通知消息...");
        logger.info("发送通知消息...");
        NotificationMessage message = NotificationMessage.fromReviewResult(result);
        
        System.out.println("  配置的通知服务数量: " + notificationServices.size());

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        int enabledCount = 0;
        for (NotificationService service : notificationServices) {
            if (service.isEnabled()) {
                enabledCount++;
                String serviceName = service.getClass().getSimpleName();
                System.out.println("  正在发送通知到: " + serviceName);
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        service.send(message);
                        System.out.println("  ✓ " + serviceName + " 通知发送成功");
                        logger.debug("通知消息发送成功: {}", serviceName);
                    } catch (NotificationService.NotificationException e) {
                        System.err.println("  ✗ " + serviceName + " 通知发送失败: [" + e.getErrorCode() + "] " + e.getMessage());
                        logger.error("发送通知消息失败: [{}] {}", e.getErrorCode(), e.getMessage(), e);
                    }
                });
                futures.add(future);
            }
        }
        
        if (enabledCount == 0) {
            System.out.println("  没有启用的通知服务");
            return;
        }

        if (!futures.isEmpty()) {
            try {
                System.out.println("  等待通知发送完成（最多等待 " + NOTIFICATION_TIMEOUT_SECONDS + " 秒）...");
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .get(NOTIFICATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                System.out.println("  ✓ 所有通知消息发送完成");
                logger.info("所有通知消息发送完成");
            } catch (java.util.concurrent.TimeoutException e) {
                System.out.println("  ⚠ 等待通知消息发送超时（" + NOTIFICATION_TIMEOUT_SECONDS + "秒），继续执行");
                logger.warn("等待通知消息发送超时（{}秒），继续执行", NOTIFICATION_TIMEOUT_SECONDS);
            } catch (Exception e) {
                System.out.println("  ⚠ 等待通知消息发送时发生异常: " + e.getMessage());
                logger.warn("等待通知消息发送时发生异常: {}", e.getMessage());
            }
        }
    }

    /**
     * 生成提示词
     */
    private String generatePrompt(String diffContent) {
        if (diffContent == null || diffContent.trim().isEmpty()) {
            throw new IllegalArgumentException("代码差异内容不能为空");
        }
        return PROMPT_TEMPLATE.replace("%s", diffContent);
    }
}
