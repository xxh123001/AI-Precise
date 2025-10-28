package com.example.tag_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 图像上传请求DTO
 * 用于接收图像上传的参数
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadRequest {

    /**
     * 标签配置ID
     */
    private Long labelConfigId;

    /**
     * 描述信息
     */
    private String description;
}
