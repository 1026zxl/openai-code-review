package org.ocr.com.sdk.infrastructure.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.ocr.com.sdk.config.CodeReviewConfig;
import org.ocr.com.sdk.exception.ApiException;
import org.ocr.com.sdk.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
    
    private final CodeReviewConfig config;
    private final ObjectMapper objectMapper;
    
    public HttpClient(CodeReviewConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 调用AI API进行代码评审
     * 
     * @param prompt 提示词
     * @return 评审结果内容
     */
    public String callAiApi(String prompt) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(config.getApiUrl());
            httpPost.setHeader("Authorization", "Bearer " + config.getApiKey());
            httpPost.setHeader("Content-Type", "application/json");
            
            // 构建请求体
            Map<String, Object> requestBody = buildRequestBody(prompt);
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            httpPost.setEntity(new StringEntity(requestBodyJson, "UTF-8"));
            
            logger.debug("HTTP Request: POST {}", config.getApiUrl());
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
                return parseResponse(responseBody);
            }
        } catch (IOException e) {
            throw new ApiException(ErrorCode.HTTP_REQUEST_FAILED, e);
        }
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
            
            if (!jsonNode.has("choices")) {
                throw new ApiException(ErrorCode.AI_API_RESPONSE_INVALID, "响应中缺少 choices 字段");
            }
            
            JsonNode choices = jsonNode.get("choices");
            if (!choices.isArray() || choices.size() == 0) {
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
}

