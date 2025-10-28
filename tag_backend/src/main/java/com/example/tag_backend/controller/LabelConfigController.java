package com.example.tag_backend.controller;

import com.example.tag_backend.common.result.PageResponse;
import com.example.tag_backend.common.result.Result;
import com.example.tag_backend.dto.request.CreateLabelConfigRequest;
import com.example.tag_backend.dto.request.LabelConfigQueryRequest;
import com.example.tag_backend.dto.response.LabelConfigResponse;
import com.example.tag_backend.service.LabelConfigService;
import com.example.tag_backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签配置控制器
 * 处理标签配置相关的HTTP请求
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@RestController
@RequestMapping("/api/admin/label-configs")
@Tag(name = "标签配置管理", description = "标签配置的创建、更新、删除等相关接口")
@RequiredArgsConstructor
@Slf4j
public class LabelConfigController {

    private final LabelConfigService labelConfigService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "获取标签配置列表", description = "分页获取标签配置列表")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResponse<LabelConfigResponse>> getLabelConfigList(
            @Valid @ParameterObject LabelConfigQueryRequest request) {
        log.info("获取标签配置列表请求: {}", request);
        PageResponse<LabelConfigResponse> response = labelConfigService.getLabelConfigList(request);
        return Result.success(response);
    }

    @GetMapping("/active")
    @Operation(summary = "获取激活的标签配置", description = "获取所有激活状态的标签配置")
    public Result<List<LabelConfigResponse>> getActiveLabelConfigs() {
        log.info("获取激活标签配置请求");
        List<LabelConfigResponse> response = labelConfigService.getActiveLabelConfigs();
        return Result.success(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取标签配置详情", description = "根据ID获取标签配置详细信息")
    public Result<LabelConfigResponse> getLabelConfigById(
            @Parameter(description = "标签配置ID") @PathVariable Long id) {
        log.info("获取标签配置详情请求，ID: {}", id);
        LabelConfigResponse response = labelConfigService.getLabelConfigById(id);
        return Result.success(response);
    }

    @PostMapping
    @Operation(summary = "创建标签配置", description = "创建新的标签配置")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<LabelConfigResponse> createLabelConfig(
            @Valid @RequestBody CreateLabelConfigRequest request) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Long createdBy = userService.getUserEntityByUsername(username).getId();

        log.info("创建标签配置请求，名称: {}, 创建者: {}", request.getName(), username);
        LabelConfigResponse response = labelConfigService.createLabelConfig(request, createdBy);
        return Result.success("标签配置创建成功", response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新标签配置", description = "更新标签配置信息")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<LabelConfigResponse> updateLabelConfig(
            @Parameter(description = "标签配置ID") @PathVariable Long id,
            @Valid @RequestBody CreateLabelConfigRequest request) {
        log.info("更新标签配置请求，ID: {}", id);
        LabelConfigResponse response = labelConfigService.updateLabelConfig(id, request);
        return Result.success("标签配置更新成功", response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除标签配置", description = "删除指定标签配置")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteLabelConfig(
            @Parameter(description = "标签配置ID") @PathVariable Long id) {
        log.info("删除标签配置请求，ID: {}", id);
        labelConfigService.deleteLabelConfig(id);
        return Result.<Void>success("标签配置删除成功", null);
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "启用标签配置", description = "启用指定标签配置")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> enableLabelConfig(
            @Parameter(description = "标签配置ID") @PathVariable Long id) {
        log.info("启用标签配置请求，ID: {}", id);
        labelConfigService.enableLabelConfig(id);
        return Result.<Void>success("标签配置启用成功", null);
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "禁用标签配置", description = "禁用指定标签配置")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> disableLabelConfig(
            @Parameter(description = "标签配置ID") @PathVariable Long id) {
        log.info("禁用标签配置请求，ID: {}", id);
        labelConfigService.disableLabelConfig(id);
        return Result.<Void>success("标签配置禁用成功", null);
    }

    @PostMapping("/validate")
    @Operation(summary = "验证标签配置", description = "验证标签配置JSON格式是否正确")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> validateLabelConfig(
            @RequestBody String configJson) {
        log.info("验证标签配置JSON请求");
        boolean isValid = labelConfigService.validateLabelConfigJson(configJson);
        String message = isValid ? "标签配置格式正确" : "标签配置格式错误";
        return Result.success(message, isValid);
    }
}
