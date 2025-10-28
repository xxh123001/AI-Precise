package com.example.tag_backend.controller;

import com.example.tag_backend.common.result.Result;
import com.example.tag_backend.dto.request.AnnotationFilterRequest;
import com.example.tag_backend.service.AnnotationFilterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 标注筛选控制器
 * 处理标注数据筛选和导出相关的HTTP请求
 * 
 * @author 辛晓红
 * @version 1.0
 * @since 2025-10-13
 */
@RestController
@RequestMapping("/api/admin/annotation-filter")
@Tag(name = "标注筛选", description = "标注数据筛选和导出相关接口")
@RequiredArgsConstructor
@Slf4j
public class AnnotationFilterController {

    private final AnnotationFilterService annotationFilterService;

    @PostMapping("/search")
    @Operation(summary = "搜索标注数据", description = "根据条件搜索标注数据")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> searchAnnotations(
            @Valid @RequestBody AnnotationFilterRequest request) {
        log.info("搜索标注数据请求: {}", request);

        try {
            Map<String, Object> result = annotationFilterService.searchAnnotations(request);
            return Result.success("搜索完成", result);
        } catch (Exception e) {
            log.error("搜索标注数据失败", e);
            return Result.error(500, "搜索失败: " + e.getMessage());
        }
    }

    @PostMapping("/filter/double-tags")
    @Operation(summary = "筛选双标签数据", description = "筛选同时包含多个指定标签的标注数据")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> filterDoubleTags(
            @Valid @RequestBody AnnotationFilterRequest request) {
        log.info("筛选双标签数据请求: {}", request);

        try {
            Map<String, Object> result = annotationFilterService.filterDoubleTags(request);
            return Result.success("筛选完成", result);
        } catch (Exception e) {
            log.error("筛选双标签数据失败", e);
            return Result.error(500, "筛选失败: " + e.getMessage());
        }
    }

    @PostMapping("/export/images")
    @Operation(summary = "导出图像文件", description = "将筛选出的图像文件打包导出")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> exportImages(
            @Valid @RequestBody AnnotationFilterRequest request) {
        log.info("导出图像文件请求: {}", request);

        try {
            Resource resource = annotationFilterService.exportImages(request);
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "filtered_images_" + timestamp + ".zip";

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);

        } catch (Exception e) {
            log.error("导出图像文件失败", e);
            return ResponseEntity.status(500).body(Result.error(500, "导出失败: " + e.getMessage()));
        }
    }

    @GetMapping("/users")
    @Operation(summary = "获取用户列表", description = "获取所有有标注记录的用户")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Map<String, Object>>> getAnnotationUsers() {
        log.info("获取标注用户列表请求");

        try {
            List<Map<String, Object>> users = annotationFilterService.getAnnotationUsers();
            return Result.success("获取成功", users);
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return Result.error(500, "获取失败: " + e.getMessage());
        }
    }

    @GetMapping("/tags/common")
    @Operation(summary = "获取常见标签", description = "获取标注中最常用的标签")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<String>> getCommonTags() {
        log.info("获取常见标签请求");

        try {
            List<String> tags = annotationFilterService.getCommonTags();
            return Result.success("获取成功", tags);
        } catch (Exception e) {
            log.error("获取常见标签失败", e);
            return Result.error(500, "获取失败: " + e.getMessage());
        }
    }

    @PostMapping("/preview")
    @Operation(summary = "预览筛选结果", description = "预览筛选结果的前几条数据")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> previewFilterResults(
            @Valid @RequestBody AnnotationFilterRequest request) {
        log.info("预览筛选结果请求: {}", request);

        try {
            Map<String, Object> result = annotationFilterService.previewFilterResults(request);
            return Result.success("预览完成", result);
        } catch (Exception e) {
            log.error("预览筛选结果失败", e);
            return Result.error(500, "预览失败: " + e.getMessage());
        }
    }
}

