package org.ocr.com.sdk.domain.port;

import org.ocr.com.sdk.domain.model.CodeInfo;

/**
 * 代码变更来源端口（DDD 端口）
 * 由领域定义，由基础设施实现（如 Git 仓库适配器）
 *
 * @author SDK Team
 * @since 1.0
 */
public interface CodeChangeSource {

    /**
     * 获取最近一次提交的代码差异
     *
     * @return 代码信息，包含提交信息与 diff 内容
     */
    CodeInfo getLatestDiff();
}
