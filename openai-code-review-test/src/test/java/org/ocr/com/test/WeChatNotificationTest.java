package org.ocr.com.test;

import org.junit.Test;
import org.ocr.com.sdk.config.CodeReviewConfig;
import org.ocr.com.sdk.domain.model.CodeInfo;
import org.ocr.com.sdk.domain.model.NotificationMessage;
import org.ocr.com.sdk.domain.model.ReviewResult;
import org.ocr.com.sdk.domain.service.NotificationMessageFactory;
import org.ocr.com.sdk.domain.service.NotificationService;
import org.ocr.com.sdk.infrastructure.notification.NotificationServiceFactory;
import org.ocr.com.sdk.infrastructure.notification.WeChatNotificationService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信公众号推送功能测试类
 * 用于单独验证微信公众号消息推送模块是否正常
 * 
 * @author SDK Team
 * @since 1.0
 */
public class WeChatNotificationTest {
    
    /**
     * 测试微信公众号推送功能
     * 
     * 使用说明：
     * 1. 配置环境变量或修改代码中的配置值：
     *    - WECHAT_APP_ID: 微信公众号 AppID
     *    - WECHAT_APP_SECRET: 微信公众号 AppSecret
     *    - WECHAT_TEMPLATE_ID: 模板消息ID
     *    - WECHAT_OPEN_ID: 接收消息的用户OpenID
     * 
     * 2. 运行此测试方法
     * 
     * 3. 检查微信是否收到推送消息
     */
    @Test
    public void testWeChatNotification() {
        System.out.println("=== 开始测试微信公众号推送功能 ===");
        
        // 方式1：从环境变量加载配置
        CodeReviewConfig config = CodeReviewConfig.fromEnvironment();
        
        // 方式2：使用Builder模式配置（如果环境变量未配置，可以取消注释使用）
        /*
        CodeReviewConfig config = CodeReviewConfig.builder()
                .apiKey("your_api_key")  // 这里可以填任意值，因为只测试推送功能
                .wechatAppId("your_app_id")
                .wechatAppSecret("your_app_secret")
                .wechatTemplateId("your_template_id")
                .wechatOpenId("your_open_id")
                .wechatEnabled(true)
                .build();
        */
        
        // 检查微信公众号配置是否启用
        if (!config.isWechatEnabled()) {
            System.err.println("❌ 微信公众号推送未启用！");
            System.err.println("请配置以下环境变量：");
            System.err.println("  - WECHAT_APP_ID");
            System.err.println("  - WECHAT_APP_SECRET");
            System.err.println("  - WECHAT_TEMPLATE_ID");
            System.err.println("  - WECHAT_OPEN_ID");
            return;
        }
        
        System.out.println("✅ 微信公众号配置已启用");
        System.out.println("AppID: " + config.getWechatAppId());
        System.out.println("TemplateID: " + config.getWechatTemplateId());
        System.out.println("OpenID: " + config.getWechatOpenId());
        
        try {
            // 创建通知服务
            NotificationService notificationService = new WeChatNotificationService(config);
            
            // 创建测试用的通知消息
            NotificationMessage message = createTestNotificationMessage(config);
            
            System.out.println("\n=== 消息内容 ===");
            System.out.println("标题: " + message.getTitle());
            System.out.println("摘要: " + message.getSummary());
            System.out.println("链接: " + message.getLinkUrl());
            System.out.println("优先级: " + message.getPriority());
            System.out.println("提交信息: " + message.getMetadata("commitMessage"));
            System.out.println("提交人: " + message.getMetadata("authorName"));
            System.out.println("问题统计: " + message.getMetadata("issueStats"));
            
            // 测试同步发送
            System.out.println("\n=== 开始发送消息（同步） ===");
            notificationService.send(message);
            System.out.println("✅ 消息发送成功！");
            
            // 等待一下，避免请求过快
            Thread.sleep(1000);
            
            // 测试异步发送
            System.out.println("\n=== 开始发送消息（异步） ===");
            notificationService.sendAsync(message);
            System.out.println("✅ 异步发送任务已提交！");
            
            // 等待异步任务完成
            Thread.sleep(3000);
            
            System.out.println("\n=== 测试完成 ===");
            System.out.println("请检查微信是否收到推送消息");
            
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试从评审结果创建通知消息
     */
    @Test
    public void testCreateNotificationFromReviewResult() {
        System.out.println("=== 测试从评审结果创建通知消息 ===");
        
        // 创建测试用的评审结果
        CodeInfo codeInfo = new CodeInfo(
                "feat: 添加新功能测试",
                "张三",
                String.valueOf(System.currentTimeMillis() / 1000 - 3600), // commitTime 是时间戳字符串
                "test-commit-hash-12345",
                "diff --git a/src/main/java/Test.java b/src/main/java/Test.java\n" +
                "@@ -1,5 +1,10 @@\n" +
                "+public class Test {\n" +
                "+    public void test() {\n" +
                "+        System.out.println(\"Hello\");\n" +
                "+    }\n" +
                "+}\n"
        );
        
        String reviewContent = "## 代码评审结果\n\n" +
                "### 问题统计\n" +
                "- 严重问题数量：高（2） 中（3） 低（1）\n\n" +
                "### 评审意见\n" +
                "1. 代码风格良好\n" +
                "2. 建议添加注释\n" +
                "3. 注意异常处理\n";
        
        ReviewResult reviewResult = new ReviewResult(
                codeInfo,
                reviewContent,
                LocalDateTime.now(),
                "代码评审记录/test/2026-01-14/test.log"
        );
        
        // 创建配置（用于构建URL）
        CodeReviewConfig config = CodeReviewConfig.builder()
                .apiKey("test")  // 测试用，不需要真实值
                .githubRepoUrl("https://github.com/user/repo.git")
                .build();
        
        // 使用工厂创建通知消息
        NotificationMessageFactory factory = new NotificationMessageFactory(config);
        NotificationMessage message = factory.createFromReviewResult(reviewResult);
        
        System.out.println("✅ 通知消息创建成功");
        System.out.println("标题: " + message.getTitle());
        System.out.println("摘要: " + message.getSummary());
        System.out.println("链接: " + message.getLinkUrl());
        System.out.println("优先级: " + message.getPriority());
        System.out.println("提交信息: " + message.getMetadata("commitMessage"));
        System.out.println("提交人: " + message.getMetadata("authorName"));
        System.out.println("问题统计: " + message.getMetadata("issueStats"));
    }
    
    /**
     * 测试通知服务工厂
     */
    @Test
    public void testNotificationServiceFactory() {
        System.out.println("=== 测试通知服务工厂 ===");
        
        // 创建配置
        CodeReviewConfig config = CodeReviewConfig.fromEnvironment();
        
        // 使用工厂创建服务
        java.util.List<NotificationService> services = NotificationServiceFactory.createServices(config);
        
        System.out.println("创建的服务数量: " + services.size());
        
        for (NotificationService service : services) {
            System.out.println("服务类型: " + service.getClass().getSimpleName());
            System.out.println("服务是否启用: " + service.isEnabled());
        }
        
        if (services.isEmpty()) {
            System.out.println("⚠️  没有可用的通知服务，请检查配置");
        } else {
            System.out.println("✅ 通知服务工厂测试通过");
        }
    }
    
    /**
     * 创建测试用的通知消息
     */
    private NotificationMessage createTestNotificationMessage(CodeReviewConfig config) {
        // 构建元数据
        Map<String, String> metadata = new HashMap<>();
        metadata.put("commitMessage", "test: 测试微信公众号推送功能");
        metadata.put("authorName", "测试用户");
        metadata.put("commitHash", "test-commit-12345");
        metadata.put("issueStats", "高:2 中:3 低:1");
        metadata.put("reportPath", "代码评审记录/test/2026-01-14/test.log");
        
        // 构建报告链接
        String reportUrl = null;
        if (config.getGithubRepoUrl() != null && !config.getGithubRepoUrl().isEmpty()) {
            String repoUrl = config.getGithubRepoUrl();
            if (repoUrl.endsWith(".git")) {
                repoUrl = repoUrl.substring(0, repoUrl.length() - 4);
            }
            reportUrl = repoUrl + "/blob/main/代码评审记录/test/2026-01-14/test.log";
        }
        
        return NotificationMessage.builder()
                .title("代码评审完成通知")
                .content("这是一条测试消息，用于验证微信公众号推送功能是否正常工作。")
                .summary("测试消息：微信公众号推送功能测试")
                .linkUrl(reportUrl)
                .linkText("查看详细报告")
                .metadata(metadata)
                .timestamp(LocalDateTime.now())
                .messageType(NotificationMessage.MessageType.CODE_REVIEW_COMPLETED)
                .priority(NotificationMessage.Priority.MEDIUM)
                .build();
    }
    
    /**
     * 主方法：可以直接运行此方法进行测试
     */
    public static void main(String[] args) {
        WeChatNotificationTest test = new WeChatNotificationTest();
        
        System.out.println("请选择测试方法：");
        System.out.println("1. 测试微信公众号推送功能");
        System.out.println("2. 测试从评审结果创建通知消息");
        System.out.println("3. 测试通知服务工厂");
        System.out.println("4. 运行所有测试");
        
        // 默认运行推送测试
        int choice = args.length > 0 ? Integer.parseInt(args[0]) : 1;
        
        try {
            switch (choice) {
                case 1:
                    test.testWeChatNotification();
                    break;
                case 2:
                    test.testCreateNotificationFromReviewResult();
                    break;
                case 3:
                    test.testNotificationServiceFactory();
                    break;
                case 4:
                    test.testNotificationServiceFactory();
                    System.out.println("\n");
                    test.testCreateNotificationFromReviewResult();
                    System.out.println("\n");
                    test.testWeChatNotification();
                    break;
                default:
                    System.out.println("无效的选择，运行默认测试");
                    test.testWeChatNotification();
            }
        } catch (Exception e) {
            System.err.println("测试执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
