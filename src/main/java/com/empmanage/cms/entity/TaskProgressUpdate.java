package com.empmanage.cms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_progress_updates")
public class TaskProgressUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    @JsonIgnore
    private Task task;

    @Column(nullable = false)
    private String updaterEmail;

    @Column(nullable = false)
    private String updaterName;

    private String updaterRole;

    @Column(nullable = false)
    private int progressPercentage;

    @Column(length = 4000)
    private String workDescription;

    @Enumerated(EnumType.STRING)
    private TaskStatus previousStatus;

    @Enumerated(EnumType.STRING)
    private TaskStatus newStatus;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    public TaskProgressUpdate() {}

    public TaskProgressUpdate(Task task, String updaterEmail, String updaterName, String updaterRole,
                              int progressPercentage, String workDescription,
                              TaskStatus previousStatus, TaskStatus newStatus) {
        this.task = task;
        this.updaterEmail = updaterEmail;
        this.updaterName = updaterName;
        this.updaterRole = updaterRole;
        this.progressPercentage = progressPercentage;
        this.workDescription = workDescription;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.timestamp = LocalDateTime.now();
    }

    @JsonProperty("taskId")
    public Long getTaskId() {
        return task != null ? task.getId() : null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

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
}
