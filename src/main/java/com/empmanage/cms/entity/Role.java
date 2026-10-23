package com.empmanage.cms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name; // e.g. ROLE_ADMIN, ROLE_HR_MANAGER, ROLE_MANAGER, ROLE_USER, ROLE_VIEWER

    @Column(nullable = false, length = 100)
    private String displayName; // e.g. Administrator, HR Manager, etc.

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean systemRole = false; // System roles cannot be deleted

    @Column(nullable = false)
    private boolean active = true; // Disabled roles cannot be assigned or authenticated

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Role() {}

    public Role(String name, String displayName, String description, boolean systemRole) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.systemRole = systemRole;
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean hasPermission(String permissionName) {
        if (permissions == null || permissionName == null) return false;
        return permissions.stream().anyMatch(p -> p.getName().equalsIgnoreCase(permissionName));
    }

    public void addPermission(Permission permission) {
        if (this.permissions == null) {
            this.permissions = new HashSet<>();
        }
        this.permissions.add(permission);
    }

    public void removePermission(Permission permission) {
        if (this.permissions != null) {
            this.permissions.remove(permission);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isSystemRole() { return systemRole; }
    public void setSystemRole(boolean systemRole) { this.systemRole = systemRole; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Set<Permission> getPermissions() { return permissions; }
    public void setPermissions(Set<Permission> permissions) { this.permissions = permissions; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role)) return false;
        Role role = (Role) o;
        return Objects.equals(name, role.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
