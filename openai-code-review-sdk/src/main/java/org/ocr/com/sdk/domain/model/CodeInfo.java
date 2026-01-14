package org.ocr.com.sdk.domain.model;

import java.util.Objects;

/**
 * 代码信息领域模型
 * 
 * @author SDK Team
 * @since 1.0
 */
public class CodeInfo {
    
    private final String commitMessage;
    private final String authorName;
    private final String commitTime;
    private final String commitHash;
    private final String diffContent;
    
    public CodeInfo(String commitMessage, String authorName, String commitTime, String commitHash, String diffContent) {
        this.commitMessage = commitMessage;
        this.authorName = authorName;
        this.commitTime = commitTime;
        this.commitHash = commitHash;
        this.diffContent = diffContent;
    }
    
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
        return diffContent;
    }
    
    public boolean isEmpty() {
        return diffContent == null || diffContent.isEmpty();
    }
    
    public int getDiffLineCount() {
        if (diffContent == null || diffContent.isEmpty()) {
            return 0;
        }
        return diffContent.split("\n").length;
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
               ", diffLineCount=" + getDiffLineCount() +
               '}';
    }
}

