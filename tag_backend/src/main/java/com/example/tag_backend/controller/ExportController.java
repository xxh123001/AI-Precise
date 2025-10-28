package com.example.tag_backend.controller;

import com.example.tag_backend.common.result.Result;
import com.example.tag_backend.dto.request.ExportRequest;
import com.example.tag_backend.service.ExportService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * 数据导出控制器
 * 处理数据导出相关的HTTP请求
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@RestController
@RequestMapping("/api/admin/export")
@Tag(name = "数据导出", description = "数据导出相关接口")
@RequiredArgsConstructor
@Slf4j
public class ExportController {

    private final ExportService exportService;

    @GetMapping("/annotations")
    @Operation(summary = "导出标注结果(GET)", description = "导出标注结果为指定格式文件(简单参数)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> exportAnnotationsGet(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "format", required = false, defaultValue = "xlsx") String format) {
        
        log.info("导出标注结果请求(GET)，筛选条件 - 用户ID: {}, 开始日期: {}, 结束日期: {}, 状态: {}, 格式: {}", 
                userId, startDate, endDate, status, format);
        
        // 构建导出请求
        ExportRequest request = new ExportRequest();
        if (userId != null) {
            request.setUserId(userId);
            request.setUserIds(Arrays.asList(userId));
        }
        if (startDate != null && !startDate.trim().isEmpty()) {
            request.setStartDate(LocalDate.parse(startDate));
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            request.setEndDate(LocalDate.parse(endDate));
        }
        if (status != null && !status.trim().isEmpty()) {
            request.setAnnotationStatus(status);
        }
        request.setFormat(format);

        return executeExport(request, format);
    }

    @PostMapping("/annotations")
    @Operation(summary = "导出标注结果(POST)", description = "导出标注结果为指定格式文件(支持复杂筛选)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> exportAnnotationsPost(@Valid @RequestBody ExportRequest request) {
        
        log.info("导出标注结果请求(POST)，筛选条件: {}", request);
        
        String format = request.getFormat() != null ? request.getFormat() : "xlsx";
        return executeExport(request, format);
    }

    private ResponseEntity<?> executeExport(ExportRequest request, String format) {
        try {
            // 根据格式参数决定导出类型
            if ("json".equalsIgnoreCase(format)) {
                // JSON格式导出
                Object jsonData = exportService.exportAnnotationsToJson(request);
                
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String filename = "annotations_export_" + timestamp + ".json";

                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(jsonData);
                    
            } else if ("csv".equalsIgnoreCase(format)) {
                // CSV格式导出 - 暂时返回错误
                throw new UnsupportedOperationException("CSV格式暂未支持");
                
            } else {
                // 默认Excel格式导出
                Resource resource = exportService.exportAnnotationsToExcel(request);
                
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String filename = "annotations_export_" + timestamp + ".xlsx";

                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
            }

        } catch (Exception e) {
            log.error("导出失败，格式: {}, 错误: ", format, e);
            throw e;
        }
    }

    @PostMapping("/annotations/excel")
    @Operation(summary = "导出标注数据为Excel", description = "根据筛选条件导出标注数据为Excel文件")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> exportAnnotationsToExcel(
            @Valid @RequestBody ExportRequest request) {
        log.info("导出标注数据到Excel请求: {}", request);

        try {
            Resource resource = exportService.exportAnnotationsToExcel(request);
            
            // 生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "annotations_export_" + timestamp + ".xlsx";

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);

        } catch (Exception e) {
            log.error("导出Excel失败", e);
            throw e;
        }
    }

    @PostMapping("/annotations/statistics")
    @Operation(summary = "获取标注统计数据", description = "根据筛选条件获取标注统计数据")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Object> getAnnotationStatistics(
            @Valid @RequestBody ExportRequest request) {
        log.info("获取标注统计数据请求: {}", request);
        
        Object statistics = exportService.getAnnotationStatistics(request);
        return Result.success(statistics);
    }

    @PostMapping("/annotations/preview")
    @Operation(summary = "预览标注数据", description = "预览标注数据，支持复杂筛选条件")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Object> previewAnnotations(@Valid @RequestBody ExportRequest request) {
        
        log.info("预览标注数据请求，筛选条件: {}", request);
        
        try {
            // 如果没有设置limit，使用默认值
            Integer limit = request.getLimit() != null ? request.getLimit() : 50;
            
            Object previewData = exportService.previewAnnotationsData(request, limit);
            return Result.success("预览数据获取成功", previewData);
            
        } catch (Exception e) {
            log.error("预览数据获取失败", e);
            return Result.error(500, "预览数据获取失败: " + e.getMessage());
        }
    }

    @GetMapping("/progress-report")
    @Operation(summary = "生成进度报告", description = "生成标注进度报告")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Object> generateProgressReport() {
        log.info("生成进度报告请求");
        
        Object report = exportService.generateProgressReport();
        return Result.success(report);
    }
}
