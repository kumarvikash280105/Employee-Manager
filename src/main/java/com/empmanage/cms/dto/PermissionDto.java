package com.empmanage.cms.dto;

public class PermissionDto {

    private Long id;
    private String name;
    private String module;
    private String action;
    private String description;

    public PermissionDto() {}

    public PermissionDto(Long id, String name, String module, String action, String description) {
        this.id = id;
        this.name = name;
        this.module = module;
        this.action = action;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
