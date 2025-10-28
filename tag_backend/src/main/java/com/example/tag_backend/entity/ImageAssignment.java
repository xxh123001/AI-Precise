package com.example.tag_backend.entity;

import com.example.tag_backend.enums.AssignmentStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 图像分配实体类
 * 记录图像标注任务的分配关系和状态
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Entity
@Table(name = "image_assignments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageAssignment {

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
     * 被分配的用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 被分配的用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    /**
     * 分配状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentStatus status = AssignmentStatus.ASSIGNED;

    /**
     * 分配者ID
     */
    @Column(name = "assigned_by")
    private Long assignedBy;

    /**
     * 分配者用户信息
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by", insertable = false, updatable = false)
    private User assigner;

    /**
     * 分配时间
     */
    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    /**
     * 完成时间
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * 是否已完成（只读模式标识）
     */
    @Column(name = "is_completed")
    private Boolean isCompleted = false;

    /**
     * 项目ID（大任务标识，不使用物理外键）
     */
    @Column(name = "project_id")
    private Long projectId = 1L; // 默认值为1，兼容现有数据
}
