package com.example.tag_backend.service.impl;

import com.example.tag_backend.dto.request.ExportRequest;
import com.example.tag_backend.entity.Annotation;
import com.example.tag_backend.entity.Image;
import com.example.tag_backend.entity.User;
import com.example.tag_backend.enums.AnnotationStatus;
import com.example.tag_backend.exception.BusinessException;
import com.example.tag_backend.repository.AnnotationRepository;
import com.example.tag_backend.service.ExportService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据导出服务实现类
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

    private final AnnotationRepository annotationRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public Resource exportAnnotationsToExcel(ExportRequest request) {
        log.info("开始导出标注数据到Excel");

        try {
            // 查询标注数据
            List<Annotation> annotations = getAnnotationsForExport(request);
            
            // 创建Excel工作簿
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("标注数据");

            // 创建样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            // 写入表头
            createHeaderRow(sheet, headerStyle);

            // 写入数据
            writeDataRows(sheet, annotations, dataStyle);

            // 自动调整列宽
            autoSizeColumns(sheet);

            // 转换为字节数组
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            log.info("Excel导出完成，共导出 {} 条标注记录", annotations.size());

            return new ByteArrayResource(outputStream.toByteArray());

        } catch (IOException e) {
            log.error("Excel导出失败", e);
            throw new BusinessException("Excel导出失败", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Object getAnnotationStatistics(ExportRequest request) {
        log.info("获取标注统计数据");

        List<Annotation> annotations = getAnnotationsForExport(request);

        Map<String, Object> statistics = new HashMap<>();
        
        // 总体统计
        statistics.put("totalAnnotations", annotations.size());
        statistics.put("completedAnnotations", annotations.stream()
            .mapToLong(a -> a.getStatus() != null && "COMPLETED".equals(a.getStatus().name()) ? 1 : 0)
            .sum());

        // 按用户统计
        Map<String, Long> userStats = new HashMap<>();
        annotations.forEach(annotation -> {
            if (annotation.getUser() != null && annotation.getUser().getUsername() != null) {
                String username = annotation.getUser().getUsername();
                userStats.put(username, userStats.getOrDefault(username, 0L) + 1);
            } else {
                userStats.put("未知用户", userStats.getOrDefault("未知用户", 0L) + 1);
            }
        });
        statistics.put("userStatistics", userStats);

        // 按日期统计
        Map<String, Long> dateStats = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        annotations.forEach(annotation -> {
            if (annotation.getCreatedAt() != null) {
                String date = annotation.getCreatedAt().toLocalDate().format(formatter);
                dateStats.put(date, dateStats.getOrDefault(date, 0L) + 1);
            } else {
                dateStats.put("未知日期", dateStats.getOrDefault("未知日期", 0L) + 1);
            }
        });
        statistics.put("dateStatistics", dateStats);

        return statistics;
    }

    @Override
    public Object generateProgressReport() {
        log.info("生成标注进度报告");

        Map<String, Object> report = new HashMap<>();
        
        // 这里可以添加更复杂的进度报告逻辑
        // 例如：按标签配置、按用户、按时间段的进度统计
        
        return report;
    }

    /**
     * 根据条件查询标注数据
     */
    private List<Annotation> getAnnotationsForExport(ExportRequest request) {
        Specification<Annotation> spec = buildSpecificationWithJoins(request);
        List<Annotation> annotations = annotationRepository.findAll(spec);
        
        // 在Java层面进行额外的过滤（避免SQL中的JSON比较问题）
        return annotations.stream()
            .filter(annotation -> {
                // 如果要求有备注，过滤掉空备注
                if (request.getHasRemark() != null && request.getHasRemark()) {
                    if (annotation.getRemark() == null || annotation.getRemark().trim().isEmpty()) {
                        return false;
                    }
                }
                
                // 如果要求排除空标注，过滤掉空的或只有"{}"的标注
                if (request.getExcludeEmpty() != null && request.getExcludeEmpty()) {
                    String labels = annotation.getLabels();
                    if (labels == null || labels.trim().isEmpty() || "{}".equals(labels.trim())) {
                        return false;
                    }
                }
                
                // 如果要求有标注，过滤掉空标注
                if (request.getHasAnnotation() != null && request.getHasAnnotation()) {
                    String labels = annotation.getLabels();
                    if (labels == null || labels.trim().isEmpty() || "{}".equals(labels.trim())) {
                        return false;
                    }
                }
                
                return true;
            })
            .collect(Collectors.toList());
    }

    /**
     * 构建查询条件（带关联查询）
     */
    private Specification<Annotation> buildSpecificationWithJoins(ExportRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 只在非Count查询时添加 FETCH JOIN
            if (query != null && query.getResultType() == Annotation.class) {
                query.distinct(true);
                root.fetch("user", JoinType.LEFT);
                root.fetch("image", JoinType.LEFT);
            }

            // 单个用户ID筛选
            if (request.getUserId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("userId"), request.getUserId()));
            }

            // 用户ID列表筛选
            if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
                predicates.add(root.get("userId").in(request.getUserIds()));
            }

            // 日期范围筛选
            if (request.getStartDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("createdAt"), request.getStartDate().atStartOfDay()));
            }
            if (request.getEndDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("createdAt"), request.getEndDate().atTime(23, 59, 59)));
            }

            // 标注状态筛选
            if (request.getAnnotationStatus() != null && !request.getAnnotationStatus().trim().isEmpty()) {
                try {
                    AnnotationStatus status = AnnotationStatus.valueOf(request.getAnnotationStatus());
                    predicates.add(criteriaBuilder.equal(root.get("status"), status));
                } catch (IllegalArgumentException e) {
                    log.warn("无效的标注状态: {}", request.getAnnotationStatus());
                }
            }

            // 注意：暂时不使用这些复杂的筛选条件，因为会导致JSON字段比较问题
            // 这些筛选将在查询结果返回后在Java代码中进行过滤
            
            // 仅包含已标注的数据 - 简化条件
            if (request.getHasAnnotation() != null && request.getHasAnnotation()) {
                predicates.add(criteriaBuilder.isNotNull(root.get("labels")));
            }

            // 仅包含有备注的数据 - 简化条件
            if (request.getHasRemark() != null && request.getHasRemark()) {
                predicates.add(criteriaBuilder.isNotNull(root.get("remark")));
            }

            // 排除空标注 - 简化条件
            if (request.getExcludeEmpty() != null && request.getExcludeEmpty()) {
                predicates.add(criteriaBuilder.isNotNull(root.get("labels")));
            }

            // TODO: projectId和fileName筛选需要通过子查询或额外的repository方法实现
            // 因为需要join到Image和ImageAssignment表，与fetch join冲突

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }


    /**
     * 创建表头行
     */
    private void createHeaderRow(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "序号", "图像文件名", "原始文件名", "标注用户", "标注时间", 
            "完成时间", "标注状态", "标注内容", "备注信息"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * 写入数据行
     */
    private void writeDataRows(Sheet sheet, List<Annotation> annotations, CellStyle dataStyle) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (int i = 0; i < annotations.size(); i++) {
            Annotation annotation = annotations.get(i);
            Row row = sheet.createRow(i + 1);

            // 序号
            createCell(row, 0, i + 1, dataStyle);

            // 图像信息
            Image image = annotation.getImage();
            createCell(row, 1, image != null ? image.getFilename() : "", dataStyle);
            createCell(row, 2, image != null ? image.getOriginalName() : "", dataStyle);

            // 用户信息
            User user = annotation.getUser();
            createCell(row, 3, user != null ? user.getUsername() : "", dataStyle);

            // 时间信息
            createCell(row, 4, annotation.getCreatedAt() != null ? 
                annotation.getCreatedAt().format(formatter) : "", dataStyle);
            createCell(row, 5, annotation.getUpdatedAt() != null ? 
                annotation.getUpdatedAt().format(formatter) : "", dataStyle);

            // 状态
            createCell(row, 6, annotation.getStatus() != null ? 
                annotation.getStatus().name() : "UNKNOWN", dataStyle);

            // 标注内容
            createCell(row, 7, formatLabelsForExcel(annotation.getLabels()), dataStyle);

            // 备注信息
            createCell(row, 8, annotation.getRemark() != null ? annotation.getRemark() : "", dataStyle);
        }
    }

    /**
     * 创建单元格
     */
    private void createCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        log.debug("Creating cell - column: {}, value: {}, type: {}", 
                 column, value, value != null ? value.getClass().getSimpleName() : "null");
        
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            // 对于其他类型的对象，转换为字符串
            String stringValue = value.toString();
            log.warn("Converting object to string - column: {}, original: {}, converted: {}", 
                    column, value.getClass().getSimpleName(), stringValue);
            cell.setCellValue(stringValue);
        }
        cell.setCellStyle(style);
    }

    /**
     * 格式化标注内容用于Excel显示
     */
    private String formatLabelsForExcel(String labelsJson) {
        log.debug("Formatting labels JSON: {}", labelsJson);
        
        if (labelsJson == null || labelsJson.trim().isEmpty()) {
            log.debug("Labels JSON is null or empty");
            return "无标注数据";
        }
        
        try {
            JsonNode labelsNode = objectMapper.readTree(labelsJson);
            if (labelsNode == null || labelsNode.isNull()) {
                return "无标注数据";
            }
            
            StringBuilder result = new StringBuilder();

            // 处理不同的JSON结构
            JsonNode labelsJsonNode = labelsNode.get("labels");
            if (labelsJsonNode != null && labelsJsonNode.isObject()) {
                // 标准结构：{ "labels": { "category1": "value1", ... } }
                labelsJsonNode.fieldNames().forEachRemaining(categoryId -> {
                    JsonNode value = labelsJsonNode.get(categoryId);
                    
                    result.append(categoryId).append(": ");
                    if (value.isArray()) {
                        // 多选类型
                        List<String> values = new ArrayList<>();
                        value.forEach(v -> values.add(v.asText()));
                        result.append(String.join(", ", values));
                    } else {
                        // 单选或文本类型
                        result.append(value.asText());
                    }
                    result.append("; ");
                });
            } else if (labelsNode.isObject()) {
                // 直接是标签对象：{ "category1": "value1", ... }
                labelsNode.fieldNames().forEachRemaining(categoryId -> {
                    JsonNode value = labelsNode.get(categoryId);
                    
                    result.append(categoryId).append(": ");
                    if (value.isArray()) {
                        List<String> values = new ArrayList<>();
                        value.forEach(v -> values.add(v.asText()));
                        result.append(String.join(", ", values));
                    } else {
                        result.append(value.asText());
                    }
                    result.append("; ");
                });
            } else {
                // 其他情况，直接返回JSON字符串的简化版本
                return labelsJson.length() > 100 ? 
                    labelsJson.substring(0, 97) + "..." : labelsJson;
            }

            String formattedResult = result.toString();
            return formattedResult.isEmpty() ? "空标注数据" : formattedResult;
            
        } catch (Exception e) {
            log.warn("解析标注内容失败: {}", labelsJson, e);
            return "标注内容解析失败: " + e.getMessage();
        }
    }

    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        return style;
    }

    /**
     * 创建数据样式
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    /**
     * 自动调整列宽
     */
    private void autoSizeColumns(Sheet sheet) {
        for (int i = 0; i < 9; i++) {
            sheet.autoSizeColumn(i);
            // 设置最大列宽，防止过宽
            if (sheet.getColumnWidth(i) > 8000) {
                sheet.setColumnWidth(i, 8000);
            }
        }
    }


    @Override
    @Transactional(readOnly = true)
    public Object exportAnnotationsToJson(ExportRequest request) {
        log.info("开始导出JSON格式标注数据");
        
        try {
            // 构建查询条件，使用join查询避免N+1问题
            List<Annotation> annotations = getAnnotationsForExport(request);
            
            log.info("查询到 {} 条标注记录", annotations.size());
            
            // 构建导出数据结构
            Map<String, Object> exportData = new HashMap<>();
            exportData.put("exportInfo", Map.of(
                "exportTime", LocalDateTime.now(),
                "totalCount", annotations.size(),
                "exportRequest", request
            ));
            
            // 转换标注数据为JSON友好格式
            List<Map<String, Object>> annotationList = annotations.stream()
                .map(this::convertAnnotationToJsonFormat)
                .collect(Collectors.toList());
                
            exportData.put("annotations", annotationList);
            
            log.info("JSON格式标注数据准备完成");
            return exportData;
            
        } catch (Exception e) {
            log.error("导出JSON格式标注数据失败", e);
            throw new RuntimeException("导出JSON格式标注数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将标注对象转换为JSON友好格式
     */
    private Map<String, Object> convertAnnotationToJsonFormat(Annotation annotation) {
        Map<String, Object> data = new HashMap<>();
        
        // 基本信息
        data.put("id", annotation.getId());
        data.put("imageId", annotation.getImageId());
        data.put("status", annotation.getStatus());
        data.put("createdAt", annotation.getCreatedAt());
        data.put("updatedAt", annotation.getUpdatedAt());
        data.put("remark", annotation.getRemark()); // 添加备注字段
        
        // 用户信息
        if (annotation.getUser() != null) {
            data.put("user", Map.of(
                "id", annotation.getUser().getId(),
                "username", annotation.getUser().getUsername()
            ));
        }
        
        // 图像信息
        if (annotation.getImage() != null) {
            Map<String, Object> imageInfo = new HashMap<>();
            imageInfo.put("id", annotation.getImage().getId());
            imageInfo.put("filename", annotation.getImage().getFilename());
            imageInfo.put("originalName", annotation.getImage().getOriginalName());
            imageInfo.put("filePath", annotation.getImage().getFilePath());
            imageInfo.put("fileSize", annotation.getImage().getFileSize());
            imageInfo.put("mimeType", annotation.getImage().getMimeType());
            imageInfo.put("labelConfigId", annotation.getImage().getLabelConfigId());
            data.put("image", imageInfo);
        }
        
        // 标注内容
        if (annotation.getLabels() != null && !annotation.getLabels().trim().isEmpty()) {
            try {
                // 解析JSON标注内容
                JsonNode labelsNode = objectMapper.readTree(annotation.getLabels());
                data.put("labels", labelsNode);
                
                // 同时提供格式化的标注内容（用于阅读）
                data.put("labelsFormatted", formatLabelsForDisplay(annotation.getLabels()));
            } catch (Exception e) {
                log.warn("解析标注JSON失败，使用原始字符串: {}", annotation.getLabels(), e);
                data.put("labels", annotation.getLabels());
                data.put("labelsFormatted", "解析失败: " + e.getMessage());
            }
        } else {
            data.put("labels", null);
            data.put("labelsFormatted", "无标注数据");
        }
        
        return data;
    }

    /**
     * 格式化标注内容用于显示
     */
    private String formatLabelsForDisplay(String labelsJson) {
        if (labelsJson == null || labelsJson.trim().isEmpty()) {
            return "无标注数据";
        }
        
        try {
            JsonNode labelsNode = objectMapper.readTree(labelsJson);
            if (labelsNode == null || labelsNode.isNull()) {
                return "无标注数据";
            }
            
            StringBuilder result = new StringBuilder();

            // 处理不同的JSON结构
            JsonNode labelsJsonNode = labelsNode.get("labels");
            if (labelsJsonNode != null && labelsJsonNode.isObject()) {
                // 标准结构：{ "labels": { "category1": "value1", ... } }
                labelsJsonNode.fieldNames().forEachRemaining(categoryId -> {
                    JsonNode value = labelsJsonNode.get(categoryId);
                    
                    result.append(categoryId).append(": ");
                    if (value.isArray()) {
                        List<String> values = new ArrayList<>();
                        value.forEach(v -> values.add(v.asText()));
                        result.append(String.join(", ", values));
                    } else {
                        result.append(value.asText());
                    }
                    result.append("; ");
                });
            } else if (labelsNode.isObject()) {
                // 直接是标签对象：{ "category1": "value1", ... }
                labelsNode.fieldNames().forEachRemaining(categoryId -> {
                    JsonNode value = labelsNode.get(categoryId);
                    
                    result.append(categoryId).append(": ");
                    if (value.isArray()) {
                        List<String> values = new ArrayList<>();
                        value.forEach(v -> values.add(v.asText()));
                        result.append(String.join(", ", values));
                    } else {
                        result.append(value.asText());
                    }
                    result.append("; ");
                });
            }

            String formattedResult = result.toString();
            return formattedResult.isEmpty() ? "空标注数据" : formattedResult;
            
        } catch (Exception e) {
            return "标注内容解析失败: " + e.getMessage();
        }
    }

    @Override
    public Object previewAnnotationsData(ExportRequest request, Integer limit) {
        log.info("预览标注数据，分页参数: page={}, size={}", request.getPage(), request.getSize());
        
        try {
            // 使用现有方法查询所有符合条件的数据（用于获取总数）
            List<Annotation> allAnnotations = getAnnotationsForExport(request);
            int totalCount = allAnnotations.size();
            
            // 分页参数
            int page = request.getPage() != null ? request.getPage() : 0;
            int size = request.getSize() != null ? request.getSize() : 20;
            
            // 计算分页
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, totalCount);
            
            // 获取当前页数据
            List<Annotation> pageAnnotations = new ArrayList<>();
            if (fromIndex < totalCount) {
                pageAnnotations = allAnnotations.subList(fromIndex, toIndex);
            }
            
            // 构建预览数据
            List<Map<String, Object>> previewData = new ArrayList<>();
            List<Map<String, Object>> columns = new ArrayList<>();
            
            // 定义列结构
            columns.add(Map.of("prop", "id", "label", "ID", "width", 80));
            columns.add(Map.of("prop", "imageName", "label", "图像名称", "width", 200));
            columns.add(Map.of("prop", "userName", "label", "标注用户", "width", 120));
            columns.add(Map.of("prop", "status", "label", "状态", "width", 100));
            columns.add(Map.of("prop", "labels", "label", "标注内容", "width", 300));
            columns.add(Map.of("prop", "remark", "label", "备注信息", "width", 200));
            columns.add(Map.of("prop", "createdAt", "label", "创建时间", "width", 160));
            columns.add(Map.of("prop", "updatedAt", "label", "更新时间", "width", 160));
            
            // 转换数据
            for (Annotation annotation : pageAnnotations) {
                Map<String, Object> row = new HashMap<>();
                
                row.put("id", annotation.getId());
                row.put("imageName", annotation.getImage() != null ? 
                    annotation.getImage().getOriginalName() : "未知图像");
                row.put("userName", annotation.getUser() != null ? 
                    annotation.getUser().getUsername() : "未知用户");
                row.put("status", getAnnotationStatusText(annotation.getStatus()));
                row.put("labels", formatLabelsForDisplay(annotation.getLabels()));
                row.put("remark", annotation.getRemark() != null ? annotation.getRemark() : "");
                row.put("createdAt", annotation.getCreatedAt() != null ? 
                    annotation.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
                row.put("updatedAt", annotation.getUpdatedAt() != null ? 
                    annotation.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
                
                previewData.add(row);
            }
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("data", previewData);
            result.put("columns", columns);
            result.put("total", totalCount);
            result.put("page", page);
            result.put("size", size);
            result.put("hasMore", toIndex < totalCount);
            
            log.info("预览数据构建完成，第 {} 页，每页 {} 条，返回 {} 条数据，总数据量: {}", 
                    page + 1, size, previewData.size(), totalCount);
            
            return result;
            
        } catch (Exception e) {
            log.error("预览数据失败", e);
            throw new BusinessException("预览数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取标注状态的中文描述
     */
    private String getAnnotationStatusText(AnnotationStatus status) {
        if (status == null) return "未知";
        
        switch (status) {
            case DRAFT:
                return "草稿";
            case COMPLETED:
                return "已完成";
            default:
                return status.name();
        }
    }
}
