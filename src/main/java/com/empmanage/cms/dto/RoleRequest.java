package com.empmanage.cms.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class RoleRequest {

    private String name; // e.g. ROLE_CUSTOM_ADMIN or CUSTOM_ADMIN

    @NotBlank(message = "Role display name is required")
    private String displayName;

    private String description;

    private Boolean active;

    private List<String> permissions;

    public RoleRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
}
