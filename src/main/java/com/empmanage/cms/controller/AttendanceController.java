package com.empmanage.cms.controller;

import com.empmanage.cms.dto.ApiResponse;
import com.empmanage.cms.dto.PunchRequest;
import com.empmanage.cms.entity.Attendance;
import com.empmanage.cms.entity.User;
import com.empmanage.cms.service.AttendanceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @PostMapping("/punch")
    public ResponseEntity<ApiResponse<Attendance>> punchClock(
            @RequestBody(required = false) PunchRequest req,
            HttpServletRequest request) {

        // Derive user identity securely from backend session (Requirement 7)
        String userEmail = getAuthenticatedEmail(request);
        if (userEmail == null) {
            if (req != null && req.getUserEmail() != null && !req.getUserEmail().isBlank()) {
                userEmail = req.getUserEmail().trim().toLowerCase();
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Authentication required to record attendance."));
            }
        }

        String mode = (req != null && req.getMode() != null) ? req.getMode() : "Office";
        String punchTime = (req != null && req.getPunchTime() != null && !req.getPunchTime().isBlank()) ? req.getPunchTime().trim() : null;
        Attendance record = attendanceService.punchToggle(userEmail, mode, punchTime);
        String action = record.isPunchedIn() ? "Clock In" : "Clock Out";
        String timeRecorded = record.isPunchedIn() ? record.getTimeIn() : record.getTimeOut();
        return ResponseEntity.ok(ApiResponse.ok(action + " recorded successfully at " + timeRecorded + "!", record));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<Attendance>>> getMyAttendance(
            @RequestParam(required = false) String email,
            HttpServletRequest request) {

        String userEmail = getAuthenticatedEmail(request);
        if (userEmail == null) {
            if (email != null && !email.isBlank()) {
                userEmail = email.trim().toLowerCase();
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Authentication required to access attendance history."));
            }
        }

        List<Attendance> logs = attendanceService.getMyAttendanceLogs(userEmail);
        return ResponseEntity.ok(ApiResponse.ok("Personal attendance logs fetched.", logs));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Attendance>>> getAllAttendance() {
        List<Attendance> logs = attendanceService.getAllAttendanceLogs();
        return ResponseEntity.ok(ApiResponse.ok("Company-wide attendance logs fetched.", logs));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Attendance>> overrideStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        Attendance updated = attendanceService.overrideStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Attendance status updated.", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAttendanceLog(@PathVariable Long id) {
        attendanceService.deleteAttendanceLog(id);
        return ResponseEntity.ok(ApiResponse.ok("Attendance record deleted successfully.", null));
    }

    @PostMapping("/{id}/admin-punch-out")
    public ResponseEntity<ApiResponse<Attendance>> adminPunchOut(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        String punchOutTime = body != null ? body.get("punchOutTime") : null;
        Attendance updated = attendanceService.adminPunchOut(id, punchOutTime);
        return ResponseEntity.ok(ApiResponse.ok(
            "Employee (" + updated.getUserName() + ") shift punched out successfully for " + updated.getDate() + " at " + updated.getTimeOut() + "!",
            updated
        ));
    }

    private String getAuthenticatedEmail(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("CURRENT_USER") != null) {
            User sessionUser = (User) session.getAttribute("CURRENT_USER");
            return sessionUser.getEmail();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return null;
    }
}
