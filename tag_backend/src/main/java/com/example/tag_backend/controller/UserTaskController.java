package com.example.tag_backend.controller;

import com.example.tag_backend.common.result.Result;
import com.example.tag_backend.dto.response.ImageResponse;
import com.example.tag_backend.dto.response.ImageAssignmentResponse;
import com.example.tag_backend.entity.Annotation;
import com.example.tag_backend.entity.ImageAssignment;
import com.example.tag_backend.entity.User;
import com.example.tag_backend.enums.AnnotationStatus;
import com.example.tag_backend.enums.AssignmentStatus;
import com.example.tag_backend.repository.ImageAssignmentRepository;
import com.example.tag_backend.repository.AnnotationRepository;
import com.example.tag_backend.service.ImageService;
import com.example.tag_backend.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 用户任务控制器
 * 处理用户标注任务相关的HTTP请求
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户任务管理", description = "用户标注任务相关接口")
@RequiredArgsConstructor
@Slf4j
public class UserTaskController {

    private final ImageService imageService;
    private final UserService userService;
    private final ImageAssignmentRepository imageAssignmentRepository;
    private final AnnotationRepository annotationRepository;
    private final ObjectMapper objectMapper;
    private final com.example.tag_backend.repository.ProjectRepository projectRepository;

    @GetMapping("/assignments")
    @Operation(summary = "获取分配的标注任务", description = "获取分配给当前用户的标注任务列表")
    public Result<List<ImageAssignmentResponse>> getAssignments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        log.info("获取用户 {} 的标注任务，第{}页，每页{}条", username, page, size);
        
        try {
            // 获取用户信息
            User user = userService.getUserEntityByUsername(username);
            Long userId = user.getId();
            
            // 实现真正的随机顺序：先获取所有任务ID，随机化后再获取具体数据
            List<ImageAssignment> allUserAssignments = imageAssignmentRepository.findByUserIdOrderByAssignedAtDesc(userId);
            
            // 创建强随机种子进行真正的随机化
            long dayHash = System.currentTimeMillis() / (1000 * 60 * 60 * 24);
            long salt = userId + dayHash + username.hashCode();
            Random strongRandom = new Random(salt);
            
            // 对所有任务ID进行随机打乱
            List<Long> allAssignmentIds = allUserAssignments.stream()
                .map(ImageAssignment::getId)
                .collect(Collectors.toList());
            Collections.shuffle(allAssignmentIds, strongRandom);
            
            log.info("用户 {} 共有 {} 个任务，已进行随机排序，种子值: {}", username, allAssignmentIds.size(), Long.toHexString(salt));
            
            // 根据分页获取当前页需要的任务ID
            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, allAssignmentIds.size());
            
