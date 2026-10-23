package com.empmanage.cms.service;

import com.empmanage.cms.dto.EmployeeRequest;
import com.empmanage.cms.entity.Department;
import com.empmanage.cms.entity.Employee;
import com.empmanage.cms.repository.DepartmentRepository;
import com.empmanage.cms.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private com.empmanage.cms.repository.UserRepository userRepository;

    @Autowired
    private AttendanceService attendanceService;

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public Page<Employee> getEmployeesPaged(String query, String dept, String status, int page, int size) {
        int pageNumber = Math.max(0, page);
        int pageSize = Math.min(100, Math.max(1, size));
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "id"));

        String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        String cleanDept = (dept != null && !dept.trim().isEmpty() && !"ALL".equalsIgnoreCase(dept.trim())) ? dept.trim() : null;
        String cleanStatus = (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) ? status.trim() : null;

        return employeeRepository.findWithFilters(cleanQuery, cleanDept, cleanStatus, pageable);
    }

    public List<Employee> searchEmployees(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllEmployees();
        }
        return employeeRepository.searchEmployees(query.trim());
    }

    public List<Employee> getEmployeesByDepartment(String departmentName) {
        if (departmentName == null || departmentName.equalsIgnoreCase("ALL")) {
            return getAllEmployees();
        }
        String trimmed = departmentName.trim();
        List<Employee> exact = employeeRepository.findByDepartment_NameIgnoreCase(trimmed);
        if (!exact.isEmpty()) return exact;
        return employeeRepository.findAll().stream()
            .filter(e -> e.getDepartment() != null && e.getDepartment().getName().toLowerCase().contains(trimmed.toLowerCase()))
            .toList();
    }

    public Optional<Employee> getEmployeeById(Long id) {
        return employeeRepository.findById(id);
    }

    public Optional<Employee> getEmployeeByEmail(String email) {
        if (email == null) return Optional.empty();
        return employeeRepository.findByEmailIgnoreCase(email.trim().toLowerCase());
    }

    @Transactional
    public Employee createEmployee(EmployeeRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        // Prevent duplicate employee emails (Requirement 3)
        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("An employee with work email '" + email + "' already exists.");
        }

        Department department = resolveDepartment(req.getDepartmentId(), req.getDepartment());

        Employee employee = new Employee();
        employee.setName(req.getName().trim());
        employee.setEmail(email);
        employee.setPhone(req.getPhone() != null ? req.getPhone().trim() : "");
        employee.setTitle(req.getTitle().trim());
        employee.setDepartment(department);
        employee.setStatus(req.getStatus() != null ? req.getStatus() : "Active");
        employee.setSalary(req.getSalary() != null ? req.getSalary() : 0.0);

        // Generate unique employee code if not provided
        long count = employeeRepository.count() + 1;
        employee.setEmployeeCode("EMP-" + String.format("%03d", count));

        Employee saved = employeeRepository.save(employee);

        // Synchronize department total members
        if (department != null && department.getId() != null) {
            department.setTotalMembers((int) employeeRepository.countByDepartment_Id(department.getId()));
            departmentRepository.save(department);
        }

        // Automatically initialize attendance tracking for this employee
        attendanceService.ensureTodayAttendanceForEmployee(saved);

        return saved;
    }

    @Transactional
    public Employee updateEmployee(Long id, EmployeeRequest req) {
        Employee existing = employeeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + id));

        String newEmail = req.getEmail().trim().toLowerCase();
        // Prevent duplicate emails across different employees
        if (!existing.getEmail().equalsIgnoreCase(newEmail) && employeeRepository.existsByEmailIgnoreCase(newEmail)) {
            throw new IllegalArgumentException("Another employee with email '" + newEmail + "' already exists.");
        }

        Department oldDepartment = existing.getDepartment();
        Department newDepartment = resolveDepartment(req.getDepartmentId(), req.getDepartment());

        existing.setName(req.getName().trim());
        existing.setEmail(newEmail);
        existing.setTitle(req.getTitle().trim());
        existing.setPhone(req.getPhone() != null ? req.getPhone().trim() : "");
        existing.setDepartment(newDepartment);
        existing.setStatus(req.getStatus() != null ? req.getStatus() : "Active");
        if (req.getSalary() != null) existing.setSalary(req.getSalary());

        Employee updated = employeeRepository.save(existing);

        // Update counts for both old and new departments
        if (oldDepartment != null && (newDepartment == null || !oldDepartment.getId().equals(newDepartment.getId()))) {
            oldDepartment.setTotalMembers((int) employeeRepository.countByDepartment_Id(oldDepartment.getId()));
            departmentRepository.save(oldDepartment);
        }
        if (newDepartment != null) {
            newDepartment.setTotalMembers((int) employeeRepository.countByDepartment_Id(newDepartment.getId()));
            departmentRepository.save(newDepartment);
        }

        return updated;
    }

    @Transactional
    public void deleteEmployee(Long id) {
        Employee emp = employeeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + id));

        Department dept = emp.getDepartment();
        // Cascade delete employee's attendance logs
        attendanceService.deleteAttendanceByUserEmail(emp.getEmail());

        // Cascade delete user account if exists
        userRepository.findByEmailIgnoreCase(emp.getEmail()).ifPresent(userRepository::delete);

        employeeRepository.delete(emp);

        if (dept != null) {
            dept.setTotalMembers((int) employeeRepository.countByDepartment_Id(dept.getId()));
            departmentRepository.save(dept);
        }
    }

    private Department resolveDepartment(Long departmentId, String departmentName) {
        if (departmentId != null) {
            Optional<Department> deptOpt = departmentRepository.findById(departmentId);
            if (deptOpt.isPresent()) return deptOpt.get();
        }
        if (departmentName != null && !departmentName.isBlank()) {
            String trimmed = departmentName.trim();
            Optional<Department> deptOpt = departmentRepository.findByNameIgnoreCase(trimmed);
            if (deptOpt.isPresent()) return deptOpt.get();

            List<Department> allDepts = departmentRepository.findAll();
            for (Department d : allDepts) {
                if (d.getName().toLowerCase().contains(trimmed.toLowerCase()) ||
                    trimmed.toLowerCase().contains(d.getName().toLowerCase())) {
                    return d;
                }
            }
        }
        // Fallback to first available department or create general
        List<Department> all = departmentRepository.findAll();
        if (!all.isEmpty()) {
            return all.get(0);
        }
        Department fallback = new Department("Engineering & Technology", "ENG", "Lead Architect", "Core Department", "code", 0);
        return departmentRepository.save(fallback);
    }

    public long getTotalCount() {
        return employeeRepository.count();
    }

    public long getActiveCount() {
        return employeeRepository.countByStatusIgnoreCase("Active");
    }

    public List<Employee> getRecentEmployees() {
        return employeeRepository.findTop5ByOrderByJoiningDateDescIdDesc();
    }
}
