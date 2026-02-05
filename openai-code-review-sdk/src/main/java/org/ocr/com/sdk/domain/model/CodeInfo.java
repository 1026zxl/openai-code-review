package org.ocr.com.sdk.domain.model;

import org.ocr.com.sdk.domain.model.valueobject.DiffContent;

import java.util.Objects;

/**
 * 代码信息领域模型
 * 封装代码变更信息，提供领域行为
 * 
 * @author SDK Team
 * @since 1.0
 */
public class CodeInfo {
    
    private final String commitMessage;
    private final String authorName;
    private final String commitTime;
    private final String commitHash;
    private final DiffContent diffContent;
    
    /**
     * 构造函数
     */
    public CodeInfo(String commitMessage, String authorName, String commitTime, 
                   String commitHash, String diffContent) {
        this.commitMessage = Objects.requireNonNull(commitMessage, "提交消息不能为空");
        this.authorName = Objects.requireNonNull(authorName, "作者名称不能为空");
        this.commitTime = Objects.requireNonNull(commitTime, "提交时间不能为空");
        this.commitHash = commitHash; // 可为空
        this.diffContent = diffContent != null ? DiffContent.of(diffContent) : DiffContent.empty();
    }
    
    /**
     * 构造函数（无commitHash）
     */
    public CodeInfo(String commitMessage, String authorName, String commitTime, String diffContent) {
        this(commitMessage, authorName, commitTime, null, diffContent);
    }
    
    public String getCommitMessage() {
        return commitMessage;
    }
    
    public String getAuthorName() {
        return authorName;
    }
    
    public String getCommitTime() {
        return commitTime;
    }
    
    public String getCommitHash() {
        return commitHash;
    }
    
    public String getDiffContent() {
        return diffContent.getContent();
    }
    
    /**
     * 获取差异内容值对象（用于领域行为）
     */
    public DiffContent getDiffContentValue() {
        return diffContent;
    }
    
    // 领域行为
    
    /**
     * 是否为空（无变更）
     */
    public boolean isEmpty() {
        return diffContent.isEmpty();
    }
    
    /**
     * 是否有变更
     */
    public boolean hasChanges() {
        return diffContent.hasChanges();
    }
    
    /**
     * 获取差异行数
     */
    public int getDiffLineCount() {
        return diffContent.getLineCount();
    }
    
    /**
     * 获取添加的行数
     */
    public int getAddedLineCount() {
        return diffContent.getAddedLineCount();
    }
    
    /**
     * 获取删除的行数
     */
    public int getDeletedLineCount() {
        return diffContent.getDeletedLineCount();
    }
    
    /**
     * 获取变更摘要
     */
    public String getChangeSummary() {
        if (isEmpty()) {
            return "无变更";
        }
        return String.format("+%d/-%d 行", getAddedLineCount(), getDeletedLineCount());
    }
    
    /**
     * 验证代码信息的有效性
     */
    public void validate() {
        if (isEmpty()) {
            throw new IllegalStateException("代码信息为空，无法进行评审");
        }
        if (!hasChanges()) {
            throw new IllegalStateException("代码无实际变更，无需评审");
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeInfo codeInfo = (CodeInfo) o;
        return Objects.equals(commitMessage, codeInfo.commitMessage) &&
               Objects.equals(authorName, codeInfo.authorName) &&
               Objects.equals(commitTime, codeInfo.commitTime) &&
               Objects.equals(commitHash, codeInfo.commitHash);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(commitMessage, authorName, commitTime, commitHash);
    }
    
    @Override
    public String toString() {
        return "CodeInfo{" +
               "commitMessage='" + commitMessage + '\'' +
               ", authorName='" + authorName + '\'' +
               ", commitTime='" + commitTime + '\'' +
               ", commitHash='" + commitHash + '\'' +
               ", changeSummary=" + getChangeSummary() +
               '}';
    }
}

