package org.ocr.com.sdk.api;

import org.ocr.com.sdk.application.CodeReviewApplicationService;
import org.ocr.com.sdk.application.DefaultReviewResultNotificationBuilder;
import org.ocr.com.sdk.application.ReviewResultNotificationBuilder;
import org.ocr.com.sdk.config.CodeReviewConfig;
import org.ocr.com.sdk.domain.model.ReviewResult;
import org.ocr.com.sdk.domain.port.CodeChangeSource;
import org.ocr.com.sdk.domain.port.CodeReviewApi;
import org.ocr.com.sdk.domain.port.ReviewReportRepository;
import org.ocr.com.sdk.domain.service.NotificationService;
import org.ocr.com.sdk.exception.CodeReviewException;
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
    private final CodeReviewApplicationService applicationService;

    public CodeReviewClient() {
        this(CodeReviewConfig.fromEnvironment());
    }

    public CodeReviewClient(CodeReviewConfig config) {
        this.config = config;
        this.applicationService = createApplicationService(config);
    }

    /**
     * 组装根：根据配置创建端口实现并组装应用服务
     */
    private static CodeReviewApplicationService createApplicationService(CodeReviewConfig config) {
        CodeChangeSource codeChangeSource = new GitRepository(config.getGitRepositoryPath());
        CodeReviewApi codeReviewApi = new HttpClient(config);
        ReviewReportRepository reviewReportRepository = new ReportStorage(config);
        List<NotificationService> notificationServices = NotificationServiceFactory.createServices(config);
        ReviewResultNotificationBuilder notificationBuilder = new DefaultReviewResultNotificationBuilder(config);

        return new CodeReviewApplicationService(
                codeChangeSource,
                codeReviewApi,
                reviewReportRepository,
                notificationServices,
                notificationBuilder
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

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 执行代码评审（委托给应用服务）
     *
     * @return 评审结果
     */
    public ReviewResult review() {
        logger.info("CodeReviewClient: 委托应用服务执行评审");
        return applicationService.execute();
    }

    public CodeReviewConfig getConfig() {
        return config;
    }

    public static class Builder {
        private final CodeReviewConfig.Builder configBuilder = CodeReviewConfig.builder();

        public Builder apiKey(String apiKey) {
            configBuilder.apiKey(apiKey);
            return this;
        }

        public Builder apiUrl(String apiUrl) {
            configBuilder.apiUrl(apiUrl);
            return this;
        }

        public Builder model(String model) {
            configBuilder.model(model);
            return this;
        }

        public Builder temperature(double temperature) {
            configBuilder.temperature(temperature);
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            configBuilder.maxTokens(maxTokens);
            return this;
        }

        public Builder reportBaseDir(String reportBaseDir) {
            configBuilder.reportBaseDir(reportBaseDir);
            return this;
        }

        public Builder gitRepositoryPath(String gitRepositoryPath) {
            configBuilder.gitRepositoryPath(gitRepositoryPath);
            return this;
        }

        public Builder wechatAppId(String wechatAppId) {
            configBuilder.wechatAppId(wechatAppId);
            return this;
        }

        public Builder wechatAppSecret(String wechatAppSecret) {
            configBuilder.wechatAppSecret(wechatAppSecret);
            return this;
        }

        public Builder wechatTemplateId(String wechatTemplateId) {
            configBuilder.wechatTemplateId(wechatTemplateId);
            return this;
        }

        public Builder wechatOpenId(String wechatOpenId) {
            configBuilder.wechatOpenId(wechatOpenId);
            return this;
        }

        public Builder wechatEnabled(boolean wechatEnabled) {
            configBuilder.wechatEnabled(wechatEnabled);
            return this;
        }

        public CodeReviewClient build() {
            return new CodeReviewClient(configBuilder.build());
        }
    }
}
