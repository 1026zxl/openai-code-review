package org.ocr.com.sdk.api;

import org.ocr.com.sdk.application.DefaultCodeReviewService;
import org.ocr.com.sdk.config.CodeReviewConfig;
import org.ocr.com.sdk.domain.model.ReviewResult;
import org.ocr.com.sdk.domain.port.CodeChangeSource;
import org.ocr.com.sdk.domain.port.CodeReviewApi;
import org.ocr.com.sdk.domain.port.ReviewReportRepository;
import org.ocr.com.sdk.domain.service.NotificationService;
import org.ocr.com.sdk.infrastructure.git.GitRepository;
import org.ocr.com.sdk.infrastructure.http.HttpClient;
import org.ocr.com.sdk.infrastructure.notification.NotificationServiceFactory;
import org.ocr.com.sdk.infrastructure.storage.ReportStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 代码评审客户端（门面 + 组装根）
 * 提供 Builder / 静态工厂等创建方式，内部按 DDD 组装应用服务与端口实现，并委托应用服务执行评审。
 *
 * @author SDK Team
 * @since 1.0
 */
public class CodeReviewClient {

    private static final Logger logger = LoggerFactory.getLogger(CodeReviewClient.class);

    private final CodeReviewConfig config;
    private final DefaultCodeReviewService codeReviewService;

    public CodeReviewClient() {
        this(CodeReviewConfig.fromEnvironment());
    }

    public CodeReviewClient(CodeReviewConfig config) {
        this.config = config;
        this.codeReviewService = createCodeReviewService(config);
    }

    /**
     * 组装根：创建端口实现并组装代码评审服务（DDD 分层）。
     * 基础设施层直接接受 CodeReviewConfig，简化配置管理。
     */
    private static DefaultCodeReviewService createCodeReviewService(CodeReviewConfig config) {
        CodeChangeSource codeChangeSource = new GitRepository(config);
        CodeReviewApi codeReviewApi = new HttpClient(config);
        ReviewReportRepository reviewReportRepository = new ReportStorage(config);
        List<NotificationService> notificationServices = NotificationServiceFactory.createServices(config);

        return new DefaultCodeReviewService(
                codeChangeSource,
                codeReviewApi,
                reviewReportRepository,
                notificationServices
        );
    }

    public static CodeReviewClient create() {
        return new CodeReviewClient(CodeReviewConfig.fromEnvironment());
    }

    public static CodeReviewClient createFromProperties() {
        return new CodeReviewClient(CodeReviewConfig.fromProperties());
    }

    public static CodeReviewClient createFromProperties(String propertiesFile) {
        return new CodeReviewClient(CodeReviewConfig.fromProperties(propertiesFile));
    }


    /**
     * 执行代码评审（委托给代码评审服务）
     *
     * @return 评审结果
     */
    public ReviewResult review() {
        logger.info("CodeReviewClient: 委托代码评审服务执行评审");
        return codeReviewService.execute();
    }

    public CodeReviewConfig getConfig() {
        return config;
    }

}
