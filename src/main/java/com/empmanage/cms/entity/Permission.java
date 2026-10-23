package com.empmanage.cms.entity;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name; // e.g. EMPLOYEE_VIEW, EMPLOYEE_CREATE, etc.

    @Column(nullable = false, length = 50)
    private String module; // e.g. EMPLOYEES, DEPARTMENTS, ATTENDANCE, REPORTS, CMS, USERS, ROLES

    @Column(nullable = false, length = 50)
    private String action; // e.g. VIEW, CREATE, EDIT, DELETE, EXPORT

    @Column(length = 255)
    private String description;

    public Permission() {}

    public Permission(String name, String module, String action, String description) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permission)) return false;
        Permission that = (Permission) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
