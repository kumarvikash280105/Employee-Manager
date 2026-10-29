package com.empmanage.cms.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks", indexes = {
    @Index(name = "idx_tasks_assigned_email", columnList = "assignedToEmail"),
    @Index(name = "idx_tasks_status", columnList = "status"),
    @Index(name = "idx_tasks_due_date", columnList = "dueDate")
})
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 4000)
    private String description;

    @Column(nullable = false)
    private String assignedToEmail;

    @Column(nullable = false)
    private String assignedToName;

    private String assignedToDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password"})
    private User assignedUser;

    private String createdByEmail;

    private String createdByName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password"})
    private User createdByUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.TO_DO;

    @Column(nullable = false)
    private int progress = 0; // 0 to 100 percentage

    private LocalDate startDate;

    private LocalDate dueDate;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("timestamp DESC")
    @JsonIgnoreProperties({"task"})
    private List<TaskProgressUpdate> progressUpdates = new ArrayList<>();

    public Task() {}

    public Task(String title, String description, String assignedToEmail, String assignedToName,
                String assignedToDepartment, String createdByEmail, String createdByName,
                TaskPriority priority, LocalDate startDate, LocalDate dueDate) {
        this.title = title;
        this.description = description;
        this.assignedToEmail = assignedToEmail != null ? assignedToEmail.trim().toLowerCase() : null;
        this.assignedToName = assignedToName;
        this.assignedToDepartment = assignedToDepartment;
        this.createdByEmail = createdByEmail != null ? createdByEmail.trim().toLowerCase() : null;
        this.createdByName = createdByName;
        this.priority = priority != null ? priority : TaskPriority.MEDIUM;
        this.status = TaskStatus.TO_DO;
        this.progress = 0;
        this.startDate = startDate;
        this.dueDate = dueDate;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @JsonProperty("overdue")
    public boolean isOverdue() {
        if (dueDate == null) {
            return false;
        }
        if (status == TaskStatus.COMPLETED || status == TaskStatus.CANCELLED) {
            return false;
        }
        return LocalDate.now().isAfter(dueDate);
    }

    public void addProgressUpdate(TaskProgressUpdate update) {
        if (update != null) {
            update.setTask(this);
            this.progressUpdates.add(0, update);
            this.updatedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAssignedToEmail() { return assignedToEmail; }
    public void setAssignedToEmail(String assignedToEmail) {
        this.assignedToEmail = assignedToEmail != null ? assignedToEmail.trim().toLowerCase() : null;
    }

    public String getAssignedToName() { return assignedToName; }
    public void setAssignedToName(String assignedToName) { this.assignedToName = assignedToName; }

    public String getAssignedToDepartment() { return assignedToDepartment; }
    public void setAssignedToDepartment(String assignedToDepartment) { this.assignedToDepartment = assignedToDepartment; }

    public User getAssignedUser() { return assignedUser; }
    public void setAssignedUser(User assignedUser) { this.assignedUser = assignedUser; }

    public String getCreatedByEmail() { return createdByEmail; }
    public void setCreatedByEmail(String createdByEmail) {
        this.createdByEmail = createdByEmail != null ? createdByEmail.trim().toLowerCase() : null;
    }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public User getCreatedByUser() { return createdByUser; }
    public void setCreatedByUser(User createdByUser) { this.createdByUser = createdByUser; }

    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) {
        if (progress < 0) this.progress = 0;
        else if (progress > 100) this.progress = 100;
        else this.progress = progress;
    }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public List<TaskProgressUpdate> getProgressUpdates() { return progressUpdates; }
    public void setProgressUpdates(List<TaskProgressUpdate> progressUpdates) { this.progressUpdates = progressUpdates; }
}
