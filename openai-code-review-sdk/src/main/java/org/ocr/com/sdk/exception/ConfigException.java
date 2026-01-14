package org.ocr.com.sdk.exception;

/**
 * 配置异常
 * 
 * @author SDK Team
 * @since 1.0
 */
public class ConfigException extends CodeReviewException {
    
    private static final long serialVersionUID = 1L;
    
    public ConfigException(ErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
    }
    
    public ConfigException(ErrorCode errorCode, String detail) {
        super(errorCode.getCode(), errorCode.getMessage() + ": " + detail);
    }
    
    public ConfigException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getCode(), errorCode.getMessage(), cause);
    }
}

