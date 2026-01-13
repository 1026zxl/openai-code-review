package org.ocr.com;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 包名称： org.ocr.com
 * 类名称：OpenAiCodeReview
 * 类描述：OpenAI代码自动评审组件
 * 创建人：@author zhengxiaolong
 * 创建时间：2026-01-12 14:51
 */
public class OpenAiCodeReview {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAiCodeReview.class);
    
    // API配置
    private static final String API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private static final String MODEL = "qwen-flash";
    private static final String API_KEY_ENV = "OPENAI_API_KEY";
    
    // 代码评审专家提示词模板
    private static final String CODE_REVIEW_PROMPT_TEMPLATE = 
        "角色设定：\n" +
        "请你担任一名经验丰富的资深技术专家，负责对提供的代码进行严格、细致、富有建设性的评审。你的目标是发现潜在问题、提升代码质量、确保最佳实践，并提供具体、可操作的改进建议。\n" +
        "\n" +
        "我的请求：\n" +
        "请对以下代码进行全面的代码评审。\n" +
        "\n" +
        "代码信息：\n" +
        "%s\n" +
        "\n" +
        "评审方向与重点：\n" +
        "请从以下维度进行评审，并对发现的问题按严重程度（高/中/低）进行分级：\n" +
        "\n" +
        "技术正确性与逻辑：\n" +
        "- 代码是否能正确实现所述功能？是否存在边界条件、极端情况或并发场景下的逻辑错误？\n" +
        "- 算法或逻辑是否高效、清晰？是否有更优的实现方式？\n" +
        "- 输入验证、错误处理是否完备？（例如：空值、无效参数、异常捕获与处理）\n" +
        "\n" +
        "安全性与可靠性：\n" +
        "- 是否存在安全漏洞？（例如：SQL注入、XSS、CSRF、命令注入、不安全的反序列化、敏感信息泄露）\n" +
        "- 资源管理是否得当？（例如：数据库连接、文件句柄、网络连接是否确保被释放？是否存在内存泄漏风险？）\n" +
        "- 是否有适当的日志记录和监控点？\n" +
        "\n" +
        "性能与可扩展性：\n" +
        "- 是否存在明显的性能瓶颈？（例如：循环内的重复计算、N+1查询、未使用索引、低效的数据结构）\n" +
        "- 代码是否能良好地应对数据量或负载的增长？\n" +
        "\n" +
        "代码风格与可维护性：\n" +
        "- 是否符合该语言的通用编码规范？（例如：PEP 8 for Python, Google Style Guides）\n" +
        "  命名（变量、函数、类）是否清晰、达意？\n" +
        "- 函数/方法是否过于庞大，职责是否单一？是否需要重构（如提取函数、简化条件表达式）？\n" +
        "- 代码注释是否充分且有意义？是否解释了\"为什么\"而不是\"是什么\"？（对于复杂的业务逻辑或算法尤其重要）\n" +
        "  魔术数字是否被提取为常量或配置？\n" +
        "\n" +
        "可测试性：\n" +
        "- 代码是否便于编写单元测试或集成测试？（例如：是否依赖全局状态、难以Mock的硬编码、函数副作用过多）\n" +
        "\n" +
        "输出格式要求：\n" +
        "请按照以下结构化格式输出你的评审报告：\n" +
        "\n" +
        "## 代码评审报告\n" +
        "\n" +
        "### 一、总结\n" +
        "*   **整体评价：** （简要概述代码质量，指出最突出的优点和最重要的问题）\n" +
        "*   **严重问题数量：** 高（x） 中（y） 低（z）\n" +
        "\n" +
        "### 二、详细问题与建议\n" +
        "请使用以下模板对每个发现的问题进行描述：\n" +
        "\n" +
        "**【严重等级】** - **【问题类别】**： 简短的问题标题\n" +
        "*   **位置：** `文件名:行号` （或函数名）\n" +
        "*   **问题描述：** 具体解释哪里有问题，以及可能导致什么后果。\n" +
        "*   **改进建议：** 提供具体的修改方案、示例代码或最佳实践参考。\n" +
        "*   **理由：** 解释为何这样修改更好。\n" +
        "\n" +
        "（示例）\n" +
        "**【高】** - **安全性**： 存在潜在的SQL注入风险\n" +
        "*   **位置：** `user_service.py:45` （`get_user`函数）\n" +
        "*   **问题描述：** 直接使用字符串拼接构造SQL查询语句，如果`user_id`来自不可信输入，可能导致SQL注入攻击。\n" +
        "*   **改进建议：** 使用参数化查询或ORM提供的方法。例如，将`cursor.execute(f\"SELECT * FROM users WHERE id = {user_id}\")` 改为 `cursor.execute(\"SELECT * FROM users WHERE id = %s\", (user_id,))`。\n" +
        "*   **理由：** 参数化查询能确保输入被安全地转义，是防止SQL注入的标准做法。\n" +
        "\n" +
        "### 三、正面评价与优点\n" +
        "*   （列出代码中做得好的地方，例如：清晰的模块划分、巧妙的算法设计、完善的错误处理等）\n" +
        "\n" +
        "### 四、综合建议与后续步骤\n" +
        "1.  **必须优先修复：** （列出所有\"高\"等级问题）\n" +
        "2.  **建议近期优化：** （列出所有\"中\"等级问题）\n" +
        "3.  **可考虑重构：** （列出所有\"低\"等级问题或代码异味）";

    public static void main(String[] args) {
        try {
            logger.info("=== OpenAI代码自动评审开始 ===");
            
            // 1. 检出提交代码
            logger.info("Step 1: 检出提交代码...");
            CodeInfo codeInfo = checkoutCode();
            
            if (codeInfo.getDiffContent() == null || codeInfo.getDiffContent().isEmpty()) {
                logger.warn("未检测到代码变更，跳过评审");
                return;
            }
            
            // 2. 调用AI进行代码评审
            logger.info("Step 2: 调用AI进行代码评审...");
            String reviewResult = callOpenAI(codeInfo.getDiffContent());
            
            if (reviewResult == null || reviewResult.isEmpty()) {
                logger.error("AI评审失败，未获得评审结果");
                return;
            }
            
            // 3. 生成代码评审记录
            logger.info("Step 3: 生成代码评审记录...");
            generateReviewReport(codeInfo, reviewResult);
            
            logger.info("=== OpenAI代码自动评审完成 ===");
            
        } catch (Exception e) {
            logger.error("代码评审过程发生异常", e);
            System.err.println("代码评审失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 检出提交代码
     */
    private static CodeInfo checkoutCode() throws GitAPIException, IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(new File(".git"))
                .readEnvironment()
                .findGitDir()
                .build();
        
        try (Git git = new Git(repository)) {
            // 获取最近两次提交
            Iterable<RevCommit> commits = git.log().setMaxCount(2).call();
            List<RevCommit> commitList = Lists.newArrayList(commits);
            
            if (commitList.size() < 2) {
                logger.warn("提交历史不足，无法进行代码对比");
                return new CodeInfo("", "", "", "");
            }
            
            RevCommit newCommit = commitList.iterator().next();
            RevCommit oldCommit = commitList.get(1);
            
            // 获取提交信息
            String commitMessage = newCommit.getFullMessage().trim();
            String authorName = newCommit.getAuthorIdent().getName();
            String commitTime = newCommit.getCommitTime() + "";
            
            // 获取代码差异
            String diffContent = getDiffContent(git, repository, oldCommit, newCommit);
            
            logger.info("提交信息: {}", commitMessage);
            logger.info("提交人: {}", authorName);
            logger.info("提交时间: {}", commitTime);
            logger.info("代码差异行数: {}", diffContent.split("\n").length);
            
            return new CodeInfo(commitMessage, authorName, commitTime, diffContent);
        }
    }
    
    /**
     * 获取代码差异内容
     */
    private static String getDiffContent(Git git, Repository repository, RevCommit oldCommit, RevCommit newCommit) 
            throws IOException, GitAPIException {
        try (ObjectReader reader = repository.newObjectReader()) {
            CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
            oldTreeParser.reset(reader, oldCommit.getTree());
            
            CanonicalTreeParser newTreeParser = new CanonicalTreeParser();
            newTreeParser.reset(reader, newCommit.getTree());
            
            List<DiffEntry> diffs = git.diff()
                    .setOldTree(oldTreeParser)
                    .setNewTree(newTreeParser)
                    .call();
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (DiffFormatter diffFormatter = new DiffFormatter(outputStream)) {
                diffFormatter.setRepository(repository);
                diffFormatter.format(diffs);
                return outputStream.toString();
            }
        }
    }
    
    /**
     * 调用OpenAI API进行代码评审
     */
    private static String callOpenAI(String codeContent) {
        String apiKey = System.getenv(API_KEY_ENV);
        if (apiKey == null || apiKey.isEmpty()) {
            logger.error("环境变量 {} 未设置", API_KEY_ENV);
            return null;
        }
        
        // 构建提示词
        String prompt = String.format(CODE_REVIEW_PROMPT_TEMPLATE, codeContent);
        
        // 构建请求体
        Map<String, Object> requestBody = Maps.newHashMap();
        requestBody.put("model", MODEL);
        Map<String, String> message = Maps.newHashMap();
        message.put("role", "user");
        message.put("content", prompt);
        
        requestBody.put("messages", Lists.newArrayList(message));
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 4000);
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(API_URL);
            httpPost.setHeader("Authorization", "Bearer " + apiKey);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(JSON.toJSONString(requestBody), "UTF-8"));
            
            logger.debug("HTTP Request: POST {}", API_URL);
            long startTime = System.currentTimeMillis();
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                long endTime = System.currentTimeMillis();
                logger.debug("HTTP Response: {} ({}ms)", statusCode, endTime - startTime);
                
                if (statusCode != 200) {
                    logger.error("OpenAI API调用失败: {}", statusCode);
                    return null;
                }
                
                String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                JSONObject jsonResponse = JSON.parseObject(responseBody);
                
                if (jsonResponse.containsKey("choices")) {
                    List<JSONObject> choices = jsonResponse.getJSONArray("choices").toJavaList(JSONObject.class);
                    if (!choices.isEmpty()) {
                        JSONObject choice = choices.get(0);
                        JSONObject messageContent = choice.getJSONObject("message");
                        return messageContent.getString("content");
                    }
                }
                
                logger.error("OpenAI API响应格式异常: {}", responseBody);
                return null;
            }
        } catch (IOException e) {
            logger.error("调用OpenAI API时发生IO异常", e);
            return null;
        }
    }
    
    /**
     * 生成代码评审报告
     */
    private static void generateReviewReport(CodeInfo codeInfo, String reviewResult) throws IOException {
        // 生成文件夹路径
        String authorName = sanitizeFileName(codeInfo.getAuthorName());
        String commitDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String baseDir = "代码评审记录/" + authorName + "/" + commitDate;
        
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
            writer.write("评审时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
            writer.write("提交信息: " + codeInfo.getCommitMessage() + "\n");
            writer.write("提交人: " + codeInfo.getAuthorName() + "\n");
            writer.write("提交时间: " + codeInfo.getCommitTime() + "\n\n");
            writer.write("=== 评审结果 ===\n\n");
            writer.write(reviewResult);
        }
        
        logger.info("评审报告已生成: {}", filePath.toAbsolutePath());
    }
    
    /**
     * 清理文件名中的非法字符
     */
    private static String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", "_");
    }
    
    /**
     * 代码信息类
     */
    static class CodeInfo {
        private String commitMessage;
        private String authorName;
        private String commitTime;
        private String diffContent;
        
        public CodeInfo(String commitMessage, String authorName, String commitTime, String diffContent) {
            this.commitMessage = commitMessage;
            this.authorName = authorName;
            this.commitTime = commitTime;
            this.diffContent = diffContent;
        }
        
        // Getters
        public String getCommitMessage() { return commitMessage; }
        public String getAuthorName() { return authorName; }
        public String getCommitTime() { return commitTime; }
        public String getDiffContent() { return diffContent; }
    }
}