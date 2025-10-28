package com.example.tag_backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 批量更新图像标签配置请求DTO
 * 用于接收批量更新图像标签配置的参数
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchUpdateLabelConfigRequest {

    /**
     * 图像ID列表
     */
    @NotEmpty(message = "图像ID列表不能为空")
    private List<Long> imageIds;

    /**
     * 标签配置ID，null 表示移除标签配置
     */
    private Long labelConfigId;
}
