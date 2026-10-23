package com.empmanage.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class DepartmentRequest {

    @NotBlank(message = "Department name is required")
    @Size(min = 2, max = 100, message = "Department name must be between 2 and 100 characters")
    private String name;

    @Pattern(regexp = "^$|[A-Za-z0-9\\-]{2,10}", message = "Department code must be 2-10 alphanumeric characters")
    private String code;

    private String headName;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    private String icon = "briefcase";

    @PositiveOrZero(message = "Budget must be zero or a positive amount")
    private Double budget = 0.0;

    private boolean active = true;

    public DepartmentRequest() {}

    public DepartmentRequest(String name, String code, String headName, String description) {
        this.name = name;
        this.code = code;
        this.headName = headName;
        this.description = description;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getHeadName() { return headName; }
    public void setHeadName(String headName) { this.headName = headName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public Double getBudget() { return budget; }
    public void setBudget(Double budget) { this.budget = budget; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
