package org.ocr.com.sdk.config;

import org.ocr.com.sdk.exception.ConfigException;
import org.ocr.com.sdk.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

/**
 * 代码评审配置类
 * 支持配置文件、环境变量、编程式配置三种方式
 * 
 * @author SDK Team
 * @since 1.0
 */
public class CodeReviewConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(CodeReviewConfig.class);
    
    // 默认配置
    private static final String DEFAULT_API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private static final String DEFAULT_MODEL = "qwen-flash";
    private static final String DEFAULT_API_KEY_ENV = "OPENAI_API_KEY";
    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final int DEFAULT_MAX_TOKENS = 4000;
    private static final String DEFAULT_REPORT_BASE_DIR = "代码评审记录";
    private static final String DEFAULT_GITHUB_REPO_URL = "https://github.com/1026zxl/code-review-repository.git";
    private static final String DEFAULT_GITHUB_TOKEN_ENV = "CODE_TOKEN";
    
    // 配置属性
    private String apiKey;
    private String apiUrl = DEFAULT_API_URL;
    private String model = DEFAULT_MODEL;
    private String apiKeyEnv = DEFAULT_API_KEY_ENV;
    private double temperature = DEFAULT_TEMPERATURE;
    private int maxTokens = DEFAULT_MAX_TOKENS;
    private String reportBaseDir = DEFAULT_REPORT_BASE_DIR;
    private String gitRepositoryPath;
    private String githubRepoUrl = DEFAULT_GITHUB_REPO_URL;
    private String githubToken;
    private String githubTokenEnv = DEFAULT_GITHUB_TOKEN_ENV;
    
    /**
     * 私有构造函数，使用Builder创建实例
     */
    private CodeReviewConfig() {
    }
    
    /**
     * 从配置文件加载配置
     * 配置文件路径：classpath:code-review.properties
     */
    public static CodeReviewConfig fromProperties() {
        return fromProperties("code-review.properties");
    }
    
    /**
     * 从指定配置文件加载配置
     */
    public static CodeReviewConfig fromProperties(String propertiesFile) {
        CodeReviewConfig config = new CodeReviewConfig();
        Properties props = new Properties();
        
        try (InputStream is = CodeReviewConfig.class.getClassLoader().getResourceAsStream(propertiesFile)) {
            if (is != null) {
                props.load(is);
                logger.debug("加载配置文件: {}", propertiesFile);
                
                config.apiKey = props.getProperty("code.review.api.key");
                config.apiUrl = props.getProperty("code.review.api.url", DEFAULT_API_URL);
                config.model = props.getProperty("code.review.model", DEFAULT_MODEL);
                config.apiKeyEnv = props.getProperty("code.review.api.key.env", DEFAULT_API_KEY_ENV);
                config.temperature = Double.parseDouble(props.getProperty("code.review.temperature", String.valueOf(DEFAULT_TEMPERATURE)));
                config.maxTokens = Integer.parseInt(props.getProperty("code.review.max.tokens", String.valueOf(DEFAULT_MAX_TOKENS)));
                config.reportBaseDir = props.getProperty("code.review.report.base.dir", DEFAULT_REPORT_BASE_DIR);
                config.gitRepositoryPath = props.getProperty("code.review.git.repository.path");
                config.githubRepoUrl = props.getProperty("code.review.github.repo.url", DEFAULT_GITHUB_REPO_URL);
                config.githubToken = props.getProperty("code.review.github.token");
                config.githubTokenEnv = props.getProperty("code.review.github.token.env", DEFAULT_GITHUB_TOKEN_ENV);
            } else {
                logger.debug("配置文件不存在: {}，使用默认配置", propertiesFile);
            }
        } catch (Exception e) {
            logger.warn("加载配置文件失败: {}", e.getMessage());
        }
        
        // 从环境变量补充配置
        config.loadFromEnvironment();
        
        // 验证配置
        config.validate();
        
        return config;
    }
    
    /**
     * 从环境变量加载配置
     */
    public static CodeReviewConfig fromEnvironment() {
        CodeReviewConfig config = new CodeReviewConfig();
        config.loadFromEnvironment();
        config.validate();
        return config;
    }
    
    /**
     * 创建Builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * 从环境变量加载配置
     */
    private void loadFromEnvironment() {
        if (apiKey == null || apiKey.isEmpty()) {
            String envApiKey = System.getenv(apiKeyEnv);
            if (envApiKey != null && !envApiKey.isEmpty()) {
                this.apiKey = envApiKey;
                logger.debug("从环境变量 {} 加载API密钥", apiKeyEnv);
            }
        }
        
        String envApiUrl = System.getenv("CODE_REVIEW_API_URL");
        if (envApiUrl != null && !envApiUrl.isEmpty()) {
            this.apiUrl = envApiUrl;
        }
        
        String envModel = System.getenv("CODE_REVIEW_MODEL");
        if (envModel != null && !envModel.isEmpty()) {
            this.model = envModel;
        }
        
        String envReportDir = System.getenv("CODE_REVIEW_REPORT_DIR");
        if (envReportDir != null && !envReportDir.isEmpty()) {
            this.reportBaseDir = envReportDir;
        }
        
        String envGithubRepoUrl = System.getenv("CODE_REVIEW_GITHUB_REPO_URL");
        if (envGithubRepoUrl != null && !envGithubRepoUrl.isEmpty()) {
            this.githubRepoUrl = envGithubRepoUrl;
        }
        
        if (githubToken == null || githubToken.isEmpty()) {
            String envGithubToken = System.getenv(githubTokenEnv);
            if (envGithubToken != null && !envGithubToken.isEmpty()) {
                this.githubToken = envGithubToken;
                logger.debug("从环境变量 {} 加载GitHub Token", githubTokenEnv);
            }
        }
    }
    
    /**
     * 验证配置
     */
    private void validate() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new ConfigException(ErrorCode.CONFIG_API_KEY_MISSING);
        }
        
        if (apiUrl == null || apiUrl.isEmpty()) {
            throw new ConfigException(ErrorCode.CONFIG_API_URL_INVALID);
        }
        
        if (model == null || model.isEmpty()) {
            throw new ConfigException(ErrorCode.CONFIG_MODEL_INVALID);
        }
    }
    
    // Getters
    public String getApiKey() {
        return apiKey;
    }
    
    public String getApiUrl() {
        return apiUrl;
    }
    
    public String getModel() {
        return model;
    }
    
    public String getApiKeyEnv() {
        return apiKeyEnv;
    }
    
    public double getTemperature() {
        return temperature;
    }
    
    public int getMaxTokens() {
        return maxTokens;
    }
    
    public String getReportBaseDir() {
        return reportBaseDir;
    }
    
    public String getGitRepositoryPath() {
        return gitRepositoryPath;
    }
    
    public String getGithubRepoUrl() {
        return githubRepoUrl;
    }
    
    public String getGithubToken() {
        return githubToken;
    }
    
    public String getGithubTokenEnv() {
        return githubTokenEnv;
    }
    
    /**
     * Builder模式
     */
    public static class Builder {
        private CodeReviewConfig config = new CodeReviewConfig();
        
        public Builder apiKey(String apiKey) {
            config.apiKey = apiKey;
            return this;
        }
        
        public Builder apiUrl(String apiUrl) {
            config.apiUrl = apiUrl;
            return this;
        }
        
        public Builder model(String model) {
            config.model = model;
            return this;
        }
        
        public Builder apiKeyEnv(String apiKeyEnv) {
            config.apiKeyEnv = apiKeyEnv;
            return this;
        }
        
        public Builder temperature(double temperature) {
            config.temperature = temperature;
            return this;
        }
        
        public Builder maxTokens(int maxTokens) {
            config.maxTokens = maxTokens;
            return this;
        }
        
        public Builder reportBaseDir(String reportBaseDir) {
            config.reportBaseDir = reportBaseDir;
            return this;
        }
        
        public Builder gitRepositoryPath(String gitRepositoryPath) {
            config.gitRepositoryPath = gitRepositoryPath;
            return this;
        }
        
        public Builder githubRepoUrl(String githubRepoUrl) {
            config.githubRepoUrl = githubRepoUrl;
            return this;
        }
        
        public Builder githubToken(String githubToken) {
            config.githubToken = githubToken;
            return this;
        }
        
        public Builder githubTokenEnv(String githubTokenEnv) {
            config.githubTokenEnv = githubTokenEnv;
            return this;
        }
        
        public CodeReviewConfig build() {
            // 从环境变量补充配置
            config.loadFromEnvironment();
            // 验证配置
            config.validate();
            return config;
        }
    }
}

