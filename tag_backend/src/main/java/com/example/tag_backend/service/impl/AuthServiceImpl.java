package com.example.tag_backend.service.impl;

import com.example.tag_backend.dto.request.LoginRequest;
import com.example.tag_backend.dto.response.LoginResponse;
import com.example.tag_backend.dto.response.UserResponse;
import com.example.tag_backend.entity.User;
import com.example.tag_backend.enums.UserStatus;
import com.example.tag_backend.exception.BusinessException;
import com.example.tag_backend.repository.UserRepository;
import com.example.tag_backend.security.JwtUtils;
import com.example.tag_backend.service.AuthService;
import com.example.tag_backend.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证服务实现类
 * 处理用户认证相关的业务逻辑
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final SessionService sessionService;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("用户登录请求，用户名: {}", request.getUsername());

        // 查找用户
        User user = userRepository.findByUsernameAndStatus(request.getUsername(), UserStatus.ACTIVE)
            .orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("用户 {} 密码验证失败", request.getUsername());
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // 生成JWT令牌
        String token = jwtUtils.generateToken(user.getUsername(), user.getRole().name());

        // 转换用户信息
        UserResponse userResponse = convertToUserResponse(user);

        // 缓存用户会话信息（24小时过期）
        sessionService.cacheUserSession(token, userResponse, 24 * 60 * 60);

        log.info("用户 {} 登录成功，会话已缓存", request.getUsername());

        return new LoginResponse(token, userResponse);
    }

    @Override
    public UserResponse getCurrentUser(String username) {
        log.debug("获取当前用户信息，用户名: {}", username);

        User user = userRepository.findByUsernameAndStatus(username, UserStatus.ACTIVE)
            .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        return convertToUserResponse(user);
    }

    @Override
    public LoginResponse refreshToken(String username) {
        log.info("刷新用户令牌，用户名: {}", username);

        User user = userRepository.findByUsernameAndStatus(username, UserStatus.ACTIVE)
            .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        // 生成新的JWT令牌
        String token = jwtUtils.generateToken(user.getUsername(), user.getRole().name());

        // 转换用户信息
        UserResponse userResponse = convertToUserResponse(user);

        // 缓存新的用户会话信息（24小时过期）
        sessionService.cacheUserSession(token, userResponse, 24 * 60 * 60);

        log.info("用户 {} 令牌刷新成功，会话已更新", username);

        return new LoginResponse(token, userResponse);
    }

    @Override
    public void logout(String token) {
        log.info("用户登出，token: {}", token);
        
        // 移除用户会话缓存
        sessionService.removeUserSession(token);
        
        log.info("用户会话已清除，token: {}", token);
    }

    /**
     * 转换为用户响应DTO
     */
    private UserResponse convertToUserResponse(User user) {
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
