package com.example.tag_backend.service;

import com.example.tag_backend.dto.request.ExportRequest;
import org.springframework.core.io.Resource;

/**
 * 数据导出服务接口
 * 负责将标注数据导出为Excel格式
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
public interface ExportService {

    /**
     * 导出标注数据为Excel文件
     * 
     * @param request 导出请求参数
     * @return Excel文件资源
     */
    Resource exportAnnotationsToExcel(ExportRequest request);

    /**
     * 获取标注统计数据
     * 
     * @param request 查询参数
     * @return 统计数据
     */
    Object getAnnotationStatistics(ExportRequest request);

    /**
     * 导出标注数据为JSON格式
     * 
     * @param request 导出请求参数
     * @return JSON格式的标注数据
     */
    Object exportAnnotationsToJson(ExportRequest request);

    /**
     * 生成标注进度报告
     * 
     * @return 进度报告数据
     */
    Object generateProgressReport();

    /**
     * 预览标注数据
     * 
     * @param request 查询参数
     * @param limit 预览数据条数限制
     * @return 预览数据
     */
    Object previewAnnotationsData(ExportRequest request, Integer limit);
}
