package com.empmanage.cms.dto;

import com.empmanage.cms.entity.Task;
import com.empmanage.cms.entity.TaskPriority;
import com.empmanage.cms.entity.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TaskDto {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    private Long id;
    private String title;
    private String description;
    private String assignedToEmail;
    private String assignedToName;
    private String assignedToDepartment;
    private String createdByEmail;
    private String createdByName;
    private TaskPriority priority;
    private TaskStatus status;
    private int progress;
    private LocalDate startDate;
    private String formattedStartDate;
    private LocalDate dueDate;
    private String formattedDueDate;
    private boolean overdue;
    private LocalDateTime createdAt;
    private String formattedCreatedAt;
    private LocalDateTime updatedAt;
    private String formattedUpdatedAt;
    private LocalDateTime completedAt;
    private String formattedCompletedAt;
    private List<TaskProgressDto> progressUpdates = new ArrayList<>();

    public TaskDto() {}

    public static TaskDto fromEntity(Task task) {
        if (task == null) return null;
        TaskDto dto = new TaskDto();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setAssignedToEmail(task.getAssignedToEmail());
        dto.setAssignedToName(task.getAssignedToName());
        dto.setAssignedToDepartment(task.getAssignedToDepartment());
        dto.setCreatedByEmail(task.getCreatedByEmail());
        dto.setCreatedByName(task.getCreatedByName());
        dto.setPriority(task.getPriority());
        dto.setStatus(task.getStatus());
        dto.setProgress(task.getProgress());
        dto.setStartDate(task.getStartDate());
        if (task.getStartDate() != null) {
            dto.setFormattedStartDate(task.getStartDate().format(DATE_FORMATTER));
        }
        dto.setDueDate(task.getDueDate());
        if (task.getDueDate() != null) {
            dto.setFormattedDueDate(task.getDueDate().format(DATE_FORMATTER));
        }
        dto.setOverdue(task.isOverdue());
        dto.setCreatedAt(task.getCreatedAt());
        if (task.getCreatedAt() != null) {
            dto.setFormattedCreatedAt(task.getCreatedAt().format(DATETIME_FORMATTER));
        }
        dto.setUpdatedAt(task.getUpdatedAt());
        if (task.getUpdatedAt() != null) {
            dto.setFormattedUpdatedAt(task.getUpdatedAt().format(DATETIME_FORMATTER));
        }
        dto.setCompletedAt(task.getCompletedAt());
        if (task.getCompletedAt() != null) {
            dto.setFormattedCompletedAt(task.getCompletedAt().format(DATETIME_FORMATTER));
        }
        if (task.getProgressUpdates() != null) {
            dto.setProgressUpdates(task.getProgressUpdates().stream()
                    .map(TaskProgressDto::fromEntity)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAssignedToEmail() { return assignedToEmail; }
    public void setAssignedToEmail(String assignedToEmail) { this.assignedToEmail = assignedToEmail; }

    public String getAssignedToName() { return assignedToName; }
    public void setAssignedToName(String assignedToName) { this.assignedToName = assignedToName; }

    public String getAssignedToDepartment() { return assignedToDepartment; }
    public void setAssignedToDepartment(String assignedToDepartment) { this.assignedToDepartment = assignedToDepartment; }

    public String getCreatedByEmail() { return createdByEmail; }
    public void setCreatedByEmail(String createdByEmail) { this.createdByEmail = createdByEmail; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public String getFormattedStartDate() { return formattedStartDate; }
    public void setFormattedStartDate(String formattedStartDate) { this.formattedStartDate = formattedStartDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getFormattedDueDate() { return formattedDueDate; }
    public void setFormattedDueDate(String formattedDueDate) { this.formattedDueDate = formattedDueDate; }

    public boolean isOverdue() { return overdue; }
    public void setOverdue(boolean overdue) { this.overdue = overdue; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getFormattedCreatedAt() { return formattedCreatedAt; }
    public void setFormattedCreatedAt(String formattedCreatedAt) { this.formattedCreatedAt = formattedCreatedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getFormattedUpdatedAt() { return formattedUpdatedAt; }
    public void setFormattedUpdatedAt(String formattedUpdatedAt) { this.formattedUpdatedAt = formattedUpdatedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getFormattedCompletedAt() { return formattedCompletedAt; }
    public void setFormattedCompletedAt(String formattedCompletedAt) { this.formattedCompletedAt = formattedCompletedAt; }

    public List<TaskProgressDto> getProgressUpdates() { return progressUpdates; }
    public void setProgressUpdates(List<TaskProgressDto> progressUpdates) { this.progressUpdates = progressUpdates; }
}
