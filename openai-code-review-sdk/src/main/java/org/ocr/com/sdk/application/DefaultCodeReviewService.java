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
        "你是资深技术专家，请对以下代码进行评审，发现问题并提供改进建议。\n" +
        "\n" +
        "代码变更：\n" +
        "%s\n" +
        "\n" +
        "评审维度（按高/中/低分级）：\n" +
        "1. 正确性：逻辑错误、边界条件、异常处理\n" +
        "2. 安全性：漏洞、资源管理、敏感信息\n" +
        "3. 性能：瓶颈、可扩展性\n" +
        "4. 可维护性：命名、结构、注释、规范\n" +
        "5. 可测试性：依赖、Mock难度\n" +
        "\n" +
        "输出格式：\n" +
        "## 代码评审报告\n" +
        "### 一、总结\n" +
        "* **整体评价：** （概述代码质量和主要问题）\n" +
        "* **问题统计：** 高（x） 中（y） 低（z）\n" +
        "### 二、详细问题\n" +
        "**【等级】** - **【类别】**：标题\n" +
        "* **位置：** `文件:行号`\n" +
        "* **问题：** 描述\n" +
        "* **建议：** 改进方案\n" +
        "### 三、优点\n" +
        "（代码亮点）\n" +
        "### 四、后续步骤\n" +
        "1. 必须修复：高等级问题\n" +
        "2. 建议优化：中等级问题\n" +
        "3. 可考虑：低等级问题";

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
