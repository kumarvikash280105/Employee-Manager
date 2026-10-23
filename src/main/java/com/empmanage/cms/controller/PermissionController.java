package com.empmanage.cms.controller;

import com.empmanage.cms.dto.ApiResponse;
import com.empmanage.cms.dto.PermissionDto;
import com.empmanage.cms.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PermissionDto>>> getAllPermissions() {
        return ResponseEntity.ok(ApiResponse.ok("Permissions retrieved successfully.", permissionService.getAllPermissionDtos()));
    }

    @GetMapping("/matrix")
    public ResponseEntity<ApiResponse<Map<String, List<PermissionDto>>>> getPermissionsMatrix() {
        return ResponseEntity.ok(ApiResponse.ok("Permission matrix retrieved successfully.", permissionService.getPermissionsGroupedByModule()));
    }
}
