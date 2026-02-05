package org.ocr.com.sdk.infrastructure.notification;

import org.ocr.com.sdk.config.CodeReviewConfig;
import org.ocr.com.sdk.domain.service.NotificationService;

import java.util.ArrayList;
import java.util.List;

/**
 * 通知服务工厂
 * 根据配置创建并返回可用的通知服务实例
 * 
 * @author SDK Team
 * @since 1.0
 */
public class NotificationServiceFactory {
    /**
     * 创建通知服务列表
     * 根据配置创建所有可用的通知服务
     * 
     * @param config 配置对象
     * @return 通知服务列表
     */
    public static List<NotificationService> createServices(CodeReviewConfig config) {
        List<NotificationService> services = new ArrayList<>();
        
        // 创建微信公众号通知服务（如果配置了）
        if (config.isWechatEnabled()) {
            services.add(new WeChatNotificationService(config));
        }
        
        // 后续可以添加其他通知服务，如：
        // if (config.isEmailEnabled()) {
        //     services.add(new EmailNotificationService(config));
        // }
        
        return services;
    }
    
    /**
     * 创建单个通知服务（优先返回第一个可用的）
     * 
     * @param config 配置对象
     * @return 通知服务，如果没有可用的则返回null
     */
    public static NotificationService createService(CodeReviewConfig config) {
        List<NotificationService> services = createServices(config);
        return services.isEmpty() ? null : services.get(0);
    }
}
