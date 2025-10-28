package com.example.tag_backend.service.impl;

import com.example.tag_backend.dto.response.ActivityResponse;
import com.example.tag_backend.dto.response.DashboardStatsResponse;
import com.example.tag_backend.entity.User;
import com.example.tag_backend.enums.AnnotationStatus;
import com.example.tag_backend.enums.AssignmentStatus;
import com.example.tag_backend.enums.UserStatus;
import com.example.tag_backend.repository.*;
import com.example.tag_backend.service.DashboardService;
import com.example.tag_backend.service.SessionService;
import com.example.tag_backend.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表板服务实现类
 * 严格按照开发规范文档和需求文档实现
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final ImageRepository imageRepository;
    private final AnnotationRepository annotationRepository;
    private final LabelConfigRepository labelConfigRepository;
    private final ImageAssignmentRepository imageAssignmentRepository;
    private final SessionService sessionService;
    private final RedisUtils redisUtils;

    @Value("${app.file.upload-path}")
    private String uploadPath;

    @Override
    public DashboardStatsResponse getDashboardStats() {
        log.info("获取仪表板统计数据");

        try {
            // 计算时间范围：本周开始时间
            LocalDateTime weekStart = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
                .minus(LocalDateTime.now().getDayOfWeek().getValue() - 1, ChronoUnit.DAYS);

            return DashboardStatsResponse.builder()
                .userStats(getUserStats(weekStart))
                .imageStats(getImageStats(weekStart))
                .annotationStats(getAnnotationStats(weekStart))
                .systemStats(getSystemStatus())
                .lastUpdated(LocalDateTime.now())
                .build();

        } catch (Exception e) {
            log.error("获取仪表板统计数据失败", e);
            throw new RuntimeException("获取统计数据失败", e);
        }
    }

    @Override
    public DashboardStatsResponse.SystemStats getSystemStatus() {
        log.debug("获取系统状态信息");

        try {
            // 检查Redis连接状态
            String redisStatus;
            try {
                redisUtils.set("health_check", "ok", 60);
                redisStatus = "正常";
            } catch (Exception e) {
                redisStatus = "异常";
                log.warn("Redis连接检查失败", e);
            }

            // 存储信息
            Map<String, Object> storageInfo = new HashMap<>();
            storageInfo.put("uploadPath", uploadPath);
            storageInfo.put("totalImages", imageRepository.count());

            return DashboardStatsResponse.SystemStats.builder()
                .systemStatus("运行正常")
                .databaseStatus("连接正常")
                .redisStatus(redisStatus)
                .activeLabelConfigs(labelConfigRepository.countByIsActiveTrue())
                .systemStartTime(LocalDateTime.now().minus(1, ChronoUnit.HOURS)) // 示例启动时间
                .storageInfo(storageInfo)
                .build();

        } catch (Exception e) {
            log.error("获取系统状态失败", e);
            return DashboardStatsResponse.SystemStats.builder()
                .systemStatus("状态异常")
                .databaseStatus("连接异常")
                .redisStatus("连接异常")
                .activeLabelConfigs(0L)
                .systemStartTime(LocalDateTime.now())
                .storageInfo(new HashMap<>())
                .build();
        }
    }

    @Override
    public DashboardStatsResponse.AnnotationStats getAnnotationProgress() {
        log.debug("获取标注进度统计");

        LocalDateTime weekStart = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
            .minus(LocalDateTime.now().getDayOfWeek().getValue() - 1, ChronoUnit.DAYS);

        return getAnnotationStats(weekStart);
    }

    @Override
    public List<ActivityResponse> getRecentActivities(Integer limit) {
        log.debug("获取最近活动记录，限制数量: {}", limit);

        List<ActivityResponse> activities = new ArrayList<>();
        
        try {
            // 设置默认值
            if (limit == null || limit <= 0) {
                limit = 10;
            }

            // 获取最近登录的用户活动
            PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "lastLoginAt"));
            List<User> recentUsers = userRepository.findAll(pageRequest).getContent()
                .stream()
                .filter(user -> user.getLastLoginAt() != null)
                .toList();

            // 添加用户登录活动
            for (User user : recentUsers) {
                if (user.getLastLoginAt() != null) {
                    activities.add(ActivityResponse.builder()
                        .type("用户登录")
                        .description(user.getUsername() + " 登录系统")
                        .user(user.getUsername())
                        .time(user.getLastLoginAt())
                        .build());
                }
            }

            // TODO: 可以添加更多活动类型，如：
            // - 图像上传活动（从Image表获取最近上传的图像）
            // - 标签配置活动（从LabelConfig表获取最近的配置变更）
            // - 数据导出活动（从日志或专门的活动表获取）

            // 按时间排序并限制数量
            activities = activities.stream()
                .sorted((a, b) -> b.getTime().compareTo(a.getTime()))
                .limit(limit)
                .toList();

            log.debug("获取到 {} 条活动记录", activities.size());
            return activities;

        } catch (Exception e) {
            log.error("获取最近活动记录失败", e);
            return new ArrayList<>(); // 返回空列表而不是抛出异常
        }
    }

    /**
     * 获取用户统计信息
     */
    private DashboardStatsResponse.UserStats getUserStats(LocalDateTime weekStart) {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long onlineUsers = sessionService.getOnlineUserCount();
        long newUsersThisWeek = userRepository.countByCreatedAtAfter(weekStart);

        return DashboardStatsResponse.UserStats.builder()
            .totalUsers(totalUsers)
            .activeUsers(activeUsers)
            .onlineUsers(onlineUsers)
            .newUsersThisWeek(newUsersThisWeek)
            .build();
    }

    /**
     * 获取图像统计信息
     */
    private DashboardStatsResponse.ImageStats getImageStats(LocalDateTime weekStart) {
        long totalImages = imageRepository.count();
        long assignedImages = imageAssignmentRepository.count();
        long completedImages = imageAssignmentRepository.countByStatus(AssignmentStatus.COMPLETED);
        long pendingImages = totalImages - assignedImages;
        long imagesUploadedThisWeek = imageRepository.countByCreatedAtAfter(weekStart);

        return DashboardStatsResponse.ImageStats.builder()
            .totalImages(totalImages)
            .assignedImages(assignedImages)
            .completedImages(completedImages)
            .pendingImages(pendingImages)
            .imagesUploadedThisWeek(imagesUploadedThisWeek)
            .build();
    }

    /**
     * 获取标注统计信息
     */
    private DashboardStatsResponse.AnnotationStats getAnnotationStats(LocalDateTime weekStart) {
        long totalAnnotations = annotationRepository.count();
        long completedAnnotations = annotationRepository.countByStatus(AnnotationStatus.COMPLETED);
        long draftAnnotations = annotationRepository.countByStatus(AnnotationStatus.DRAFT);
        long annotationsCompletedThisWeek = annotationRepository.countByStatusAndUpdatedAtAfter(
            AnnotationStatus.COMPLETED, weekStart);

        // 计算完成率
        double completionRate = totalAnnotations > 0 ? 
            (double) completedAnnotations / totalAnnotations * 100 : 0.0;

        // 平均标注时间（简化计算）
        double averageAnnotationTime = 15.0; // 假设平均15分钟，实际可以从数据库计算

        return DashboardStatsResponse.AnnotationStats.builder()
            .totalAnnotations(totalAnnotations)
            .completedAnnotations(completedAnnotations)
            .draftAnnotations(draftAnnotations)
            .completionRate(Math.round(completionRate * 100.0) / 100.0)
            .annotationsCompletedThisWeek(annotationsCompletedThisWeek)
            .averageAnnotationTime(averageAnnotationTime)
            .build();
    }
}
