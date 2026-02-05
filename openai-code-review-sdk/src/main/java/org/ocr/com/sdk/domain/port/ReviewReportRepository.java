package org.ocr.com.sdk.domain.port;

import org.ocr.com.sdk.domain.model.CodeInfo;

/**
 * 评审报告仓储端口（DDD 端口）
 * 由领域定义，由基础设施实现（如文件/GitHub 存储适配器）
 *
 * @author SDK Team
 * @since 1.0
 */
public interface ReviewReportRepository {

    /**
     * 保存评审报告并返回报告路径（或存储中的标识）
     *
     * @param codeInfo       代码信息
     * @param reviewContent  评审内容
     * @return 报告路径，用于后续访问或通知链接
     */
    String save(CodeInfo codeInfo, String reviewContent);
}
