package com.empmanage.cms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "role_audit_logs")
public class RoleAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String action; // ROLE_CREATED, ROLE_UPDATED, PERMISSIONS_UPDATED, ROLE_DEACTIVATED, ROLE_ACTIVATED, ROLE_DELETED, USER_ROLE_ASSIGNED

    @Column(length = 100)
    private String targetRole;

    @Column(length = 150)
    private String targetUser;

    @Column(nullable = false, length = 150)
    private String performedBy;

    @Column(length = 1000)
    private String details;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    public RoleAuditLog() {}

    public RoleAuditLog(String action, String targetRole, String targetUser, String performedBy, String details) {
        this.action = action;
        this.targetRole = targetRole;
        this.targetUser = targetUser;
        this.performedBy = performedBy;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }

    public String getTargetUser() { return targetUser; }
    public void setTargetUser(String targetUser) { this.targetUser = targetUser; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
