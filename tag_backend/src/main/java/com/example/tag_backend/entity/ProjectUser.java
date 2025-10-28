package com.example.tag_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 项目用户关联实体类
 * 存储用户参与项目的关联关系
 * 
 * @author 辛晓红
 * @version 1.0
 * @since 2025-10-16
 */
@Entity
@Table(name = "project_users", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "user_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectUser {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 项目ID（不使用物理外键）
     */
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    /**
     * 用户ID（不使用物理外键）
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 添加者ID（哪个管理员添加的）
     */
    @Column(name = "added_by")
    private Long addedBy;

    /**
     * 添加时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 用户角色（在项目中的角色：成员、负责人等）
     */
    @Column(name = "role", length = 20)
    private String role = "member"; // member, leader, observer

    /**
     * 状态
     */
    @Column(name = "status", length = 20)
    private String status = "active"; // active, inactive

    /**
     * 备注
     */
    @Column(columnDefinition = "TEXT")
    private String remark;
}


