package com.empmanage.cms.service;

import com.empmanage.cms.dto.RoleDto;
import com.empmanage.cms.dto.RoleRequest;
import com.empmanage.cms.entity.Permission;
import com.empmanage.cms.entity.Role;
import com.empmanage.cms.entity.User;
import com.empmanage.cms.repository.PermissionRepository;
import com.empmanage.cms.repository.RoleRepository;
import com.empmanage.cms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleAuditLogService auditLogService;

    @InjectMocks
    private RoleService roleService;

    private Role adminRole;
    private Role customRole;
    private Permission employeeViewPerm;

    @BeforeEach
    void setUp() {
        employeeViewPerm = new Permission("EMPLOYEE_VIEW", "EMPLOYEES", "VIEW", "View workforce directory");

        adminRole = new Role("ROLE_ADMIN", "Administrator", "Full system access", true);
        adminRole.setId(1L);
        adminRole.addPermission(employeeViewPerm);

        customRole = new Role("ROLE_TEST_LEAD", "Test Lead", "Test team supervisor", false);
        customRole.setId(2L);
        customRole.addPermission(employeeViewPerm);
    }

    @Test
    void testGetAllRoles() {
        when(roleRepository.findAllByOrderByCreatedAtAsc()).thenReturn(List.of(adminRole, customRole));
        when(userRepository.countByRoleEntity_Id(any())).thenReturn(1L);
        when(userRepository.countByRole(any())).thenReturn(0L);

        List<RoleDto> result = roleService.getAllRoles();
        assertEquals(2, result.size());
        assertEquals("ROLE_ADMIN", result.get(0).getName());
        assertEquals("ROLE_TEST_LEAD", result.get(1).getName());
    }

    @Test
    void testCreateRoleSuccess() {
        RoleRequest req = new RoleRequest();
        req.setName("ROLE_FINANCE_HEAD");
        req.setDisplayName("Finance Head");
        req.setDescription("Manages accounting");
        req.setPermissions(List.of("EMPLOYEE_VIEW"));

        when(roleRepository.existsByNameIgnoreCase("ROLE_FINANCE_HEAD")).thenReturn(false);
        when(permissionRepository.findByNameIn(List.of("EMPLOYEE_VIEW"))).thenReturn(List.of(employeeViewPerm));
        when(roleRepository.save(any(Role.class))).thenAnswer(i -> {
            Role r = i.getArgument(0);
            r.setId(3L);
            return r;
        });

        RoleDto created = roleService.createRole(req);
        assertNotNull(created);
        assertEquals("ROLE_FINANCE_HEAD", created.getName());
        assertEquals("Finance Head", created.getDisplayName());
        verify(auditLogService, times(1)).log(eq("ROLE_CREATED"), any(), any(), any());
    }

    @Test
    void testCreateRoleDuplicateRejection() {
        RoleRequest req = new RoleRequest();
        req.setName("ROLE_ADMIN");
        req.setDisplayName("Admin Clone");

        when(roleRepository.existsByNameIgnoreCase("ROLE_ADMIN")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> roleService.createRole(req));
    }

    @Test
    void testDeleteSystemRoleRejection() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));

        assertThrows(IllegalArgumentException.class, () -> roleService.deleteRole(1L));
        verify(roleRepository, never()).delete(any());
    }

    @Test
    void testDeleteRoleWithAssignedUsersRejection() {
        when(roleRepository.findById(2L)).thenReturn(Optional.of(customRole));
        when(userRepository.countByRoleEntity_Id(2L)).thenReturn(2L);

        assertThrows(IllegalStateException.class, () -> roleService.deleteRole(2L));
        verify(roleRepository, never()).delete(any());
    }

    @Test
    void testDeleteCustomRoleSuccess() {
        when(roleRepository.findById(2L)).thenReturn(Optional.of(customRole));
        when(userRepository.countByRoleEntity_Id(2L)).thenReturn(0L);
        when(userRepository.countByRole("ROLE_TEST_LEAD")).thenReturn(0L);

        roleService.deleteRole(2L);
        verify(roleRepository, times(1)).delete(customRole);
        verify(auditLogService, times(1)).log(eq("ROLE_DELETED"), eq("ROLE_TEST_LEAD"), any(), any());
    }

    @Test
    void testDeactivateMasterAdminRejection() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));

        assertThrows(IllegalArgumentException.class, () -> roleService.toggleRoleStatus(1L, false));
    }

    @Test
    void testAssignRoleToUserSuccess() {
        User user = new User("John Doe", "john@company.com", "hash", "ROLE_USER", "Engineering");
        user.setId(10L);

        when(userRepository.findByEmailIgnoreCase("john@company.com")).thenReturn(Optional.of(user));
        when(roleRepository.findByNameIgnoreCase("ROLE_TEST_LEAD")).thenReturn(Optional.of(customRole));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User updated = roleService.assignRoleToUser("john@company.com", "ROLE_TEST_LEAD");
        assertEquals("ROLE_TEST_LEAD", updated.getRole());
        assertEquals(customRole, updated.getRoleEntity());
        verify(auditLogService, times(1)).log(eq("USER_ROLE_ASSIGNED"), eq("ROLE_TEST_LEAD"), eq("john@company.com"), any());
    }

    @Test
    void testDemoteLastAdministratorRejection() {
        User adminUser = new User("Admin", "admin2@company.com", "hash", "ROLE_ADMIN", "IT");
        adminUser.setId(20L);

        when(userRepository.findByEmailIgnoreCase("admin2@company.com")).thenReturn(Optional.of(adminUser));
        when(roleRepository.findByNameIgnoreCase("ROLE_TEST_LEAD")).thenReturn(Optional.of(customRole));
        when(userRepository.findAll()).thenReturn(List.of(adminUser)); // Only 1 admin in the system

        assertThrows(IllegalArgumentException.class, () -> roleService.assignRoleToUser("admin2@company.com", "ROLE_TEST_LEAD"));
    }
}
