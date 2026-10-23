package com.empmanage.cms.service;

import com.empmanage.cms.entity.Attendance;
import com.empmanage.cms.entity.Employee;
import com.empmanage.cms.entity.User;
import com.empmanage.cms.repository.AttendanceRepository;
import com.empmanage.cms.repository.EmployeeRepository;
import com.empmanage.cms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm:ss a");

    public Attendance ensureTodayAttendanceForUser(User user) {
        if (user == null || user.getEmail() == null) return null;
        String dateKey = LocalDate.now().toString(); // "2026-10-14"
        String email = user.getEmail().trim().toLowerCase();

        Optional<Attendance> existing = attendanceRepository.findByUserEmailIgnoreCaseAndDateKey(email, dateKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        String formattedDate = LocalDate.now().format(DATE_FORMATTER);
        Attendance att = new Attendance(
            email,
            user.getFullName() != null ? user.getFullName() : "User",
            user.getRole() != null ? user.getRole() : "EMPLOYEE",
            user.getDepartment() != null ? user.getDepartment() : "General",
            formattedDate,
            dateKey
        );
        return attendanceRepository.save(att);
    }

    public Attendance ensureTodayAttendanceForEmployee(Employee emp) {
        if (emp == null || emp.getEmail() == null) return null;
        String dateKey = LocalDate.now().toString();
        String email = emp.getEmail().trim().toLowerCase();

        Optional<Attendance> existing = attendanceRepository.findByUserEmailIgnoreCaseAndDateKey(email, dateKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        String formattedDate = LocalDate.now().format(DATE_FORMATTER);
        Attendance att = new Attendance(
            email,
            emp.getName() != null ? emp.getName() : "Employee",
            "EMPLOYEE",
            emp.getDepartmentName() != null ? emp.getDepartmentName() : "General",
            formattedDate,
            dateKey
        );
        return attendanceRepository.save(att);
    }

    public Attendance punchToggle(String userEmail, String mode) {
        return punchToggle(userEmail, mode, null);
    }

    public Attendance punchToggle(String userEmail, String mode, String customTime) {
        String dateKey = LocalDate.now().toString();
        String email = userEmail.trim().toLowerCase();

        Attendance att = attendanceRepository.findByUserEmailIgnoreCaseAndDateKey(email, dateKey)
            .orElseGet(() -> {
                String formattedDate = LocalDate.now().format(DATE_FORMATTER);
                Attendance newAtt = new Attendance(email, "Employee", "EMPLOYEE", "General", formattedDate, dateKey);
                return attendanceRepository.save(newAtt);
            });

        // Capture exact local punch time
        String currentTimeStr = (customTime != null && !customTime.isBlank())
            ? customTime.trim()
            : LocalTime.now().format(TIME_FORMATTER);

        String selectedMode = (mode != null && !mode.isBlank()) ? mode : "Office";

        if (!att.isPunchedIn()) {
            // Punch In (Clock In)
            att.setPunchedIn(true);
            att.setTimeIn(currentTimeStr);
            att.setPunchInTimestamp(System.currentTimeMillis());
            att.setMode(selectedMode);
            att.setTimeOut("Ongoing Shift");
            att.setHours("In Progress");

            if ("Leave".equalsIgnoreCase(selectedMode)) {
                att.setStatus("On Leave");
            } else if ("WFH".equalsIgnoreCase(selectedMode)) {
                att.setStatus("WFH");
            } else {
                att.setStatus("Present");
            }
        } else {
            // Punch Out (Clock Out)
            att.setPunchedIn(false);
            att.setTimeOut(currentTimeStr);

            long startTs = att.getPunchInTimestamp() != null ? att.getPunchInTimestamp() : (System.currentTimeMillis() - 8 * 3600 * 1000);
            long elapsedSecs = Math.max(1, (System.currentTimeMillis() - startTs) / 1000);
            long hrs = elapsedSecs / 3600;
            long mins = (elapsedSecs % 3600) / 60;
            long secs = elapsedSecs % 60;

            String hoursStr;
            if (hrs > 0) {
                hoursStr = hrs + " hr " + mins + " min";
            } else if (mins > 0) {
                hoursStr = mins + " min " + secs + " sec";
            } else {
                hoursStr = secs + " sec";
            }
            att.setHours(hoursStr);
        }

        return attendanceRepository.save(att);
    }

    public List<Attendance> getMyAttendanceLogs(String userEmail) {
        return attendanceRepository.findByUserEmailIgnoreCaseOrderByTimestampDesc(userEmail.trim().toLowerCase());
    }

    public List<Attendance> getAllAttendanceLogs() {
        Set<String> validEmails = getValidUserEmails();
        List<Attendance> all = attendanceRepository.findAllByOrderByTimestampDesc().stream()
            .filter(a -> a.getUserEmail() != null && validEmails.contains(a.getUserEmail().trim().toLowerCase()))
            .toList();

        // Department-level access control for Managers
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            boolean isFullAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean isManager = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
            if (isManager && !isFullAdmin) {
                Optional<User> managerOpt = userRepository.findByEmailIgnoreCase(auth.getName());
                if (managerOpt.isPresent() && managerOpt.get().getDepartment() != null) {
                    String managerDept = managerOpt.get().getDepartment().trim().toLowerCase();
                    return all.stream()
                        .filter(a -> a.getDepartment() != null && a.getDepartment().trim().toLowerCase().contains(managerDept))
                        .toList();
                }
            }
        }
        return all;
    }

    public Attendance overrideStatus(Long id, String newStatus) {
        Attendance att = attendanceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Attendance log not found with ID: " + id));

        att.setStatus(newStatus);
        String nowTime = LocalTime.now().format(TIME_FORMATTER);

        if ("Present".equalsIgnoreCase(newStatus) || "WFH".equalsIgnoreCase(newStatus)) {
            if ("--:--".equals(att.getTimeIn())) {
                att.setTimeIn(nowTime);
                att.setTimeOut("Ongoing Shift");
                att.setHours("In Progress");
                att.setPunchedIn(true);
            }
        } else if ("On Leave".equalsIgnoreCase(newStatus) || "Absent".equalsIgnoreCase(newStatus) || "Not Punched In".equalsIgnoreCase(newStatus)) {
            att.setTimeIn("--:--");
            att.setTimeOut("--:--");
            att.setHours("0 hrs");
            att.setPunchedIn(false);
        }

        return attendanceRepository.save(att);
    }

    public long getTodayPresentCount() {
        String todayKey = LocalDate.now().toString();
        Set<String> validEmails = getValidUserEmails();
        List<Attendance> todayLogs = attendanceRepository.findByDateKey(todayKey);
        return todayLogs.stream()
            .filter(a -> a.getUserEmail() != null && validEmails.contains(a.getUserEmail().trim().toLowerCase()))
            .filter(a -> "Present".equalsIgnoreCase(a.getStatus()) || "WFH".equalsIgnoreCase(a.getStatus()) || a.isPunchedIn())
            .count();
    }

    public long getTodayAbsentCount() {
        String todayKey = LocalDate.now().toString();
        Set<String> validEmails = getValidUserEmails();
        List<Attendance> todayLogs = attendanceRepository.findByDateKey(todayKey);
        return todayLogs.stream()
            .filter(a -> a.getUserEmail() != null && validEmails.contains(a.getUserEmail().trim().toLowerCase()))
            .filter(a -> "Absent".equalsIgnoreCase(a.getStatus()) || "Not Punched In".equalsIgnoreCase(a.getStatus()))
            .count();
    }

    public int getTodayAttendanceRate() {
        String todayKey = LocalDate.now().toString();
        Set<String> validEmails = getValidUserEmails();
        List<Attendance> todayLogs = attendanceRepository.findByDateKey(todayKey).stream()
            .filter(a -> a.getUserEmail() != null && validEmails.contains(a.getUserEmail().trim().toLowerCase()))
            .toList();
        if (todayLogs.isEmpty()) {
            return 100;
        }

        long presentCount = todayLogs.stream()
            .filter(a -> "Present".equalsIgnoreCase(a.getStatus()) || "WFH".equalsIgnoreCase(a.getStatus()) || a.isPunchedIn())
            .count();
        return (int) Math.round(((double) presentCount / todayLogs.size()) * 100);
    }

    public java.util.List<java.util.Map<String, Object>> getAttendanceTrends() {
        Set<String> validEmails = getValidUserEmails();
        java.util.List<java.util.Map<String, Object>> trends = new java.util.ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            String dateKey = d.toString();
            List<Attendance> logs = attendanceRepository.findByDateKey(dateKey).stream()
                .filter(a -> a.getUserEmail() != null && validEmails.contains(a.getUserEmail().trim().toLowerCase()))
                .toList();
            long present = logs.stream()
                .filter(a -> "Present".equalsIgnoreCase(a.getStatus()) || "WFH".equalsIgnoreCase(a.getStatus()) || a.isPunchedIn())
                .count();
            long total = logs.size();
            int rate = total > 0 ? (int) Math.round(((double) present / total) * 100) : 100;

            java.util.Map<String, Object> day = new java.util.HashMap<>();
            day.put("date", d.format(DateTimeFormatter.ofPattern("EEE, MMM dd")));
            day.put("dateKey", dateKey);
            day.put("present", present);
            day.put("total", total);
            day.put("rate", rate);
            trends.add(day);
        }
        return trends;
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteAttendanceByUserEmail(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) return;
        attendanceRepository.deleteByUserEmailIgnoreCase(userEmail.trim());
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteAttendanceLog(Long id) {
        if (id != null && attendanceRepository.existsById(id)) {
            attendanceRepository.deleteById(id);
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public Attendance adminPunchOut(Long id, String customPunchOutTime) {
        Attendance att = attendanceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Attendance log record not found with ID: " + id));

        // Derive appropriate punch-out time
        String timeOutStr;
        if (customPunchOutTime != null && !customPunchOutTime.isBlank()) {
            timeOutStr = customPunchOutTime.trim();
        } else {
            // Default: if today, use current time; if past date, default to standard shift end 06:00:00 PM
            if (LocalDate.now().toString().equals(att.getDateKey())) {
                timeOutStr = LocalTime.now().format(TIME_FORMATTER);
            } else {
                timeOutStr = "06:00:00 PM";
            }
        }

        att.setPunchedIn(false);
        att.setTimeOut(timeOutStr);

        // Compute total hours between timeIn and timeOut
        String calculatedHours = computeDurationHours(att.getTimeIn(), timeOutStr);
        att.setHours(calculatedHours);

        // Keep or set status to Present/WFH
        if ("Not Punched In".equalsIgnoreCase(att.getStatus()) || "Absent".equalsIgnoreCase(att.getStatus())) {
            att.setStatus("WFH".equalsIgnoreCase(att.getMode()) ? "WFH" : "Present");
        }

        return attendanceRepository.save(att);
    }

    public String computeDurationHours(String timeIn, String timeOut) {
        if (timeIn == null || timeIn.isBlank() || "--:--".equals(timeIn) ||
            timeOut == null || timeOut.isBlank() || "--:--".equals(timeOut)) {
            return "0 hrs";
        }

        LocalTime inTime = parseTime(timeIn);
        LocalTime outTime = parseTime(timeOut);

        if (inTime != null && outTime != null) {
            long totalSeconds;
            if (outTime.isAfter(inTime)) {
                totalSeconds = java.time.Duration.between(inTime, outTime).getSeconds();
            } else {
                // If overnight or same, compute wrap
                totalSeconds = java.time.Duration.between(inTime, LocalTime.MAX).getSeconds() +
                               java.time.Duration.between(LocalTime.MIN, outTime).getSeconds() + 1;
            }

            long hrs = totalSeconds / 3600;
            long mins = (totalSeconds % 3600) / 60;
            long secs = totalSeconds % 60;

            if (hrs > 0) {
                return hrs + " hr " + mins + " min";
            } else if (mins > 0) {
                return mins + " min " + secs + " sec";
            } else {
                return secs + " sec";
            }
        }
        return "8 hrs";
    }

    private LocalTime parseTime(String str) {
        if (str == null) return null;
        String s = str.trim().toUpperCase();
        String[] patterns = {
            "hh:mm:ss a", "h:mm:ss a", "hh:mm a", "h:mm a",
            "HH:mm:ss", "H:mm:ss", "HH:mm", "H:mm"
        };
        for (String pattern : patterns) {
            try {
                return LocalTime.parse(s, DateTimeFormatter.ofPattern(pattern, java.util.Locale.ENGLISH));
            } catch (Exception ignored) {}
        }
        return null;
    }

    public Set<String> getValidUserEmails() {
        Set<String> validEmails = userRepository.findAll().stream()
            .map(u -> u.getEmail().trim().toLowerCase())
            .collect(Collectors.toSet());
        employeeRepository.findAll().forEach(e -> {
            if (e.getEmail() != null && !e.getEmail().isBlank()) {
                validEmails.add(e.getEmail().trim().toLowerCase());
            }
        });
        return validEmails;
    }
}
