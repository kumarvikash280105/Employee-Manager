package com.empmanage.cms.dto;

import com.empmanage.cms.entity.TaskProgressUpdate;
import com.empmanage.cms.entity.TaskStatus;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TaskProgressDto {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    private Long id;
    private Long taskId;
    private String updaterEmail;
    private String updaterName;
    private String updaterRole;
    private int progressPercentage;
    private String workDescription;
    private TaskStatus previousStatus;
    private TaskStatus newStatus;
    private LocalDateTime timestamp;
    private String formattedTimestamp;

    public TaskProgressDto() {}

    public static TaskProgressDto fromEntity(TaskProgressUpdate update) {
        if (update == null) return null;
        TaskProgressDto dto = new TaskProgressDto();
        dto.setId(update.getId());
        dto.setTaskId(update.getTaskId());
        dto.setUpdaterEmail(update.getUpdaterEmail());
        dto.setUpdaterName(update.getUpdaterName());
        dto.setUpdaterRole(update.getUpdaterRole());
        dto.setProgressPercentage(update.getProgressPercentage());
        dto.setWorkDescription(update.getWorkDescription());
        dto.setPreviousStatus(update.getPreviousStatus());
        dto.setNewStatus(update.getNewStatus());
        dto.setTimestamp(update.getTimestamp());
        if (update.getTimestamp() != null) {
            dto.setFormattedTimestamp(update.getTimestamp().format(FORMATTER));
        }
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public String getUpdaterEmail() { return updaterEmail; }
    public void setUpdaterEmail(String updaterEmail) { this.updaterEmail = updaterEmail; }

    public String getUpdaterName() { return updaterName; }
    public void setUpdaterName(String updaterName) { this.updaterName = updaterName; }

    public String getUpdaterRole() { return updaterRole; }
    public void setUpdaterRole(String updaterRole) { this.updaterRole = updaterRole; }

    public int getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(int progressPercentage) { this.progressPercentage = progressPercentage; }

    public String getWorkDescription() { return workDescription; }
    public void setWorkDescription(String workDescription) { this.workDescription = workDescription; }

    public TaskStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(TaskStatus previousStatus) { this.previousStatus = previousStatus; }

    public TaskStatus getNewStatus() { return newStatus; }
    public void setNewStatus(TaskStatus newStatus) { this.newStatus = newStatus; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getFormattedTimestamp() { return formattedTimestamp; }
    public void setFormattedTimestamp(String formattedTimestamp) { this.formattedTimestamp = formattedTimestamp; }
}
