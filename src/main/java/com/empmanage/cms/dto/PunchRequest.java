package com.empmanage.cms.dto;

public class PunchRequest {
    private String userEmail;
    private String mode; // "Office", "WFH", "Leave"
    private String punchTime;

    public PunchRequest() {}

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getPunchTime() { return punchTime; }
    public void setPunchTime(String punchTime) { this.punchTime = punchTime; }
}
