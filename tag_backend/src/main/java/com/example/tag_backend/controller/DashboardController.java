package com.example.tag_backend.controller;

import com.example.tag_backend.common.result.Result;
import com.example.tag_backend.dto.response.ActivityResponse;
import com.example.tag_backend.dto.response.DashboardStatsResponse;
import com.example.tag_backend.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 仪表板控制器
 * 严格按照开发规范文档和需求文档5.1节管理员界面要求实现
 * 处理仪表板相关的HTTP请求，显示系统概览、标注进度统计
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "仪表板管理", description = "管理员仪表板，显示系统概览和标注进度统计")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "获取仪表板统计数据", description = "获取系统概览和标注进度统计信息")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<DashboardStatsResponse> getDashboardStats() {
        log.info("获取仪表板统计数据请求");
        
        try {
            DashboardStatsResponse stats = dashboardService.getDashboardStats();
            return Result.success("获取统计数据成功", stats);
        } catch (Exception e) {
            log.error("获取仪表板统计数据失败", e);
            return Result.error(500, "获取统计数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/system-status")
    @Operation(summary = "获取系统状态", description = "获取实时系统运行状态信息")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<DashboardStatsResponse.SystemStats> getSystemStatus() {
        log.info("获取系统状态请求");
        
        try {
            DashboardStatsResponse.SystemStats systemStats = dashboardService.getSystemStatus();
            return Result.success("获取系统状态成功", systemStats);
        } catch (Exception e) {
            log.error("获取系统状态失败", e);
            return Result.error(500, "获取系统状态失败: " + e.getMessage());
        }
    }

    @GetMapping("/annotation-progress")
    @Operation(summary = "获取标注进度", description = "获取标注进度统计信息")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<DashboardStatsResponse.AnnotationStats> getAnnotationProgress() {
        log.info("获取标注进度请求");
        
        try {
            DashboardStatsResponse.AnnotationStats annotationStats = dashboardService.getAnnotationProgress();
            return Result.success("获取标注进度成功", annotationStats);
        } catch (Exception e) {
            log.error("获取标注进度失败", e);
            return Result.error(500, "获取标注进度失败: " + e.getMessage());
        }
    }

    @GetMapping("/activities")
    @Operation(summary = "获取最近活动记录", description = "获取系统最近的活动记录用于仪表板显示")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<ActivityResponse>> getRecentActivities(
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "10") Integer limit) {
        log.info("获取最近活动记录请求，限制数量: {}", limit);
        
        try {
            List<ActivityResponse> activities = dashboardService.getRecentActivities(limit);
            return Result.success("获取活动记录成功", activities);
        } catch (Exception e) {
            log.error("获取最近活动记录失败", e);
            return Result.error(500, "获取活动记录失败: " + e.getMessage());
        }
    }
}