package com.empmanage.cms.dto;

public class RoleStatsDto {

    private long totalRoles;
    private long totalPermissions;
    private long assignedUsers;
    private long restrictedModules;

    public RoleStatsDto() {}

    public RoleStatsDto(long totalRoles, long totalPermissions, long assignedUsers, long restrictedModules) {
        this.totalRoles = totalRoles;
        this.totalPermissions = totalPermissions;
        this.assignedUsers = assignedUsers;
        this.restrictedModules = restrictedModules;
    }

    public long getTotalRoles() { return totalRoles; }
    public void setTotalRoles(long totalRoles) { this.totalRoles = totalRoles; }

    public long getTotalPermissions() { return totalPermissions; }
    public void setTotalPermissions(long totalPermissions) { this.totalPermissions = totalPermissions; }

    public long getAssignedUsers() { return assignedUsers; }
    public void setAssignedUsers(long assignedUsers) { this.assignedUsers = assignedUsers; }

    public long getRestrictedModules() { return restrictedModules; }
    public void setRestrictedModules(long restrictedModules) { this.restrictedModules = restrictedModules; }
}
