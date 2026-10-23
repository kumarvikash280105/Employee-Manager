package com.empmanage.cms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class EmployeeRequest {

    @NotBlank(message = "Employee full name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Work email address is required")
    @Email(message = "Please provide a valid work email address")
    private String email;

    @Pattern(regexp = "^$|[0-9+\\-\\s()]{7,20}", message = "Please enter a valid phone number (7-20 digits/symbols)")
    private String phone;

    @NotBlank(message = "Job title is required")
    @Size(min = 2, max = 80, message = "Job title must be between 2 and 80 characters")
    private String title;

    private Long departmentId;

    private String department; // Department name string fallback

    @Pattern(regexp = "^(Active|On Leave|Inactive)$", message = "Status must be 'Active', 'On Leave', or 'Inactive'")
    private String status = "Active";

    @PositiveOrZero(message = "Salary must be zero or a positive amount")
    private Double salary = 0.0;

    public EmployeeRequest() {}

    public EmployeeRequest(String name, String email, String phone, String title, String department, String status) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.title = title;
        this.department = department;
        this.status = status != null ? status : "Active";
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getSalary() { return salary; }
    public void setSalary(Double salary) { this.salary = salary; }
}
