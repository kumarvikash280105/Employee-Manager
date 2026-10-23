package com.empmanage.cms.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String employeeCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    @JsonIgnoreProperties({"employees"})
    private Department department;

    private String status = "Active"; // Active, On Leave, Inactive

    private Double salary = 0.0;

    private LocalDate joiningDate = LocalDate.now();

    public Employee() {}

    public Employee(String name, String email, Department department, String title, String status) {
        this.name = name;
        this.email = email;
        this.department = department;
        this.title = title;
        this.status = status != null ? status : "Active";
        this.joiningDate = LocalDate.now();
    }

    public Employee(String employeeCode, String name, String email, String phone, String title, Department department, String status) {
        this.employeeCode = employeeCode;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.title = title;
        this.department = department;
        this.status = status != null ? status : "Active";
        this.joiningDate = LocalDate.now();
    }

    @JsonProperty("departmentName")
    public String getDepartmentName() {
        return department != null ? department.getName() : "General";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getSalary() { return salary; }
    public void setSalary(Double salary) { this.salary = salary; }

    public LocalDate getJoiningDate() { return joiningDate; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }
}
