package org.ocr.com.sdk.infrastructure.notification;

import org.ocr.com.sdk.config.CodeReviewConfig;
import org.ocr.com.sdk.domain.service.NotificationService;

import java.util.ArrayList;
import java.util.List;

/**
 * 通知服务工厂（基础设施层）
 * 根据配置创建并返回可用的通知服务实例。
 *
 * @author SDK Team
 * @since 1.0
 */
public class NotificationServiceFactory {
    /**
     * 根据配置创建通知服务列表
     *
     * @param config 代码评审配置，若为 null 或未启用微信通知则不会添加微信服务
     * @return 通知服务列表
     */
    public static List<NotificationService> createServices(CodeReviewConfig config) {
        List<NotificationService> services = new ArrayList<>();
        if (config != null && config.isWechatEnabled()) {
            services.add(new WeChatNotificationService(config));
        }
        return services;
    }

    /**
     * 创建单个通知服务（优先返回第一个可用的）
     */
    public static NotificationService createService(CodeReviewConfig config) {
        List<NotificationService> services = createServices(config);
        return services.isEmpty() ? null : services.get(0);
    }
}
