package com.example.tag_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 创建标签配置请求DTO
 * 用于接收创建标签配置的请求参数
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateLabelConfigRequest {

    /**
     * 配置名称
     */
    @NotBlank(message = "配置名称不能为空")
    private String name;

    /**
     * 配置描述
     */
    private String description;

    /**
     * 标签配置JSON数据
     * 包含标签类别、选项等动态配置信息
     */
    @NotBlank(message = "标签配置不能为空")
    private String config;

    /**
     * 是否激活
     */
    private Boolean isActive = true;
}
