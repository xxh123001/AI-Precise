package com.example.tag_backend.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 活动记录响应DTO
 * 用于仪表板显示最近的系统活动
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityResponse {

    /**
     * 活动类型
     */
    private String type;

    /**
     * 活动描述
     */
    private String description;

    /**
     * 执行用户
     */
    private String user;

    /**
     * 活动时间
     */
    private LocalDateTime time;
}
