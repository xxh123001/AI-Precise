package com.example.tag_backend.common.result;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 错误详情类
 * 用于提供具体的错误信息
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDetail {

    /**
     * 错误字段名
     */
    private String field;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 创建错误详情
     */
    public static ErrorDetail of(String field, String message) {
        return new ErrorDetail(field, message);
    }
}
