package com.example.tag_backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 标注筛选请求DTO
 * 用于封装标注数据筛选的请求参数
 * 
 * @author 辛晓红
 * @version 1.0
 * @since 2025-10-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnotationFilterRequest {

    /**
     * 用户ID列表
     */
    private List<Long> userIds;

    /**
     * 用户名列表（用于按用户名筛选）
     */
    private List<String> usernames;

    /**
     * 标注状态
     */
    private String annotationStatus;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 必须包含的标签列表（用于双标签或多标签筛选）
     */
    private List<String> requiredTags;

    /**
     * 可选包含的标签列表
     */
    private List<String> optionalTags;

    /**
     * 排除的标签列表
     */
    private List<String> excludeTags;

    /**
     * 标签匹配模式
     * - ALL: 必须包含所有required标签
     * - ANY: 包含任意一个required标签即可
     * - EXACT: 精确匹配标签组合
     */
    private String tagMatchMode = "ALL";

    /**
     * 图像ID列表（用于精确筛选）
     */
    private List<Long> imageIds;

    /**
     * 文件名模式（支持通配符）
     */
    private String filenamePattern;

    /**
     * 是否只返回有图像的记录
     */
    private Boolean hasImageOnly = true;

    /**
     * 分页大小
     */
    private Integer pageSize = 50;

    /**
     * 页码（从0开始）
     */
    private Integer pageNumber = 0;

    /**
     * 排序字段
     */
    private String sortBy = "createdAt";

    /**
     * 排序方向（ASC/DESC）
     */
    private String sortDirection = "DESC";

    /**
     * 是否导出文件
     */
    private Boolean exportFiles = false;

    /**
     * 导出格式（JSON/EXCEL/ZIP）
     */
    private String exportFormat = "ZIP";

    /**
     * 最大结果数量限制
     */
    private Integer maxResults = 1000;

    // 便捷方法

    /**
     * 创建用户筛选请求
     */
    public static AnnotationFilterRequest forUser(Long userId) {
        AnnotationFilterRequest request = new AnnotationFilterRequest();
        request.setUserIds(List.of(userId));
        return request;
    }

    /**
     * 创建双标签筛选请求
     */
    public static AnnotationFilterRequest forDoubleTags(Long userId, List<String> tags) {
        AnnotationFilterRequest request = new AnnotationFilterRequest();
        request.setUserIds(List.of(userId));
        request.setRequiredTags(tags);
        request.setTagMatchMode("ALL");
        return request;
    }

    /**
     * 创建用户名筛选请求
     */
    public static AnnotationFilterRequest forUsername(String username) {
        AnnotationFilterRequest request = new AnnotationFilterRequest();
        request.setUsernames(List.of(username));
        return request;
    }

    /**
     * 检查是否有有效的筛选条件
     */
    public boolean hasValidFilters() {
        return (userIds != null && !userIds.isEmpty()) ||
               (usernames != null && !usernames.isEmpty()) ||
               (requiredTags != null && !requiredTags.isEmpty()) ||
               (optionalTags != null && !optionalTags.isEmpty()) ||
               (imageIds != null && !imageIds.isEmpty()) ||
               (filenamePattern != null && !filenamePattern.trim().isEmpty()) ||
               startDate != null || endDate != null ||
               (annotationStatus != null && !annotationStatus.trim().isEmpty());
    }
}

