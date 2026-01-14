package org.ocr.com.sdk.exception;

/**
 * 代码评审基础异常类
 * 
 * @author SDK Team
 * @since 1.0
 */
public class CodeReviewException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    private final String errorCode;
    
    public CodeReviewException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public CodeReviewException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s", errorCode, getMessage());
    }
}

