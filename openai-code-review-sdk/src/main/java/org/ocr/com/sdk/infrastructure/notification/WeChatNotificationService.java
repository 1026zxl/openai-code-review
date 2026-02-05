package org.ocr.com.sdk.infrastructure.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.ocr.com.sdk.config.CodeReviewConfig;
import org.ocr.com.sdk.domain.model.NotificationMessage;
import org.ocr.com.sdk.domain.service.NotificationService;
import org.ocr.com.sdk.exception.ApiException;
import org.ocr.com.sdk.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 微信公众号通知服务实现
 * 实现 NotificationService 接口，负责向微信公众号发送通知消息
 * 
 * @author SDK Team
 * @since 1.0
 */
public class WeChatNotificationService implements NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(WeChatNotificationService.class);
    
    // 微信公众号API地址
    private static final String WECHAT_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String WECHAT_SEND_TEMPLATE_URL = "https://api.weixin.qq.com/cgi-bin/message/template/send";
    
    // access_token 缓存（有效期7200秒，提前5分钟刷新）
    private static final long TOKEN_REFRESH_BUFFER = 5 * 60 * 1000L; // 5分钟缓冲
    
    private final CodeReviewConfig config;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    
    // access_token 缓存
    private volatile String cachedAccessToken;
    private volatile long tokenExpireTime;
    
    public WeChatNotificationService(CodeReviewConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        // 使用单线程池执行异步推送任务
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "WeChatNotificationService-Thread");
            t.setDaemon(true);
            return t;
        });
    }
    
    @Override
    public void send(NotificationMessage message) throws NotificationException {
        if (!isEnabled()) {
            logger.debug("微信公众号推送未启用，跳过");
            return;
        }
        
        try {
            // 1. 获取 access_token
            String accessToken = getAccessToken();
            
            // 2. 构建模板消息
            String templateMessage = buildTemplateMessage(message);
            
            // 3. 发送模板消息
            sendTemplateMessage(accessToken, templateMessage);
            
            logger.info("微信公众号消息发送成功");
            
        } catch (ApiException e) {
            logger.error("发送微信公众号消息失败", e);
            throw new NotificationException(e.getErrorCode(), e.getMessage(), e);
        } catch (Exception e) {
            logger.error("发送微信公众号消息失败", e);
            throw new NotificationException(ErrorCode.WECHAT_SEND_MESSAGE_FAILED.getCode(),
                    "发送微信公众号消息失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void sendAsync(NotificationMessage message) {
        if (!isEnabled()) {
            logger.debug("微信公众号推送未启用，跳过");
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                send(message);
            } catch (NotificationException e) {
                logger.error("异步发送微信公众号消息失败: [{}] {}", e.getErrorCode(), e.getMessage(), e);
            }
        }, executorService);
    }
    
    @Override
    public boolean isEnabled() {
        return config.isWechatEnabled();
    }
    
    /**
     * 获取 access_token（带缓存）
     */
    private String getAccessToken() {
        long currentTime = System.currentTimeMillis();
        
        // 检查缓存是否有效
        if (cachedAccessToken != null && currentTime < tokenExpireTime) {
            logger.debug("使用缓存的 access_token");
            return cachedAccessToken;
        }
        
        // 重新获取 access_token
        logger.info("获取微信公众号 access_token...");
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = String.format("%s?grant_type=client_credential&appid=%s&secret=%s",
                    WECHAT_TOKEN_URL, config.getWechatAppId(), config.getWechatAppSecret());
            
            HttpGet httpGet = new HttpGet(url);
            
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    throw new ApiException(ErrorCode.WECHAT_ACCESS_TOKEN_FAILED, statusCode,
                            "HTTP状态码: " + statusCode);
                }
                
                String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                
                // 检查是否有错误
                if (jsonNode.has("errcode")) {
                    int errcode = jsonNode.get("errcode").asInt();
                    String errmsg = jsonNode.has("errmsg") ? jsonNode.get("errmsg").asText() : "未知错误";
                    throw new ApiException(ErrorCode.WECHAT_ACCESS_TOKEN_FAILED,
                            "获取 access_token 失败: [" + errcode + "] " + errmsg);
                }
                
                if (!jsonNode.has("access_token")) {
                    throw new ApiException(ErrorCode.WECHAT_ACCESS_TOKEN_FAILED,
                            "响应中缺少 access_token 字段");
                }
                
                String accessToken = jsonNode.get("access_token").asText();
                int expiresIn = jsonNode.has("expires_in") ? jsonNode.get("expires_in").asInt() : 7200;
                
                // 更新缓存（提前5分钟刷新）
                cachedAccessToken = accessToken;
                tokenExpireTime = currentTime + (expiresIn * 1000L) - TOKEN_REFRESH_BUFFER;
                
                logger.info("access_token 获取成功，有效期: {}秒", expiresIn);
                return accessToken;
            }
        } catch (IOException e) {
            throw new ApiException(ErrorCode.WECHAT_ACCESS_TOKEN_FAILED, e);
        }
    }
    
    /**
     * 构建模板消息
     * 参考参考项目的实现方式，使用模板键枚举
     */
    private String buildTemplateMessage(NotificationMessage message) throws IOException {
        Map<String, Object> template = new HashMap<>();
        template.put("touser", config.getWechatOpenId());
        template.put("template_id", config.getWechatTemplateId());
        
        // 构建报告链接：优先使用消息中的 linkUrl，否则根据配置拼接报告地址
        String linkUrl = message.getLinkUrl();
        if ((linkUrl == null || linkUrl.isEmpty()) && config.getGithubRepoUrl() != null) {
            String reportPath = message.getMetadata("reportPath");
            if (reportPath != null && !reportPath.isEmpty()) {
                String repoUrl = config.getGithubRepoUrl();
                if (repoUrl.endsWith(".git")) {
                    repoUrl = repoUrl.substring(0, repoUrl.length() - 4);
                }
                linkUrl = repoUrl + "/blob/main/" + reportPath;
            }
        }
        if (linkUrl != null && !linkUrl.isEmpty()) {
            template.put("url", linkUrl);
        }
        
        // 构建消息数据（参考参考项目的实现方式）
        Map<String, Map<String, String>> data = new HashMap<>();
        
        // 统一从环境变量获取信息（GitHub Actions 工作流脚本中已设置）
        String repoName = getEnvOrDefault("REPO_NAME", extractRepoName(config.getGithubRepoUrl()));
        String branchName = getEnvOrDefault("BRANCH_NAME", getEnvOrDefault("GITHUB_REF", "未知"));
        String commitAuthor = getEnvOrDefault("COMMIT_AUTHOR", message.getMetadata("authorName"));
        String commitMessage = getEnvOrDefault("COMMIT_MESSAGE", message.getMetadata("commitMessage"));
        
        // 处理 GITHUB_REF 格式（refs/heads/branch-name）
        if (branchName.startsWith("refs/heads/")) {
            branchName = branchName.substring(11);
        }
        
        // 使用模板键构建数据（参考参考项目的 TemplateKey 枚举）
        putTemplateData(data, TemplateKey.REPO_NAME, repoName);
        putTemplateData(data, TemplateKey.BRANCH_NAME, branchName);
        putTemplateData(data, TemplateKey.COMMIT_AUTHOR, commitAuthor);
        putTemplateData(data, TemplateKey.COMMIT_MESSAGE, commitMessage);
        
        template.put("data", data);
        
        return objectMapper.writeValueAsString(template);
    }
    
    /**
     * 模板键枚举（参考参考项目的实现）
     */
    private enum TemplateKey {
        REPO_NAME("repo_name"),
        BRANCH_NAME("branch_name"),
        COMMIT_AUTHOR("commit_author"),
        COMMIT_MESSAGE("commit_message");
        
        private final String code;
        
        TemplateKey(String code) {
            this.code = code;
        }
        
        public String getCode() {
            return code;
        }
    }
    
    /**
     * 添加模板数据（参考参考项目的 TemplateMessageDTO.put 方法）
     */
    private void putTemplateData(Map<String, Map<String, String>> data, TemplateKey key, String value) {
        if (value == null) {
            value = "未知";
        }
        Map<String, String> item = new HashMap<>();
        item.put("value", value);
        item.put("color", "#173177");
        data.put(key.getCode(), item);
    }
    
    /**
     * 从环境变量获取值，如果不存在则使用默认值
     */
    private String getEnvOrDefault(String envKey, String defaultValue) {
        String value = System.getenv(envKey);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        return defaultValue != null ? defaultValue : "未知";
    }
    
    /**
     * 从 GitHub 仓库 URL 提取仓库名称（备用方案）
     */
    private String extractRepoName(String githubRepoUrl) {
        if (githubRepoUrl == null || githubRepoUrl.isEmpty()) {
            return "未知";
        }
        
        // 移除 .git 后缀
        String repoUrl = githubRepoUrl;
        if (repoUrl.endsWith(".git")) {
            repoUrl = repoUrl.substring(0, repoUrl.length() - 4);
        }
        
        // 提取仓库名（最后一个 / 之后的部分）
        int lastSlash = repoUrl.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < repoUrl.length() - 1) {
            return repoUrl.substring(lastSlash + 1);
        }
        
        return "未知";
    }
    
    
    /**
     * 发送模板消息
     */
    private void sendTemplateMessage(String accessToken, String templateMessage) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = WECHAT_SEND_TEMPLATE_URL + "?access_token=" + accessToken;
            
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(templateMessage, "UTF-8"));
            
            logger.debug("发送微信公众号模板消息: {}", templateMessage);
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    throw new ApiException(ErrorCode.WECHAT_SEND_MESSAGE_FAILED, statusCode,
                            "HTTP状态码: " + statusCode);
                }
                
                String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                
                // 检查是否有错误
                if (jsonNode.has("errcode")) {
                    int errcode = jsonNode.get("errcode").asInt();
                    if (errcode != 0) {
                        String errmsg = jsonNode.has("errmsg") ? jsonNode.get("errmsg").asText() : "未知错误";
                        throw new ApiException(ErrorCode.WECHAT_SEND_MESSAGE_FAILED,
                                "发送模板消息失败: [" + errcode + "] " + errmsg);
                    }
                }
                
                logger.info("微信公众号模板消息发送成功");
            }
        }
    }
    
    /**
     * 关闭资源
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
