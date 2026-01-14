package org.ocr.com.sdk.infrastructure.storage;

import org.ocr.com.sdk.config.CodeReviewConfig;
import org.ocr.com.sdk.domain.model.CodeInfo;
import org.ocr.com.sdk.exception.CodeReviewException;
import org.ocr.com.sdk.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 评审报告存储基础设施
 * 
 * @author SDK Team
 * @since 1.0
 */
public class ReportStorage {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportStorage.class);
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final CodeReviewConfig config;
    
    public ReportStorage(CodeReviewConfig config) {
        this.config = config;
    }
    
    /**
     * 保存评审报告
     * 
     * @param codeInfo 代码信息
     * @param reviewContent 评审内容
     * @return 保存的文件路径
     */
    public String saveReport(CodeInfo codeInfo, String reviewContent) {
        try {
            // 生成文件夹路径
            String authorName = sanitizeFileName(codeInfo.getAuthorName());
            String commitDate = LocalDateTime.now().format(DATE_FORMATTER);
            String baseDir = config.getReportBaseDir() + "/" + authorName + "/" + commitDate;
            
            // 创建目录
            Path dirPath = Paths.get(baseDir);
            Files.createDirectories(dirPath);
            
            // 生成文件名
            String commitDesc = sanitizeFileName(codeInfo.getCommitMessage());
            String fileName = commitDesc + " - " + authorName + ".log";
            Path filePath = dirPath.resolve(fileName);
            
            // 写入评审报告
            try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
                writer.write("=== OpenAI代码评审报告 ===\n\n");
                writer.write("评审时间: " + LocalDateTime.now().format(DATETIME_FORMATTER) + "\n");
                writer.write("提交信息: " + codeInfo.getCommitMessage() + "\n");
                writer.write("提交人: " + codeInfo.getAuthorName() + "\n");
                writer.write("提交时间: " + codeInfo.getCommitTime() + "\n");
                if (codeInfo.getCommitHash() != null) {
                    writer.write("提交哈希: " + codeInfo.getCommitHash() + "\n");
                }
                writer.write("\n=== 评审结果 ===\n\n");
                writer.write(reviewContent);
            }
            
            String absolutePath = filePath.toAbsolutePath().toString();
            logger.info("评审报告已生成: {}", absolutePath);
            return absolutePath;
        } catch (IOException e) {
            throw new CodeReviewException(ErrorCode.FILE_WRITE_FAILED.getCode(), 
                    ErrorCode.FILE_WRITE_FAILED.getMessage(), e);
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

