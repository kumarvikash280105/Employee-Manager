package com.empmanage.cms.service;

import com.empmanage.cms.dto.*;
import com.empmanage.cms.entity.*;
import com.empmanage.cms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskProgressUpdateRepository progressUpdateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    public TaskDto createTask(TaskCreateRequest req, String creatorEmail) {
        if (req == null) {
            throw new IllegalArgumentException("Task request cannot be null.");
        }
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new IllegalArgumentException("Task title is required.");
        }
        if (req.getAssignedToEmail() == null || req.getAssignedToEmail().isBlank()) {
            throw new IllegalArgumentException("Assignee email is required.");
        }
        if (req.getStartDate() != null && req.getDueDate() != null && req.getDueDate().isBefore(req.getStartDate())) {
            throw new IllegalArgumentException("Due date cannot be before start date.");
        }

        String targetEmail = req.getAssignedToEmail().trim().toLowerCase();
        String assignedName = targetEmail;
        String assignedDept = "General";
        User assignedUser = userRepository.findByEmailIgnoreCase(targetEmail).orElse(null);
        if (assignedUser != null) {
            assignedName = assignedUser.getFullName();
            if (assignedUser.getDepartment() != null && !assignedUser.getDepartment().isBlank()) {
                assignedDept = assignedUser.getDepartment();
            }
        } else {
            Employee emp = employeeRepository.findByEmailIgnoreCase(targetEmail).orElse(null);
            if (emp != null) {
                assignedName = emp.getName();
                assignedDept = emp.getDepartmentName();
            }
        }

        User creatorUser = (creatorEmail != null && !creatorEmail.isBlank()) 
                ? userRepository.findByEmailIgnoreCase(creatorEmail.trim().toLowerCase()).orElse(null) 
                : null;
        String creatorName = (creatorUser != null) ? creatorUser.getFullName() : (creatorEmail != null ? creatorEmail : "System Admin");

        Task task = new Task();
        task.setTitle(req.getTitle().trim());
        task.setDescription(req.getDescription() != null ? req.getDescription().trim() : "");
        task.setAssignedToEmail(targetEmail);
        task.setAssignedToName(assignedName);
        task.setAssignedToDepartment(assignedDept);
        task.setAssignedUser(assignedUser);
        task.setCreatedByEmail(creatorEmail != null ? creatorEmail.trim().toLowerCase() : null);
        task.setCreatedByName(creatorName);
        task.setCreatedByUser(creatorUser);
        task.setPriority(req.getPriority() != null ? req.getPriority() : TaskPriority.MEDIUM);
        task.setStatus(TaskStatus.TO_DO);
        task.setProgress(0);
        task.setStartDate(req.getStartDate());
        task.setDueDate(req.getDueDate());
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        Task savedTask = taskRepository.save(task);

        // Record initial creation log
        TaskProgressUpdate initialLog = new TaskProgressUpdate(
                savedTask,
                creatorEmail != null ? creatorEmail.trim().toLowerCase() : "system",
                creatorName,
                creatorUser != null ? creatorUser.getRole() : "ROLE_ADMIN",
                0,
                "Task created and assigned to " + assignedName + " (" + targetEmail + ").",
                null,
                TaskStatus.TO_DO
        );
        progressUpdateRepository.save(initialLog);
        savedTask.getProgressUpdates().add(initialLog);

        return TaskDto.fromEntity(savedTask);
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getMyTasks(String employeeEmail, TaskStatus status, TaskPriority priority, String query) {
        if (employeeEmail == null || employeeEmail.isBlank()) {
            return Collections.emptyList();
        }
        String email = employeeEmail.trim().toLowerCase();
        List<Task> tasks = taskRepository.findMyTasksWithFilter(
                email,
                status,
                priority,
                (query != null && !query.isBlank()) ? query.trim() : null
        );
        return tasks.stream().map(TaskDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getAllTasks(String callerEmail, TaskStatus status, TaskPriority priority,
                                     String department, String assigneeEmail, String query) {
        String cleanAssignee = (assigneeEmail != null && !assigneeEmail.isBlank()) ? assigneeEmail.trim().toLowerCase() : null;
        String cleanDept = (department != null && !department.isBlank() && !"ALL".equalsIgnoreCase(department)) ? department.trim() : null;
        String cleanQuery = (query != null && !query.isBlank()) ? query.trim() : null;

        List<Task> tasks = taskRepository.findTasksWithFilter(
                cleanAssignee,
                status,
                priority,
                cleanDept,
                cleanQuery
        );
        return tasks.stream().map(TaskDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaskDto getTaskById(Long id, String callerEmail, boolean canViewAll) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + id));

        if (!canViewAll) {
            String safeCaller = callerEmail != null ? callerEmail.trim().toLowerCase() : "";
            if (!task.getAssignedToEmail().equalsIgnoreCase(safeCaller)) {
                throw new AccessDeniedException("You do not have permission to view another employee's task.");
            }
        }

        return TaskDto.fromEntity(task);
    }

    public TaskDto updateProgress(Long taskId, TaskProgressRequest req, String updaterEmail, boolean canUpdateAll) {
        if (req == null) {
            throw new IllegalArgumentException("Progress update request cannot be null.");
        }
        if (req.getProgressPercentage() < 0 || req.getProgressPercentage() > 100) {
            throw new IllegalArgumentException("Progress percentage must be between 0 and 100.");
        }
        if (req.getWorkDescription() == null || req.getWorkDescription().isBlank()) {
            throw new IllegalArgumentException("Work description is required.");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + taskId));

        String safeUpdater = updaterEmail != null ? updaterEmail.trim().toLowerCase() : "";
        if (!canUpdateAll && !task.getAssignedToEmail().equalsIgnoreCase(safeUpdater)) {
            throw new AccessDeniedException("You are not authorized to update progress on this task.");
        }

        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new IllegalStateException("Cannot update progress on an already completed task.");
        }
        if (task.getStatus() == TaskStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update progress on a cancelled task.");
        }

        TaskStatus prevStatus = task.getStatus();
        TaskStatus nextStatus = prevStatus;

        // Auto-transition: TO_DO moves to IN_PROGRESS upon active work progress
        if (prevStatus == TaskStatus.TO_DO && req.getProgressPercentage() > 0) {
            nextStatus = TaskStatus.IN_PROGRESS;
        }

        task.setStatus(nextStatus);
        task.setProgress(req.getProgressPercentage());
        task.setUpdatedAt(LocalDateTime.now());

        // Resolve updater metadata
        User updaterUser = userRepository.findByEmailIgnoreCase(safeUpdater).orElse(null);
        String updaterName = updaterUser != null ? updaterUser.getFullName() : safeUpdater;
        String updaterRole = updaterUser != null ? updaterUser.getRole() : "ROLE_USER";

        TaskProgressUpdate progressUpdate = new TaskProgressUpdate(
                task,
                safeUpdater,
                updaterName,
                updaterRole,
                req.getProgressPercentage(),
                req.getWorkDescription().trim(),
                prevStatus,
                nextStatus
        );

        task.addProgressUpdate(progressUpdate);
        progressUpdateRepository.save(progressUpdate);
        Task saved = taskRepository.save(task);

        return TaskDto.fromEntity(saved);
    }

    public TaskDto submitForReview(Long taskId, String workNotes, String submitterEmail, boolean canUpdateAll) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + taskId));

        String safeSubmitter = submitterEmail != null ? submitterEmail.trim().toLowerCase() : "";
        if (!canUpdateAll && !task.getAssignedToEmail().equalsIgnoreCase(safeSubmitter)) {
            throw new AccessDeniedException("You are not authorized to submit this task for review.");
        }

        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new IllegalStateException("Task is already marked as completed.");
        }
        if (task.getStatus() == TaskStatus.CANCELLED) {
            throw new IllegalStateException("Cannot submit a cancelled task for review.");
        }
        if (task.getStatus() == TaskStatus.IN_REVIEW) {
            throw new IllegalStateException("Task is already under review.");
        }

        TaskStatus prevStatus = task.getStatus();
        TaskStatus nextStatus = TaskStatus.IN_REVIEW;

        task.setStatus(nextStatus);
        // Setting progress to 100 on submission if not already
        if (task.getProgress() < 100) {
            task.setProgress(100);
        }
        task.setUpdatedAt(LocalDateTime.now());

        User submitterUser = userRepository.findByEmailIgnoreCase(safeSubmitter).orElse(null);
        String submitterName = submitterUser != null ? submitterUser.getFullName() : safeSubmitter;
        String submitterRole = submitterUser != null ? submitterUser.getRole() : "ROLE_USER";

        String notes = (workNotes != null && !workNotes.isBlank()) 
                ? workNotes.trim() 
                : "Work submitted for review and approval.";

        TaskProgressUpdate update = new TaskProgressUpdate(
                task,
                safeSubmitter,
                submitterName,
                submitterRole,
                task.getProgress(),
                notes,
                prevStatus,
                nextStatus
        );

        task.addProgressUpdate(update);
        progressUpdateRepository.save(update);
        Task saved = taskRepository.save(task);

        return TaskDto.fromEntity(saved);
    }

    public TaskDto reviewTask(Long taskId, TaskReviewRequest req, String reviewerEmail) {
        if (req == null) {
            throw new IllegalArgumentException("Review request cannot be null.");
        }
        String action = req.getAction() != null ? req.getAction().trim().toUpperCase() : "";
        if (!"APPROVE".equals(action) && !"REQUEST_CHANGES".equals(action)) {
            throw new IllegalArgumentException("Invalid review action. Must be APPROVE or REQUEST_CHANGES.");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + taskId));

        if (task.getStatus() == TaskStatus.CANCELLED) {
            throw new IllegalStateException("Cannot review a cancelled task.");
        }

        String safeReviewer = reviewerEmail != null ? reviewerEmail.trim().toLowerCase() : "";
        User reviewerUser = userRepository.findByEmailIgnoreCase(safeReviewer).orElse(null);
        String reviewerName = reviewerUser != null ? reviewerUser.getFullName() : safeReviewer;
        String reviewerRole = reviewerUser != null ? reviewerUser.getRole() : "ROLE_MANAGER";

        TaskStatus prevStatus = task.getStatus();
        TaskStatus nextStatus;
        String logDesc;

        if ("APPROVE".equals(action)) {
            nextStatus = TaskStatus.COMPLETED;
            task.setProgress(100);
            task.setCompletedAt(LocalDateTime.now());
            String feedback = (req.getFeedback() != null && !req.getFeedback().isBlank()) ? req.getFeedback().trim() : "Approved.";
            logDesc = "Task reviewed and approved by " + reviewerName + ". Feedback: " + feedback;
        } else {
            // REQUEST_CHANGES returns task to IN_PROGRESS
            nextStatus = TaskStatus.IN_PROGRESS;
            task.setCompletedAt(null);
            String feedback = (req.getFeedback() != null && !req.getFeedback().isBlank()) ? req.getFeedback().trim() : "Changes requested. Please revise and resubmit.";
            logDesc = "Changes requested by " + reviewerName + ". Feedback: " + feedback;
        }

        task.setStatus(nextStatus);
        task.setUpdatedAt(LocalDateTime.now());

        TaskProgressUpdate update = new TaskProgressUpdate(
                task,
                safeReviewer,
                reviewerName,
                reviewerRole,
                task.getProgress(),
                logDesc,
                prevStatus,
                nextStatus
        );

        task.addProgressUpdate(update);
        progressUpdateRepository.save(update);
        Task saved = taskRepository.save(task);

        return TaskDto.fromEntity(saved);
    }

    public TaskDto updateTask(Long taskId, TaskUpdateRequest req, String callerEmail) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + taskId));

        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            task.setTitle(req.getTitle().trim());
        }
        if (req.getDescription() != null) {
            task.setDescription(req.getDescription().trim());
        }
        if (req.getPriority() != null) {
            task.setPriority(req.getPriority());
        }
        if (req.getStartDate() != null) {
            task.setStartDate(req.getStartDate());
        }
        if (req.getDueDate() != null) {
            task.setDueDate(req.getDueDate());
        }
        if (task.getStartDate() != null && task.getDueDate() != null && task.getDueDate().isBefore(task.getStartDate())) {
            throw new IllegalArgumentException("Due date cannot be before start date.");
        }
        if (req.getProgress() != null) {
            if (req.getProgress() < 0 || req.getProgress() > 100) {
                throw new IllegalArgumentException("Progress must be between 0 and 100.");
            }
            task.setProgress(req.getProgress());
        }
        if (req.getStatus() != null) {
            TaskStatus oldStatus = task.getStatus();
            task.setStatus(req.getStatus());
            if (req.getStatus() == TaskStatus.COMPLETED && task.getCompletedAt() == null) {
                task.setCompletedAt(LocalDateTime.now());
            } else if (req.getStatus() != TaskStatus.COMPLETED) {
                task.setCompletedAt(null);
            }
            if (oldStatus != req.getStatus()) {
                TaskProgressUpdate statusChangeLog = new TaskProgressUpdate(
                        task,
                        callerEmail != null ? callerEmail : "system",
                        callerEmail != null ? callerEmail : "Administrator",
                        "ROLE_ADMIN",
                        task.getProgress(),
                        "Task status updated manually from " + oldStatus + " to " + req.getStatus(),
                        oldStatus,
                        req.getStatus()
                );
                progressUpdateRepository.save(statusChangeLog);
                task.addProgressUpdate(statusChangeLog);
            }
        }

        if (req.getAssignedToEmail() != null && !req.getAssignedToEmail().isBlank() 
                && !req.getAssignedToEmail().equalsIgnoreCase(task.getAssignedToEmail())) {
            String newEmail = req.getAssignedToEmail().trim().toLowerCase();
            task.setAssignedToEmail(newEmail);
            User newAssignee = userRepository.findByEmailIgnoreCase(newEmail).orElse(null);
            if (newAssignee != null) {
                task.setAssignedToName(newAssignee.getFullName());
                task.setAssignedUser(newAssignee);
                if (newAssignee.getDepartment() != null) task.setAssignedToDepartment(newAssignee.getDepartment());
            } else {
                Employee emp = employeeRepository.findByEmailIgnoreCase(newEmail).orElse(null);
                if (emp != null) {
                    task.setAssignedToName(emp.getName());
                    task.setAssignedToDepartment(emp.getDepartmentName());
                }
            }
        }

        task.setUpdatedAt(LocalDateTime.now());
        Task saved = taskRepository.save(task);
        return TaskDto.fromEntity(saved);
    }

    public void deleteTask(Long taskId, String callerEmail) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + taskId));
        taskRepository.delete(task);
    }

    @Transactional(readOnly = true)
    public TaskAnalyticsDto getAnalytics(String callerEmail, String department) {
        List<Task> allTasks;
        if (department != null && !department.isBlank() && !"ALL".equalsIgnoreCase(department)) {
            allTasks = taskRepository.findByAssignedToDepartmentIgnoreCaseOrderByDueDateAsc(department.trim());
        } else {
            allTasks = taskRepository.findAll();
        }

        long total = allTasks.size();
        long toDo = 0;
        long inProgress = 0;
        long inReview = 0;
        long completed = 0;
        long cancelled = 0;
        long overdue = 0;

        Map<String, Long> statusBreakdown = new HashMap<>();
        Map<String, Long> priorityBreakdown = new HashMap<>();
        Map<String, Map<String, Object>> deptMap = new HashMap<>();

        for (Task t : allTasks) {
            // Status counts
            if (t.getStatus() == TaskStatus.TO_DO) toDo++;
            else if (t.getStatus() == TaskStatus.IN_PROGRESS) inProgress++;
            else if (t.getStatus() == TaskStatus.IN_REVIEW) inReview++;
            else if (t.getStatus() == TaskStatus.COMPLETED) completed++;
            else if (t.getStatus() == TaskStatus.CANCELLED) cancelled++;

            statusBreakdown.put(t.getStatus().name(), statusBreakdown.getOrDefault(t.getStatus().name(), 0L) + 1L);

            // Priority counts
            if (t.getPriority() != null) {
                priorityBreakdown.put(t.getPriority().name(), priorityBreakdown.getOrDefault(t.getPriority().name(), 0L) + 1L);
            }

            // Overdue check
            if (t.isOverdue()) {
                overdue++;
            }

            // Department grouping
            String dept = (t.getAssignedToDepartment() != null && !t.getAssignedToDepartment().isBlank()) 
                    ? t.getAssignedToDepartment() : "General";
            deptMap.putIfAbsent(dept, new HashMap<>(Map.of("total", 0L, "completed", 0L, "overdue", 0L)));
            Map<String, Object> deptStats = deptMap.get(dept);
            deptStats.put("total", ((Long) deptStats.get("total")) + 1L);
            if (t.getStatus() == TaskStatus.COMPLETED) {
                deptStats.put("completed", ((Long) deptStats.get("completed")) + 1L);
            }
            if (t.isOverdue()) {
                deptStats.put("overdue", ((Long) deptStats.get("overdue")) + 1L);
            }
        }

        double rate = total > 0 ? Math.round(((double) completed / total) * 1000.0) / 10.0 : 0.0;

        TaskAnalyticsDto dto = new TaskAnalyticsDto();
        dto.setTotalTasks(total);
        dto.setToDoTasks(toDo);
        dto.setInProgressTasks(inProgress);
        dto.setInReviewTasks(inReview);
        dto.setCompletedTasks(completed);
        dto.setCancelledTasks(cancelled);
        dto.setOverdueTasks(overdue);
        dto.setCompletionRate(rate);
        dto.setStatusBreakdown(statusBreakdown);
        dto.setPriorityBreakdown(priorityBreakdown);
        dto.setDepartmentBreakdown(deptMap);

        return dto;
    }

    @Transactional(readOnly = true)
    public List<TaskProgressDto> getTaskHistory(Long taskId, String callerEmail, boolean canViewAll) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + taskId));

        if (!canViewAll) {
            String safeCaller = callerEmail != null ? callerEmail.trim().toLowerCase() : "";
            if (!task.getAssignedToEmail().equalsIgnoreCase(safeCaller)) {
                throw new AccessDeniedException("You do not have permission to view this task's history.");
            }
        }

        List<TaskProgressUpdate> updates = progressUpdateRepository.findByTaskIdOrderByTimestampDesc(taskId);
        return updates.stream().map(TaskProgressDto::fromEntity).collect(Collectors.toList());
    }
}
