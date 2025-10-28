package com.example.tag_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 项目实体类
 * 存储大任务项目信息
 * 
 * @author 辛晓红
 * @version 1.0
 * @since 2025-10-16
 */
@Entity
@Table(name = "projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 项目名称
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * 项目描述
     */
    @Column(length = 500)
    private String description;

    /**
     * 项目状态
     */
    @Column(length = 20)
    private String status = "active"; // active, inactive, completed, paused

    /**
     * 创建者ID
     */
    @Column(name = "created_by")
    private Long createdBy;

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
     * 开始时间
     */
    @Column(name = "start_date")
    private LocalDateTime startDate;

    /**
     * 结束时间
     */
    @Column(name = "end_date")
    private LocalDateTime endDate;

    /**
     * 项目备注
     */
    @Column(columnDefinition = "TEXT")
    private String remark;
}
