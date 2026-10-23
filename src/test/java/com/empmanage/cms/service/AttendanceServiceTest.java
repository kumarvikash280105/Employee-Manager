package com.empmanage.cms.service;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.empmanage.cms.entity.Attendance;
import com.empmanage.cms.entity.User;
import com.empmanage.cms.repository.AttendanceRepository;

@ExtendWith(MockitoExtension.class)
public class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @InjectMocks
    private AttendanceService attendanceService;

    @Test
    @DisplayName("Attendance - Initialize attendance log for user if not yet created today")
    void ensureTodayAttendanceForUser_createsNew() {
        User user = new User("John Employee", "john@empmanage.com", "pass", "ROLE_USER", "Engineering");
        String todayKey = LocalDate.now().toString();

        when(attendanceRepository.findByUserEmailIgnoreCaseAndDateKey(eq("john@empmanage.com"), eq(todayKey)))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        Attendance log = attendanceService.ensureTodayAttendanceForUser(user);

        assertNotNull(log);
        assertEquals("john@empmanage.com", log.getUserEmail());
        assertEquals("Not Punched In", log.getStatus());
        assertFalse(log.isPunchedIn());
        verify(attendanceRepository, times(1)).save(any(Attendance.class));
    }

    @Test
    @DisplayName("Attendance - Punch In sets shift status to Present and records time in")
    void punchToggle_clockIn() {
        String todayKey = LocalDate.now().toString();
        Attendance existing = new Attendance("john@empmanage.com", "John Employee", "EMPLOYEE", "Engineering", "Oct 14, 2026", todayKey);
        existing.setPunchedIn(false);

        when(attendanceRepository.findByUserEmailIgnoreCaseAndDateKey(eq("john@empmanage.com"), eq(todayKey)))
                .thenReturn(Optional.of(existing));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        Attendance punchedIn = attendanceService.punchToggle("john@empmanage.com", "Office");

        assertTrue(punchedIn.isPunchedIn());
        assertEquals("Present", punchedIn.getStatus());
        assertEquals("Ongoing Shift", punchedIn.getTimeOut());
        assertNotNull(punchedIn.getTimeIn());
    }

    @Test
    @DisplayName("Attendance - Punch Out records clock out time and calculates work duration")
    void punchToggle_clockOut() {
        String todayKey = LocalDate.now().toString();
        Attendance existing = new Attendance("john@empmanage.com", "John Employee", "EMPLOYEE", "Engineering", "Oct 14, 2026", todayKey);
        existing.setPunchedIn(true);
        existing.setTimeIn("09:00 AM");
        existing.setPunchInTimestamp(System.currentTimeMillis() - (4 * 3600 * 1000L));

        when(attendanceRepository.findByUserEmailIgnoreCaseAndDateKey(eq("john@empmanage.com"), eq(todayKey)))
                .thenReturn(Optional.of(existing));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        Attendance punchedOut = attendanceService.punchToggle("john@empmanage.com", "Office");

        assertFalse(punchedOut.isPunchedIn());
        assertNotEquals("Ongoing Shift", punchedOut.getTimeOut());
        assertTrue(punchedOut.getHours().contains("hr"));
    }
}
