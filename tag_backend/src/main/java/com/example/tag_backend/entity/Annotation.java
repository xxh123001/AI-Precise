package com.example.tag_backend.entity;

import com.example.tag_backend.enums.AnnotationStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 标注实体类
 * 存储用户对图像的标注结果，使用JSON格式存储标注数据
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Entity
@Table(name = "annotations", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"image_id", "user_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Annotation {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 图像ID
     */
    @Column(name = "image_id", nullable = false)
    private Long imageId;

    /**
     * 关联的图像
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", insertable = false, updatable = false)
    private Image image;

    /**
     * 标注用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 标注用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    /**
     * 标注结果JSON数据
     * 存储具体的标注内容和元数据
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "JSON")
    private String labels;

    /**
     * 标注状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnnotationStatus status = AnnotationStatus.DRAFT;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 标注备注
     * 用于标注者添加额外的说明信息
     */
    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;
}
