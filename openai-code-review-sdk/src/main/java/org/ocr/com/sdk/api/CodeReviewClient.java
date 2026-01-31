package org.ocr.com.sdk.api;

import org.ocr.com.sdk.config.CodeReviewConfig;
import org.ocr.com.sdk.domain.model.CodeInfo;
import org.ocr.com.sdk.domain.model.ReviewResult;
import org.ocr.com.sdk.domain.service.CodeReviewPromptTemplate;
import org.ocr.com.sdk.exception.CodeReviewException;
import org.ocr.com.sdk.exception.ErrorCode;
import org.ocr.com.sdk.domain.model.NotificationMessage;
import org.ocr.com.sdk.domain.service.NotificationMessageFactory;
import org.ocr.com.sdk.domain.service.NotificationService;
import org.ocr.com.sdk.infrastructure.git.GitRepository;
import org.ocr.com.sdk.infrastructure.http.HttpClient;
import org.ocr.com.sdk.infrastructure.notification.NotificationServiceFactory;
import org.ocr.com.sdk.infrastructure.storage.ReportStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 代码评审客户端
 * 提供Builder模式、静态工厂、构造函数三种创建方式
 * 
 * @author SDK Team
 * @since 1.0
 */
public class CodeReviewClient {
    
    private static final Logger logger = LoggerFactory.getLogger(CodeReviewClient.class);
    
    private final CodeReviewConfig config;
    private final GitRepository gitRepository;
    private final HttpClient httpClient;
    private final ReportStorage reportStorage;
    private final List<NotificationService> notificationServices;
    private final NotificationMessageFactory messageFactory;
    
    /**
     * 构造函数方式创建客户端
     * 使用默认配置（从环境变量和配置文件加载）
     */
    public CodeReviewClient() {
        this(CodeReviewConfig.fromEnvironment());
    }
    
    /**
     * 构造函数方式创建客户端
     * 
     * @param config 配置对象
     */
    public CodeReviewClient(CodeReviewConfig config) {
        this.config = config;
        this.gitRepository = new GitRepository(config.getGitRepositoryPath());
        this.httpClient = new HttpClient(config);
        this.reportStorage = new ReportStorage(config);
        this.notificationServices = NotificationServiceFactory.createServices(config);
        this.messageFactory = new NotificationMessageFactory(config);
    }
    
    /**
     * 静态工厂方法：从环境变量创建
     */
    public static CodeReviewClient create() {
        return new CodeReviewClient(CodeReviewConfig.fromEnvironment());
    }
    
    /**
     * 静态工厂方法：从配置文件创建
     */
    public static CodeReviewClient createFromProperties() {
        return new CodeReviewClient(CodeReviewConfig.fromProperties());
    }
    
    /**
     * 静态工厂方法：从指定配置文件创建
     */
    public static CodeReviewClient createFromProperties(String propertiesFile) {
        return new CodeReviewClient(CodeReviewConfig.fromProperties(propertiesFile));
    }
    
    /**
     * Builder模式创建客户端
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * 执行代码评审
     * 
     * @return 评审结果
     */
    public ReviewResult review() {
        logger.info("=== 开始代码评审 ===");
        
        try {
            // 1. 获取代码差异
            logger.info("Step 1: 获取代码差异...");
            CodeInfo codeInfo = gitRepository.getLatestDiff();
            
            if (codeInfo.isEmpty()) {
                logger.warn("未检测到代码变更，跳过评审");
                throw new CodeReviewException(ErrorCode.GIT_COMMIT_HISTORY_INSUFFICIENT.getCode(),
                        "未检测到代码变更");
            }
            
            // 2. 调用AI进行代码评审
            logger.info("Step 2: 调用AI进行代码评审...");
            String prompt = CodeReviewPromptTemplate.generatePrompt(codeInfo.getDiffContent());

            System.out.println("开始调用AI 进行代码评审... ..." );
            String reviewContent = httpClient.callAiApi(prompt);
            System.out.println("AI 代码评审结果: " + reviewContent);
            
            if (reviewContent == null || reviewContent.isEmpty()) {
                throw new CodeReviewException(ErrorCode.AI_API_RESPONSE_EMPTY.getCode(),
                        ErrorCode.AI_API_RESPONSE_EMPTY.getMessage());
            }
            
            // 3. 保存评审报告
            logger.info("Step 3: 保存评审报告...");
            System.out.println("保存评审报告... ...");
            String reportPath = reportStorage.saveReport(codeInfo, reviewContent);
            
            ReviewResult result = new ReviewResult(codeInfo, reviewContent, LocalDateTime.now(), reportPath);
            
            // 4. 发送通知消息（如果配置了）
            if (!notificationServices.isEmpty()) {
                logger.info("Step 4: 发送通知消息...");
                System.out.println("发送通知消息... ...");
                NotificationMessage message = messageFactory.createFromReviewResult(result);
                
                // 收集所有异步任务
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                
                for (NotificationService service : notificationServices) {
                    System.out.println("进入for循环准备发送消息... ...");
                    if (service.isEnabled()) {
                        System.out.println("进入if循环准备发送消息... ...");
                        // 创建异步任务并收集
                        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                            try {
                                service.send(message); // 使用同步方法，在异步线程中执行
                                System.out.println("通知消息发送成功");
                            } catch (NotificationService.NotificationException e) {
                                logger.error("发送通知消息失败: [{}] {}", e.getErrorCode(), e.getMessage(), e);
                                System.out.println("发送通知消息失败: " + e.getMessage());
                            }
                        });
                        futures.add(future);
                    }
                }
                
                // 等待所有异步任务完成（最多等待5秒）
                if (!futures.isEmpty()) {
                    try {
                        System.out.println("等待通知消息发送完成（最多5秒）...");
                        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                                .get(5, TimeUnit.SECONDS);
                        logger.info("所有通知消息发送完成");
                        System.out.println("所有通知消息发送完成");
                    } catch (java.util.concurrent.TimeoutException e) {
                        logger.warn("等待通知消息发送超时（5秒），继续执行");
                        System.out.println("等待通知消息发送超时（5秒），继续执行");
                    } catch (Exception e) {
                        logger.warn("等待通知消息发送时发生异常: {}", e.getMessage());
                        System.out.println("等待通知消息发送时发生异常: " + e.getMessage());
                    }
                }
            }
            
            logger.info("=== 代码评审完成 ===");
            return result;
            
        } catch (CodeReviewException e) {
            logger.error("代码评审失败: [{}] {}", e.getErrorCode(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("代码评审过程发生未知异常", e);
            throw new CodeReviewException(ErrorCode.UNKNOWN_ERROR.getCode(),
                    ErrorCode.UNKNOWN_ERROR.getMessage(), e);
        }
    }
    
    /**
     * 获取配置
     */
    public CodeReviewConfig getConfig() {
        return config;
    }
    
    /**
     * Builder类
     */
    public static class Builder {
        private CodeReviewConfig.Builder configBuilder = CodeReviewConfig.builder();
        
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
            CodeReviewConfig config = configBuilder.build();
            return new CodeReviewClient(config);
        }
    }
}

