package com.example.tag_backend.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 图像查询请求DTO
 * 用于接收图像列表查询的参数
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageQueryRequest {

    /**
     * 图像ID（精确查询）
     */
    private Long id;

    /**
     * 关键字（模糊查询：文件名、原始名称）
     */
    private String keyword;

    /**
     * 原始文件名（模糊查询）
     */
    private String originalName;

    /**
     * 上传者ID
     */
    private Long uploadBy;

    /**
     * 标签配置ID
     */
    private Long labelConfigId;

    /**
     * 是否已分配（null-全部，true-已分配，false-未分配）
     */
    private Boolean isAssigned;

    /**
     * 分配给的用户ID
     */
    private Long assignedUserId;

    /**
     * 分配给的用户名（模糊查询）
     */
    private String assignedUsername;

    /**
     * 页码，从1开始
     */
    private Integer page = 1;

    /**
     * 每页大小，默认10
     */
    private Integer size = 10;

    /**
     * 排序字段，默认按创建时间降序
     */
    private String sort = "createdAt,desc";
}
