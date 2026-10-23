package com.empmanage.cms.dto;

import jakarta.validation.constraints.NotBlank;

public class AssignRoleRequest {

    @NotBlank(message = "User email or identifier is required")
    private String userEmail;

    @NotBlank(message = "Role name is required")
    private String roleName;

    public AssignRoleRequest() {}

    public AssignRoleRequest(String userEmail, String roleName) {
        this.userEmail = userEmail;
        this.roleName = roleName;
    }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
}
