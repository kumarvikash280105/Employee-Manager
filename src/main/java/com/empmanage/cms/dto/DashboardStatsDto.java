package com.empmanage.cms.dto;

import com.empmanage.cms.entity.Employee;
import java.util.List;
import java.util.Map;

public class DashboardStatsDto {

    private long registeredUsers;
    private long totalEmployees;
    private long activeEmployees;
    private long totalDepartments;
    private long presentEmployeesToday;
    private long absentEmployeesToday;
    private int attendanceRate;
    private long pendingInquiries;

    private List<Map<String, Object>> departmentBreakdown;
    private List<Employee> recentEmployees;
    private List<Map<String, Object>> attendanceTrends;

    public DashboardStatsDto() {}

    public long getRegisteredUsers() { return registeredUsers; }
    public void setRegisteredUsers(long registeredUsers) { this.registeredUsers = registeredUsers; }

    public long getTotalEmployees() { return totalEmployees; }
    public void setTotalEmployees(long totalEmployees) { this.totalEmployees = totalEmployees; }

    public long getActiveEmployees() { return activeEmployees; }
    public void setActiveEmployees(long activeEmployees) { this.activeEmployees = activeEmployees; }

    public long getTotalDepartments() { return totalDepartments; }
    public void setTotalDepartments(long totalDepartments) { this.totalDepartments = totalDepartments; }

    public long getPresentEmployeesToday() { return presentEmployeesToday; }
    public void setPresentEmployeesToday(long presentEmployeesToday) { this.presentEmployeesToday = presentEmployeesToday; }

    public long getAbsentEmployeesToday() { return absentEmployeesToday; }
    public void setAbsentEmployeesToday(long absentEmployeesToday) { this.absentEmployeesToday = absentEmployeesToday; }

    public int getAttendanceRate() { return attendanceRate; }
    public void setAttendanceRate(int attendanceRate) { this.attendanceRate = attendanceRate; }

    public long getPendingInquiries() { return pendingInquiries; }
    public void setPendingInquiries(long pendingInquiries) { this.pendingInquiries = pendingInquiries; }

    public List<Map<String, Object>> getDepartmentBreakdown() { return departmentBreakdown; }
    public void setDepartmentBreakdown(List<Map<String, Object>> departmentBreakdown) { this.departmentBreakdown = departmentBreakdown; }

    public List<Employee> getRecentEmployees() { return recentEmployees; }
    public void setRecentEmployees(List<Employee> recentEmployees) { this.recentEmployees = recentEmployees; }

    public List<Map<String, Object>> getAttendanceTrends() { return attendanceTrends; }
    public void setAttendanceTrends(List<Map<String, Object>> attendanceTrends) { this.attendanceTrends = attendanceTrends; }
}
