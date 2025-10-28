package com.example.tag_backend.config;

import com.example.tag_backend.entity.User;
import com.example.tag_backend.enums.UserRole;
import com.example.tag_backend.enums.UserStatus;
import com.example.tag_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器
 * 在应用启动时初始化必要的数据
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initAdminUser();
    }

    /**
     * 初始化管理员账户
     */
    private void initAdminUser() {
        String adminUsername = "admin";
        
        if (!userRepository.existsByUsername(adminUsername)) {
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setPasswordHash(passwordEncoder.encode("Hong123001"));
            admin.setRole(UserRole.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            admin.setEmail("xxh123001@gmail.com");
            
            userRepository.save(admin);
            
            log.info("默认管理员账户创建成功 - 用户名: {}, 密码: {}", adminUsername, "Hong123001");
        } else {
            log.info("管理员账户已存在，跳过初始化");
        }
    }
}
