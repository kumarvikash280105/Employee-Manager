package com.empmanage.cms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String code;

    private String headName;

    @Column(length = 1000)
    private String description;

    private String icon = "briefcase";

    private Integer totalMembers = 0;

    private Double budget = 0.0;

    private boolean active = true;

    @OneToMany(mappedBy = "department", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Employee> employees = new ArrayList<>();

    public Department() {}

    public Department(String name, String code, String headName, String description, String icon, Integer totalMembers) {
        this.name = name;
        this.code = code;
        this.headName = headName;
        this.description = description;
        this.icon = icon;
        this.totalMembers = totalMembers;
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public Integer getTotalMembers() { return totalMembers; }
    public void setTotalMembers(Integer totalMembers) { this.totalMembers = totalMembers; }

    public Double getBudget() { return budget; }
    public void setBudget(Double budget) { this.budget = budget; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public List<Employee> getEmployees() { return employees; }
    public void setEmployees(List<Employee> employees) { this.employees = employees; }
}
