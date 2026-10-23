package com.empmanage.cms.dto;

import java.util.List;

public class RoleDto {

    private Long id;
    private String name;
    private String displayName;
    private String description;
    private boolean systemRole;
    private boolean active;
    private long assignedUsersCount;
    private List<String> permissions;

    public RoleDto() {}

    public RoleDto(Long id, String name, String displayName, String description, boolean systemRole, boolean active, long assignedUsersCount, List<String> permissions) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.systemRole = systemRole;
        this.active = active;
        this.assignedUsersCount = assignedUsersCount;
        this.permissions = permissions;
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

    public long getAssignedUsersCount() { return assignedUsersCount; }
    public void setAssignedUsersCount(long assignedUsersCount) { this.assignedUsersCount = assignedUsersCount; }

    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
}
