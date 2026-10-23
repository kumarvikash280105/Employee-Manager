package com.empmanage.cms.controller;

import com.empmanage.cms.dto.ApiResponse;
import com.empmanage.cms.entity.ContactInquiry;
import com.empmanage.cms.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
@CrossOrigin(origins = "*")
public class ContactController {

    @Autowired
    private ContactService contactService;

    @PostMapping
    public ResponseEntity<ApiResponse<ContactInquiry>> submitInquiry(@RequestBody ContactInquiry inquiry) {
        try {
            ContactInquiry saved = contactService.submitInquiry(inquiry);
            return ResponseEntity.ok(ApiResponse.ok("Thank you! Your message has been received.", saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ContactInquiry>>> getAllInquiries() {
        return ResponseEntity.ok(ApiResponse.ok("Contact inquiries fetched.", contactService.getAllInquiries()));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ContactInquiry>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        try {
            ContactInquiry updated = contactService.updateStatus(id, status);
            return ResponseEntity.ok(ApiResponse.ok("Inquiry status updated to " + status + ".", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInquiry(@PathVariable Long id) {
        contactService.deleteInquiry(id);
        return ResponseEntity.ok(ApiResponse.ok("Inquiry deleted."));
    }
}
