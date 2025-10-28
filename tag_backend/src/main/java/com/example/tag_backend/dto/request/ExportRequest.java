package com.example.tag_backend.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 数据导出请求DTO
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequest {

    /**
     * 项目ID（大任务ID）
     */
    private Long projectId;

    /**
     * 用户ID（单个用户）
     */
    private Long userId;

    /**
     * 用户ID列表（筛选特定用户的标注）
     */
    private List<Long> userIds;

    /**
     * 标签配置ID列表（筛选特定配置的标注）
     */
    private List<Long> labelConfigIds;

    /**
     * 任务状态（ASSIGNED, IN_PROGRESS, COMPLETED）
     */
    private String taskStatus;

    /**
     * 标注状态筛选（DRAFT, COMPLETED）
     */
    private String annotationStatus;

    /**
     * 文件名关键词搜索
     */
    private String fileName;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 仅包含已标注的数据
     */
    private Boolean hasAnnotation;

    /**
     * 仅包含有备注的数据
     */
    private Boolean hasRemark;

    /**
     * 排除空标注
     */
    private Boolean excludeEmpty;

    /**
     * 是否包含图像信息
     */
    private Boolean includeImageInfo = true;

    /**
     * 是否包含用户信息
     */
    private Boolean includeUserInfo = true;

    /**
     * 导出格式（excel, csv, json）
     */
    private String format = "xlsx";

    /**
     * 导出格式（兼容旧字段）
     */
    private String exportFormat = "excel";

    /**
     * 自定义列配置
     */
    private List<String> customColumns;

    /**
     * 预览数据限制条数
     */
    private Integer limit = 50;

    /**
     * 分页页码（从0开始）
     */
    private Integer page = 0;

    /**
     * 分页大小
     */
    private Integer size = 20;
}
