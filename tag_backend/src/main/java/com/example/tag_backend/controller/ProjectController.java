package com.example.tag_backend.controller;

import com.example.tag_backend.common.result.Result;
import com.example.tag_backend.entity.Project;
import com.example.tag_backend.entity.ImageAssignment;
import com.example.tag_backend.entity.Image;
import com.example.tag_backend.entity.User;
import com.example.tag_backend.entity.ProjectUser;
import com.example.tag_backend.repository.ProjectRepository;
import com.example.tag_backend.repository.ImageAssignmentRepository;
import com.example.tag_backend.repository.ImageRepository;
import com.example.tag_backend.repository.UserRepository;
import com.example.tag_backend.repository.ProjectUserRepository;
import com.example.tag_backend.enums.AssignmentStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;

/**
 * 项目管理控制器
 * 处理大任务项目的管理
 * 
 * @author 辛晓红
 * @version 1.0
 * @since 2025-10-16
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "项目管理", description = "大任务项目管理相关接口")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final ImageAssignmentRepository imageAssignmentRepository;
    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final ProjectUserRepository projectUserRepository;

    /**
     * 获取所有项目列表
     */
    @GetMapping("/projects")
    @Operation(summary = "获取项目列表", description = "获取所有大任务项目列表")
    public Result<List<Map<String, Object>>> getProjects() {
        log.info("获取项目列表");

        try {
            List<Project> projects = projectRepository.findByStatusInOrderByCreatedAtDesc(
                List.of("active", "inactive", "completed", "paused")
            );

            List<Map<String, Object>> result = new ArrayList<>();
            
            for (Project project : projects) {
                Map<String, Object> projectMap = new HashMap<>();
                projectMap.put("id", project.getId());
                projectMap.put("name", project.getName());
                projectMap.put("description", project.getDescription());
                projectMap.put("status", project.getStatus());
                projectMap.put("createdAt", project.getCreatedAt());
                projectMap.put("createdBy", project.getCreatedBy());

                // 统计项目任务数量
                List<ImageAssignment> assignments = imageAssignmentRepository.findByProjectId(project.getId());
                int totalTasks = assignments.size();
                int completedTasks = (int) assignments.stream()
                    .filter(a -> a.getStatus() == AssignmentStatus.COMPLETED)
                    .count();
                int inProgressTasks = (int) assignments.stream()
                    .filter(a -> a.getStatus() == AssignmentStatus.IN_PROGRESS)
                    .count();

                // 统计分配的用户数量
                long assignedUsers = assignments.stream()
                    .map(ImageAssignment::getUserId)
                    .distinct()
                    .count();

                projectMap.put("totalTasks", totalTasks);
                projectMap.put("completedTasks", completedTasks);
                projectMap.put("inProgressTasks", inProgressTasks);
                projectMap.put("assignedUsers", assignedUsers);
                
                result.add(projectMap);
                
                log.info("项目统计 - ID: {}, 名称: {}, 总任务: {}, 完成: {}, 进行中: {}, 分配用户: {}", 
                    project.getId(), project.getName(), totalTasks, completedTasks, inProgressTasks, assignedUsers);
            }

            return Result.success("获取项目列表成功", result);

        } catch (Exception e) {
            log.error("获取项目列表失败", e);
            return Result.error(500, "获取项目列表失败: " + e.getMessage());
        }
    }

    /**
     * 创建新项目
     */
    @PostMapping("/projects")
    @Operation(summary = "创建项目", description = "创建新的大任务项目")
    public Result<Project> createProject(@RequestBody Map<String, Object> projectData) {
        log.info("创建项目: {}", projectData);

        try {
            Project project = new Project();
            project.setName((String) projectData.get("name"));
            project.setDescription((String) projectData.get("description"));
            project.setStatus("active");
            project.setCreatedBy(1L); // 这里应该从当前用户获取
            project.setRemark((String) projectData.get("remark"));

            Project savedProject = projectRepository.save(project);
            
            log.info("项目创建成功: {}", savedProject.getId());
            return Result.success("项目创建成功", savedProject);

        } catch (Exception e) {
            log.error("创建项目失败", e);
            return Result.error(500, "创建项目失败: " + e.getMessage());
        }
    }

    /**
     * 获取项目详情
     */
    @GetMapping("/projects/{projectId}")
    @Operation(summary = "获取项目详情", description = "获取指定项目的详细信息")
    public Result<Map<String, Object>> getProjectDetail(@PathVariable Long projectId) {
        log.info("获取项目详情: projectId={}", projectId);

        try {
            Project project = projectRepository.findById(projectId)
                .orElse(null);

            if (project == null) {
                return Result.error(404, "项目不存在");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("id", project.getId());
            result.put("name", project.getName());
            result.put("description", project.getDescription());
            result.put("status", project.getStatus());
            result.put("createdAt", project.getCreatedAt());

            // 统计任务信息
            List<ImageAssignment> assignments = imageAssignmentRepository.findByProjectId(projectId);
            result.put("totalTasks", assignments.size());
            result.put("completedTasks", assignments.stream()
                .mapToInt(a -> a.getStatus() == AssignmentStatus.COMPLETED ? 1 : 0)
                .sum());

            return Result.success("获取项目详情成功", result);

        } catch (Exception e) {
            log.error("获取项目详情失败: projectId={}", projectId, e);
            return Result.error(500, "获取项目详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取项目的用户列表
     */
    @GetMapping("/projects/{projectId}/users")
    @Operation(summary = "获取项目用户", description = "获取项目中的所有用户及其任务统计")
    public Result<List<Map<String, Object>>> getProjectUsers(@PathVariable Long projectId) {
        log.info("获取项目用户列表: projectId={}", projectId);

        try {
            // ✅ 从ProjectUser表获取项目成员
            List<ProjectUser> projectUsers = projectUserRepository.findByProjectId(projectId);
            
            if (projectUsers.isEmpty()) {
                log.info("项目 {} 暂无成员", projectId);
                return Result.success("获取项目用户成功", new ArrayList<>());
            }
            
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (ProjectUser projectUser : projectUsers) {
                Long userId = projectUser.getUserId();
                
                // 获取用户信息
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    log.warn("用户不存在: userId={}", userId);
                    continue;
                }
                
                // 统计用户在该项目中的任务数据
                List<ImageAssignment> userAssignments = 
                    imageAssignmentRepository.findByProjectIdAndUserId(projectId, userId);
                
                int totalTasks = userAssignments.size();
                int completedTasks = (int) userAssignments.stream()
                    .filter(a -> a.getStatus() == AssignmentStatus.COMPLETED)
                    .count();
                int inProgressTasks = (int) userAssignments.stream()
                    .filter(a -> a.getStatus() == AssignmentStatus.IN_PROGRESS)
                    .count();
                
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", userId);
                userInfo.put("username", user.getUsername());
                userInfo.put("email", user.getEmail());
                userInfo.put("totalTasks", totalTasks);
                userInfo.put("completedTasks", completedTasks);
                userInfo.put("inProgressTasks", inProgressTasks);
                userInfo.put("role", projectUser.getRole());
                userInfo.put("joinedAt", projectUser.getCreatedAt());
                
                result.add(userInfo);
            }

            log.info("项目 {} 共有 {} 个成员", projectId, result.size());
            return Result.success("获取项目用户成功", result);

        } catch (Exception e) {
            log.error("获取项目用户失败: projectId={}", projectId, e);
            return Result.error(500, "获取项目用户失败: " + e.getMessage());
        }
    }

    /**
     * 添加用户到项目
     */
    @PostMapping("/projects/{projectId}/users")
    @Operation(summary = "添加用户到项目", description = "将指定用户添加到项目中")
    public Result<String> addUsersToProject(
            @PathVariable Long projectId,
            @RequestBody Map<String, Object> requestData) {
        log.info("添加用户到项目: projectId={}, requestData={}", projectId, requestData);

        try {
            // 安全处理用户ID列表，支持不同类型
            Object userIdsObj = requestData.get("userIds");
            List<Long> userIds = new ArrayList<>();
            
            if (userIdsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> rawList = (List<Object>) userIdsObj;
                
                for (Object idObj : rawList) {
                    try {
                        if (idObj instanceof Number) {
                            userIds.add(((Number) idObj).longValue());
                        } else if (idObj instanceof String) {
                            userIds.add(Long.valueOf((String) idObj));
                        } else {
                            log.warn("不支持的用户ID类型: {} -> {}", idObj, idObj.getClass());
                        }
                    } catch (Exception e) {
                        log.error("转换用户ID失败: {}", idObj, e);
                    }
                }
            }
            
            log.info("转换后的用户ID列表: {}", userIds);
            
            if (userIds.isEmpty()) {
                return Result.error(400, "用户ID列表为空或格式不正确");
            }

            // 检查项目是否存在
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project == null) {
                return Result.error(404, "项目不存在");
            }

            int successCount = 0;
            int totalCount = userIds.size();

            // 获取当前登录管理员ID（用于记录是谁添加的）
            org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            String adminUsername = auth.getName();
            
            for (Long userId : userIds) {
                try {
                    // 检查用户是否存在
                    User user = userRepository.findById(userId).orElse(null);
                    if (user == null) {
                        log.warn("用户不存在: userId={}", userId);
                        continue;
                    }

                    // 检查用户是否已经在项目中
                    boolean alreadyInProject = projectUserRepository.existsByProjectIdAndUserId(projectId, userId);
                    
                    if (alreadyInProject) {
                        log.info("用户 {} 已经在项目 {} 中，跳过", userId, projectId);
                        successCount++;
                        continue;
                    }

                    // ✅ 真实数据库操作：创建项目-用户关联记录
                    ProjectUser projectUser = new ProjectUser();
                    projectUser.setProjectId(projectId);
                    projectUser.setUserId(userId);
                    projectUser.setAddedBy(1L); // TODO: 从当前登录的管理员获取
                    projectUser.setRole("member");
                    projectUser.setStatus("active");
                    projectUser.setRemark("通过管理后台添加");
                    
                    // 保存到数据库
                    projectUserRepository.save(projectUser);
                    
                    log.info("✅ 用户 {} 成功添加到项目 {} 中（数据库ID: {}）", 
                        userId, projectId, projectUser.getId());
                    successCount++;
                    
                } catch (Exception e) {
                    log.error("添加用户 {} 到项目 {} 失败: {}", userId, projectId, e.getMessage(), e);
                }
            }

            String message = String.format("成功添加 %d/%d 个用户到项目", successCount, totalCount);
            log.info(message);
            return Result.success(message);

        } catch (Exception e) {
            log.error("添加用户到项目失败: projectId={}", projectId, e);
            return Result.error(500, "添加用户到项目失败: " + e.getMessage());
        }
    }

    /**
     * 删除项目
     */
    @DeleteMapping("/projects/{id}")
    @Operation(summary = "删除项目", description = "删除指定的项目")
    public Result<String> deleteProject(@PathVariable Long id) {
        log.info("删除项目: projectId={}", id);

        try {
            // 检查项目是否存在
            Project project = projectRepository.findById(id).orElse(null);
            if (project == null) {
                return Result.error(404, "项目不存在");
            }

            // 删除项目关联的所有任务分配
            List<ImageAssignment> assignments = imageAssignmentRepository.findByProjectId(id);
            if (!assignments.isEmpty()) {
                imageAssignmentRepository.deleteAll(assignments);
                log.info("删除了 {} 个任务分配记录", assignments.size());
            }

            // 删除项目关联的所有用户
            List<ProjectUser> projectUsers = projectUserRepository.findByProjectId(id);
            if (!projectUsers.isEmpty()) {
                projectUserRepository.deleteAll(projectUsers);
                log.info("删除了 {} 个项目用户关联", projectUsers.size());
            }

            // 删除项目本身
            projectRepository.delete(project);
            log.info("✅ 项目删除成功: {}", id);

            return Result.success("项目删除成功");

        } catch (Exception e) {
            log.error("删除项目失败: projectId={}", id, e);
            return Result.error(500, "删除项目失败: " + e.getMessage());
        }
    }

    /**
     * 从项目中移除用户
     */
    @DeleteMapping("/projects/{projectId}/users/{userId}")
    @Operation(summary = "移除项目用户", description = "从项目中移除指定用户")
    public Result<String> removeUserFromProject(
            @PathVariable Long projectId,
            @PathVariable Long userId) {
        log.info("从项目中移除用户: projectId={}, userId={}", projectId, userId);

        try {
            // 检查项目-用户关联是否存在
            ProjectUser projectUser = projectUserRepository.findByProjectIdAndUserId(projectId, userId)
                .orElse(null);
            
            if (projectUser == null) {
                return Result.error(404, "用户不在此项目中");
            }

            // ✅ 真实数据库操作：删除项目-用户关联记录
            projectUserRepository.delete(projectUser);
            log.info("✅ 已删除项目-用户关联记录: id={}", projectUser.getId());
            
            // 可选：同时删除该用户在此项目中的所有任务分配
            List<ImageAssignment> userAssignments = 
                imageAssignmentRepository.findByProjectIdAndUserId(projectId, userId);
            
            if (!userAssignments.isEmpty()) {
                imageAssignmentRepository.deleteAll(userAssignments);
                log.info("✅ 同时删除了 {} 个任务分配记录", userAssignments.size());
            }
            
            String message = String.format("成功移除用户（删除关联记录和 %d 个任务分配）", userAssignments.size());
            log.info("成功从项目 {} 中移除用户 {}", projectId, userId);
            return Result.success(message);

        } catch (Exception e) {
            log.error("从项目中移除用户失败: projectId={}, userId={}", projectId, userId, e);
            return Result.error(500, "移除用户失败: " + e.getMessage());
        }
    }
}
