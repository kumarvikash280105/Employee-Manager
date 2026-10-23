package com.empmanage.cms.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role; // "ROLE_ADMIN", "ROLE_USER", "ROLE_HR_MANAGER", "ROLE_MANAGER", "ROLE_VIEWER"

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role roleEntity;

    private String department;

    private LocalDateTime createdAt = LocalDateTime.now();

    public User() {}

    public User(String fullName, String email, String password, String role, String department) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.role = normalizeRole(role);
        this.department = department;
        this.createdAt = LocalDateTime.now();
    }

    public static String normalizeRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return "ROLE_USER";
        }
        String clean = rawRole.trim().toUpperCase();
        if (clean.equals("ADMIN") || clean.equals("ROLE_ADMIN")) {
            return "ROLE_ADMIN";
        }
        if (clean.equals("USER") || clean.equals("EMPLOYEE") || clean.equals("ROLE_USER")) {
            return "ROLE_USER";
        }
        if (clean.startsWith("ROLE_")) {
            return clean;
        }
        return "ROLE_" + clean;
    }

    public boolean isAdmin() {
        return "ROLE_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
    }

    @JsonProperty("displayRole")
    public String getDisplayRole() {
        if (roleEntity != null && roleEntity.getDisplayName() != null) {
            return roleEntity.getDisplayName();
        }
        if (isAdmin()) {
            return "Administrator";
        }
        if (role != null) {
            String clean = role.replace("ROLE_", "");
            return clean.replace("_", " ");
        }
        return "Standard Employee";
    }

    @JsonProperty("permissions")
    public java.util.List<String> getPermissionNames() {
        if (roleEntity != null && roleEntity.getPermissions() != null) {
            return roleEntity.getPermissions().stream()
                    .map(Permission::getName)
                    .sorted()
                    .collect(java.util.stream.Collectors.toList());
        }
        return java.util.Collections.emptyList();
    }

    public boolean hasPermission(String permissionName) {
        if (permissionName == null || permissionName.isBlank()) {
            return false;
        }
        if (roleEntity != null && roleEntity.getPermissions() != null) {
            return roleEntity.getPermissions().stream()
                    .anyMatch(p -> permissionName.equalsIgnoreCase(p.getName()));
        }
        return false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { 
        this.role = normalizeRole(role); 
    }

    public Role getRoleEntity() { return roleEntity; }
    public void setRoleEntity(Role roleEntity) { 
        this.roleEntity = roleEntity;
        if (roleEntity != null) {
            this.role = roleEntity.getName();
        }
    }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
