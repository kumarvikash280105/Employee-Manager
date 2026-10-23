package com.empmanage.cms.service;

import com.empmanage.cms.dto.DepartmentRequest;
import com.empmanage.cms.entity.Department;
import com.empmanage.cms.repository.DepartmentRepository;
import com.empmanage.cms.repository.EmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private DepartmentService departmentService;

    @Test
    @DisplayName("Department CRUD - Should create department successfully")
    void createDepartment_success() {
        DepartmentRequest req = new DepartmentRequest();
        req.setName("Cloud Infrastructure");
        req.setCode("CLD");
        req.setHeadName("DevOps Lead");
        req.setDescription("Handles cloud deployments");

        when(departmentRepository.existsByNameIgnoreCase("Cloud Infrastructure")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

        Department saved = departmentService.createDepartment(req);

        assertNotNull(saved);
        assertEquals("Cloud Infrastructure", saved.getName());
        assertEquals("CLD", saved.getCode());
        verify(departmentRepository, times(1)).save(any(Department.class));
    }

    @Test
    @DisplayName("Department CRUD - Should block creation with duplicate department name")
    void createDepartment_duplicate_throwsException() {
        DepartmentRequest req = new DepartmentRequest();
        req.setName("Engineering & Technology");

        when(departmentRepository.existsByNameIgnoreCase("Engineering & Technology")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> departmentService.createDepartment(req));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(departmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Requirement 4 - Prevent deletion of departments that are still assigned to employees")
    void deleteDepartment_withAssignedEmployees_throwsIllegalStateException() {
        Department dept = new Department("Engineering & Technology", "ENG", "Lead Architect", "Description", "code", 5);
        dept.setId(10L);

        when(departmentRepository.findById(10L)).thenReturn(Optional.of(dept));
        when(employeeRepository.countByDepartment_Id(10L)).thenReturn(4L); // 4 employees assigned

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> departmentService.deleteDepartment(10L));
        assertTrue(ex.getMessage().contains("Cannot delete department"));
        assertTrue(ex.getMessage().contains("4 employee(s) are currently assigned"));
        verify(departmentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Department CRUD - Should delete department when 0 employees are assigned")
    void deleteDepartment_withoutAssignedEmployees_success() {
        Department dept = new Department("Temporary Department", "TMP", "Unassigned", "Description", "folder", 0);
        dept.setId(20L);

        when(departmentRepository.findById(20L)).thenReturn(Optional.of(dept));
        when(employeeRepository.countByDepartment_Id(20L)).thenReturn(0L); // 0 employees

        assertDoesNotThrow(() -> departmentService.deleteDepartment(20L));
        verify(departmentRepository, times(1)).delete(dept);
    }

    @Test
    @DisplayName("Requirement 4 - Dynamically compute real-time employee counts by department")
    void getAllDepartments_dynamicEmployeeCount() {
        Department dept1 = new Department("Engineering", "ENG", "Head 1", "Desc 1", "code", 0);
        dept1.setId(1L);
        Department dept2 = new Department("Marketing", "MKT", "Head 2", "Desc 2", "activity", 0);
        dept2.setId(2L);

        when(departmentRepository.findAll()).thenReturn(List.of(dept1, dept2));
        when(employeeRepository.countByDepartment_Id(1L)).thenReturn(8L);
        when(employeeRepository.countByDepartment_Id(2L)).thenReturn(3L);

        List<Department> departments = departmentService.getAllDepartments();

        assertEquals(2, departments.size());
        assertEquals(8, departments.get(0).getTotalMembers());
        assertEquals(3, departments.get(1).getTotalMembers());
    }
}
