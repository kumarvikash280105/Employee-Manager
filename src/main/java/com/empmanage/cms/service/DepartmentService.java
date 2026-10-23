package com.empmanage.cms.service;

import com.empmanage.cms.dto.DepartmentRequest;
import com.empmanage.cms.entity.Department;
import com.empmanage.cms.repository.DepartmentRepository;
import com.empmanage.cms.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    public List<Department> getAllDepartments() {
        List<Department> departments = departmentRepository.findAll();
        // Dynamically compute real-time employee count per department
        for (Department dept : departments) {
            long count = employeeRepository.countByDepartment_Id(dept.getId());
            if (dept.getTotalMembers() == null || dept.getTotalMembers() != (int) count) {
                dept.setTotalMembers((int) count);
            }
        }
        return departments;
    }

    public Optional<Department> getDepartmentById(Long id) {
        Optional<Department> deptOpt = departmentRepository.findById(id);
        deptOpt.ifPresent(dept -> dept.setTotalMembers((int) employeeRepository.countByDepartment_Id(dept.getId())));
        return deptOpt;
    }

    public Optional<Department> getDepartmentByName(String name) {
        if (name == null) return Optional.empty();
        return departmentRepository.findByNameIgnoreCase(name.trim());
    }

    @Transactional
    public Department createDepartment(DepartmentRequest req) {
        String name = req.getName().trim();
        if (departmentRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("A department named '" + name + "' already exists.");
        }

        String code = req.getCode();
        if (code == null || code.isBlank()) {
            code = name.replaceAll("[^A-Za-z0-9]", "").substring(0, Math.min(4, name.length())).toUpperCase();
        } else {
            code = code.trim().toUpperCase();
        }

        Department dept = new Department(
            name,
            code,
            req.getHeadName() != null ? req.getHeadName().trim() : "Unassigned",
            req.getDescription() != null ? req.getDescription().trim() : "",
            req.getIcon() != null ? req.getIcon() : "briefcase",
            0
        );
        if (req.getBudget() != null) dept.setBudget(req.getBudget());
        dept.setActive(req.isActive());

        return departmentRepository.save(dept);
    }

    @Transactional
    public Department updateDepartment(Long id, DepartmentRequest req) {
        Department existing = departmentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Department not found with ID: " + id));

        String newName = req.getName().trim();
        if (!existing.getName().equalsIgnoreCase(newName) && departmentRepository.existsByNameIgnoreCase(newName)) {
            throw new IllegalArgumentException("Another department named '" + newName + "' already exists.");
        }

        existing.setName(newName);
        if (req.getCode() != null && !req.getCode().isBlank()) {
            existing.setCode(req.getCode().trim().toUpperCase());
        }
        if (req.getHeadName() != null) existing.setHeadName(req.getHeadName().trim());
        if (req.getDescription() != null) existing.setDescription(req.getDescription().trim());
        if (req.getIcon() != null) existing.setIcon(req.getIcon());
        if (req.getBudget() != null) existing.setBudget(req.getBudget());
        existing.setActive(req.isActive());

        Department saved = departmentRepository.save(existing);
        saved.setTotalMembers((int) employeeRepository.countByDepartment_Id(saved.getId()));
        return saved;
    }

    @Transactional
    public void deleteDepartment(Long id) {
        Department dept = departmentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Department not found with ID: " + id));

        // Prevent deletion of departments that are still assigned to employees (Requirement 4)
        long empCount = employeeRepository.countByDepartment_Id(dept.getId());
        if (empCount > 0) {
            throw new IllegalStateException("Cannot delete department '" + dept.getName() + "' because " +
                    empCount + " employee(s) are currently assigned to it. Please reassign the employees first.");
        }

        departmentRepository.delete(dept);
    }

    public long getTotalCount() {
        return departmentRepository.count();
    }
}
