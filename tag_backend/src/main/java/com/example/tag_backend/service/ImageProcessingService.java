package com.example.tag_backend.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * 图片处理服务接口
 * 提供图片裁剪等处理功能
 * 
 * @author Felix
 * @version 1.0
 * @since 2025-10-21
 */
public interface ImageProcessingService {
    
    /**
     * 裁剪图片，只保留上面1224px的高度，宽度保持不变
     * 如果图片高度小于等于1224px，则不进行裁剪
     * 
     * @param inputStream 原始图片输入流
     * @param formatName 图片格式（如jpg, png等）
     * @return 裁剪后的图片字节流
     * @throws Exception 处理失败时抛出异常
     */
    ByteArrayOutputStream cropImageTop1224px(InputStream inputStream, String formatName) throws Exception;
    
    /**
     * 根据指定区域裁剪图片
     * 
     * @param inputStream 原始图片输入流
     * @param formatName 图片格式（如jpg, png等）
     * @param x 裁剪区域X坐标（左上角）
     * @param y 裁剪区域Y坐标（左上角）
     * @param width 裁剪区域宽度
     * @param height 裁剪区域高度
     * @return 裁剪后的图片字节流
     * @throws Exception 处理失败时抛出异常
     */
    ByteArrayOutputStream cropImageByRegion(InputStream inputStream, String formatName, 
                                           int x, int y, int width, int height) throws Exception;
    
    /**
     * 判断文件是否为图片
     * 
     * @param filename 文件名
     * @return 是否为图片文件
     */
    boolean isImageFile(String filename);
}

