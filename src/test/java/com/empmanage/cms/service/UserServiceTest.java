package com.empmanage.cms.service;

import com.empmanage.cms.dto.LoginRequest;
import com.empmanage.cms.dto.RegisterRequest;
import com.empmanage.cms.entity.User;
import com.empmanage.cms.repository.DepartmentRepository;
import com.empmanage.cms.repository.EmployeeRepository;
import com.empmanage.cms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AttendanceService attendanceService;

    @InjectMocks
    private UserService userService;

    private RegisterRequest regRequest;

    @BeforeEach
    void setUp() {
        regRequest = new RegisterRequest();
        regRequest.setFullName("John Doe");
        regRequest.setEmail("john.doe@example.com");
        regRequest.setPassword("securePassword123");
        regRequest.setDepartment("Engineering & Technology");
    }

    @Test
    @DisplayName("Registration - Should register employee successfully with BCrypt hashed password and create employee directory entry")
    void register_success() {
        when(userRepository.existsByEmailIgnoreCase("john.doe@example.com")).thenReturn(false);
        when(passwordEncoder.encode("securePassword123")).thenReturn("$2a$10$encodedHashString");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(employeeRepository.existsByEmailIgnoreCase("john.doe@example.com")).thenReturn(false);

        User registered = userService.register(regRequest);

        assertNotNull(registered);
        assertEquals("John Doe", registered.getFullName());
        assertEquals("john.doe@example.com", registered.getEmail());
        assertEquals("$2a$10$encodedHashString", registered.getPassword());
        assertEquals("ROLE_USER", registered.getRole());
        verify(attendanceService, times(1)).ensureTodayAttendanceForUser(registered);
        verify(employeeRepository, times(1)).save(any(com.empmanage.cms.entity.Employee.class));
    }

    @Test
    @DisplayName("Security Requirement 1 - Public self-registration with ADMIN role must be rejected")
    void register_preventAdminSelfRegistration() {
        regRequest.setRole("ADMIN");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.register(regRequest));
        assertTrue(ex.getMessage().contains("Administrator role is restricted"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registration - Should throw exception when email already exists")
    void register_duplicateEmail_throwsException() {
        when(userRepository.existsByEmailIgnoreCase("john.doe@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.register(regRequest));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Authentication - Should authenticate user with matching BCrypt password")
    void login_success() {
        User user = new User("Jane Admin", "admin@empmanage.com", "$2a$10$encodedAdminPass", "ROLE_ADMIN", "Executive");
        when(userRepository.findByEmailIgnoreCase("admin@empmanage.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("admin123", "$2a$10$encodedAdminPass")).thenReturn(true);

        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("admin@empmanage.com");
        loginReq.setPassword("admin123");
        loginReq.setRole("ADMIN");

        User loggedIn = userService.login(loginReq);

        assertNotNull(loggedIn);
        assertEquals("admin@empmanage.com", loggedIn.getEmail());
        assertTrue(loggedIn.isAdmin());
        verify(attendanceService, times(1)).ensureTodayAttendanceForUser(loggedIn);
    }

    @Test
    @DisplayName("Authentication - Support shorthand login 'admin' alias")
    void login_adminAlias_success() {
        User user = new User("System Administrator", "admin@empmanage.com", "$2a$10$encodedAdminPass", "ROLE_ADMIN", "Executive");
        when(userRepository.findByEmailIgnoreCase("admin@empmanage.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("admin123", "$2a$10$encodedAdminPass")).thenReturn(true);

        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("admin"); // shorthand
        loginReq.setPassword("admin123");

        User loggedIn = userService.login(loginReq);
        assertEquals("admin@empmanage.com", loggedIn.getEmail());
    }

    @Test
    @DisplayName("Security Requirement 1 - Reject login when BCrypt password does not match")
    void login_wrongPassword_throwsException() {
        User user = new User("Demo User", "employee@empmanage.com", "$2a$10$correctPassHash", "ROLE_USER", "Engineering");
        when(userRepository.findByEmailIgnoreCase("employee@empmanage.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "$2a$10$correctPassHash")).thenReturn(false);

        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("employee@empmanage.com");
        loginReq.setPassword("wrongPassword");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.login(loginReq));
        assertEquals("Invalid email or password.", ex.getMessage());
    }

    @Test
    @DisplayName("Security Requirement 1 - Reject login on role mismatch")
    void login_roleMismatch_throwsException() {
        User user = new User("Demo Employee", "employee@empmanage.com", "$2a$10$empPassHash", "ROLE_USER", "Engineering");
        when(userRepository.findByEmailIgnoreCase("employee@empmanage.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("emp123", "$2a$10$empPassHash")).thenReturn(true);

        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("employee@empmanage.com");
        loginReq.setPassword("emp123");
        loginReq.setRole("ADMIN"); // Requesting admin login with an employee account

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.login(loginReq));
        assertTrue(ex.getMessage().contains("Role mismatch"));
    }

    @Test
    @DisplayName("User Account Deletion - Should cascade delete user attendance records upon deletion")
    void deleteUserByEmail_cascadeDeletesAttendance() {
        User user = new User("Test User", "test@empmanage.com", "hash", "ROLE_USER", "Engineering");
        when(userRepository.findByEmailIgnoreCase("test@empmanage.com")).thenReturn(Optional.of(user));

        userService.deleteUserByEmail("test@empmanage.com");

        verify(attendanceService, times(1)).deleteAttendanceByUserEmail("test@empmanage.com");
        verify(userRepository, times(1)).delete(user);
    }

    @Test
    @DisplayName("User Management - Should update user role and synchronize with department")
    void updateUser_success() {
        User user = new User("Rahul Sharma", "rahul@company.com", "hash", "ROLE_USER", "Engineering");
        when(userRepository.findByEmailIgnoreCase("rahul@company.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.updateUser("rahul@company.com", "ROLE_ADMIN", "Human Resources (HR)", "HR Specialist");

        assertNotNull(updated);
        assertEquals("ROLE_ADMIN", updated.getRole());
        assertEquals("Human Resources (HR)", updated.getDepartment());
        verify(userRepository, times(1)).save(user);
    }
}
