package com.example.tag_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC 配置类
 * 配置静态资源访问
 * 
 * @author 辛晓红
 * @version 1.0
 * @since 2025-10-14
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.file.upload-path:./uploads/}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // 配置上传文件的访问路径
        String absolutePath = Paths.get(uploadPath).toAbsolutePath().toString();
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }
}

