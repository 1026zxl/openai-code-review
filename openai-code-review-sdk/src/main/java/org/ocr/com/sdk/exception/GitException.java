package org.ocr.com.sdk.exception;

/**
 * Git操作异常
 * 
 * @author SDK Team
 * @since 1.0
 */
public class GitException extends CodeReviewException {
    
    private static final long serialVersionUID = 1L;
    
    public GitException(ErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
    }
    
    public GitException(ErrorCode errorCode, String detail) {
        super(errorCode.getCode(), errorCode.getMessage() + ": " + detail);
    }
    
    public GitException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getCode(), errorCode.getMessage(), cause);
    }
    
    public GitException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode.getCode(), errorCode.getMessage() + ": " + detail, cause);
    }
}

