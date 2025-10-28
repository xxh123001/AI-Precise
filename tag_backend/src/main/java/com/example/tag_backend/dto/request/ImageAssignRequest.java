package com.example.tag_backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 图像分配请求DTO
 * 用于接收图像分配的参数
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageAssignRequest {

    /**
     * 图像ID列表
     */
    @NotEmpty(message = "图像ID列表不能为空")
    private List<Long> imageIds;

    /**
     * 用户ID列表
     */
    @NotEmpty(message = "用户ID列表不能为空") 
    private List<Long> userIds;
}
