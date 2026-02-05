package org.ocr.com.sdk.application;

import org.ocr.com.sdk.domain.model.CodeInfo;
import org.ocr.com.sdk.domain.model.NotificationMessage;
import org.ocr.com.sdk.domain.model.ReviewResult;
import org.ocr.com.sdk.domain.port.CodeChangeSource;
import org.ocr.com.sdk.domain.port.CodeReviewApi;
import org.ocr.com.sdk.domain.port.ReviewReportRepository;
import org.ocr.com.sdk.domain.service.CodeReviewPromptTemplate;
import org.ocr.com.sdk.domain.service.NotificationService;
import org.ocr.com.sdk.exception.CodeReviewException;
import org.ocr.com.sdk.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 代码评审应用服务（DDD 应用层）
 * 编排用例：获取变更 → 生成 prompt → 调用 AI → 保存报告 → 发送通知。
 * 仅依赖领域端口与领域服务，不依赖具体基础设施实现。
 *
 * @author SDK Team
 * @since 1.0
 */
public class CodeReviewApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(CodeReviewApplicationService.class);
    private static final int NOTIFICATION_TIMEOUT_SECONDS = 5;

    private final CodeChangeSource codeChangeSource;
    private final CodeReviewApi codeReviewApi;
    private final ReviewReportRepository reviewReportRepository;
    private final List<NotificationService> notificationServices;
    private final ReviewResultNotificationBuilder notificationBuilder;

    public CodeReviewApplicationService(
            CodeChangeSource codeChangeSource,
            CodeReviewApi codeReviewApi,
            ReviewReportRepository reviewReportRepository,
            List<NotificationService> notificationServices,
            ReviewResultNotificationBuilder notificationBuilder) {
        this.codeChangeSource = codeChangeSource;
        this.codeReviewApi = codeReviewApi;
        this.reviewReportRepository = reviewReportRepository;
        this.notificationServices = notificationServices != null ? notificationServices : new ArrayList<>();
        this.notificationBuilder = notificationBuilder;
    }

    /**
     * 执行代码评审用例
     *
     * @return 评审结果
     */
    public ReviewResult execute() {
        logger.info("=== 开始代码评审 ===");

        CodeInfo codeInfo = codeChangeSource.getLatestDiff();
        if (codeInfo.isEmpty()) {
            logger.warn("未检测到代码变更，跳过评审");
            throw new CodeReviewException(ErrorCode.GIT_COMMIT_HISTORY_INSUFFICIENT.getCode(), "未检测到代码变更");
        }

        String prompt = CodeReviewPromptTemplate.generatePrompt(codeInfo.getDiffContent());
        String reviewContent = codeReviewApi.reviewByPrompt(prompt);
        if (reviewContent == null || reviewContent.isEmpty()) {
            throw new CodeReviewException(ErrorCode.AI_API_RESPONSE_EMPTY.getCode(),
                    ErrorCode.AI_API_RESPONSE_EMPTY.getMessage());
        }

        String reportPath = reviewReportRepository.save(codeInfo, reviewContent);
        ReviewResult result = new ReviewResult(codeInfo, reviewContent, LocalDateTime.now(), reportPath);

        sendNotifications(result);

        logger.info("=== 代码评审完成 ===");
        return result;
    }

    private void sendNotifications(ReviewResult result) {
        if (notificationServices.isEmpty()) {
            return;
        }
        logger.info("发送通知消息...");
        NotificationMessage message = notificationBuilder.build(result);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (NotificationService service : notificationServices) {
            if (service.isEnabled()) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        service.send(message);
                        logger.debug("通知消息发送成功");
                    } catch (NotificationService.NotificationException e) {
                        logger.error("发送通知消息失败: [{}] {}", e.getErrorCode(), e.getMessage(), e);
                    }
                });
                futures.add(future);
            }
        }

        if (!futures.isEmpty()) {
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .get(NOTIFICATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                logger.info("所有通知消息发送完成");
            } catch (java.util.concurrent.TimeoutException e) {
                logger.warn("等待通知消息发送超时（{}秒），继续执行", NOTIFICATION_TIMEOUT_SECONDS);
            } catch (Exception e) {
                logger.warn("等待通知消息发送时发生异常: {}", e.getMessage());
            }
        }
    }
}
