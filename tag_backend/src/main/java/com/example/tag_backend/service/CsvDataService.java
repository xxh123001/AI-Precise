package com.example.tag_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * CSV数据服务
 * 用于读取和查询matched_data_cleaned.csv文件中的数据
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Service
@Slf4j
public class CsvDataService {

    /**
     * CSV文件路径
     */
    private static final String CSV_FILE_PATH = "/data/hong/tag/uploads/matched_data_cleaned(2).csv";
    
    /**
     * CSV数据缓存
     */
    private Map<String, CsvRowData> csvDataCache = new HashMap<>();
    
    /**
     * 是否已加载数据
     */
    private boolean dataLoaded = false;

    /**
     * CSV行数据结构
     */
    public static class CsvRowData {
        private String folder;
        private String id;
        private String reid;
        private String imageClass;
        private String firstTrainLabel;
        private String testLabel;
        private String model1;
        private Double pasConf;
        private String ifLabel;
        private String predictLabel;
        private Double predictConfidence;

        // Getters and Setters
        public String getFolder() { return folder; }
        public void setFolder(String folder) { this.folder = folder; }
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getReid() { return reid; }
        public void setReid(String reid) { this.reid = reid; }
        
        public String getImageClass() { return imageClass; }
        public void setImageClass(String imageClass) { this.imageClass = imageClass; }
        
        public String getFirstTrainLabel() { return firstTrainLabel; }
        public void setFirstTrainLabel(String firstTrainLabel) { this.firstTrainLabel = firstTrainLabel; }
        
        public String getTestLabel() { return testLabel; }
        public void setTestLabel(String testLabel) { this.testLabel = testLabel; }
        
        public String getModel1() { return model1; }
        public void setModel1(String model1) { this.model1 = model1; }
        
        public Double getPasConf() { return pasConf; }
        public void setPasConf(Double pasConf) { this.pasConf = pasConf; }
        
        public String getIfLabel() { return ifLabel; }
        public void setIfLabel(String ifLabel) { this.ifLabel = ifLabel; }
        
        public String getPredictLabel() { return predictLabel; }
        public void setPredictLabel(String predictLabel) { this.predictLabel = predictLabel; }
        
        public Double getPredictConfidence() { return predictConfidence; }
        public void setPredictConfidence(Double predictConfidence) { this.predictConfidence = predictConfidence; }
    }

    /**
     * 根据文件名查找CSV数据
     * @param filename 文件名（不含扩展名）
     * @return CSV行数据，如果未找到则返回null
     */
    public CsvRowData findByFilename(String filename) {
        if (!dataLoaded) {
            loadCsvData();
        }
        
        // 尝试多种匹配方式
        String[] searchKeys = {
            filename + ".jpg",
            filename + ".jpeg", 
            filename + ".png",
            filename + ".JPG",
            filename + ".JPEG",
            filename + ".PNG",
            filename
        };
        
        for (String key : searchKeys) {
            CsvRowData data = csvDataCache.get(key);
            if (data != null) {
                log.debug("Found CSV data for filename: {} with key: {}", filename, key);
                return data;
            }
        }
        
        log.warn("No CSV data found for filename: {}", filename);
        return null;
    }

    /**
     * 加载CSV数据到内存缓存
     */
    private synchronized void loadCsvData() {
        if (dataLoaded) {
            return;
        }
        
        log.info("Loading CSV data from: {}", CSV_FILE_PATH);
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(CSV_FILE_PATH), StandardCharsets.UTF_8))) {
            
            String line;
            boolean isFirstLine = true;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                // 跳过标题行
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                
                try {
                    CsvRowData rowData = parseCsvLine(line);
                    if (rowData != null && StringUtils.hasText(rowData.getId())) {
                        csvDataCache.put(rowData.getId(), rowData);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse CSV line {}: {}", lineNumber, e.getMessage());
                }
            }
            
            dataLoaded = true;
            log.info("Successfully loaded {} records from CSV file", csvDataCache.size());
            
        } catch (IOException e) {
            log.error("Failed to load CSV data: {}", e.getMessage());
        }
    }

    /**
     * 解析CSV行数据
     * @param line CSV行字符串
     * @return 解析后的数据对象
     */
    private CsvRowData parseCsvLine(String line) {
        if (!StringUtils.hasText(line)) {
            return null;
        }
        
        // 简单的CSV解析（可能需要处理引号内的逗号）
        String[] fields = parseCsvFields(line);
        
        if (fields.length < 11) {
            log.warn("CSV line has insufficient fields: {}", line);
            return null;
        }
        
        CsvRowData data = new CsvRowData();
        data.setFolder(fields[0].trim());
        data.setId(fields[1].trim());
        data.setReid(fields[2].trim());
        data.setImageClass(fields[3].trim());
        data.setFirstTrainLabel(fields[4].trim());
        data.setTestLabel(fields[5].trim());
        data.setModel1(fields[6].trim());
        
        // 解析数值字段
        try {
            if (StringUtils.hasText(fields[7])) {
                data.setPasConf(Double.parseDouble(fields[7].trim()));
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse pas_conf: {}", fields[7]);
        }
        
        data.setIfLabel(fields[8].trim());
        data.setPredictLabel(fields[9].trim());
        
        try {
            if (StringUtils.hasText(fields[10])) {
                data.setPredictConfidence(Double.parseDouble(fields[10].trim()));
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse predict_confidence: {}", fields[10]);
        }
        
        return data;
    }

    /**
     * 解析CSV字段（处理引号内的逗号）
     * @param line CSV行
     * @return 字段数组
     */
    private String[] parseCsvFields(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        
        // 添加最后一个字段
        fields.add(currentField.toString());
        
        return fields.toArray(new String[0]);
    }

    /**
     * 重新加载CSV数据
     */
    public void reloadCsvData() {
        csvDataCache.clear();
        dataLoaded = false;
        loadCsvData();
    }
}
