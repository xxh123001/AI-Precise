package com.example.tag_backend.service.impl;

import com.example.tag_backend.service.ImageProcessingService;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * 图片处理服务实现类
 * 使用Thumbnailator库处理图片裁剪
 * 
 * @author Felix
 * @version 1.0
 * @since 2025-10-21
 */
@Service
@Slf4j
public class ImageProcessingServiceImpl implements ImageProcessingService {

    /**
     * 图片裁剪的最大高度（px）
     */
    private static final int MAX_HEIGHT = 1224;
    
    /**
     * 支持的图片文件扩展名
     */
    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList(
        "jpg", "jpeg", "png", "gif", "bmp", "webp"
    );

    @Override
    public ByteArrayOutputStream cropImageTop1224px(InputStream inputStream, String formatName) throws Exception {
        try {
            // 读取原始图片
            BufferedImage originalImage = ImageIO.read(inputStream);
            
            if (originalImage == null) {
                throw new Exception("无法读取图片，可能不是有效的图片文件");
            }
            
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            
            log.debug("原始图片尺寸: {}x{}", originalWidth, originalHeight);
            
            // 如果图片高度小于等于1224px，则不裁剪
            if (originalHeight <= MAX_HEIGHT) {
                log.debug("图片高度({})小于等于{}px，不进行裁剪", originalHeight, MAX_HEIGHT);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                Thumbnails.of(originalImage)
                    .size(originalWidth, originalHeight)
                    .outputFormat(formatName)
                    .toOutputStream(outputStream);
                return outputStream;
            }
            
            // 裁剪图片，只保留上面1224px
            log.info("裁剪图片: 原始尺寸{}x{} -> 裁剪后尺寸{}x{}", 
                     originalWidth, originalHeight, originalWidth, MAX_HEIGHT);
            
            // 使用Thumbnails进行裁剪
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Thumbnails.of(originalImage)
                .sourceRegion(0, 0, originalWidth, MAX_HEIGHT)  // 从(0,0)开始，裁剪宽度为原宽度，高度为1224px
                .size(originalWidth, MAX_HEIGHT)  // 保持裁剪后的尺寸
                .outputFormat(formatName)
                .toOutputStream(outputStream);
            
            log.info("图片裁剪成功");
            return outputStream;
            
        } catch (Exception e) {
            log.error("图片裁剪失败", e);
            throw e;
        }
    }

    @Override
    public ByteArrayOutputStream cropImageByRegion(InputStream inputStream, String formatName, 
                                                   int x, int y, int width, int height) throws Exception {
        try {
            // 读取原始图片
            BufferedImage originalImage = ImageIO.read(inputStream);
            
            if (originalImage == null) {
                throw new Exception("无法读取图片，可能不是有效的图片文件");
            }
            
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            
            log.debug("原始图片尺寸: {}x{}, 裁剪区域: x={}, y={}, width={}, height={}", 
                     originalWidth, originalHeight, x, y, width, height);
            
            // 验证裁剪区域是否合法
            if (x < 0 || y < 0 || width <= 0 || height <= 0) {
                throw new Exception("裁剪区域参数无效");
            }
            
            if (x + width > originalWidth || y + height > originalHeight) {
                throw new Exception("裁剪区域超出图片边界");
            }
            
            // 使用Thumbnails进行裁剪
            log.info("裁剪图片: 原始尺寸{}x{} -> 裁剪区域({},{},{}x{})", 
                     originalWidth, originalHeight, x, y, width, height);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Thumbnails.of(originalImage)
                .sourceRegion(x, y, width, height)  // 指定裁剪区域
                .size(width, height)  // 保持裁剪后的尺寸
                .outputFormat(formatName)
                .toOutputStream(outputStream);
            
            log.info("自定义区域裁剪成功");
            return outputStream;
            
        } catch (Exception e) {
            log.error("自定义区域裁剪失败", e);
            throw e;
        }
    }

    @Override
    public boolean isImageFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        
        String extension = "";
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            extension = filename.substring(lastDotIndex + 1).toLowerCase();
        }
        
        return IMAGE_EXTENSIONS.contains(extension);
    }
}

