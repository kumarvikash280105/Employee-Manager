package com.empmanage.cms.dto;

import com.empmanage.cms.entity.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public class TaskCreateRequest {

    @NotBlank(message = "Task title is required")
    private String title;

    private String description;

    @NotBlank(message = "Assignee email is required")
    private String assignedToEmail;

    private TaskPriority priority = TaskPriority.MEDIUM;

    private LocalDate startDate;

    private LocalDate dueDate;

    public TaskCreateRequest() {}

    public TaskCreateRequest(String title, String description, String assignedToEmail,
                             TaskPriority priority, LocalDate startDate, LocalDate dueDate) {
        this.title = title;
        this.description = description;
        this.assignedToEmail = assignedToEmail;
        this.priority = priority;
        this.startDate = startDate;
        this.dueDate = dueDate;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAssignedToEmail() { return assignedToEmail; }
    public void setAssignedToEmail(String assignedToEmail) { this.assignedToEmail = assignedToEmail; }

    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
}
