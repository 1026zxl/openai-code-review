package org.ocr.com.sdk.domain.service;

import org.ocr.com.sdk.domain.model.NotificationMessage;

/**
 * 通知服务端口（领域服务接口）
 * 定义通知发送的抽象接口，由基础设施层实现
 * 
 * @author SDK Team
 * @since 1.0
 */
public interface NotificationService {
    
    /**
     * 发送通知消息
     * 
     * @param message 通知消息
     * @throws NotificationException 发送失败时抛出
     */
    void send(NotificationMessage message) throws NotificationException;
    
    /**
     * 异步发送通知消息
     * 
     * @param message 通知消息
     */
    void sendAsync(NotificationMessage message);
    
    /**
     * 检查服务是否可用
     * 
     * @return true 如果服务可用，false 否则
     */
    boolean isEnabled();
    
    /**
     * 通知异常
     */
    class NotificationException extends Exception {
        private final String errorCode;
        
        public NotificationException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
        
        public NotificationException(String errorCode, String message, Throwable cause) {
            super(message, cause);
            this.errorCode = errorCode;
        }
        
        public String getErrorCode() {
            return errorCode;
        }
    }
}
