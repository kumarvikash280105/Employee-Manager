package com.empmanage.cms.controller;

import com.empmanage.cms.dto.ApiResponse;
import com.empmanage.cms.dto.DepartmentRequest;
import com.empmanage.cms.entity.Department;
import com.empmanage.cms.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Department>>> getAllDepartments() {
        List<Department> list = departmentService.getAllDepartments();
        return ResponseEntity.ok(ApiResponse.ok("Departments fetched successfully.", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Department>> getDepartmentById(@PathVariable Long id) {
        return departmentService.getDepartmentById(id)
            .map(dept -> ResponseEntity.ok(ApiResponse.ok("Department found.", dept)))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Department not found with ID: " + id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Department>> createDepartment(@Valid @RequestBody DepartmentRequest req) {
        Department created = departmentService.createDepartment(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Department '" + created.getName() + "' created successfully.", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Department>> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentRequest req) {
        Department updated = departmentService.updateDepartment(id, req);
        return ResponseEntity.ok(ApiResponse.ok("Department updated successfully.", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok(ApiResponse.ok("Department deleted successfully."));
    }
}
