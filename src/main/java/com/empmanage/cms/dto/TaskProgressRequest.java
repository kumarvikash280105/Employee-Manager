package com.empmanage.cms.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class TaskProgressRequest {

    @Min(value = 0, message = "Progress cannot be less than 0%")
    @Max(value = 100, message = "Progress cannot exceed 100%")
    private int progressPercentage;

    @NotBlank(message = "Work description is required")
    private String workDescription;

    public TaskProgressRequest() {}

    public TaskProgressRequest(int progressPercentage, String workDescription) {
        this.progressPercentage = progressPercentage;
        this.workDescription = workDescription;
    }

    public int getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(int progressPercentage) { this.progressPercentage = progressPercentage; }

    public String getWorkDescription() { return workDescription; }
    public void setWorkDescription(String workDescription) { this.workDescription = workDescription; }
}
