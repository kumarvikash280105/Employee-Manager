package com.empmanage.cms.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "attendance_logs")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String userName;

    private String userRole = "EMPLOYEE";

    private String department;

    @Column(nullable = false)
    private String date; // "Oct 14, 2026"

    @Column(nullable = false)
    private String dateKey; // "2026-10-14"

    private String timeIn = "--:--";

    private String timeOut = "--:--";

    private String hours = "0 hrs";

    private String mode = "Office"; // Office, WFH, Leave

    private String status = "Not Punched In"; // Present, WFH, On Leave, Absent, Not Punched In

    private boolean punchedIn = false;

    private Long punchInTimestamp;

    private Long timestamp = System.currentTimeMillis();

    public Attendance() {}

    public Attendance(String userEmail, String userName, String userRole, String department, String date, String dateKey) {
        this.userEmail = userEmail;
        this.userName = userName;
        this.userRole = userRole;
        this.department = department;
        this.date = date;
        this.dateKey = dateKey;
        this.timeIn = "--:--";
        this.timeOut = "--:--";
        this.hours = "0 hrs";
        this.mode = "Office";
        this.status = "Not Punched In";
        this.punchedIn = false;
        this.timestamp = System.currentTimeMillis();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getDateKey() { return dateKey; }
    public void setDateKey(String dateKey) { this.dateKey = dateKey; }

    public String getTimeIn() { return timeIn; }
    public void setTimeIn(String timeIn) { this.timeIn = timeIn; }

    public String getTimeOut() { return timeOut; }
    public void setTimeOut(String timeOut) { this.timeOut = timeOut; }

    public String getHours() { return hours; }
    public void setHours(String hours) { this.hours = hours; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isPunchedIn() { return punchedIn; }
    public void setPunchedIn(boolean punchedIn) { this.punchedIn = punchedIn; }

    public Long getPunchInTimestamp() { return punchInTimestamp; }
    public void setPunchInTimestamp(Long punchInTimestamp) { this.punchInTimestamp = punchInTimestamp; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}
