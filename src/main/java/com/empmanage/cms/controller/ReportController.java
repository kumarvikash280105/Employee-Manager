package com.empmanage.cms.controller;

import com.empmanage.cms.entity.Attendance;
import com.empmanage.cms.entity.Employee;
import com.empmanage.cms.entity.User;
import com.empmanage.cms.repository.UserRepository;
import com.empmanage.cms.service.AttendanceService;
import com.empmanage.cms.service.EmployeeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/attendance/csv")
    public ResponseEntity<byte[]> exportAttendanceCsv(HttpServletRequest request) {
        if (!hasCallerPermission(request, "ATTENDANCE_EXPORT") && !hasCallerPermission(request, "REPORT_EXPORT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"success\":false,\"message\":\"Access Denied: You do not have permission to export attendance records (ATTENDANCE_EXPORT required).\"}".getBytes(StandardCharsets.UTF_8));
        }

        List<Attendance> logs = attendanceService.getAllAttendanceLogs();

        StringBuilder sb = new StringBuilder();
        sb.append("ID,Date,Employee Name,Work Email,Role,Department,Time In,Time Out,Work Hours,Status\n");

        for (Attendance a : logs) {
            sb.append(escapeCsvCell(a.getId())).append(",")
              .append(escapeCsvCell(a.getDate())).append(",")
              .append(escapeCsvCell(a.getUserName())).append(",")
              .append(escapeCsvCell(a.getUserEmail())).append(",")
              .append(escapeCsvCell(a.getUserRole())).append(",")
              .append(escapeCsvCell(a.getDepartment())).append(",")
              .append(escapeCsvCell(a.getTimeIn())).append(",")
              .append(escapeCsvCell(a.getTimeOut())).append(",")
              .append(escapeCsvCell(a.getHours())).append(",")
              .append(escapeCsvCell(a.getStatus())).append("\n");
        }

        byte[] bytes = createUtf8CsvBytes(sb.toString());
        String filename = "EmpManage_Attendance_Report_" + LocalDate.now() + ".csv";

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .body(bytes);
    }

    @GetMapping("/attendance/user/csv")
    public ResponseEntity<byte[]> exportUserAttendanceCsv(
            @org.springframework.web.bind.annotation.RequestParam String email,
            HttpServletRequest request) {

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String targetEmail = email.trim().toLowerCase();
        String callerEmail = getCallerEmail(request);
        boolean isSelf = callerEmail != null && callerEmail.equalsIgnoreCase(targetEmail);
        boolean hasAdminExport = hasCallerPermission(request, "ATTENDANCE_EXPORT") || hasCallerPermission(request, "REPORT_EXPORT");

        if (!isSelf && !hasAdminExport) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"success\":false,\"message\":\"Access Denied: You do not have permission to export another user's attendance records.\"}".getBytes(StandardCharsets.UTF_8));
        }

        List<Attendance> logs = attendanceService.getMyAttendanceLogs(targetEmail);

        StringBuilder sb = new StringBuilder();
        sb.append("ID,Date,Employee Name,Work Email,Role,Department,Time In,Time Out,Work Hours,Status\n");

        for (Attendance a : logs) {
            sb.append(escapeCsvCell(a.getId())).append(",")
              .append(escapeCsvCell(a.getDate())).append(",")
              .append(escapeCsvCell(a.getUserName())).append(",")
              .append(escapeCsvCell(a.getUserEmail())).append(",")
              .append(escapeCsvCell(a.getUserRole())).append(",")
              .append(escapeCsvCell(a.getDepartment())).append(",")
              .append(escapeCsvCell(a.getTimeIn())).append(",")
              .append(escapeCsvCell(a.getTimeOut())).append(",")
              .append(escapeCsvCell(a.getHours())).append(",")
              .append(escapeCsvCell(a.getStatus())).append("\n");
        }

        byte[] bytes = createUtf8CsvBytes(sb.toString());
        String safeEmail = targetEmail.replaceAll("[^a-zA-Z0-9.-]", "_");
        String filename = "Attendance_" + safeEmail + "_" + LocalDate.now() + ".csv";

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .body(bytes);
    }

    @GetMapping("/employees/csv")
    public ResponseEntity<byte[]> exportEmployeesCsv(HttpServletRequest request) {
        if (!hasCallerPermission(request, "EMPLOYEE_EXPORT") && !hasCallerPermission(request, "REPORT_EXPORT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"success\":false,\"message\":\"Access Denied: You do not have permission to export workforce records (EMPLOYEE_EXPORT required).\"}".getBytes(StandardCharsets.UTF_8));
        }

        List<Employee> employees = employeeService.getAllEmployees();

        StringBuilder sb = new StringBuilder();
        sb.append("ID,Code,Name,Email,Phone,Job Title,Department,Status\n");

        for (Employee e : employees) {
            sb.append(escapeCsvCell(e.getId())).append(",")
              .append(escapeCsvCell(e.getEmployeeCode())).append(",")
              .append(escapeCsvCell(e.getName())).append(",")
              .append(escapeCsvCell(e.getEmail())).append(",")
              .append(escapeCsvCell(e.getPhone())).append(",")
              .append(escapeCsvCell(e.getTitle())).append(",")
              .append(escapeCsvCell(e.getDepartmentName())).append(",")
              .append(escapeCsvCell(e.getStatus())).append("\n");
        }

        byte[] bytes = createUtf8CsvBytes(sb.toString());
        String filename = "EmpManage_Employees_Report_" + LocalDate.now() + ".csv";

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .body(bytes);
    }

    private String getCallerEmail(HttpServletRequest request) {
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

    private boolean hasCallerPermission(HttpServletRequest request, String permissionName) {
        String authEmail = null;
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("CURRENT_USER") != null) {
            User sessionUser = (User) session.getAttribute("CURRENT_USER");
            authEmail = sessionUser.getEmail();
        }
        if (authEmail == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                authEmail = auth.getName();
            }
        }
        if (authEmail != null) {
            Optional<User> uOpt = userRepository.findByEmailIgnoreCase(authEmail);
            if (uOpt.isPresent()) {
                User user = uOpt.get();
                if (user.getRoleEntity() != null && user.getRoleEntity().getPermissions() != null) {
                    return user.getRoleEntity().getPermissions().stream()
                            .anyMatch(p -> permissionName.equalsIgnoreCase(p.getName()));
                }
            }
        }
        return false;
    }

    /**
     * Prevents Spreadsheet Formula Injection (CSV Injection) attacks and formats cell content safely.
     * If a cell begins with '=', '+', '-', '@', '\t', or '\r', an apostrophe prefix is added to neutralize execution.
     */
    private String escapeCsvCell(Object value) {
        if (value == null) {
            return "\"\"";
        }
        String str = String.valueOf(value);
        if (!str.isEmpty()) {
            char firstChar = str.charAt(0);
            if (firstChar == '=' || firstChar == '+' || firstChar == '-' || firstChar == '@' || firstChar == '\t' || firstChar == '\r') {
                str = "'" + str;
            }
        }
        return "\"" + str.replace("\"", "\"\"") + "\"";
    }

    /**
     * Prepends the UTF-8 Byte Order Mark (BOM) so Excel and spreadsheet tools correctly decode UTF-8 characters.
     */
    private byte[] createUtf8CsvBytes(String csvContent) {
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] contentBytes = csvContent.getBytes(StandardCharsets.UTF_8);
        byte[] combined = new byte[bom.length + contentBytes.length];
        System.arraycopy(bom, 0, combined, 0, bom.length);
        System.arraycopy(contentBytes, 0, combined, bom.length, contentBytes.length);
        return combined;
    }
}
