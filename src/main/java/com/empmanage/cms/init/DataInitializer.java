package com.empmanage.cms.init;

import com.empmanage.cms.entity.CmsSection;
import com.empmanage.cms.entity.Department;
import com.empmanage.cms.entity.Employee;
import com.empmanage.cms.entity.Permission;
import com.empmanage.cms.entity.Role;
import com.empmanage.cms.entity.User;
import com.empmanage.cms.repository.CmsSectionRepository;
import com.empmanage.cms.repository.DepartmentRepository;
import com.empmanage.cms.repository.EmployeeRepository;
import com.empmanage.cms.repository.PermissionRepository;
import com.empmanage.cms.repository.RoleRepository;
import com.empmanage.cms.repository.UserRepository;
import com.empmanage.cms.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.*;

@Component
public class DataInitializer implements CommandLineRunner {

    @Value("${app.security.demo-accounts-enabled:true}")
    private boolean demoAccountsEnabled;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CmsSectionRepository cmsSectionRepository;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        runSchemaMigrations();
        initRolesAndPermissions();
        initDepartments();
        initUsers();
        syncUsersWithRoles();
        initCmsSections();
        migrateEmployeeDepartments();
        syncExistingUsersToEmployeeDirectory();
        cleanupTestAndOrphanAttendanceLogs();
    }

    private void runSchemaMigrations() {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("DO $$ BEGIN " +
                         "  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='employees' AND column_name='department') THEN " +
                         "    UPDATE employees e SET department_id = d.id FROM departments d WHERE e.department_id IS NULL AND LOWER(e.department) = LOWER(d.name); " +
                         "    ALTER TABLE employees DROP COLUMN department; " +
                         "  END IF; " +
                         "END $$;");
        } catch (Exception e) {
            System.err.println("Schema migration info: " + e.getMessage());
        }
    }

    private void initRolesAndPermissions() {
        // 1. Initialize Standard Permissions
        List<Permission> standardPerms = List.of(
            // EMPLOYEES Module
            new Permission("EMPLOYEE_VIEW", "EMPLOYEES", "VIEW", "View workforce directory and employee profiles"),
            new Permission("EMPLOYEE_CREATE", "EMPLOYEES", "CREATE", "Create new employee records"),
            new Permission("EMPLOYEE_EDIT", "EMPLOYEES", "EDIT", "Edit existing employee information"),
            new Permission("EMPLOYEE_DELETE", "EMPLOYEES", "DELETE", "Remove employee records"),
            new Permission("EMPLOYEE_EXPORT", "EMPLOYEES", "EXPORT", "Export workforce directory to CSV"),

            // DEPARTMENTS Module
            new Permission("DEPARTMENT_VIEW", "DEPARTMENTS", "VIEW", "View organizational departments"),
            new Permission("DEPARTMENT_CREATE", "DEPARTMENTS", "CREATE", "Create new departments"),
            new Permission("DEPARTMENT_EDIT", "DEPARTMENTS", "EDIT", "Modify department details and heads"),
            new Permission("DEPARTMENT_DELETE", "DEPARTMENTS", "DELETE", "Delete unassigned departments"),
            new Permission("DEPARTMENT_EXPORT", "DEPARTMENTS", "EXPORT", "Export department list"),

            // ATTENDANCE Module
            new Permission("ATTENDANCE_VIEW", "ATTENDANCE", "VIEW", "View company-wide attendance and shift records"),
            new Permission("ATTENDANCE_CREATE", "ATTENDANCE", "CREATE", "Clock in / clock out shifts"),
            new Permission("ATTENDANCE_EDIT", "ATTENDANCE", "EDIT", "Override attendance status or perform admin punch out"),
            new Permission("ATTENDANCE_DELETE", "ATTENDANCE", "DELETE", "Delete attendance entries"),
            new Permission("ATTENDANCE_EXPORT", "ATTENDANCE", "EXPORT", "Export attendance timesheets to CSV"),

            // REPORTS Module
            new Permission("REPORT_VIEW", "REPORTS", "VIEW", "View operational reports and statistics"),
            new Permission("REPORT_EXPORT", "REPORTS", "EXPORT", "Download analytical reports and audit logs"),

            // CMS Module
            new Permission("CMS_VIEW", "CMS", "VIEW", "Preview dynamic website CMS content"),
            new Permission("CMS_EDIT", "CMS", "EDIT", "Modify website hero, about, and contact sections"),

            // USERS Module
            new Permission("USER_VIEW", "USERS", "VIEW", "View registered user accounts"),
            new Permission("USER_MANAGE", "USERS", "EDIT", "Manage user accounts, passwords, and statuses"),

            // ROLES Module
            new Permission("ROLE_MANAGE", "ROLES", "EDIT", "Create, configure, and assign roles & permission matrix"),

            // TASKS Module
            new Permission("TASK_VIEW_OWN", "TASKS", "VIEW", "View personal assigned tasks"),
            new Permission("TASK_VIEW_ALL", "TASKS", "VIEW", "View all company and department tasks"),
            new Permission("TASK_CREATE", "TASKS", "CREATE", "Create and define new tasks"),
            new Permission("TASK_ASSIGN", "TASKS", "EDIT", "Assign tasks to workforce employees"),
            new Permission("TASK_UPDATE_OWN", "TASKS", "EDIT", "Update progress and work notes on personal tasks"),
            new Permission("TASK_UPDATE_ALL", "TASKS", "EDIT", "Update and modify any company task"),
            new Permission("TASK_SUBMIT", "TASKS", "EDIT", "Submit completed work for manager review"),
            new Permission("TASK_REVIEW", "TASKS", "EDIT", "Review, approve, or request changes on submitted tasks"),
            new Permission("TASK_DELETE", "TASKS", "DELETE", "Delete tasks from the system"),
            new Permission("TASK_ANALYTICS_VIEW", "TASKS", "VIEW", "View task analytics, completion rates, and metrics")
        );

        Map<String, Permission> permMap = new HashMap<>();
        for (Permission p : standardPerms) {
            Optional<Permission> existing = permissionRepository.findByNameIgnoreCase(p.getName());
            if (existing.isPresent()) {
                permMap.put(p.getName(), existing.get());
            } else {
                permMap.put(p.getName(), permissionRepository.save(p));
            }
        }

        // 2. Initialize Default Roles
        // ROLE_ADMIN: All permissions
        initRoleIfMissing("ROLE_ADMIN", "Administrator", "Full unrestricted administrative access to all system features.", true, permMap.values());

        // ROLE_HR_MANAGER: Workforce, attendance, reports, departments view, task management oversight
        Set<Permission> hrPerms = new HashSet<>();
        addPerms(hrPerms, permMap, "EMPLOYEE_VIEW", "EMPLOYEE_CREATE", "EMPLOYEE_EDIT", "EMPLOYEE_EXPORT",
                "DEPARTMENT_VIEW", "ATTENDANCE_VIEW", "ATTENDANCE_CREATE", "ATTENDANCE_EDIT", "ATTENDANCE_EXPORT",
                "REPORT_VIEW", "REPORT_EXPORT", "USER_VIEW",
                "TASK_VIEW_OWN", "TASK_VIEW_ALL", "TASK_CREATE", "TASK_ASSIGN", "TASK_ANALYTICS_VIEW");
        initRoleIfMissing("ROLE_HR_MANAGER", "HR Manager", "Human resources manager with workforce, shift scheduling, and reporting access.", false, hrPerms);

        // ROLE_MANAGER: Department workforce & attendance management, task creation, assignment & review
        Set<Permission> mgrPerms = new HashSet<>();
        addPerms(mgrPerms, permMap, "EMPLOYEE_VIEW", "EMPLOYEE_EDIT", "DEPARTMENT_VIEW",
                "ATTENDANCE_VIEW", "ATTENDANCE_CREATE", "ATTENDANCE_EDIT", "REPORT_VIEW",
                "TASK_VIEW_OWN", "TASK_VIEW_ALL", "TASK_CREATE", "TASK_ASSIGN", "TASK_UPDATE_OWN",
                "TASK_UPDATE_ALL", "TASK_SUBMIT", "TASK_REVIEW", "TASK_ANALYTICS_VIEW");
        initRoleIfMissing("ROLE_MANAGER", "Department Manager", "Departmental supervisor with team workforce oversight and shift management.", false, mgrPerms);

        // ROLE_USER: Standard employee shift clock & task execution
        Set<Permission> userPerms = new HashSet<>();
        addPerms(userPerms, permMap, "EMPLOYEE_VIEW", "DEPARTMENT_VIEW", "ATTENDANCE_CREATE", "CMS_VIEW",
                "TASK_VIEW_OWN", "TASK_UPDATE_OWN", "TASK_SUBMIT");
        initRoleIfMissing("ROLE_USER", "Standard Employee", "Standard employee role with personal attendance shift clock and public directory access.", true, userPerms);

        // ROLE_VIEWER: Read-only access
        Set<Permission> viewerPerms = new HashSet<>();
        addPerms(viewerPerms, permMap, "EMPLOYEE_VIEW", "DEPARTMENT_VIEW", "CMS_VIEW", "REPORT_VIEW",
                "TASK_VIEW_ALL", "TASK_ANALYTICS_VIEW");
        initRoleIfMissing("ROLE_VIEWER", "Read-Only Viewer", "Auditor and read-only access to workforce directories, departments, and general reports.", false, viewerPerms);
    }

    private void addPerms(Set<Permission> target, Map<String, Permission> map, String... names) {
        for (String n : names) {
            Permission p = map.get(n);
            if (p != null) target.add(p);
        }
    }

    private void initRoleIfMissing(String name, String displayName, String description, boolean systemRole, Collection<Permission> permissions) {
        Optional<Role> roleOpt = roleRepository.findByNameIgnoreCase(name);
        if (roleOpt.isEmpty()) {
            Role role = new Role(name, displayName, description, systemRole);
            role.setPermissions(new HashSet<>(permissions));
            roleRepository.save(role);
        } else {
            Role role = roleOpt.get();
            // Ensure newly added permissions are merged
            if (role.getPermissions() == null) {
                role.setPermissions(new HashSet<>(permissions));
            } else {
                role.getPermissions().addAll(permissions);
            }
            roleRepository.save(role);
        }
    }

    private void syncUsersWithRoles() {
        Role adminRole = roleRepository.findByNameIgnoreCase("ROLE_ADMIN").orElse(null);
        Role userRole = roleRepository.findByNameIgnoreCase("ROLE_USER").orElse(null);

        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (user.getRoleEntity() == null) {
                if (user.isAdmin()) {
                    user.setRoleEntity(adminRole);
                    user.setRole("ROLE_ADMIN");
                } else {
                    String userRoleName = User.normalizeRole(user.getRole());
                    Role matchedRole = roleRepository.findByNameIgnoreCase(userRoleName).orElse(userRole);
                    user.setRoleEntity(matchedRole);
                    user.setRole(matchedRole != null ? matchedRole.getName() : "ROLE_USER");
                }
                userRepository.save(user);
            }
        }
    }

    private void cleanupTestAndOrphanAttendanceLogs() {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM attendance_logs WHERE LOWER(user_email) NOT IN (SELECT LOWER(email) FROM users) " +
                               "AND LOWER(user_email) NOT IN (SELECT LOWER(email) FROM employees WHERE email IS NOT NULL);");
        } catch (Exception e) {
            System.err.println("Attendance cleanup info: " + e.getMessage());
        }
    }

    private void initUsers() {
        if (!demoAccountsEnabled) {
            return;
        }

        Role adminRole = roleRepository.findByNameIgnoreCase("ROLE_ADMIN").orElse(null);
        Role userRole = roleRepository.findByNameIgnoreCase("ROLE_USER").orElse(null);

        if (!userRepository.existsByEmailIgnoreCase("admin@empmanage.com")) {
            User admin = new User(
                "System Administrator",
                "admin@empmanage.com",
                passwordEncoder.encode("admin123"),
                "ROLE_ADMIN",
                "Engineering & Technology"
            );
            admin.setRoleEntity(adminRole);
            User savedAdmin = userRepository.save(admin);
            attendanceService.ensureTodayAttendanceForUser(savedAdmin);
        }

        if (!userRepository.existsByEmailIgnoreCase("employee@empmanage.com")) {
            User emp = new User(
                "Demo Employee",
                "employee@empmanage.com",
                passwordEncoder.encode("emp123"),
                "ROLE_USER",
                "Engineering & Technology"
            );
            emp.setRoleEntity(userRole);
            User savedEmp = userRepository.save(emp);
            attendanceService.ensureTodayAttendanceForUser(savedEmp);
        }
    }

    private void initDepartments() {
        if (departmentRepository.count() == 0) {
            departmentRepository.save(new Department(
                "Engineering & Technology", "ENG", "Lead Software Architect",
                "Software engineering, cloud infrastructure, AI architecture and digital core development.",
                "code", 0
            ));
            departmentRepository.save(new Department(
                "Human Resources (HR)", "HR", "VP of People Operations",
                "Talent acquisition, organizational culture, compliance, employee wellness and workplace benefits.",
                "users", 0
            ));
            departmentRepository.save(new Department(
                "Sales & Business", "SLS", "Chief Commercial Officer",
                "B2B enterprise partnerships, global client acquisition and revenue generation engines.",
                "briefcase", 0
            ));
            departmentRepository.save(new Department(
                "Finance & Accounts", "FIN", "Head of Financial Planning",
                "Corporate governance, financial forecasting, payroll automation, audits and taxation compliance.",
                "dollar-sign", 0
            ));
            departmentRepository.save(new Department(
                "Marketing & Communications", "MKT", "Chief Brand Officer",
                "Brand strategy, performance marketing, social engagement and international media communication.",
                "trending-up", 0
            ));
            departmentRepository.save(new Department(
                "Operations & QA", "OPS", "Director of Quality Assurance",
                "Logistical coordination, automated quality assurance pipelines and business process efficiency.",
                "activity", 0
            ));
        }
    }

    private void initCmsSections() {
        if (!cmsSectionRepository.existsBySectionKeyIgnoreCase("hero")) {
            cmsSectionRepository.save(new CmsSection(
                "hero",
                "Enterprise Employee Directory & Workforce Hub",
                "Streamline organizational workforce tracking, department rosters, real-time employee attendance and corporate hierarchy in one secure hub.",
                "Explore our complete enterprise management system built with Spring Boot and PostgreSQL.",
                "{\"ctaText\":\"Explore Workforce\",\"ctaLink\":\"#employees\"}"
            ));
        }

        if (!cmsSectionRepository.existsBySectionKeyIgnoreCase("about")) {
            cmsSectionRepository.save(new CmsSection(
                "about",
                "Empowering Modern Enterprises with Intelligent Workforce Management",
                "Designed from the ground up for modern businesses needing simplicity, speed, and real-time oversight.",
                "EmpManage provides a centralized digital operational cockpit built for enterprises.",
                "{\"companies\":\"500+\",\"employees\":\"50k+\",\"uptime\":\"99.9%\"}"
            ));
        }

        if (!cmsSectionRepository.existsBySectionKeyIgnoreCase("contact")) {
            cmsSectionRepository.save(new CmsSection(
                "contact",
                "Contact Support & Headquarters",
                "Have questions, feedback, or need enterprise support? We are here to help.",
                "Reach our corporate headquarters or customer success department 24/7.",
                "{\"address\":\"123 Tech Park, Sector 62, Noida, UP - 201301\",\"email\":\"contact@empmanage.com\",\"phone\":\"+91 (0120) 456-7890\"}"
            ));
        }
    }

    private void migrateEmployeeDepartments() {
        List<Employee> employees = employeeRepository.findAll();
        List<Department> departments = departmentRepository.findAll();
        if (departments.isEmpty()) return;

        Department defaultDept = departments.get(0);
        for (Employee emp : employees) {
            if (emp.getDepartment() == null) {
                emp.setDepartment(defaultDept);
                employeeRepository.save(emp);
            }
        }
    }

    private void syncExistingUsersToEmployeeDirectory() {
        List<User> users = userRepository.findAll();
        for (User u : users) {
            if (!employeeRepository.existsByEmailIgnoreCase(u.getEmail())) {
                Department dept = resolveDepartment(u.getDepartment());
                Employee emp = new Employee();
                emp.setName(u.getFullName());
                emp.setEmail(u.getEmail());
                emp.setTitle(u.isAdmin() ? "System Administrator" : "Software Engineer");
                emp.setDepartment(dept);
                emp.setStatus("Active");
                long count = employeeRepository.count() + 1;
                emp.setEmployeeCode("EMP-" + String.format("%03d", count));
                emp.setSalary(0.0);
                employeeRepository.save(emp);

                if (dept != null && dept.getId() != null) {
                    dept.setTotalMembers((int) employeeRepository.countByDepartment_Id(dept.getId()));
                    departmentRepository.save(dept);
                }
            }
        }
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
}
