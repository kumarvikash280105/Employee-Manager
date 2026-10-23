package com.empmanage.cms.controller;

import com.empmanage.cms.dto.*;
import com.empmanage.cms.entity.RoleAuditLog;
import com.empmanage.cms.entity.User;
import com.empmanage.cms.service.RoleAuditLogService;
import com.empmanage.cms.service.RoleService;
import com.empmanage.cms.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @Autowired
    private RoleAuditLogService auditLogService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleDto>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.ok("Roles retrieved successfully.", roleService.getAllRoles()));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<RoleStatsDto>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok("Role statistics loaded.", roleService.getDashboardStats()));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<List<RoleAuditLog>>> getAuditLogs() {
        return ResponseEntity.ok(ApiResponse.ok("Audit logs retrieved successfully.", auditLogService.getRecentLogs()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleDto>> getRoleById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Role retrieved.", roleService.getRoleById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoleDto>> createRole(@Valid @RequestBody RoleRequest req) {
        RoleDto created = roleService.createRole(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Role '" + created.getDisplayName() + "' created successfully.", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleDto>> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleRequest req) {
        RoleDto updated = roleService.updateRole(id, req);
        return ResponseEntity.ok(ApiResponse.ok("Role '" + updated.getDisplayName() + "' updated successfully.", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.ok("Role deleted successfully."));
    }

    @GetMapping("/{id}/permissions")
    public ResponseEntity<ApiResponse<List<String>>> getRolePermissions(@PathVariable Long id) {
        RoleDto role = roleService.getRoleById(id);
        return ResponseEntity.ok(ApiResponse.ok("Permissions for role " + role.getDisplayName(), role.getPermissions()));
    }

    @PutMapping("/{id}/permissions")
    public ResponseEntity<ApiResponse<RoleDto>> updateRolePermissions(
            @PathVariable Long id,
            @RequestBody Map<String, List<String>> body,
            HttpServletRequest request) {
        List<String> perms = body.getOrDefault("permissions", List.of());
        RoleDto updated = roleService.updateRolePermissions(id, perms);

        // Permission-change session refresh: if the current admin updated their own role's permissions
        refreshCurrentSessionAuth(request);

        return ResponseEntity.ok(ApiResponse.ok("Permissions updated successfully for " + updated.getDisplayName(), updated));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<RoleDto>> toggleRoleStatus(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Boolean> body) {
        Boolean active = (body != null) ? body.get("active") : null;
        RoleDto updated = roleService.toggleRoleStatus(id, active);
        String stateMsg = updated.isActive() ? "activated" : "deactivated";
        return ResponseEntity.ok(ApiResponse.ok("Role '" + updated.getDisplayName() + "' has been " + stateMsg + " successfully.", updated));
    }

    @PostMapping("/assign-user")
    public ResponseEntity<ApiResponse<User>> assignRoleToUser(
            @Valid @RequestBody AssignRoleRequest req,
            HttpServletRequest request) {
        User updatedUser = roleService.assignRoleToUser(req.getUserEmail(), req.getRoleName());
        updatedUser.setPassword(null);

        // If the current logged-in user changed their own role, refresh security context & session
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth != null && currentAuth.getName().equalsIgnoreCase(updatedUser.getEmail())) {
            refreshCurrentSessionAuth(request);
        }

        return ResponseEntity.ok(ApiResponse.ok("Role assigned successfully to " + updatedUser.getFullName() + ".", updatedUser));
    }

    private void refreshCurrentSessionAuth(HttpServletRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                userService.findByEmail(auth.getName()).ifPresent(user -> {
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    // Add primary role
                    authorities.add(new SimpleGrantedAuthority(user.getRole()));
                    if (user.isAdmin() && !user.getRole().equals("ROLE_ADMIN")) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    }
                    // Add all permissions granted to this role
                    if (user.getRoleEntity() != null && user.getRoleEntity().getPermissions() != null) {
                        user.getRoleEntity().getPermissions().forEach(p -> 
                            authorities.add(new SimpleGrantedAuthority(p.getName()))
                        );
                    }
                    Authentication newAuth = new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(newAuth);
                    SecurityContextHolder.setContext(context);

                    HttpSession session = request.getSession(false);
                    if (session != null) {
                        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
                        session.setAttribute("CURRENT_USER", user);
                    }
                });
            }
        } catch (Exception e) {
            // Ignore session refresh errors
        }
    }
}
