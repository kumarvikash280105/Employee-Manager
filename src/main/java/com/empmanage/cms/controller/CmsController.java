package com.empmanage.cms.controller;

import com.empmanage.cms.dto.ApiResponse;
import com.empmanage.cms.dto.CmsUpdateRequest;
import com.empmanage.cms.entity.CmsSection;
import com.empmanage.cms.service.CmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cms")
@CrossOrigin(origins = "*")
public class CmsController {

    @Autowired
    private CmsService cmsService;

    @GetMapping("/sections")
    public ResponseEntity<ApiResponse<List<CmsSection>>> getAllSections() {
        return ResponseEntity.ok(ApiResponse.ok("CMS sections retrieved.", cmsService.getAllSections()));
    }

    @GetMapping("/sections/{key}")
    public ResponseEntity<ApiResponse<CmsSection>> getSection(@PathVariable String key) {
        return cmsService.getSection(key)
            .map(sec -> ResponseEntity.ok(ApiResponse.ok("Section found.", sec)))
            .orElseGet(() -> ResponseEntity.badRequest().body(ApiResponse.error("Section not found.")));
    }

    @PutMapping("/sections/{key}")
    public ResponseEntity<ApiResponse<CmsSection>> updateSection(
            @PathVariable String key,
            @RequestBody CmsUpdateRequest req) {
        try {
            CmsSection updated = cmsService.updateSection(key, req);
            return ResponseEntity.ok(ApiResponse.ok("CMS section '" + key + "' updated successfully.", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to update CMS section: " + e.getMessage()));
        }
    }
}
