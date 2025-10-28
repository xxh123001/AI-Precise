package com.example.tag_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 图像实体类
 * 存储上传的图像信息和元数据
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Entity
@Table(name = "images")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Image {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 文件名（系统生成的唯一文件名）
     */
    @Column(nullable = false, length = 255)
    private String filename;

    /**
     * 原始文件名（用户上传时的文件名）
     */
    @Column(nullable = false, length = 255, name = "original_name")
    private String originalName;

    /**
     * 文件存储路径
     */
    @Column(nullable = false, length = 500, name = "file_path")
    private String filePath;

    /**
     * 文件大小（字节）
     */
    @Column(nullable = false, name = "file_size")
    private Long fileSize;

    /**
     * MIME类型
     */
    @Column(length = 100, name = "mime_type")
    private String mimeType;

    /**
     * 上传者ID
     */
    @Column(name = "upload_by")
    private Long uploadBy;

    /**
     * 上传者用户信息
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upload_by", insertable = false, updatable = false)
    private User uploader;

    /**
     * 标签配置ID
     */
    @Column(name = "label_config_id")
    private Long labelConfigId;

    /**
     * 关联的标签配置
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "label_config_id", insertable = false, updatable = false)
    private LabelConfig labelConfig;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ========== CSV相关字段 ==========
    
    /**
     * 分类文件夹
     */
    @Column(length = 100)
    private String folder;

    /**
     * 重新命名的ID
     */
    @Column(length = 255)
    private String reid;

    /**
     * 图像分类标签
     */
    @Column(name = "image_class", length = 500)
    private String imageClass;

    /**
     * 第一次训练标签
     */
    @Column(name = "first_train_label", length = 100)
    private String firstTrainLabel;

    /**
     * 测试标签
     */
    @Column(name = "test_label", length = 100)
    private String testLabel;

    /**
     * 模型1
     */
    @Column(length = 100)
    private String model1;

    /**
     * PAS置信度
     */
    @Column(name = "pas_conf")
    private Double pasConf;

    /**
     * IF标签
     */
    @Column(name = "if_label", length = 100)
    private String ifLabel;

    /**
     * 预测标签
     */
    @Column(name = "predict_label", length = 100)
    private String predictLabel;

    /**
     * 预测置信度
     */
    @Column(name = "predict_confidence")
    private Double predictConfidence;

    /**
     * 图像分配记录列表
     */
    @OneToMany(mappedBy = "image", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ImageAssignment> imageAssignments;

    /**
     * 标注记录列表
     */
    @OneToMany(mappedBy = "image", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Annotation> annotations;
}
