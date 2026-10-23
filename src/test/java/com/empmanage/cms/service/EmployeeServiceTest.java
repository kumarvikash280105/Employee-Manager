package com.empmanage.cms.service;

import com.empmanage.cms.dto.EmployeeRequest;
import com.empmanage.cms.entity.Department;
import com.empmanage.cms.entity.Employee;
import com.empmanage.cms.repository.DepartmentRepository;
import com.empmanage.cms.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private com.empmanage.cms.repository.UserRepository userRepository;

    @Mock
    private AttendanceService attendanceService;

    @InjectMocks
    private EmployeeService employeeService;

    private Department dept;
    private EmployeeRequest empRequest;

    @BeforeEach
    void setUp() {
        dept = new Department("Engineering & Technology", "ENG", "Lead Architect", "Description", "code", 0);
        dept.setId(1L);

        empRequest = new EmployeeRequest();
        empRequest.setName("Alice Smith");
        empRequest.setEmail("alice.smith@empmanage.com");
        empRequest.setTitle("Software Engineer");
        empRequest.setDepartmentId(1L);
        empRequest.setStatus("Active");
    }

    @Test
    @DisplayName("Employee CRUD - Create employee with JPA Department relationship")
    void createEmployee_success() {
        when(employeeRepository.existsByEmailIgnoreCase("alice.smith@empmanage.com")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> {
            Employee e = inv.getArgument(0);
            e.setId(101L);
            return e;
        });

        Employee created = employeeService.createEmployee(empRequest);

        assertNotNull(created);
        assertEquals("Alice Smith", created.getName());
        assertEquals("alice.smith@empmanage.com", created.getEmail());
        assertEquals(dept, created.getDepartment());
        assertEquals("Engineering & Technology", created.getDepartmentName());
        verify(attendanceService, times(1)).ensureTodayAttendanceForEmployee(created);
    }

    @Test
    @DisplayName("Requirement 3 - Prevent duplicate employee emails")
    void createEmployee_duplicateEmail_throwsException() {
        when(employeeRepository.existsByEmailIgnoreCase("alice.smith@empmanage.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> employeeService.createEmployee(empRequest));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Requirement 3 - Pagination and filtering support")
    void getEmployeesPaged_success() {
        Employee emp = new Employee("Alice Smith", "alice.smith@empmanage.com", dept, "Software Engineer", "Active");
        Page<Employee> page = new PageImpl<>(List.of(emp));

        when(employeeRepository.findWithFilters(eq("Alice"), eq("Engineering & Technology"), eq("Active"), any(Pageable.class)))
                .thenReturn(page);

        Page<Employee> result = employeeService.getEmployeesPaged("Alice", "Engineering & Technology", "Active", 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Alice Smith", result.getContent().get(0).getName());
    }

    @Test
    @DisplayName("Employee CRUD - Delete employee updates department member count")
    void deleteEmployee_success() {
        Employee emp = new Employee("Bob Jones", "bob@empmanage.com", dept, "QA Engineer", "Active");
        emp.setId(55L);

        when(employeeRepository.findById(55L)).thenReturn(Optional.of(emp));
        when(employeeRepository.countByDepartment_Id(1L)).thenReturn(0L);

        assertDoesNotThrow(() -> employeeService.deleteEmployee(55L));
        verify(attendanceService, times(1)).deleteAttendanceByUserEmail("bob@empmanage.com");
        verify(employeeRepository, times(1)).delete(emp);
        verify(departmentRepository, times(1)).save(dept);
    }
}
