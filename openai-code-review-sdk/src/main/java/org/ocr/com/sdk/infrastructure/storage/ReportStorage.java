package org.ocr.com.sdk.infrastructure.storage;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.ocr.com.sdk.config.CodeReviewConfig;
import org.ocr.com.sdk.domain.model.CodeInfo;
import org.ocr.com.sdk.exception.CodeReviewException;
import org.ocr.com.sdk.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 评审报告存储基础设施
 * 支持将评审报告上传到 GitHub 仓库
 * 
 * @author SDK Team
 * @since 1.0
 */
public class ReportStorage {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportStorage.class);
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String TEMP_REPO_DIR = ".code-review-repo";
    
    private final CodeReviewConfig config;
    
    public ReportStorage(CodeReviewConfig config) {
        this.config = config;
    }
    
    /**
     * 保存评审报告到 GitHub 仓库
     * 
     * @param codeInfo 代码信息
     * @param reviewContent 评审内容
     * @return GitHub 仓库中的文件路径
     */
    public String saveReport(CodeInfo codeInfo, String reviewContent) {
        String githubToken = config.getGithubToken();
        if (githubToken == null || githubToken.isEmpty()) {
            throw new CodeReviewException(ErrorCode.CONFIG_API_KEY_MISSING.getCode(),
                    "GitHub Token 未配置，请设置环境变量 " + config.getGithubTokenEnv() + " 或配置文件中的 code.review.github.token");
        }
        
        String githubRepoUrl = config.getGithubRepoUrl();
        if (githubRepoUrl == null || githubRepoUrl.isEmpty()) {
            throw new CodeReviewException(ErrorCode.CONFIG_API_URL_INVALID.getCode(),
                    "GitHub 仓库 URL 未配置");
        }
        
        Path tempRepoPath = null;
        try {
            // 1. 克隆或拉取 GitHub 仓库
            tempRepoPath = Paths.get(TEMP_REPO_DIR).toAbsolutePath();
            Git git = cloneOrPullRepository(githubRepoUrl, githubToken, tempRepoPath);
            
            // 2. 生成文件路径和内容
            String authorName = sanitizeFileName(codeInfo.getAuthorName());
            String commitDate = LocalDateTime.now().format(DATE_FORMATTER);
            String relativePath = config.getReportBaseDir() + "/" + authorName + "/" + commitDate;
            String commitDesc = sanitizeFileName(codeInfo.getCommitMessage());
            String fileName = commitDesc + " - " + authorName + ".md";
            Path filePath = tempRepoPath.resolve(relativePath).resolve(fileName);
            
            // 3. 创建目录并写入 Markdown 格式的报告
            Files.createDirectories(filePath.getParent());
            writeMarkdownReport(filePath, codeInfo, reviewContent);
            
            // 4. 提交并推送到 GitHub
            commitAndPush(git, filePath, relativePath + "/" + fileName, codeInfo);
            
            // 5. 清理临时仓库
            cleanupTempRepository(tempRepoPath);
            
            String githubFilePath = relativePath + "/" + fileName;
            logger.info("评审报告已上传到 GitHub: {}", githubRepoUrl + "/blob/main/" + githubFilePath);
            return githubFilePath;
            
        } catch (IOException | GitAPIException e) {
            // 清理临时仓库
            cleanupTempRepository(tempRepoPath);
            throw new CodeReviewException(ErrorCode.FILE_WRITE_FAILED.getCode(),
                    "保存评审报告到 GitHub 失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 克隆或拉取 GitHub 仓库
     */
    public Git cloneOrPullRepository(String repoUrl, String token, Path repoPath) throws GitAPIException, IOException {
        Git git;
        UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(token, "");
        
        if (Files.exists(repoPath) && Files.exists(repoPath.resolve(".git"))) {
            // 仓库已存在，执行拉取
            logger.info("拉取 GitHub 仓库: {}", repoUrl);
            git = Git.open(repoPath.toFile());
            git.pull()
                    .setCredentialsProvider(credentialsProvider)
                    .call();
        } else {
            // 仓库不存在，执行克隆
            logger.info("克隆 GitHub 仓库: {}", repoUrl);
            // 构建带 token 的 URL（用于克隆）
            String urlWithToken = buildUrlWithToken(repoUrl, token);
            
            // 确保父目录存在
            Path parentPath = repoPath.getParent();
            if (parentPath != null) {
                Files.createDirectories(parentPath);
            }
            
            git = Git.cloneRepository()
                    .setURI(urlWithToken)
                    .setDirectory(repoPath.toFile())
                    .setCredentialsProvider(credentialsProvider)
                    .call();
        }
        
        return git;
    }
    
    /**
     * 构建带 Token 的 URL
     */
    private String buildUrlWithToken(String repoUrl, String token) {
        // 将 https://github.com/owner/repo.git 转换为 https://token@github.com/owner/repo.git
        if (repoUrl.startsWith("https://")) {
            return repoUrl.replace("https://", "https://" + token + "@");
        } else if (repoUrl.startsWith("http://")) {
            return repoUrl.replace("http://", "http://" + token + "@");
        }
        return repoUrl;
    }
    
    /**
     * 写入 Markdown 格式的评审报告
     */
    private void writeMarkdownReport(Path filePath, CodeInfo codeInfo, String reviewContent) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            writer.write("# OpenAI 代码评审报告\n\n");
            writer.write("## 基本信息\n\n");
            writer.write("| 项目 | 内容 |\n");
            writer.write("|------|------|\n");
            writer.write("| 评审时间 | " + LocalDateTime.now().format(DATETIME_FORMATTER) + " |\n");
            writer.write("| 提交信息 | " + escapeMarkdown(codeInfo.getCommitMessage()) + " |\n");
            writer.write("| 提交人 | " + escapeMarkdown(codeInfo.getAuthorName()) + " |\n");
            writer.write("| 提交时间 | " + codeInfo.getCommitTime() + " |\n");
            if (codeInfo.getCommitHash() != null) {
                writer.write("| 提交哈希 | `" + codeInfo.getCommitHash() + "` |\n");
            }
            writer.write("\n## 评审结果\n\n");
            
            // 将评审内容写入，如果内容本身不是 Markdown 格式，则作为代码块处理
            String formattedContent = formatReviewContent(reviewContent);
            writer.write(formattedContent);
        }
    }
    
    /**
     * 转义 Markdown 特殊字符
     */
    private String escapeMarkdown(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("|", "\\|").replace("\n", "<br>");
    }
    
    /**
     * 格式化评审内容为 Markdown
     */
    private String formatReviewContent(String content) {
        if (content == null || content.isEmpty()) {
            return "*暂无评审内容*";
        }
        
        // 如果内容看起来已经是 Markdown 格式（包含 Markdown 标记），直接返回
        if (content.contains("```") || content.contains("#") || content.contains("*") || content.contains("-")) {
            return content + "\n";
        }
        
        // 否则作为代码块处理
        return "```\n" + content + "\n```\n";
    }
    
    /**
     * 提交并推送到 GitHub
     */
    private void commitAndPush(Git git, Path filePath, String relativePath, CodeInfo codeInfo) 
            throws GitAPIException, IOException {
        // 添加文件
        git.add().addFilepattern(relativePath).call();
        
        // 检查是否有变更
        org.eclipse.jgit.api.Status status = git.status().call();
        if (status.getAdded().isEmpty() && status.getChanged().isEmpty()) {
            logger.info("没有变更需要提交");
            return;
        }
        
        // 提交
        String commitMessage = String.format("Add code review report: %s - %s", 
                codeInfo.getCommitMessage(), codeInfo.getAuthorName());
        git.commit()
                .setMessage(commitMessage)
                .setAuthor(new PersonIdent("Code Review Bot", "code-review@bot.com"))
                .call();
        
        // 推送
        logger.info("推送评审报告到 GitHub...");
        git.push()
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(config.getGithubToken(), ""))
                .call();
        
        logger.info("评审报告已成功推送到 GitHub");
    }
    
    /**
     * 清理临时仓库目录
     */
    private void cleanupTempRepository(Path repoPath) {
        try {
            if (Files.exists(repoPath)) {
                deleteDirectory(repoPath.toFile());
                logger.debug("已清理临时仓库目录: {}", repoPath);
            }
        } catch (Exception e) {
            logger.warn("清理临时仓库目录失败: {}", e.getMessage());
        }
    }
    
    /**
     * 递归删除目录
     */
    private void deleteDirectory(File directory) throws IOException {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        Files.delete(file.toPath());
                    }
                }
            }
            Files.delete(directory.toPath());
        }
    }
    
    /**
     * 清理文件名中的非法字符
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "unknown";
        }
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", "_");
    }
}

