package com.example.tag_backend.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 标签配置查询请求DTO
 * 用于接收标签配置列表查询的参数
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabelConfigQueryRequest {

    /**
     * 配置名称（模糊查询）
     */
    private String name;

    /**
     * 是否激活
     */
    private Boolean isActive;

    /**
     * 创建者ID
     */
    private Long createdBy;

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
