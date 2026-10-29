package com.empmanage.cms.dto;

import com.empmanage.cms.entity.TaskPriority;
import com.empmanage.cms.entity.TaskStatus;
import java.time.LocalDate;

public class TaskUpdateRequest {

    private String title;
    private String description;
    private String assignedToEmail;
    private TaskPriority priority;
    private TaskStatus status;
    private Integer progress;
    private LocalDate startDate;
    private LocalDate dueDate;

    public TaskUpdateRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAssignedToEmail() { return assignedToEmail; }
    public void setAssignedToEmail(String assignedToEmail) { this.assignedToEmail = assignedToEmail; }

    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
}
