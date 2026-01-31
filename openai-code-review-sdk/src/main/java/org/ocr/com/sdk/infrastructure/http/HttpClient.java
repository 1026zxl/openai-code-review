package org.ocr.com.sdk.infrastructure.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.ocr.com.sdk.config.CodeReviewConfig;
import org.ocr.com.sdk.exception.ApiException;
import org.ocr.com.sdk.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP客户端基础设施
 * 
 * @author SDK Team
 * @since 1.0
 */
public class HttpClient {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpClient.class);
    
    // 默认配置
    private static final int DEFAULT_CONNECT_TIMEOUT = 10000; // 10秒连接超时
    private static final int DEFAULT_SOCKET_TIMEOUT = 30000;  // 30秒读取超时
    private static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = 10000; // 10秒请求超时
    private static final int DEFAULT_MAX_RETRIES = 3; // 默认最大重试3次
    
    private final CodeReviewConfig config;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;
    
    public HttpClient(CodeReviewConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.httpClient = createHttpClient();
    }
    
    /**
     * 创建配置了重试和超时的HttpClient
     */
    private CloseableHttpClient createHttpClient() {
        // 配置超时
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
                .setSocketTimeout(DEFAULT_SOCKET_TIMEOUT)
                .setConnectionRequestTimeout(DEFAULT_CONNECTION_REQUEST_TIMEOUT)
                .build();
        
        // 配置重试策略
        HttpRequestRetryHandler retryHandler = new HttpRequestRetryHandler() {
            @Override
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                if (executionCount > DEFAULT_MAX_RETRIES) {
                    logger.warn("达到最大重试次数 {}，停止重试", DEFAULT_MAX_RETRIES);
                    return false;
                }
                
                // 如果是中断异常，不重试
                if (exception instanceof InterruptedIOException) {
                    logger.warn("请求被中断，不重试");
                    return false;
                }
                
                // 如果是未知主机异常，不重试
                if (exception instanceof UnknownHostException) {
                    logger.warn("未知主机，不重试");
                    return false;
                }
                
                // 如果是SSL握手异常，重试
                if (exception instanceof SSLException) {
                    logger.warn("SSL握手异常，第 {} 次重试: {}", executionCount, exception.getMessage());
                    return true;
                }


                // 如果是连接重置，重试
                if (exception instanceof SocketException) {
                    String message = exception.getMessage();
                    if (message != null && (message.contains("Connection reset") || 
                                             message.contains("Broken pipe"))) {
                        logger.warn("连接重置，第 {} 次重试: {}", executionCount, exception.getMessage());
                        return true;
                    }
                }
                
                // 检查上下文，判断请求是否已经发送
                HttpClientContext clientContext = HttpClientContext.adapt(context);
                HttpRequest request = clientContext.getRequest();
                boolean idempotent = !(request instanceof HttpPost) || 
                                    ((HttpPost) request).getEntity() == null ||
                                    ((HttpPost) request).getEntity().isRepeatable();
                
                if (idempotent) {
                    logger.warn("网络异常，第 {} 次重试: {}", executionCount, exception.getMessage());
                    return true;
                }
                
                return false;
            }
        };
        
        return HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .setRetryHandler(retryHandler)
                .build();
    }
    
    /**
     * 调用AI API进行代码评审
     * 带重试机制和指数退避
     * 
     * @param prompt 提示词
     * @return 评审结果内容
     */
    public String callAiApi(String prompt) {
        return callAiApiWithRetry(prompt, DEFAULT_MAX_RETRIES);
    }
    
    /**
     * 带重试机制的API调用
     * 
     * @param prompt 提示词
     * @param maxRetries 最大重试次数
     * @return 评审结果内容
     */
    private String callAiApiWithRetry(String prompt, int maxRetries) {
        int retryCount = 0;
        long baseDelay = 1000; // 基础延迟1秒
        
        while (retryCount <= maxRetries) {
            try {
                HttpPost httpPost = new HttpPost(config.getApiUrl());
                httpPost.setHeader("Authorization", "Bearer " + config.getApiKey());
                httpPost.setHeader("Content-Type", "application/json");
                
                // 构建请求体
                Map<String, Object> requestBody = buildRequestBody(prompt);
                String requestBodyJson = objectMapper.writeValueAsString(requestBody);
                httpPost.setEntity(new StringEntity(requestBodyJson, "UTF-8"));
                
                if (retryCount > 0) {
                    logger.info("第 {} 次重试请求: POST {}", retryCount, config.getApiUrl());
                } else {
                    logger.debug("HTTP Request: POST {}", config.getApiUrl());
                }
                
                long startTime = System.currentTimeMillis();
                
                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    long endTime = System.currentTimeMillis();
                    logger.debug("HTTP Response: {} ({}ms)", statusCode, endTime - startTime);
                    
                    if (statusCode != 200) {
                        String errorBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                        throw new ApiException(ErrorCode.AI_API_CALL_FAILED, statusCode, errorBody);
                    }
                    
                    String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                    String result = parseResponse(responseBody);
                    
                    if (retryCount > 0) {
                        logger.info("重试成功，在第 {} 次重试后获得响应", retryCount);
                    }
                    
                    return result;
                }
            } catch (ApiException e) {
                // API异常（如状态码错误），不重试
                throw e;
            } catch (IOException e) {
                retryCount++;
                
                if (retryCount > maxRetries) {
                    logger.error("达到最大重试次数 {}，请求失败", maxRetries);
                    throw new ApiException(ErrorCode.HTTP_REQUEST_FAILED, 0,
                            "请求失败，已重试 " + maxRetries + " 次: " + e.getMessage(), e);
                }
                
                // 计算指数退避延迟：1s, 2s, 4s...
                long delay = baseDelay * (1L << (retryCount - 1));
                logger.warn("请求失败，{}ms 后进行第 {} 次重试: {}", delay, retryCount, e.getMessage());
                
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    // 中断异常不应该重试，直接抛出
                    throw new ApiException(ErrorCode.HTTP_REQUEST_FAILED, ie);
                }
            }
        }
        
        throw new ApiException(ErrorCode.HTTP_REQUEST_FAILED, 
                "请求失败，已重试 " + maxRetries + " 次");
    }
    
    /**
     * 构建请求体
     */
    private Map<String, Object> buildRequestBody(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getModel());
        
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message);
        requestBody.put("messages", messages);
        requestBody.put("temperature", config.getTemperature());
        requestBody.put("max_tokens", config.getMaxTokens());
        
        return requestBody;
    }
    
    /**
     * 解析响应
     */
    private String parseResponse(String responseBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            logger.info("响应内容: {}", jsonNode);
            if (!jsonNode.has("choices")) {
                throw new ApiException(ErrorCode.AI_API_RESPONSE_INVALID, "响应中缺少 choices 字段");
            }
            
            JsonNode choices = jsonNode.get("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new ApiException(ErrorCode.AI_API_RESPONSE_EMPTY);
            }
            
            JsonNode choice = choices.get(0);
            if (!choice.has("message")) {
                throw new ApiException(ErrorCode.AI_API_RESPONSE_INVALID, "响应中缺少 message 字段");
            }
            
            JsonNode message = choice.get("message");
            if (!message.has("content")) {
                throw new ApiException(ErrorCode.AI_API_RESPONSE_INVALID, "响应中缺少 content 字段");
            }
            
            String content = message.get("content").asText();
            if (content == null || content.isEmpty()) {
                throw new ApiException(ErrorCode.AI_API_RESPONSE_EMPTY);
            }
            
            return content;
        } catch (IOException e) {
            throw new ApiException(ErrorCode.HTTP_RESPONSE_PARSE_ERROR, e);
        }
    }
    
    /**
     * 关闭HttpClient资源
     * 注意：HttpClient是共享的，通常不需要手动关闭
     * 如果需要关闭，可以调用此方法
     */
    public void close() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (IOException e) {
            logger.warn("关闭HttpClient失败", e);
        }
    }
}

