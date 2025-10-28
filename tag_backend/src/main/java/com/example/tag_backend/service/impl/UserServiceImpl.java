package com.example.tag_backend.service.impl;

import com.example.tag_backend.dto.request.CreateUserRequest;
import com.example.tag_backend.dto.request.UpdateUserRequest;
import com.example.tag_backend.dto.request.UserQueryRequest;
import com.example.tag_backend.dto.response.UserResponse;
import com.example.tag_backend.common.result.PageResponse;
import com.example.tag_backend.entity.User;
import com.example.tag_backend.enums.UserStatus;
import com.example.tag_backend.exception.BusinessException;
import com.example.tag_backend.repository.UserRepository;
import com.example.tag_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 * 负责处理用户相关的业务逻辑
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("创建用户开始，用户名: {}", request.getUsername());

        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        // 创建用户实体
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setStatus(UserStatus.ACTIVE);
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        // 保存用户
        user = userRepository.save(user);

        log.info("用户创建成功，用户ID: {}", user.getId());

        return convertToResponse(user);
    }

    @Override
    @Cacheable(value = "users", key = "#id")
    public UserResponse getUserById(Long id) {
        log.debug("从数据库查询用户，ID: {}", id);
        User user = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        return convertToResponse(user);
    }

    @Override
    @Cacheable(value = "usersByUsername", key = "#username")
    public UserResponse getUserByUsername(String username) {
        log.debug("从数据库查询用户，用户名: {}", username);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        return convertToResponse(user);
    }

    @Override
    public User getUserEntityByUsername(String username) {
        log.debug("从数据库查询用户实体，用户名: {}", username);
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    @Override
    public User getUserEntityById(Long id) {
        log.debug("从数据库查询用户实体，ID: {}", id);
        return userRepository.findById(id)
            .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    @Override
    public PageResponse<UserResponse> getUserList(UserQueryRequest request) {
        log.debug("查询用户列表，参数: {}", request);

        // 构建分页参数
        Pageable pageable = buildPageable(request);

        // 执行查询
        Page<User> userPage = userRepository.findByUsernameAndStatus(
            request.getUsername(), 
            request.getStatus(), 
            pageable
        );

        // 转换结果
        List<UserResponse> userResponses = userPage.getContent().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());

        return PageResponse.of(userResponses, userPage);
    }

    @Override
    @Transactional
    @CachePut(value = "users", key = "#id")
    @CacheEvict(value = "usersByUsername", allEntries = true)
    public UserResponse updateUser(Long id, CreateUserRequest request) {
        log.info("更新用户开始，用户ID: {}", id);

        User user = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException("用户不存在"));

        // 检查用户名是否已被其他用户使用
        if (!user.getUsername().equals(request.getUsername()) && 
            userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        // 更新用户信息
        user.setUsername(request.getUsername());
        user.setRole(request.getRole());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        // 如果提供了新密码，则更新密码
        if (StringUtils.hasText(request.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        user = userRepository.save(user);

        log.info("用户更新成功，用户ID: {}，已清除相关缓存", user.getId());

        return convertToResponse(user);
    }

    @Override
    @Transactional
    @CachePut(value = "users", key = "#id")
    @CacheEvict(value = "usersByUsername", allEntries = true)
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        log.info("更新用户开始，用户ID: {}", id);

        User user = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException("用户不存在"));

        // 检查用户名是否已被其他用户使用
        if (!user.getUsername().equals(request.getUsername()) && 
            userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        // 更新用户信息
        user.setUsername(request.getUsername());
        user.setRole(request.getRole());
        user.setStatus(request.getStatus());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        // 如果提供了新密码，则更新密码
        if (StringUtils.hasText(request.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        log.info("用户更新成功，用户ID: {}", id);
        return convertToResponse(savedUser);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"users", "usersByUsername"}, allEntries = true)
    public void deleteUser(Long id) {
        log.info("删除用户开始，用户ID: {}", id);

        User user = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException("用户不存在"));

        userRepository.delete(user);

        log.info("用户删除成功，用户ID: {}，已清除相关缓存", id);
    }

    @Override
    @Transactional
    public void enableUser(Long id) {
        updateUserStatus(id, UserStatus.ACTIVE);
    }

    @Override
    @Transactional
    public void disableUser(Long id) {
        updateUserStatus(id, UserStatus.INACTIVE);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        log.info("重置用户密码开始，用户ID: {}", id);

        User user = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException("用户不存在"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("用户密码重置成功，用户ID: {}", id);
    }

    @Override
    public UserResponse validateLogin(String username, String password) {
        log.debug("验证用户登录，用户名: {}", username);

        User user = userRepository.findByUsernameAndStatus(username, UserStatus.ACTIVE)
            .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("用户登录成功，用户ID: {}", user.getId());

        return convertToResponse(user);
    }

    /**
     * 更新用户状态
     */
    @CacheEvict(value = {"users", "usersByUsername"}, allEntries = true)
    private void updateUserStatus(Long id, UserStatus status) {
        log.info("更新用户状态，用户ID: {}, 状态: {}", id, status);

        User user = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException("用户不存在"));

        user.setStatus(status);
        userRepository.save(user);

        log.info("用户状态更新成功，用户ID: {}，已清除相关缓存", id);
    }

    /**
     * 构建分页参数
     */
    private Pageable buildPageable(UserQueryRequest request) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        
        if (StringUtils.hasText(request.getSort())) {
            String[] sortParts = request.getSort().split(",");
            if (sortParts.length == 2) {
                Sort.Direction direction = "asc".equalsIgnoreCase(sortParts[1]) 
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
                sort = Sort.by(direction, sortParts[0]);
            }
        }

        return PageRequest.of(
            Math.max(0, request.getPage() - 1), // Spring Data Page是从0开始的
            Math.max(1, request.getSize()),
            sort
        );
    }

    /**
     * 转换为响应DTO
     */
    private UserResponse convertToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setLastLoginAt(user.getLastLoginAt());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}
