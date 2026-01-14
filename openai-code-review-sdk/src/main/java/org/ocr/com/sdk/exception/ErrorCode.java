package org.ocr.com.sdk.exception;

/**
 * 错误码定义
 * 
 * @author SDK Team
 * @since 1.0
 */
public enum ErrorCode {
    
    // 配置相关错误 (1000-1999)
    CONFIG_API_KEY_MISSING("1001", "API密钥未配置，请设置环境变量 OPENAI_API_KEY 或在配置中指定"),
    CONFIG_API_KEY_INVALID("1002", "API密钥格式无效"),
    CONFIG_API_URL_INVALID("1003", "API地址配置无效"),
    CONFIG_MODEL_INVALID("1004", "模型名称配置无效"),
    
    // Git相关错误 (2000-2999)
    GIT_REPOSITORY_NOT_FOUND("2001", "未找到Git仓库，请确保在Git仓库目录下运行"),
    GIT_COMMIT_HISTORY_INSUFFICIENT("2002", "提交历史不足，无法进行代码对比"),
    GIT_DIFF_FAILED("2003", "获取代码差异失败"),
    GIT_OPERATION_FAILED("2004", "Git操作失败"),
    
    // HTTP相关错误 (3000-3999)
    HTTP_REQUEST_FAILED("3001", "HTTP请求失败"),
    HTTP_RESPONSE_ERROR("3002", "HTTP响应错误"),
    HTTP_TIMEOUT("3003", "HTTP请求超时"),
    HTTP_RESPONSE_PARSE_ERROR("3004", "HTTP响应解析失败"),
    
    // AI API相关错误 (4000-4999)
    AI_API_CALL_FAILED("4001", "AI API调用失败"),
    AI_API_RESPONSE_INVALID("4002", "AI API响应格式无效"),
    AI_API_RESPONSE_EMPTY("4003", "AI API返回空结果"),
    AI_API_QUOTA_EXCEEDED("4004", "AI API配额已用完"),
    
    // 文件操作错误 (5000-5999)
    FILE_WRITE_FAILED("5001", "文件写入失败"),
    FILE_CREATE_FAILED("5002", "文件创建失败"),
    FILE_PATH_INVALID("5003", "文件路径无效"),
    
    // 通用错误 (9000-9999)
    UNKNOWN_ERROR("9001", "未知错误"),
    PARAMETER_INVALID("9002", "参数无效"),
    OPERATION_FAILED("9003", "操作失败");
    
    private final String code;
    private final String message;
    
    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public CodeReviewException toException() {
        return new CodeReviewException(code, message);
    }
    
    public CodeReviewException toException(Throwable cause) {
        return new CodeReviewException(code, message, cause);
    }
    
    public CodeReviewException toException(String detail) {
        return new CodeReviewException(code, message + ": " + detail);
    }
}

