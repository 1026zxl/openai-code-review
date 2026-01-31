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
import org.ocr.com.sdk.domain.model.ReviewResult;
import org.ocr.com.sdk.exception.ApiException;
import org.ocr.com.sdk.exception.CodeReviewException;
import org.ocr.com.sdk.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 微信公众号通知器
 * 负责向微信公众号发送代码评审结果
 * 
 * @author SDK Team
 * @since 1.0
 */
public class WeChatNotifier {
    
    private static final Logger logger = LoggerFactory.getLogger(WeChatNotifier.class);
    
    // 微信公众号API地址
    private static final String WECHAT_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String WECHAT_SEND_TEMPLATE_URL = "https://api.weixin.qq.com/cgi-bin/message/template/send";
    
    // access_token 缓存（有效期7200秒，提前5分钟刷新）
    private static final long TOKEN_EXPIRE_TIME = 7200 * 1000L; // 7200秒
    private static final long TOKEN_REFRESH_BUFFER = 5 * 60 * 1000L; // 5分钟缓冲
    
    private final CodeReviewConfig config;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    
    // access_token 缓存
    private volatile String cachedAccessToken;
    private volatile long tokenExpireTime;
    
    public WeChatNotifier(CodeReviewConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        // 使用单线程池执行异步推送任务
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "WeChatNotifier-Thread");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * 异步发送代码评审结果到微信公众号
     * 
     * @param reviewResult 评审结果
     */
    public void sendAsync(ReviewResult reviewResult) {
        if (!config.isWechatEnabled()) {
            logger.debug("微信公众号推送未启用，跳过");
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                send(reviewResult);
            } catch (Exception e) {
                logger.error("异步发送微信公众号消息失败", e);
            }
        }, executorService);
    }
    
    /**
     * 同步发送代码评审结果到微信公众号
     * 
     * @param reviewResult 评审结果
     */
    public void send(ReviewResult reviewResult) {
        if (!config.isWechatEnabled()) {
            logger.debug("微信公众号推送未启用，跳过");
            return;
        }
        
        try {
            // 1. 获取 access_token
            String accessToken = getAccessToken();
            
            // 2. 构建模板消息
            String templateMessage = buildTemplateMessage(reviewResult);
            
            // 3. 发送模板消息
            sendTemplateMessage(accessToken, templateMessage);
            
            logger.info("微信公众号消息发送成功");
            
        } catch (Exception e) {
            logger.error("发送微信公众号消息失败", e);
            // 不抛出异常，避免影响主流程
        }
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
     */
    private String buildTemplateMessage(ReviewResult reviewResult) throws IOException {
        Map<String, Object> message = new HashMap<>();
        message.put("touser", config.getWechatOpenId());
        message.put("template_id", config.getWechatTemplateId());
        
        // 构建报告链接（如果有）
        if (reviewResult.getReportPath() != null && !reviewResult.getReportPath().isEmpty()) {
            // 如果报告路径是GitHub路径，构建GitHub链接
            String reportUrl = buildReportUrl(reviewResult.getReportPath());
            if (reportUrl != null) {
                message.put("url", reportUrl);
            }
        }
        
        // 构建消息数据
        Map<String, Map<String, String>> data = new HashMap<>();
        
        // 提取评审内容摘要
        String summary = extractSummary(reviewResult.getReviewContent());
        
        // first: 标题
        data.put("first", createDataItem("代码评审完成通知", "#173177"));
        
        // keyword1: 提交信息
        String commitMessage = reviewResult.getCodeInfo().getCommitMessage();
        if (commitMessage.length() > 20) {
            commitMessage = commitMessage.substring(0, 20) + "...";
        }
        data.put("keyword1", createDataItem(commitMessage, "#173177"));
        
        // keyword2: 提交人
        data.put("keyword2", createDataItem(reviewResult.getCodeInfo().getAuthorName(), "#173177"));
        
        // keyword3: 评审时间
        String reviewTime = reviewResult.getReviewTime().toString().replace("T", " ");
        data.put("keyword3", createDataItem(reviewTime, "#173177"));
        
        // keyword4: 问题统计（从评审内容中提取）
        String issueStats = extractIssueStats(reviewResult.getReviewContent());
        data.put("keyword4", createDataItem(issueStats, "#FF0000"));
        
        // remark: 摘要
        if (summary.length() > 100) {
            summary = summary.substring(0, 100) + "...";
        }
        data.put("remark", createDataItem(summary, "#173177"));
        
        message.put("data", data);
        
        return objectMapper.writeValueAsString(message);
    }
    
    /**
     * 创建模板消息数据项
     */
    private Map<String, String> createDataItem(String value, String color) {
        Map<String, String> item = new HashMap<>();
        item.put("value", value);
        item.put("color", color);
        return item;
    }
    
    /**
     * 提取评审内容摘要
     */
    private String extractSummary(String reviewContent) {
        if (reviewContent == null || reviewContent.isEmpty()) {
            return "评审完成";
        }
        
        // 提取第一段作为摘要
        String[] lines = reviewContent.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.length() > 20 && !line.startsWith("#") && !line.startsWith("*")) {
                return line;
            }
        }
        
        // 如果没有合适的行，返回前100个字符
        return reviewContent.length() > 100 ? reviewContent.substring(0, 100) : reviewContent;
    }
    
    /**
     * 提取问题统计
     */
    private String extractIssueStats(String reviewContent) {
        if (reviewContent == null || reviewContent.isEmpty()) {
            return "无问题";
        }
        
        // 尝试从评审内容中提取问题数量
        // 格式：高（x） 中（y） 低（z）
        int highCount = 0, mediumCount = 0, lowCount = 0;
        
        // 简单的正则匹配
        String[] lines = reviewContent.split("\n");
        for (String line : lines) {
            if (line.contains("严重问题数量") || line.contains("问题数量")) {
                // 尝试提取数字，格式：高（x） 中（y） 低（z）
                // 使用正则表达式提取
                Pattern pattern = Pattern.compile("高[（(]\\s*(\\d+)\\s*[）)]|中[（(]\\s*(\\d+)\\s*[）)]|低[（(]\\s*(\\d+)\\s*[）)]");
                Matcher matcher = pattern.matcher(line);
                while (matcher.find()) {
                    if (matcher.group(1) != null) {
                        highCount = Integer.parseInt(matcher.group(1));
                    } else if (matcher.group(2) != null) {
                        mediumCount = Integer.parseInt(matcher.group(2));
                    } else if (matcher.group(3) != null) {
                        lowCount = Integer.parseInt(matcher.group(3));
                    }
                }
                break;
            }
        }
        
        if (highCount == 0 && mediumCount == 0 && lowCount == 0) {
            return "查看详情";
        }
        
        return String.format("高:%d 中:%d 低:%d", highCount, mediumCount, lowCount);
    }
    
    /**
     * 构建报告URL
     */
    private String buildReportUrl(String reportPath) {
        // 如果报告路径是GitHub路径，构建GitHub链接
        if (config.getGithubRepoUrl() != null && !config.getGithubRepoUrl().isEmpty()) {
            // 从 git URL 提取仓库信息
            // 例如: https://github.com/user/repo.git -> https://github.com/user/repo/blob/main/path
            String repoUrl = config.getGithubRepoUrl();
            if (repoUrl.endsWith(".git")) {
                repoUrl = repoUrl.substring(0, repoUrl.length() - 4);
            }
            return repoUrl + "/blob/main/" + reportPath;
        }
        return null;
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