            List<ImageAssignment> assignments = new ArrayList<>();
            if (startIndex < allAssignmentIds.size()) {
                List<Long> currentPageIds = allAssignmentIds.subList(startIndex, endIndex);
                // 根据随机化后的ID列表获取具体的任务数据
                assignments = imageAssignmentRepository.findAllById(currentPageIds);
                
                // 保持随机顺序（因为findAllById可能不保持顺序）
                Map<Long, ImageAssignment> assignmentMap = assignments.stream()
                    .collect(Collectors.toMap(ImageAssignment::getId, assignment -> assignment));
                assignments = currentPageIds.stream()
                    .map(assignmentMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            }
            
            // 手动加载关联对象以避免懒加载问题
            for (ImageAssignment assignment : assignments) {
                if (assignment.getImage() != null) {
                    assignment.getImage().getLabelConfig(); // 触发懒加载
                }
                if (assignment.getAssigner() != null) {
                    assignment.getAssigner().getUsername(); // 触发懒加载
                }
            }
            
            log.info("用户 {} 第{}页查询到 {} 个分配任务", username, page, assignments.size());
            
            // 转换为响应DTO
            List<ImageAssignmentResponse> responses = assignments.stream()
                .map(this::convertToAssignmentResponse)
                .collect(Collectors.toList());
            
            log.info("已返回用户 {} 第 {} 页的 {} 个随机排序任务", username, page, responses.size());
            
            return Result.success("获取任务列表成功", responses);
            
        } catch (Exception e) {
            log.error("获取用户任务失败", e);
            return Result.error(500, "获取任务列表失败");
        }
    }

    @GetMapping("/images/{id}")
    @Operation(summary = "获取图像详情", description = "获取指定图像的详细信息")
    public Result<ImageResponse> getImageDetail(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        log.info("用户 {} 获取图像详情，图像ID: {}", username, id);
        
        ImageResponse response = imageService.getImageById(id);
        return Result.success(response);
    }

    @PostMapping("/annotations")
    @Operation(summary = "保存标注结果", description = "保存用户的标注结果")
    public Result<Void> saveAnnotation(@RequestBody Object annotationData) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        log.info("用户 {} 保存标注结果，接收到数据: {}", username, annotationData);
        
        try {
            // 获取用户信息
            User user = userService.getUserEntityByUsername(username);
            log.info("获取到用户信息: ID={}, 用户名={}", user.getId(), user.getUsername());
            
            // 将Object转换为Map来处理
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) annotationData;
            
            // 提取必要的字段
            Object imageIdObj = dataMap.get("imageId");
            Object labelsObj = dataMap.get("labels");
            Object statusObj = dataMap.get("status");
            Object remarkObj = dataMap.get("remark"); // 新增备注字段
            
            log.info("解析标注数据 - imageId: {}, labels: {}, status: {}, remark: {}", imageIdObj, labelsObj, statusObj, remarkObj);
            
            if (imageIdObj == null) {
                log.error("缺少必要参数: imageId");
                throw new IllegalArgumentException("缺少必要参数: imageId");
            }
            
            Long imageId = Long.valueOf(imageIdObj.toString());
            String labelsJson = objectMapper.writeValueAsString(labelsObj != null ? labelsObj : new java.util.HashMap<>());
            AnnotationStatus status = statusObj != null ? 
                AnnotationStatus.valueOf(statusObj.toString()) : AnnotationStatus.DRAFT;
            String remark = remarkObj != null ? remarkObj.toString().trim() : null; // 处理备注
            
            log.info("转换后的数据 - imageId: {}, labelsJson: {}, status: {}, remark: {}", imageId, labelsJson, status, remark);
            
            // 查找现有标注记录
            java.util.Optional<Annotation> existingAnnotation = 
                annotationRepository.findByImageIdAndUserId(imageId, user.getId());
            
            Annotation annotation;
            if (existingAnnotation.isPresent()) {
                // 更新现有记录
                annotation = existingAnnotation.get();
                log.info("找到现有标注记录，ID: {}，将更新数据", annotation.getId());
                annotation.setLabels(labelsJson);
                annotation.setStatus(status);
                annotation.setRemark(remark); // 更新备注
            } else {
                // 创建新记录
                log.info("创建新的标注记录");
                annotation = new Annotation();
                annotation.setImageId(imageId);
                annotation.setUserId(user.getId());
                annotation.setLabels(labelsJson);
                annotation.setStatus(status);
                annotation.setRemark(remark); // 设置备注
            }
            
            // 保存到数据库
            log.info("准备保存标注记录到数据库...");
            Annotation savedAnnotation = annotationRepository.save(annotation);
            log.info("标注记录保存成功！ID: {}, imageId: {}, userId: {}, status: {}", 
                    savedAnnotation.getId(), savedAnnotation.getImageId(), 
                    savedAnnotation.getUserId(), savedAnnotation.getStatus());
            
            // 如果标注状态是已完成，同时更新任务分配状态
            if (status == AnnotationStatus.COMPLETED) {
                updateImageAssignmentStatus(imageId, user.getId(), AssignmentStatus.COMPLETED);
            } else if (status == AnnotationStatus.DRAFT) {
                updateImageAssignmentStatus(imageId, user.getId(), AssignmentStatus.IN_PROGRESS);
            }
            
            return Result.<Void>success("标注结果保存成功", null);
            
        } catch (Exception e) {
            log.error("保存标注结果失败，用户: {}, 数据: {}, 错误: ", username, annotationData, e);
            throw new RuntimeException("保存标注结果失败: " + e.getMessage(), e);
        }
    }

    @PutMapping("/annotations/{id}")
    @Operation(summary = "更新标注结果", description = "更新已有的标注结果")
    public Result<Void> updateAnnotation(@PathVariable Long id, @RequestBody Object annotationData) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        log.info("用户 {} 更新标注结果，标注ID: {}", username, id);
        
        // TODO: 实现更新标注结果逻辑
        return Result.<Void>success("标注结果更新成功", null);
    }

    @GetMapping("/annotations")
    @Operation(summary = "获取标注历史", description = "获取用户的标注历史记录")
    public Result<List<Object>> getAnnotationHistory() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        log.info("获取用户 {} 的标注历史", username);
        
        // TODO: 实现获取标注历史逻辑
        return Result.success("获取标注历史成功", List.of());
    }

    @GetMapping("/task-list")
    @Operation(summary = "获取用户任务ID列表", description = "获取当前用户的所有任务ID，用于快速导航")
    public Result<List<Map<String, Object>>> getTaskIdList() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        log.info("获取用户 {} 的任务ID列表", username);
        
        try {
            // 获取用户信息
            User user = userService.getUserEntityByUsername(username);
            Long userId = user.getId();
            
            // 查找用户的所有任务，只获取ID和基本信息
            List<ImageAssignment> assignments = imageAssignmentRepository.findByUserIdOrderByAssignedAtDesc(userId);
            
            // 使用相同的随机化算法确保任务导航列表也是随机的
            long dayHash = System.currentTimeMillis() / (1000 * 60 * 60 * 24);
            long salt = userId + dayHash + username.hashCode();
            Random strongRandom = new Random(salt);
            
            // 对任务进行随机排序
            Collections.shuffle(assignments, strongRandom);
            
            List<Map<String, Object>> taskList = assignments.stream().map(assignment -> {
                Map<String, Object> taskInfo = new HashMap<>();
                taskInfo.put("id", assignment.getId());
                taskInfo.put("status", assignment.getStatus());
                taskInfo.put("imageId", assignment.getImage().getId());
                taskInfo.put("filename", assignment.getImage().getFilename());
                taskInfo.put("isCompleted", assignment.getIsCompleted() != null ? assignment.getIsCompleted() : false);
                taskInfo.put("projectId", assignment.getProjectId() != null ? assignment.getProjectId() : 1L);
                return taskInfo;
            }).collect(Collectors.toList());
            
            log.info("用户 {} 共有 {} 个任务，任务导航列表已随机排序，种子值: {}", username, taskList.size(), Long.toHexString(salt));
            return Result.success("获取任务列表成功", taskList);
            
        } catch (Exception e) {
            log.error("获取用户任务ID列表失败", e);
            return Result.error(500, "获取任务列表失败");
        }
    }

    @GetMapping("/task/{taskId}")
    @Operation(summary = "根据任务ID获取任务详情", description = "获取指定任务的完整信息")
    public Result<Map<String, Object>> getTaskById(@PathVariable Long taskId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        log.info("用户 {} 获取任务 {} 的详情", username, taskId);
        
        try {
            // 获取用户信息
            User user = userService.getUserEntityByUsername(username);
            Long userId = user.getId();
            
            // 查找任务并验证用户权限
            Optional<ImageAssignment> assignmentOpt = imageAssignmentRepository.findByIdAndUserId(taskId, userId);
            
            if (assignmentOpt.isPresent()) {
                ImageAssignment assignment = assignmentOpt.get();
                
                // 手动触发懒加载
                if (assignment.getImage() != null) {
                    assignment.getImage().getLabelConfig();
                }
                if (assignment.getAssigner() != null) {
                    assignment.getAssigner().getUsername();
                }
                
                // 构建响应对象
                Map<String, Object> response = new HashMap<>();
                response.put("id", assignment.getId());
                response.put("status", assignment.getStatus().toString());
                response.put("isCompleted", assignment.getIsCompleted() != null ? assignment.getIsCompleted() : false);
                response.put("assignedAt", assignment.getAssignedAt());
                response.put("completedAt", assignment.getCompletedAt());
                response.put("projectId", assignment.getProjectId() != null ? assignment.getProjectId() : 1L);
                
                // 添加图像信息
                Map<String, Object> image = new HashMap<>();
                image.put("id", assignment.getImage().getId());
                image.put("filename", assignment.getImage().getFilename());
                image.put("originalName", assignment.getImage().getOriginalName());
                image.put("url", "/uploads/" + assignment.getImage().getFilename());
                if (assignment.getImage().getLabelConfig() != null) {
                    Map<String, Object> labelConfig = new HashMap<>();
                    labelConfig.put("id", assignment.getImage().getLabelConfig().getId());
                    labelConfig.put("name", assignment.getImage().getLabelConfig().getName());
                    labelConfig.put("config", assignment.getImage().getLabelConfig().getConfig());
                    image.put("labelConfig", labelConfig);
                }
                response.put("image", image);
                
                return Result.success("获取任务详情成功", response);
            } else {
                return Result.error(404, "任务不存在或无权限访问");
            }
            
        } catch (Exception e) {
            log.error("获取任务详情失败", e);
            return Result.error(500, "获取任务详情失败");
        }
    }

    @GetMapping("/annotations/image/{imageId}")
    @Operation(summary = "获取用户对特定图片的标注", description = "获取当前用户对指定图片的已有标注数据")
    public Result<Map<String, Object>> getUserAnnotationForImage(@PathVariable Long imageId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        log.info("获取用户 {} 对图片 {} 的标注数据", username, imageId);
        
        try {
            // 获取用户信息
            User user = userService.getUserEntityByUsername(username);
            Long userId = user.getId();
            
            // 查找用户对该图片的标注记录
            Optional<Annotation> annotationOpt = annotationRepository.findByImageIdAndUserId(imageId, userId);
            
            if (annotationOpt.isPresent()) {
                Annotation annotation = annotationOpt.get();
                Map<String, Object> result = new HashMap<>();
                result.put("id", annotation.getId());
                result.put("labels", annotation.getLabels());
                result.put("status", annotation.getStatus());
                result.put("remark", annotation.getRemark()); // 添加备注字段
                result.put("createdAt", annotation.getCreatedAt());
                result.put("updatedAt", annotation.getUpdatedAt());
                
                log.info("找到用户 {} 对图片 {} 的标注记录，ID: {}", username, imageId, annotation.getId());
                return Result.success("获取标注数据成功", result);
            } else {
                log.info("用户 {} 对图片 {} 暂无标注记录", username, imageId);
                return Result.success("暂无标注记录", null);
            }
            
        } catch (Exception e) {
            log.error("获取用户标注数据失败", e);
            return Result.error(500, "获取标注数据失败");
        }
    }


    @DeleteMapping("/revoke-assignment/{assignmentId}")
    @Operation(summary = "收回图像分配", description = "管理员收回已分配的图像任务")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> revokeAssignment(@PathVariable Long assignmentId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminUsername = auth.getName();
        log.info("管理员 {} 收回分配任务 {}", adminUsername, assignmentId);
        
        try {
            // 查找分配记录
            Optional<ImageAssignment> assignmentOpt = imageAssignmentRepository.findById(assignmentId);
            
            if (assignmentOpt.isPresent()) {
                ImageAssignment assignment = assignmentOpt.get();
                
                // 检查任务状态，如果已经标注完成，需要特殊处理
                if (assignment.getStatus() == AssignmentStatus.COMPLETED) {
                    log.warn("尝试收回已完成的任务 {}", assignmentId);
                    return Result.error(400, "无法收回已完成的任务，请联系系统管理员");
                }
                
                // 删除分配记录
                imageAssignmentRepository.delete(assignment);
                
                log.info("成功收回分配任务 {}，原分配用户ID: {}", assignmentId, assignment.getUserId());
                return Result.success("分配已成功收回", null);
                
            } else {
                log.warn("未找到分配记录 {}", assignmentId);
                return Result.error(404, "分配记录不存在");
            }
            
        } catch (Exception e) {
            log.error("收回分配失败", e);
            return Result.error(500, "收回分配失败");
        }
    }

    @DeleteMapping("/revoke-assignment-by-image/{imageId}")
    @Operation(summary = "根据图像ID收回分配", description = "管理员根据图像ID收回所有相关的分配任务")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> revokeAssignmentByImage(@PathVariable Long imageId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminUsername = auth.getName();
        log.info("管理员 {} 收回图像 {} 的所有分配", adminUsername, imageId);
        
        try {
            // 查找该图像的所有分配记录
            List<ImageAssignment> assignments = imageAssignmentRepository.findByImageId(imageId);
            
            if (assignments.isEmpty()) {
                log.warn("图像 {} 没有分配记录", imageId);
                return Result.error(404, "该图像没有分配记录");
            }
            
            int revokedCount = 0;
            int completedCount = 0;
            List<String> completedUsers = new ArrayList<>();
            
            for (ImageAssignment assignment : assignments) {
                if (assignment.getStatus() == AssignmentStatus.COMPLETED) {
                    completedCount++;
                    // 避免懒加载问题，使用userId
                    completedUsers.add("用户ID: " + assignment.getUserId());
                } else {
                    imageAssignmentRepository.delete(assignment);
                    revokedCount++;
                    log.info("收回分配：图像 {} 从用户 {} 处收回", imageId, assignment.getUserId());
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("revokedCount", revokedCount);
            result.put("completedCount", completedCount);
            result.put("completedUsers", completedUsers);
            
            String message;
            if (completedCount > 0) {
                message = String.format("收回完成：成功收回 %d 个分配，%d 个已完成的分配无法收回", revokedCount, completedCount);
            } else {
                message = String.format("成功收回图像的 %d 个分配", revokedCount);
            }
            
            log.info("管理员 {} 收回图像 {} 结果: {}", adminUsername, imageId, message);
            return Result.success(message, result);
            
        } catch (Exception e) {
            log.error("收回图像分配失败", e);
            return Result.error(500, "收回图像分配失败");
        }
    }

    @PostMapping("/batch-revoke-assignments")
    @Operation(summary = "批量收回图像分配", description = "管理员批量收回图像分配（支持按图像ID）")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> batchRevokeAssignments(@RequestBody Map<String, Object> request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminUsername = auth.getName();
        
        @SuppressWarnings("unchecked")
        List<Object> rawImageIds = (List<Object>) request.get("assignmentIds"); // 这里实际传入的是图像ID
        
        // 安全地转换为Long类型，处理Integer到Long的转换
        List<Long> imageIds = rawImageIds.stream()
            .map(id -> {
                if (id instanceof Number) {
                    return ((Number) id).longValue();
                } else {
                    return Long.parseLong(id.toString());
                }
            })
            .collect(Collectors.toList());
        
        log.info("管理员 {} 批量收回 {} 个图像的分配", adminUsername, imageIds.size());
        
        try {
            int successCount = 0;
            int failedCount = 0;
            int completedCount = 0;
            List<String> errors = new ArrayList<>();
            
            for (Long imageId : imageIds) {
                try {
                    // 查找该图像的所有分配记录
                    List<ImageAssignment> assignments = imageAssignmentRepository.findByImageId(imageId);
                    
                    if (assignments.isEmpty()) {
                        errors.add("图像 " + imageId + " 没有分配记录");
                        failedCount++;
                        continue;
                    }
                    
                    boolean hasRevoked = false;
                    for (ImageAssignment assignment : assignments) {
                        if (assignment.getStatus() == AssignmentStatus.COMPLETED) {
                            completedCount++;
                        } else {
                            imageAssignmentRepository.delete(assignment);
                            hasRevoked = true;
                            log.info("收回分配：图像 {} 从用户 {} 处收回", imageId, assignment.getUserId());
                        }
                    }
                    
                    if (hasRevoked) {
                        successCount++;
                    }
                    
                } catch (Exception e) {
                    log.error("收回图像 {} 分配失败", imageId, e);
                    errors.add("图像 " + imageId + " 收回失败: " + e.getMessage());
                    failedCount++;
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("failedCount", failedCount);
            result.put("completedCount", completedCount);
            result.put("errors", errors);
            
            String message = String.format("批量收回完成：成功 %d 个图像，失败 %d 个，%d 个已完成无法收回", 
                successCount, failedCount, completedCount);
            log.info("管理员 {} 批量收回结果: {}", adminUsername, message);
            
            return Result.success(message, result);
            
        } catch (Exception e) {
            log.error("批量收回分配失败", e);
            return Result.error(500, "批量收回分配失败");
        }
    }

    /**
     * 转换为分配任务响应DTO
     */
    private ImageAssignmentResponse convertToAssignmentResponse(ImageAssignment assignment) {
        ImageAssignmentResponse response = new ImageAssignmentResponse();
        response.setId(assignment.getId());
        response.setStatus(assignment.getStatus());
        response.setAssignedAt(assignment.getAssignedAt());
        response.setCompletedAt(assignment.getCompletedAt());
        
        // 设置图像信息
        if (assignment.getImage() != null) {
            ImageResponse imageResponse = imageService.getImageById(assignment.getImageId());
            response.setImage(imageResponse);
        }
        
        // 设置分配者信息
        if (assignment.getAssigner() != null) {
            response.setAssignerUsername(assignment.getAssigner().getUsername());
        }
        
        // 检查是否有标注记录
        boolean hasAnnotation = annotationRepository.existsByImageIdAndUserId(
            assignment.getImageId(), assignment.getUserId());
        response.setHasAnnotation(hasAnnotation);
        
        // 计算进度
        int progress = calculateProgress(assignment.getStatus(), hasAnnotation);
        response.setProgress(progress);
        
        return response;
    }

    /**
     * 计算任务完成进度
     */
    private int calculateProgress(com.example.tag_backend.enums.AssignmentStatus status, boolean hasAnnotation) {
        switch (status) {
            case ASSIGNED:
                return hasAnnotation ? 25 : 0;
            case IN_PROGRESS:
                return hasAnnotation ? 75 : 50;
            case COMPLETED:
                return 100;
            default:
                return 0;
        }
    }

    /**
     * 更新任务分配状态
     */
    private void updateImageAssignmentStatus(Long imageId, Long userId, AssignmentStatus newStatus) {
        try {
            log.info("更新任务分配状态：imageId={}, userId={}, newStatus={}", imageId, userId, newStatus);
            
            Optional<ImageAssignment> assignmentOpt = imageAssignmentRepository.findByImageIdAndUserId(imageId, userId);
            if (assignmentOpt.isPresent()) {
                ImageAssignment assignment = assignmentOpt.get();
                assignment.setStatus(newStatus);
                
                // 如果是完成状态，设置完成时间
                if (newStatus == AssignmentStatus.COMPLETED) {
                    assignment.setCompletedAt(java.time.LocalDateTime.now());
                }
                
                imageAssignmentRepository.save(assignment);
                log.info("任务分配状态更新成功：assignmentId={}, status={}", assignment.getId(), newStatus);
            } else {
                log.warn("未找到对应的任务分配记录：imageId={}, userId={}", imageId, userId);
            }
        } catch (Exception e) {
            log.error("更新任务分配状态失败：imageId={}, userId={}, newStatus={}", imageId, userId, newStatus, e);
        }
    }

    /**
     * 标记任务为完成状态
     */
    @PostMapping("/task/{taskId}/complete")
    @Operation(summary = "完成任务", description = "标记指定任务为完成状态，任务将变为只读")
    public Result<Void> completeTask(@PathVariable Long taskId) {
        log.info("用户完成任务请求：taskId={}", taskId);

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User currentUser = userService.getUserEntityByUsername(username);

            // 查找用户的任务分配
            Optional<ImageAssignment> assignmentOpt = imageAssignmentRepository.findByIdAndUserId(taskId, currentUser.getId());
            
            if (assignmentOpt.isEmpty()) {
                log.warn("未找到对应的任务分配：taskId={}, userId={}", taskId, currentUser.getId());
                return Result.error(404, "未找到对应的任务");
            }

            ImageAssignment assignment = assignmentOpt.get();
            
            // 更新任务状态为完成
            assignment.setStatus(AssignmentStatus.COMPLETED);
            assignment.setCompletedAt(java.time.LocalDateTime.now());
            assignment.setIsCompleted(true); // 添加完成标识
            
            imageAssignmentRepository.save(assignment);
            
            log.info("任务完成成功：assignmentId={}, taskId={}", assignment.getId(), taskId);
            return Result.success("任务已完成", null);

        } catch (Exception e) {
            log.error("完成任务失败：taskId={}", taskId, e);
            return Result.error(500, "完成任务失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户的项目列表
     */
    @GetMapping("/projects")
    @Operation(summary = "获取项目列表", description = "获取当前用户分配的项目列表")
    public Result<List<Map<String, Object>>> getUserProjects() {
        log.info("获取用户项目列表");

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User currentUser = userService.getUserEntityByUsername(username);

            // 查询用户的所有任务分配，按项目分组
            List<ImageAssignment> assignments = imageAssignmentRepository.findByUserIdOrderByAssignedAtDesc(currentUser.getId());
            
            // 按项目分组统计
            Map<String, List<ImageAssignment>> projectGroups = assignments.stream()
                .collect(Collectors.groupingBy(assignment -> {
                    // 使用project_id作为项目标识
                    return assignment.getProjectId() != null 
                        ? assignment.getProjectId().toString() 
                        : "1"; // 默认项目ID为1
                }));

            List<Map<String, Object>> projects = new ArrayList<>();
            
            for (Map.Entry<String, List<ImageAssignment>> entry : projectGroups.entrySet()) {
                String projectId = entry.getKey();
                List<ImageAssignment> projectAssignments = entry.getValue();
                
                Map<String, Object> project = new HashMap<>();
                project.put("id", projectId);
                
                // 从数据库读取项目信息
                try {
                    Long projectIdLong = Long.parseLong(projectId);
                    Optional<com.example.tag_backend.entity.Project> projectEntity = projectRepository.findById(projectIdLong);
                    
                    if (projectEntity.isPresent()) {
                        com.example.tag_backend.entity.Project p = projectEntity.get();
                        project.put("name", p.getName());
                        project.put("description", p.getDescription());
                        project.put("status", p.getStatus());
                        project.put("createdAt", p.getCreatedAt());
                    } else {
                        // 如果数据库中找不到项目，使用默认值
                        project.put("name", "标注项目 " + projectId);
                        project.put("description", "包含 " + projectAssignments.size() + " 个标注任务");
                        project.put("status", "active");
                        project.put("createdAt", projectAssignments.stream()
                            .map(ImageAssignment::getAssignedAt)
                            .filter(Objects::nonNull)
                            .min(java.time.LocalDateTime::compareTo)
                            .orElse(java.time.LocalDateTime.now()));
                    }
                } catch (NumberFormatException e) {
                    // 如果projectId不是数字，使用默认值
                    project.put("name", "标注项目 " + projectId);
                    project.put("description", "包含 " + projectAssignments.size() + " 个标注任务");
                    project.put("status", "active");
                    project.put("createdAt", projectAssignments.stream()
                        .map(ImageAssignment::getAssignedAt)
                        .filter(Objects::nonNull)
                        .min(java.time.LocalDateTime::compareTo)
                        .orElse(java.time.LocalDateTime.now()));
                }
                
                project.put("totalTasks", projectAssignments.size());
                project.put("completedTasks", projectAssignments.stream()
                    .mapToInt(a -> a.getStatus() == AssignmentStatus.COMPLETED ? 1 : 0)
                    .sum());
                project.put("inProgressTasks", projectAssignments.stream()
                    .mapToInt(a -> a.getStatus() == AssignmentStatus.IN_PROGRESS ? 1 : 0)
                    .sum());
                
                projects.add(project);
            }

            log.info("获取到 {} 个项目", projects.size());
            return Result.success("获取项目列表成功", projects);

        } catch (Exception e) {
            log.error("获取项目列表失败", e);
            return Result.error(500, "获取项目列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取项目详情
     */
    @GetMapping("/projects/{projectId}")
    @Operation(summary = "获取项目详情", description = "获取指定项目的详细信息")
    public Result<Map<String, Object>> getProjectDetail(@PathVariable String projectId) {
        log.info("获取项目详情：projectId={}", projectId);

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User currentUser = userService.getUserEntityByUsername(username);

            // 获取项目相关的任务
            List<ImageAssignment> assignments = imageAssignmentRepository.findByUserIdOrderByAssignedAtDesc(currentUser.getId())
                .stream()
                .filter(a -> {
                    String assignmentProjectId = a.getProjectId() != null 
                        ? a.getProjectId().toString() 
                        : "1"; // 默认项目ID为1
                    return projectId.equals(assignmentProjectId);
                })
                .collect(Collectors.toList());

            if (assignments.isEmpty()) {
                return Result.error(404, "项目不存在");
            }

            Map<String, Object> project = new HashMap<>();
            project.put("id", projectId);
            
            // 从数据库读取项目信息
            try {
                Long projectIdLong = Long.parseLong(projectId);
                Optional<com.example.tag_backend.entity.Project> projectEntity = projectRepository.findById(projectIdLong);
                
                if (projectEntity.isPresent()) {
                    com.example.tag_backend.entity.Project p = projectEntity.get();
                    project.put("name", p.getName());
                    project.put("description", p.getDescription());
                    project.put("status", p.getStatus());
                } else {
                    project.put("name", "标注项目 " + projectId);
                    project.put("description", "包含 " + assignments.size() + " 个标注任务");
                    project.put("status", "active");
                }
            } catch (NumberFormatException e) {
                project.put("name", "标注项目 " + projectId);
                project.put("description", "包含 " + assignments.size() + " 个标注任务");
                project.put("status", "active");
            }
            
            project.put("totalTasks", assignments.size());
            project.put("completedTasks", assignments.stream()
                .mapToInt(a -> a.getStatus() == AssignmentStatus.COMPLETED ? 1 : 0)
                .sum());

            return Result.success("获取项目详情成功", project);

        } catch (Exception e) {
            log.error("获取项目详情失败：projectId={}", projectId, e);
            return Result.error(500, "获取项目详情失败：" + e.getMessage());
        }
    }

    /**
     * 获取项目的任务列表
     */
    @GetMapping("/projects/{projectId}/tasks")
    @Operation(summary = "获取项目任务", description = "获取指定项目的任务列表")
    public Result<List<Map<String, Object>>> getProjectTasks(@PathVariable String projectId) {
        log.info("获取项目任务列表：projectId={}", projectId);

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User currentUser = userService.getUserEntityByUsername(username);

            // 获取项目相关的任务
            List<ImageAssignment> assignments = imageAssignmentRepository.findByUserIdOrderByAssignedAtDesc(currentUser.getId())
                .stream()
                .filter(a -> {
                    String assignmentProjectId = a.getProjectId() != null 
                        ? a.getProjectId().toString() 
                        : "1"; // 默认项目ID为1
                    return projectId.equals(assignmentProjectId);
                })
                .collect(Collectors.toList());

            // 转换为任务列表格式
            List<Map<String, Object>> tasks = assignments.stream()
                .map(assignment -> {
                    Map<String, Object> task = new HashMap<>();
                    task.put("id", assignment.getId());
                    task.put("imageId", assignment.getImage().getId());
                    task.put("filename", assignment.getImage().getFilename());
                    task.put("status", assignment.getStatus().toString());
                    task.put("isCompleted", assignment.getIsCompleted() != null ? assignment.getIsCompleted() : false);
                    task.put("assignedAt", assignment.getAssignedAt());
                    task.put("completedAt", assignment.getCompletedAt());
                    task.put("projectId", assignment.getProjectId() != null ? assignment.getProjectId() : 1L);
                    
                    // 添加图像信息
                    Map<String, Object> image = new HashMap<>();
                    image.put("id", assignment.getImage().getId());
                    image.put("filename", assignment.getImage().getFilename());
                    image.put("originalName", assignment.getImage().getOriginalName());
                    image.put("url", "/uploads/" + assignment.getImage().getFilename());
                    task.put("image", image);
                    
                    return task;
                })
                .collect(Collectors.toList());

            // 随机打乱任务顺序
            Collections.shuffle(tasks, new Random());

            log.info("获取到项目 {} 的 {} 个任务", projectId, tasks.size());
            return Result.success("获取项目任务成功", tasks);

        } catch (Exception e) {
            log.error("获取项目任务失败：projectId={}", projectId, e);
            return Result.error(500, "获取项目任务失败：" + e.getMessage());
        }
    }
}
