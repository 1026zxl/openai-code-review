package org.ocr.com.sdk.domain.model.valueobject;

import java.util.Objects;

/**
 * 代码差异内容值对象（Value Object）
 * 封装代码差异内容，提供领域行为
 * 
 * @author SDK Team
 * @since 1.0
 */
public class DiffContent {
    
    private final String content;
    
    private DiffContent(String content) {
        this.content = content != null ? content : "";
    }
    
    /**
     * 创建差异内容值对象
     * 
     * @param content 差异内容
     * @return DiffContent实例
     */
    public static DiffContent of(String content) {
        return new DiffContent(content);
    }
    
    /**
     * 创建空的差异内容
     */
    public static DiffContent empty() {
        return new DiffContent("");
    }
    
    /**
     * 获取原始内容
     */
    public String getContent() {
        return content;
    }
    
    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return content == null || content.trim().isEmpty();
    }
    
    /**
     * 获取差异行数
     */
    public int getLineCount() {
        if (isEmpty()) {
            return 0;
        }
        return content.split("\n").length;
    }
    
    /**
     * 获取添加的行数（以+开头的行，排除+++）
     */
    public int getAddedLineCount() {
        if (isEmpty()) {
            return 0;
        }
        int count = 0;
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.startsWith("+") && !line.startsWith("+++")) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 获取删除的行数（以-开头的行，排除---）
     */
    public int getDeletedLineCount() {
        if (isEmpty()) {
            return 0;
        }
        int count = 0;
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.startsWith("-") && !line.startsWith("---")) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 是否有变更
     */
    public boolean hasChanges() {
        return !isEmpty() && (getAddedLineCount() > 0 || getDeletedLineCount() > 0);
    }
    
    /**
     * 截取前N行（用于摘要）
     */
    public String getPreview(int maxLines) {
        if (isEmpty()) {
            return "";
        }
        String[] lines = content.split("\n");
        if (lines.length <= maxLines) {
            return content;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxLines; i++) {
            sb.append(lines[i]).append("\n");
        }
        sb.append("... (共 ").append(lines.length).append(" 行)");
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiffContent that = (DiffContent) o;
        return Objects.equals(content, that.content);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(content);
    }
    
    @Override
    public String toString() {
        return isEmpty() ? "DiffContent(empty)" : 
            String.format("DiffContent(lines=%d, +%d/-%d)", 
                getLineCount(), getAddedLineCount(), getDeletedLineCount());
    }
}
