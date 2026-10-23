package com.empmanage.cms.service;

import com.empmanage.cms.dto.RoleDto;
import com.empmanage.cms.dto.RoleRequest;
import com.empmanage.cms.dto.RoleStatsDto;
import com.empmanage.cms.entity.Permission;
import com.empmanage.cms.entity.Role;
import com.empmanage.cms.entity.User;
import com.empmanage.cms.repository.PermissionRepository;
import com.empmanage.cms.repository.RoleRepository;
import com.empmanage.cms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleAuditLogService auditLogService;

    private static final List<String> ROLE_PRIORITY_ORDER = List.of(
        "ROLE_ADMIN",
        "ROLE_HR_MANAGER",
        "ROLE_MANAGER",
        "ROLE_USER",
        "ROLE_VIEWER"
    );

    private int getRolePriority(String roleName) {
        if (roleName == null) return 999;
        int idx = ROLE_PRIORITY_ORDER.indexOf(roleName.toUpperCase());
        return idx >= 0 ? idx : 100;
    }

    public List<RoleDto> getAllRoles() {
        return roleRepository.findAllByOrderByCreatedAtAsc().stream()
                .sorted(Comparator.comparingInt((Role r) -> getRolePriority(r.getName()))
                        .thenComparing(Role::getDisplayName))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public RoleDto getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + id));
        return toDto(role);
    }

    public RoleDto getRoleByName(String name) {
        Role role = roleRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with name: " + name));
        return toDto(role);
    }

    @Transactional
    public RoleDto createRole(RoleRequest req) {
        String rawName = req.getName();
        if (rawName == null || rawName.isBlank()) {
            rawName = req.getDisplayName().trim().toUpperCase().replace(" ", "_");
        }
        String cleanName = User.normalizeRole(rawName);

        if (roleRepository.existsByNameIgnoreCase(cleanName)) {
            throw new IllegalArgumentException("A role with identifier '" + cleanName + "' already exists.");
        }

        Role role = new Role(cleanName, req.getDisplayName().trim(), req.getDescription(), false);
        if (req.getActive() != null) {
            role.setActive(req.getActive());
        }

        if (req.getPermissions() != null && !req.getPermissions().isEmpty()) {
            List<Permission> perms = permissionRepository.findByNameIn(req.getPermissions());
            role.setPermissions(new HashSet<>(perms));
        }

        Role saved = roleRepository.save(role);
        auditLogService.log("ROLE_CREATED", saved.getName(), null, 
                "Created new role '" + saved.getDisplayName() + "' (" + saved.getName() + ") with " + saved.getPermissions().size() + " permissions");
        return toDto(saved);
    }

    @Transactional
    public RoleDto updateRole(Long id, RoleRequest req) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + id));

        if (req.getDisplayName() != null && !req.getDisplayName().isBlank()) {
            role.setDisplayName(req.getDisplayName().trim());
        }
        if (req.getDescription() != null) {
            role.setDescription(req.getDescription().trim());
        }
        if (req.getActive() != null) {
            if (role.isSystemRole() && "ROLE_ADMIN".equalsIgnoreCase(role.getName()) && !req.getActive()) {
                throw new IllegalArgumentException("The master Administrator system role cannot be deactivated.");
            }
            role.setActive(req.getActive());
        }
        if (req.getPermissions() != null) {
            List<Permission> perms = permissionRepository.findByNameIn(req.getPermissions());
            role.setPermissions(new HashSet<>(perms));
        }

        Role updated = roleRepository.save(role);
        auditLogService.log("ROLE_UPDATED", updated.getName(), null, 
                "Updated role '" + updated.getDisplayName() + "'");
        return toDto(updated);
    }

    @Transactional
    public RoleDto updateRolePermissions(Long id, List<String> permissionNames) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + id));

        if (permissionNames == null) {
            permissionNames = Collections.emptyList();
        }

        List<Permission> perms = permissionRepository.findByNameIn(permissionNames);
        role.setPermissions(new HashSet<>(perms));

        Role updated = roleRepository.save(role);
        auditLogService.log("PERMISSIONS_UPDATED", updated.getName(), null, 
                "Updated permissions for role '" + updated.getDisplayName() + "': " + perms.size() + " permissions assigned");
        return toDto(updated);
    }

    @Transactional
    public RoleDto toggleRoleStatus(Long id, Boolean active) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + id));

        boolean targetStatus = (active != null) ? active : !role.isActive();

        if (!targetStatus && role.isSystemRole() && "ROLE_ADMIN".equalsIgnoreCase(role.getName())) {
            throw new IllegalArgumentException("The master Administrator system role cannot be deactivated.");
        }

        role.setActive(targetStatus);
        Role updated = roleRepository.save(role);
        auditLogService.log(targetStatus ? "ROLE_ACTIVATED" : "ROLE_DEACTIVATED", 
                updated.getName(), null, 
                "Role '" + updated.getDisplayName() + "' " + (targetStatus ? "activated" : "deactivated"));
        return toDto(updated);
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + id));

        if (role.isSystemRole()) {
            throw new IllegalArgumentException("System-defined role '" + role.getDisplayName() + "' cannot be deleted.");
        }

        long assignedCount = userRepository.countByRoleEntity_Id(role.getId()) + userRepository.countByRole(role.getName());
        if (assignedCount > 0) {
            throw new IllegalStateException("Cannot delete role '" + role.getDisplayName() + "' because " + assignedCount + " user(s) are currently assigned to it. Reassign users first.");
        }

        String roleName = role.getName();
        String roleDisplayName = role.getDisplayName();
        roleRepository.delete(role);
        auditLogService.log("ROLE_DELETED", roleName, null, "Deleted custom role '" + roleDisplayName + "'");
    }

    @Transactional
    public User assignRoleToUser(String email, String roleName) {
        User user = userRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        String cleanRoleName = User.normalizeRole(roleName);
        Role role = roleRepository.findByNameIgnoreCase(cleanRoleName)
                .orElseThrow(() -> new IllegalArgumentException("Role '" + cleanRoleName + "' does not exist."));

        if (!role.isActive()) {
            throw new IllegalArgumentException("Cannot assign inactive role '" + role.getDisplayName() + "'. Please activate it first.");
        }

        // Protection: Master administrator account protection
        if ("admin@empmanage.com".equalsIgnoreCase(user.getEmail()) && !"ROLE_ADMIN".equalsIgnoreCase(cleanRoleName)) {
            throw new IllegalArgumentException("Protected master administrator account cannot be demoted.");
        }

        // Protection: Last active administrator protection
        if (user.isAdmin() && !"ROLE_ADMIN".equalsIgnoreCase(cleanRoleName)) {
            long adminCount = userRepository.findAll().stream()
                    .filter(User::isAdmin)
                    .count();
            if (adminCount <= 1) {
                throw new IllegalArgumentException("Cannot change role. At least one Administrator must remain in the system.");
            }
        }

        String oldRole = user.getRole();
        user.setRoleEntity(role);
        user.setRole(role.getName());
        User savedUser = userRepository.save(user);

        auditLogService.log("USER_ROLE_ASSIGNED", role.getName(), user.getEmail(), 
                "Assigned role '" + role.getDisplayName() + "' to user " + user.getEmail() + " (Previous role: " + oldRole + ")");

        return savedUser;
    }

    public RoleStatsDto getDashboardStats() {
        long totalRoles = roleRepository.count();
        long totalPermissions = permissionRepository.count();
        long assignedUsers = userRepository.count();
        // Modules that require elevated permissions (e.g. CMS, Users, Roles, Reports, Admin Attendance)
        long restrictedModules = 5;
        return new RoleStatsDto(totalRoles, totalPermissions, assignedUsers, restrictedModules);
    }

    private RoleDto toDto(Role role) {
        long count = userRepository.countByRoleEntity_Id(role.getId()) 
                   + userRepository.countByRole(role.getName());
        List<String> perms = role.getPermissions().stream()
                .map(Permission::getName)
                .sorted()
                .collect(Collectors.toList());
        return new RoleDto(
                role.getId(),
                role.getName(),
                role.getDisplayName(),
                role.getDescription(),
                role.isSystemRole(),
                role.isActive(),
                count,
                perms
        );
    }
}
