package com.empmanage.cms.dto;

import jakarta.validation.constraints.NotBlank;

public class TaskReviewRequest {

    @NotBlank(message = "Review action is required (APPROVE or REQUEST_CHANGES)")
    private String action; // APPROVE or REQUEST_CHANGES

    private String feedback;

    public TaskReviewRequest() {}

    public TaskReviewRequest(String action, String feedback) {
        this.action = action;
        this.feedback = feedback;
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
}
