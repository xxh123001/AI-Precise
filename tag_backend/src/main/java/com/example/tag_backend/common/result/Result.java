package com.example.tag_backend.common.result;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 统一响应结果类
 * 提供标准化的API响应格式
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * 请求是否成功
     */
    private Boolean success;

    /**
     * 业务状态码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 详细错误信息（可选）
     */
    private List<ErrorDetail> errors;

    /**
     * 响应时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 成功响应
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(true, 200, "操作成功", data, null, LocalDateTime.now());
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 成功响应（自定义消息）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(true, 200, message, data, null, LocalDateTime.now());
    }

    /**
     * 错误响应
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(false, code, message, null, null, LocalDateTime.now());
    }

    /**
     * 错误响应（带详细错误信息）
     */
    public static <T> Result<T> error(Integer code, String message, List<ErrorDetail> errors) {
        return new Result<>(false, code, message, null, errors, LocalDateTime.now());
    }

    /**
     * 参数验证错误响应
     */
    public static <T> Result<T> validationError(String message, List<ErrorDetail> errors) {
        return error(400, message, errors);
    }

    /**
     * 未授权错误响应
     */
    public static <T> Result<T> unauthorized() {
        return error(401, "认证失败");
    }

    /**
     * 权限不足错误响应
     */
    public static <T> Result<T> forbidden() {
        return error(403, "权限不足");
    }

    /**
     * 资源不存在错误响应
     */
    public static <T> Result<T> notFound() {
        return error(404, "资源不存在");
    }

    /**
     * 服务器内部错误响应
     */
    public static <T> Result<T> internalError() {
        return error(500, "服务器内部错误");
    }
}
