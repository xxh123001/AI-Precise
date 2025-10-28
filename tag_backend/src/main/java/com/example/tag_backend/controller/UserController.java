package com.example.tag_backend.controller;

import com.example.tag_backend.common.result.PageResponse;
import com.example.tag_backend.common.result.Result;
import com.example.tag_backend.dto.request.CreateUserRequest;
import com.example.tag_backend.dto.request.UpdateUserRequest;
import com.example.tag_backend.dto.request.UserQueryRequest;
import com.example.tag_backend.dto.response.UserResponse;
import com.example.tag_backend.service.UserService;
import com.example.tag_backend.repository.ImageAssignmentRepository;
import com.example.tag_backend.entity.ImageAssignment;
import com.example.tag_backend.enums.AssignmentStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户控制器
 * 处理用户相关的HTTP请求
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "用户管理", description = "用户管理相关接口")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final ImageAssignmentRepository imageAssignmentRepository;

    @GetMapping
    @Operation(summary = "获取用户列表", description = "分页获取用户列表")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResponse<UserResponse>> getUserList(
            @Valid @ParameterObject UserQueryRequest request) {
        log.info("获取用户列表请求: {}", request);
        PageResponse<UserResponse> response = userService.getUserList(request);
        return Result.success(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情", description = "根据ID获取用户详细信息")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserResponse> getUserById(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("获取用户详情请求，ID: {}", id);
        UserResponse response = userService.getUserById(id);
        return Result.success(response);
    }

    @PostMapping
    @Operation(summary = "创建用户", description = "创建新用户")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        log.info("创建用户请求: {}", request.getUsername());
        UserResponse response = userService.createUser(request);
        return Result.success("用户创建成功", response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户", description = "更新用户信息")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserResponse> updateUser(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        log.info("更新用户请求，ID: {}", id);
        UserResponse response = userService.updateUser(id, request);
        return Result.success("用户更新成功", response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "删除指定用户")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteUser(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("删除用户请求，ID: {}", id);
        userService.deleteUser(id);
        return Result.<Void>success("用户删除成功", null);
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "启用用户", description = "启用指定用户")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> enableUser(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("启用用户请求，ID: {}", id);
        userService.enableUser(id);
        return Result.<Void>success("用户启用成功", null);
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "禁用用户", description = "禁用指定用户")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> disableUser(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("禁用用户请求，ID: {}", id);
        userService.disableUser(id);
        return Result.<Void>success("用户禁用成功", null);
    }

    @PutMapping("/{id}/reset-password")
    @Operation(summary = "重置密码", description = "重置用户密码")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> resetPassword(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @RequestParam String newPassword) {
        log.info("重置密码请求，用户ID: {}", id);
        userService.resetPassword(id, newPassword);
        return Result.<Void>success("密码重置成功", null);
    }

    @GetMapping("/{userId}/projects")
    @Operation(summary = "获取用户项目", description = "获取指定用户参与的项目列表")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Map<String, Object>>> getUserProjects(@PathVariable Long userId) {
        log.info("获取用户项目列表，用户ID: {}", userId);

        try {
            // 获取用户的所有任务分配
            List<ImageAssignment> assignments = imageAssignmentRepository.findByUserIdOrderByAssignedAtDesc(userId);
            
            // 按项目分组统计
            Map<Long, List<ImageAssignment>> projectGroups = assignments.stream()
                .collect(Collectors.groupingBy(assignment -> 
                    assignment.getProjectId() != null ? assignment.getProjectId() : 1L
                ));

            List<Map<String, Object>> projects = new ArrayList<>();
            
            for (Map.Entry<Long, List<ImageAssignment>> entry : projectGroups.entrySet()) {
                Long projectId = entry.getKey();
                List<ImageAssignment> projectAssignments = entry.getValue();
                
                Map<String, Object> project = new HashMap<>();
                project.put("id", projectId);
                project.put("name", "肾小管标注任务<0929>"); // 目前所有任务都是项目1
                project.put("description", "对肾小管图像进行精准标注，识别和标记肾小管结构特征");
                project.put("totalTasks", projectAssignments.size());
                project.put("completedTasks", projectAssignments.stream()
                    .mapToInt(a -> a.getStatus() == AssignmentStatus.COMPLETED ? 1 : 0)
                    .sum());
                project.put("inProgressTasks", projectAssignments.stream()
                    .mapToInt(a -> a.getStatus() == AssignmentStatus.IN_PROGRESS ? 1 : 0)
                    .sum());
                
                projects.add(project);
            }

            return Result.success("获取用户项目成功", projects);

        } catch (Exception e) {
            log.error("获取用户项目失败，用户ID: {}", userId, e);
            return Result.error(500, "获取用户项目失败: " + e.getMessage());
        }
    }
}
