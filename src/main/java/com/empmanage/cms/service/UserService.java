package com.empmanage.cms.service;

import com.empmanage.cms.dto.LoginRequest;
import com.empmanage.cms.dto.RegisterRequest;
import com.empmanage.cms.entity.Department;
import com.empmanage.cms.entity.Employee;
import com.empmanage.cms.entity.User;
import com.empmanage.cms.repository.DepartmentRepository;
import com.empmanage.cms.repository.EmployeeRepository;
import com.empmanage.cms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired(required = false)
    private com.empmanage.cms.repository.RoleRepository roleRepository;

    @Transactional
    public User register(RegisterRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("An account with email '" + email + "' already exists.");
        }

        // Prevent users from self-registering as Administrator (Security Requirement 1)
        if (req.getRole() != null && (req.getRole().equalsIgnoreCase("ADMIN") || req.getRole().equalsIgnoreCase("ROLE_ADMIN"))) {
            throw new IllegalArgumentException("Registration with Administrator role is restricted. Only standard employee accounts can be registered.");
        }

        String encodedPassword = passwordEncoder.encode(req.getPassword());
        User user = new User(req.getFullName().trim(), email, encodedPassword, "ROLE_USER", req.getDepartment());
        if (roleRepository != null) {
            roleRepository.findByNameIgnoreCase("ROLE_USER").ifPresent(user::setRoleEntity);
        }
        User savedUser = userRepository.save(user);

        // Automatically initialize today's attendance for the newly registered user
        attendanceService.ensureTodayAttendanceForUser(savedUser);

        // Synchronize and add registered employee to the Employee Management Directory
        Department department = resolveDepartment(req.getDepartment());
        if (!employeeRepository.existsByEmailIgnoreCase(email)) {
            Employee employee = new Employee();
            employee.setName(savedUser.getFullName());
            employee.setEmail(savedUser.getEmail());
            String jobTitle = (req.getTitle() != null && !req.getTitle().isBlank())
                ? req.getTitle().trim()
                : getDefaultTitleForDepartment(department != null ? department.getName() : req.getDepartment());
            employee.setTitle(jobTitle);
            employee.setDepartment(department);
            employee.setStatus("Active");
            employee.setPhone(req.getPhone() != null ? req.getPhone().trim() : "");
            long count = employeeRepository.count() + 1;
            employee.setEmployeeCode("EMP-" + String.format("%03d", count));
            employee.setSalary(0.0);
            employeeRepository.save(employee);

            if (department != null && department.getId() != null) {
                department.setTotalMembers((int) employeeRepository.countByDepartment_Id(department.getId()));
                departmentRepository.save(department);
            }
        }

        return savedUser;
    }

    @Transactional
    public User login(LoginRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        // Support shorthand alias logins for convenient demo access
        if ("admin".equals(email)) {
            email = "admin@empmanage.com";
        } else if ("employee".equals(email)) {
            email = "employee@empmanage.com";
        }

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        User user = userOpt.get();

        // Enforce BCrypt hashing strictly; reject plaintext passwords (Security Requirement 1)
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        // Verify role if specified
        if (req.getRole() != null && !req.getRole().isBlank()) {
            boolean requestedAdmin = req.getRole().equalsIgnoreCase("ADMIN") || req.getRole().equalsIgnoreCase("ROLE_ADMIN");
            if (requestedAdmin != user.isAdmin()) {
                throw new IllegalArgumentException("Role mismatch. This account is registered as " + user.getDisplayRole() +
                        ". Please select '" + (user.isAdmin() ? "Admin" : "Employee") + "' option.");
            }
        }

        // Make sure today's attendance log exists for this user upon login
        attendanceService.ensureTodayAttendanceForUser(user);

        return user;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return userRepository.findByEmailIgnoreCase(email.trim().toLowerCase());
    }

    @Transactional
    public void deleteUser(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if ("admin@empmanage.com".equalsIgnoreCase(user.getEmail())) {
                throw new IllegalArgumentException("Protected master administrator account cannot be deleted.");
            }
            // Cascade delete user's attendance records
            attendanceService.deleteAttendanceByUserEmail(user.getEmail());

            // Cascade delete employee directory record if exists
            employeeRepository.findByEmailIgnoreCase(user.getEmail()).ifPresent(emp -> {
                Department dept = emp.getDepartment();
                employeeRepository.delete(emp);
                if (dept != null && dept.getId() != null) {
                    dept.setTotalMembers((int) employeeRepository.countByDepartment_Id(dept.getId()));
                    departmentRepository.save(dept);
                }
            });

            userRepository.deleteById(id);
        } else {
            throw new IllegalArgumentException("User not found with ID: " + id);
        }
    }

    @Transactional
    public void deleteUserByEmail(String email) {
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if ("admin@empmanage.com".equalsIgnoreCase(user.getEmail())) {
                throw new IllegalArgumentException("Protected master administrator account cannot be deleted.");
            }
            // Cascade delete user's attendance records
            attendanceService.deleteAttendanceByUserEmail(user.getEmail());

            // Cascade delete employee directory record if exists
            employeeRepository.findByEmailIgnoreCase(user.getEmail()).ifPresent(emp -> {
                Department dept = emp.getDepartment();
                employeeRepository.delete(emp);
                if (dept != null && dept.getId() != null) {
                    dept.setTotalMembers((int) employeeRepository.countByDepartment_Id(dept.getId()));
                    departmentRepository.save(dept);
                }
            });

            userRepository.delete(user);
        } else {
            throw new IllegalArgumentException("User not found with email: " + email);
        }
    }

    @Transactional
    public User updateUser(String email, String newRole, String newDepartment, String newTitle) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User account not found with email: " + email));

        if ("admin@empmanage.com".equalsIgnoreCase(user.getEmail()) && newRole != null && !newRole.toUpperCase().contains("ADMIN")) {
            throw new IllegalArgumentException("Protected master administrator role cannot be altered.");
        }

        if (newRole != null && !newRole.isBlank()) {
            String cleanRole = User.normalizeRole(newRole);
            user.setRole(cleanRole);
            if (roleRepository != null) {
                roleRepository.findByNameIgnoreCase(cleanRole).ifPresent(user::setRoleEntity);
            }
        }

        Department oldDept = null;
        Department newDept = null;

        if (newDepartment != null && !newDepartment.isBlank()) {
            user.setDepartment(newDepartment.trim());
            newDept = resolveDepartment(newDepartment);
        }

        User updatedUser = userRepository.save(user);

        // Synchronize with employee directory
        Optional<Employee> empOpt = employeeRepository.findByEmailIgnoreCase(user.getEmail());
        if (empOpt.isPresent()) {
            Employee emp = empOpt.get();
            oldDept = emp.getDepartment();

            if (newDept != null) {
                emp.setDepartment(newDept);
            }
            if (newTitle != null && !newTitle.isBlank()) {
                emp.setTitle(newTitle.trim());
            }
            employeeRepository.save(emp);

            if (oldDept != null && (newDept == null || !oldDept.getId().equals(newDept.getId()))) {
                oldDept.setTotalMembers((int) employeeRepository.countByDepartment_Id(oldDept.getId()));
                departmentRepository.save(oldDept);
            }
            if (newDept != null) {
                newDept.setTotalMembers((int) employeeRepository.countByDepartment_Id(newDept.getId()));
                departmentRepository.save(newDept);
            }
        }

        return updatedUser;
    }

    private Department resolveDepartment(String departmentName) {
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
        List<Department> all = departmentRepository.findAll();
        if (!all.isEmpty()) return all.get(0);
        return null;
    }

    private String getDefaultTitleForDepartment(String deptName) {
        if (deptName == null) return "Team Associate";
        String lower = deptName.toLowerCase();
        if (lower.contains("engine") || lower.contains("tech")) return "Software Engineer";
        if (lower.contains("human") || lower.contains("hr")) return "HR Specialist";
        if (lower.contains("sales") || lower.contains("busin")) return "Business Development Executive";
        if (lower.contains("finan") || lower.contains("account")) return "Financial Analyst";
        if (lower.contains("market")) return "Marketing Specialist";
        if (lower.contains("operat") || lower.contains("qa")) return "Operations Coordinator";
        return "Team Member";
    }
}
