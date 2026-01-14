package org.ocr.com.sdk.exception;

/**
 * API调用异常
 * 
 * @author SDK Team
 * @since 1.0
 */
public class ApiException extends CodeReviewException {
    
    private static final long serialVersionUID = 1L;
    
    private final int httpStatusCode;
    
    public ApiException(ErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
        this.httpStatusCode = 0;
    }
    
    public ApiException(ErrorCode errorCode, String detail) {
        super(errorCode.getCode(), errorCode.getMessage() + ": " + detail);
        this.httpStatusCode = 0;
    }
    
    public ApiException(ErrorCode errorCode, int httpStatusCode, String detail) {
        super(errorCode.getCode(), errorCode.getMessage() + ": HTTP " + httpStatusCode + ", " + detail);
        this.httpStatusCode = httpStatusCode;
    }
    
    public ApiException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getCode(), errorCode.getMessage(), cause);
        this.httpStatusCode = 0;
    }
    
    public ApiException(ErrorCode errorCode, int httpStatusCode, String detail, Throwable cause) {
        super(errorCode.getCode(), errorCode.getMessage() + ": HTTP " + httpStatusCode + ", " + detail, cause);
        this.httpStatusCode = httpStatusCode;
    }
    
    public int getHttpStatusCode() {
        return httpStatusCode;
    }
}

