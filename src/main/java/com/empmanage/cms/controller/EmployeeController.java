package com.empmanage.cms.controller;

import com.empmanage.cms.dto.ApiResponse;
import com.empmanage.cms.dto.EmployeeRequest;
import com.empmanage.cms.entity.Employee;
import com.empmanage.cms.entity.User;
import com.empmanage.cms.repository.UserRepository;
import com.empmanage.cms.service.EmployeeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEmployees(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String dept,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String userEmail,
            HttpServletRequest request) {

        boolean canViewWorkforce = hasCallerPermission(request, "EMPLOYEE_VIEW");
        boolean isAdmin = isCurrentUserAdmin(request);
        String authEmail = getAuthenticatedEmail(request);

        if (authEmail == null && userEmail != null && !userEmail.isBlank()) {
            authEmail = userEmail.trim().toLowerCase();
            Optional<User> uOpt = userRepository.findByEmailIgnoreCase(authEmail);
            if (uOpt.isPresent()) {
                if (uOpt.get().isAdmin()) {
                    isAdmin = true;
                }
                if (uOpt.get().hasPermission("EMPLOYEE_VIEW")) {
                    canViewWorkforce = true;
                }
            }
        }

        // 1. If has EMPLOYEE_VIEW permission (or Administrator with permission granted):
        if (canViewWorkforce) {
            if (page != null || size != null) {
                int p = page != null ? page : 0;
                int s = size != null ? size : 10;
                Page<Employee> empPage = employeeService.getEmployeesPaged(query, dept, status, p, s);

                Map<String, Object> response = new HashMap<>();
                response.put("employees", empPage.getContent());
                response.put("content", empPage.getContent());
                response.put("currentPage", empPage.getNumber());
                response.put("totalItems", empPage.getTotalElements());
                response.put("totalPages", empPage.getTotalPages());
                response.put("pageSize", empPage.getSize());

                return ResponseEntity.ok(ApiResponse.ok("Employees fetched successfully.", response));
            }

            List<Employee> list;
            if (query != null && !query.isBlank()) {
                list = employeeService.searchEmployees(query);
            } else if (dept != null && !dept.isBlank() && !"ALL".equalsIgnoreCase(dept)) {
                list = employeeService.getEmployeesByDepartment(dept);
            } else {
                list = employeeService.getAllEmployees();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("employees", list);
            response.put("content", list);
            response.put("totalItems", list.size());
            response.put("totalPages", 1);
            response.put("currentPage", 0);

            return ResponseEntity.ok(ApiResponse.ok("Employees fetched successfully.", response));
        }

        // 2. If caller is an Admin / Manager but EMPLOYEE_VIEW has been revoked:
        if (isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access Denied: Your active role does not possess the EMPLOYEE_VIEW permission to browse workforce directory."));
        }

        // 3. If Standard Employee (authenticated) without EMPLOYEE_VIEW: strictly return ONLY their own employee profile
        if (authEmail != null && !authEmail.isBlank()) {
            Optional<Employee> empOpt = employeeService.getEmployeeByEmail(authEmail);
            List<Employee> list = empOpt.map(List::of).orElseGet(Collections::emptyList);

            Map<String, Object> response = new HashMap<>();
            response.put("employees", list);
            response.put("content", list);
            response.put("totalItems", list.size());
            response.put("totalPages", 1);
            response.put("currentPage", 0);

            return ResponseEntity.ok(ApiResponse.ok("Personal employee profile fetched.", response));
        }

        // 4. Unauthenticated Guest / Visitor: do not leak workforce directory
        Map<String, Object> response = new HashMap<>();
        response.put("employees", Collections.emptyList());
        response.put("content", Collections.emptyList());
        response.put("totalItems", 0);
        response.put("totalPages", 0);
        response.put("currentPage", 0);

        return ResponseEntity.ok(ApiResponse.ok("Workforce records are protected. Sign in required.", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Employee>> getMyEmployeeProfile(
            @RequestParam(required = false) String email,
            HttpServletRequest request) {

        String authEmail = getAuthenticatedEmail(request);
        if (authEmail == null && email != null && !email.isBlank()) {
            authEmail = email.trim().toLowerCase();
        }

        if (authEmail == null || authEmail.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required to view employee profile."));
        }

        final String targetEmail = authEmail;
        Optional<Employee> empOpt = employeeService.getEmployeeByEmail(targetEmail);
        if (empOpt.isPresent()) {
            return ResponseEntity.ok(ApiResponse.ok("Employee profile found.", empOpt.get()));
        }

        // If employee record doesn't exist yet, derive from User account
        return userRepository.findByEmailIgnoreCase(targetEmail)
                .map(user -> {
                    Employee emp = new Employee();
                    emp.setName(user.getFullName());
                    emp.setEmail(user.getEmail());
                    emp.setTitle("Staff");
                    emp.setStatus("Active");
                    emp.setEmployeeCode("EMP-" + String.format("%03d", user.getId() != null ? user.getId() : 1));
                    return ResponseEntity.ok(ApiResponse.ok("User profile found.", emp));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Employee record not found for " + targetEmail)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Employee>> getEmployeeById(@PathVariable Long id) {
        return employeeService.getEmployeeById(id)
            .map(emp -> ResponseEntity.ok(ApiResponse.ok("Employee found.", emp)))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Employee not found with ID: " + id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Employee>> createEmployee(
            @Valid @RequestBody EmployeeRequest req,
            HttpServletRequest request) {
        if (!hasCallerPermission(request, "EMPLOYEE_CREATE")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access Denied: You do not have permission to add new employees (EMPLOYEE_CREATE required)."));
        }
        Employee created = employeeService.createEmployee(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Employee '" + created.getName() + "' added successfully.", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Employee>> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequest req,
            HttpServletRequest request) {
        if (!hasCallerPermission(request, "EMPLOYEE_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access Denied: You do not have permission to edit employee records (EMPLOYEE_EDIT required)."));
        }
        Employee updated = employeeService.updateEmployee(id, req);
        return ResponseEntity.ok(ApiResponse.ok("Employee updated successfully.", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(
            @PathVariable Long id,
            HttpServletRequest request) {
        if (!hasCallerPermission(request, "EMPLOYEE_DELETE")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access Denied: You do not have permission to delete employee records (EMPLOYEE_DELETE required)."));
        }
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok(ApiResponse.ok("Employee removed successfully."));
    }

    private boolean hasCallerPermission(HttpServletRequest request, String permissionName) {
        String authEmail = getAuthenticatedEmail(request);
        if (authEmail == null || authEmail.isBlank()) {
            return false;
        }

        // 1. Direct check against active database role permissions
        Optional<User> uOpt = userRepository.findByEmailIgnoreCase(authEmail);
        if (uOpt.isPresent()) {
            User user = uOpt.get();
            if (user.getRoleEntity() != null && user.getRoleEntity().getPermissions() != null) {
                return user.getRoleEntity().getPermissions().stream()
                        .anyMatch(p -> permissionName.equalsIgnoreCase(p.getName()));
            }
        }

        // 2. Fallback to SecurityContext authorities
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getAuthorities().stream()
                    .anyMatch(a -> permissionName.equalsIgnoreCase(a.getAuthority()));
        }

        return false;
    }

    private String getAuthenticatedEmail(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("CURRENT_USER") != null) {
            User sessionUser = (User) session.getAttribute("CURRENT_USER");
            return sessionUser.getEmail();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return null;
    }

    private boolean isCurrentUserAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("CURRENT_USER") != null) {
            User sessionUser = (User) session.getAttribute("CURRENT_USER");
            return sessionUser.isAdmin();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()) || "ADMIN".equalsIgnoreCase(a.getAuthority()));
        }
        return false;
    }
}
