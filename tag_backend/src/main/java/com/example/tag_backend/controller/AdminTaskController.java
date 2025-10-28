package com.example.tag_backend.controller;

import com.example.tag_backend.common.result.Result;
import com.example.tag_backend.entity.Annotation;
import com.example.tag_backend.entity.ImageAssignment;
import com.example.tag_backend.entity.User;
import com.example.tag_backend.enums.AnnotationStatus;
import com.example.tag_backend.enums.AssignmentStatus;
import com.example.tag_backend.repository.AnnotationRepository;
import com.example.tag_backend.repository.ImageAssignmentRepository;
import com.example.tag_backend.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员任务管理控制器
 * 处理管理员视角的任务管理功能
 * 
 * @author 辛晓红
 * @version 1.0
 * @since 2025-10-14
 */
@RestController
@RequestMapping("/api/admin/tasks")
@Tag(name = "管理员任务管理", description = "管理员任务管理相关接口")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminTaskController {

    private final ImageAssignmentRepository imageAssignmentRepository;
    private final AnnotationRepository annotationRepository;
    private final UserRepository userRepository;

    @GetMapping("/assignments")
    @Operation(summary = "获取任务分配列表", description = "管理员获取所有用户的任务分配列表")
    public Result<Map<String, Object>> getTaskAssignments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword) {
        
        log.info("管理员获取任务分配列表，参数：page={}, size={}, userId={}, status={}, startDate={}, endDate={}, keyword={}", 
                page, size, userId, status, startDate, endDate, keyword);
        
        try {
            // 构建查询条件
            Specification<ImageAssignment> spec = buildSpecification(userId, status, startDate, endDate, keyword);
            
            // 分页查询
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "assignedAt"));
            Page<ImageAssignment> assignmentPage = imageAssignmentRepository.findAll(spec, pageable);
            
            // 转换为响应DTO
            List<Map<String, Object>> assignments = assignmentPage.getContent().stream()
                    .map(this::convertToTaskInfo)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", assignments);
            response.put("totalElements", assignmentPage.getTotalElements());
            response.put("totalPages", assignmentPage.getTotalPages());
            response.put("currentPage", assignmentPage.getNumber());
            response.put("pageSize", assignmentPage.getSize());
            response.put("hasNext", assignmentPage.hasNext());
            response.put("hasPrevious", assignmentPage.hasPrevious());
            
            log.info("管理员任务分配列表查询完成，找到 {} 条记录", assignmentPage.getTotalElements());
            return Result.success("获取任务列表成功", response);
            
        } catch (Exception e) {
            log.error("获取管理员任务列表失败", e);
            return Result.error(500, "获取任务列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取任务统计信息", description = "获取任务分配和完成情况的统计信息，支持按用户和项目筛选")
    public Result<Map<String, Object>> getTaskStatistics(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "projectId", required = false) Long projectId) {
        log.info("获取任务统计信息，用户ID: {}, 项目ID: {}", userId, projectId);
        
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 根据筛选条件统计
            long totalAssignments;
            long assignedCount;
            long inProgressCount;
            long completedCount;
            
            if (userId != null && projectId != null) {
                // 同时筛选用户和项目
                totalAssignments = imageAssignmentRepository.countByUserIdAndProjectId(userId, projectId);
                assignedCount = imageAssignmentRepository.countByUserIdAndProjectIdAndStatus(userId, projectId, AssignmentStatus.ASSIGNED);
                inProgressCount = imageAssignmentRepository.countByUserIdAndProjectIdAndStatus(userId, projectId, AssignmentStatus.IN_PROGRESS);
                completedCount = imageAssignmentRepository.countByUserIdAndProjectIdAndStatus(userId, projectId, AssignmentStatus.COMPLETED);
            } else if (userId != null) {
                // 只筛选用户
                totalAssignments = imageAssignmentRepository.countByUserId(userId);
                assignedCount = imageAssignmentRepository.countByUserIdAndStatus(userId, AssignmentStatus.ASSIGNED);
                inProgressCount = imageAssignmentRepository.countByUserIdAndStatus(userId, AssignmentStatus.IN_PROGRESS);
                completedCount = imageAssignmentRepository.countByUserIdAndStatus(userId, AssignmentStatus.COMPLETED);
            } else if (projectId != null) {
                // 只筛选项目
                totalAssignments = imageAssignmentRepository.countByProjectId(projectId);
                assignedCount = imageAssignmentRepository.countByProjectIdAndStatus(projectId, AssignmentStatus.ASSIGNED);
                inProgressCount = imageAssignmentRepository.countByProjectIdAndStatus(projectId, AssignmentStatus.IN_PROGRESS);
                completedCount = imageAssignmentRepository.countByProjectIdAndStatus(projectId, AssignmentStatus.COMPLETED);
            } else {
                // 无筛选，全局统计
                totalAssignments = imageAssignmentRepository.count();
                assignedCount = imageAssignmentRepository.countByStatus(AssignmentStatus.ASSIGNED);
                inProgressCount = imageAssignmentRepository.countByStatus(AssignmentStatus.IN_PROGRESS);
                completedCount = imageAssignmentRepository.countByStatus(AssignmentStatus.COMPLETED);
            }
            
            stats.put("totalAssignments", totalAssignments);
            stats.put("assignedCount", assignedCount);
            stats.put("inProgressCount", inProgressCount);
            stats.put("completedCount", completedCount);
            
            // 计算完成率
            double completionRate = totalAssignments > 0 ? (double) completedCount / totalAssignments * 100 : 0;
            stats.put("completionRate", Math.round(completionRate * 100.0) / 100.0);
            
            // 统计总标注数量（根据筛选条件）
            long totalAnnotations;
            if (userId != null) {
                totalAnnotations = annotationRepository.countByUserId(userId);
            } else {
                totalAnnotations = annotationRepository.count();
            }
            stats.put("totalAnnotations", totalAnnotations);
            
            log.info("任务统计信息获取完成，筛选条件 - 用户: {}, 项目: {}", userId, projectId);
            return Result.success("获取统计信息成功", stats);
            
        } catch (Exception e) {
            log.error("获取任务统计信息失败", e);
            return Result.error(500, "获取统计信息失败: " + e.getMessage());
        }
    }

    @GetMapping("/users")
    @Operation(summary = "获取用户列表", description = "获取有任务分配的用户列表")
    public Result<List<Map<String, Object>>> getTaskUsers() {
        log.info("获取任务用户列表");
        
        try {
            List<User> users = userRepository.findAll();
            
            List<Map<String, Object>> userList = users.stream().map(user -> {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("username", user.getUsername());
                userMap.put("email", user.getEmail());
                
                // 统计该用户的任务数量
                long totalTasks = imageAssignmentRepository.countByUserId(user.getId());
                long completedTasks = imageAssignmentRepository.countByUserIdAndStatus(user.getId(), AssignmentStatus.COMPLETED);
                
                userMap.put("totalTasks", totalTasks);
                userMap.put("completedTasks", completedTasks);
                
                return userMap;
            }).collect(Collectors.toList());
            
            return Result.success("获取用户列表成功", userList);
            
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return Result.error(500, "获取用户列表失败: " + e.getMessage());
        }
    }

    /**
     * 构建查询条件
     */
    private Specification<ImageAssignment> buildSpecification(Long userId, String status, 
                                                              String startDate, String endDate, String keyword) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 用户ID筛选
            if (userId != null) {
                predicates.add(criteriaBuilder.equal(root.get("userId"), userId));
            }

            // 状态筛选
            if (status != null && !status.trim().isEmpty()) {
                try {
                    AssignmentStatus assignmentStatus = AssignmentStatus.valueOf(status);
                    predicates.add(criteriaBuilder.equal(root.get("status"), assignmentStatus));
                } catch (IllegalArgumentException e) {
                    log.warn("无效的任务状态: {}", status);
                }
            }

            // 日期范围筛选
            if (startDate != null && !startDate.trim().isEmpty()) {
                try {
                    LocalDateTime startDateTime = LocalDate.parse(startDate).atStartOfDay();
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("assignedAt"), startDateTime));
                } catch (Exception e) {
                    log.warn("无效的开始日期: {}", startDate);
                }
            }

            if (endDate != null && !endDate.trim().isEmpty()) {
                try {
                    LocalDateTime endDateTime = LocalDate.parse(endDate).atTime(23, 59, 59);
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("assignedAt"), endDateTime));
                } catch (Exception e) {
                    log.warn("无效的结束日期: {}", endDate);
                }
            }

            // 关键词搜索（搜索图像文件名）
            if (keyword != null && !keyword.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("image").get("filename")), 
                    "%" + keyword.toLowerCase() + "%"));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    @GetMapping("/annotation/{imageId}/{userId}")
    @Operation(summary = "获取标注数据", description = "管理员获取指定图像和用户的标注数据")
    public Result<Map<String, Object>> getAnnotation(@PathVariable Long imageId, @PathVariable Long userId) {
        log.info("管理员获取标注数据：imageId={}, userId={}", imageId, userId);
        
        try {
            Optional<Annotation> annotationOpt = annotationRepository.findByImageIdAndUserId(imageId, userId);
            
            if (annotationOpt.isPresent()) {
                Annotation annotation = annotationOpt.get();
                Map<String, Object> result = new HashMap<>();
                result.put("id", annotation.getId());
                result.put("imageId", annotation.getImageId());
                
                result.put("userId", annotation.getUserId());
                result.put("labels", annotation.getLabels());
                result.put("remark", annotation.getRemark());
                result.put("status", annotation.getStatus());
                result.put("createdAt", annotation.getCreatedAt());
                result.put("updatedAt", annotation.getUpdatedAt());
                
                return Result.success("获取标注数据成功", result);
            } else {
                return Result.error(404, "未找到标注数据");
            }
        } catch (Exception e) {
            log.error("获取标注数据失败", e);
            return Result.error(500, "获取失败: " + e.getMessage());
        }
    }

    @PutMapping("/annotation/{annotationId}")
    @Operation(summary = "更新标注数据", description = "管理员更新标注数据")
    public Result<Void> updateAnnotation(@PathVariable Long annotationId, @RequestBody Map<String, Object> data) {
        log.info("管理员更新标注数据：annotationId={}, data={}", annotationId, data);
        
        try {
            Optional<Annotation> annotationOpt = annotationRepository.findById(annotationId);
            
            if (annotationOpt.isPresent()) {
                Annotation annotation = annotationOpt.get();
                
                // 更新标注数据
                if (data.containsKey("labels")) {
                    annotation.setLabels(data.get("labels").toString());
                }
                if (data.containsKey("remark")) {
                    annotation.setRemark(data.get("remark") != null ? data.get("remark").toString() : null);
                }
                if (data.containsKey("status")) {
                    annotation.setStatus(AnnotationStatus.valueOf(data.get("status").toString()));
                }
                
                annotationRepository.save(annotation);
                log.info("标注数据更新成功：annotationId={}", annotationId);
                
                return Result.success("更新成功", null);
            } else {
                return Result.error(404, "标注数据不存在");
            }
        } catch (Exception e) {
            log.error("更新标注数据失败", e);
            return Result.error(500, "更新失败: " + e.getMessage());
        }
    }

    /**
     * 转换为任务信息
     */
    private Map<String, Object> convertToTaskInfo(ImageAssignment assignment) {
        Map<String, Object> taskInfo = new HashMap<>();
        taskInfo.put("id", assignment.getId());
        taskInfo.put("status", assignment.getStatus());
        taskInfo.put("assignedAt", assignment.getAssignedAt());
        taskInfo.put("completedAt", assignment.getCompletedAt());

        // 用户信息
        if (assignment.getUser() != null) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", assignment.getUser().getId());
            userInfo.put("username", assignment.getUser().getUsername());
            taskInfo.put("user", userInfo);
        }

        // 图像信息
        if (assignment.getImage() != null) {
            Map<String, Object> imageInfo = new HashMap<>();
            imageInfo.put("id", assignment.getImage().getId());
            imageInfo.put("filename", assignment.getImage().getFilename());
            imageInfo.put("originalName", assignment.getImage().getOriginalName());
            taskInfo.put("image", imageInfo);
        }

        // 分配者信息
        if (assignment.getAssigner() != null) {
            taskInfo.put("assignerUsername", assignment.getAssigner().getUsername());
        }

        // 检查标注状态
        Optional<Annotation> annotation = annotationRepository.findByImageIdAndUserId(
            assignment.getImageId(), assignment.getUserId());
        taskInfo.put("hasAnnotation", annotation.isPresent());
        if (annotation.isPresent()) {
            taskInfo.put("annotationStatus", annotation.get().getStatus());
        }

        return taskInfo;
    }
}
