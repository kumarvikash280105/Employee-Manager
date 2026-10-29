package com.empmanage.cms.service;

import com.empmanage.cms.dto.*;
import com.empmanage.cms.entity.*;
import com.empmanage.cms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskProgressUpdateRepository progressUpdateRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private TaskService taskService;

    private User sampleUser;
    private User managerUser;
    private Task sampleTask;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Alice Engineer", "alice@empmanage.com", "secret", "ROLE_USER", "Engineering");
        sampleUser.setId(10L);

        managerUser = new User("Bob Manager", "bob@empmanage.com", "secret", "ROLE_MANAGER", "Engineering");
        managerUser.setId(20L);

        sampleTask = new Task(
                "Build REST Endpoints",
                "Create task controller endpoints",
                "alice@empmanage.com",
                "Alice Engineer",
                "Engineering",
                "bob@empmanage.com",
                "Bob Manager",
                TaskPriority.HIGH,
                LocalDate.now(),
                LocalDate.now().plusDays(5)
        );
        sampleTask.setId(100L);
    }

    @Test
    @DisplayName("Task Creation - Assign task to employee with initial TO_DO status and 0 progress")
    void createTask_success() {
        TaskCreateRequest req = new TaskCreateRequest(
                "Design Database Schema",
                "Create PostgreSQL entities",
                "alice@empmanage.com",
                TaskPriority.URGENT,
                LocalDate.now(),
                LocalDate.now().plusDays(3)
        );

        when(userRepository.findByEmailIgnoreCase("alice@empmanage.com")).thenReturn(Optional.of(sampleUser));
        when(userRepository.findByEmailIgnoreCase("bob@empmanage.com")).thenReturn(Optional.of(managerUser));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(101L);
            return t;
        });
        when(progressUpdateRepository.save(any(TaskProgressUpdate.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskDto result = taskService.createTask(req, "bob@empmanage.com");

        assertNotNull(result);
        assertEquals(101L, result.getId());
        assertEquals("Design Database Schema", result.getTitle());
        assertEquals("alice@empmanage.com", result.getAssignedToEmail());
        assertEquals("Alice Engineer", result.getAssignedToName());
        assertEquals("Engineering", result.getAssignedToDepartment());
        assertEquals(TaskStatus.TO_DO, result.getStatus());
        assertEquals(0, result.getProgress());
        assertEquals(TaskPriority.URGENT, result.getPriority());
        verify(taskRepository, times(1)).save(any(Task.class));
        verify(progressUpdateRepository, times(1)).save(any(TaskProgressUpdate.class));
    }

    @Test
    @DisplayName("Task Creation - Due date before start date throws IllegalArgumentException")
    void createTask_invalidDueDate_throws() {
        TaskCreateRequest req = new TaskCreateRequest(
                "Invalid Task",
                "Dates are invalid",
                "alice@empmanage.com",
                TaskPriority.LOW,
                LocalDate.now().plusDays(5),
                LocalDate.now() // Due date is before start date
        );

        assertThrows(IllegalArgumentException.class, () -> taskService.createTask(req, "bob@empmanage.com"));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("Progress Update - Progress percentage transitions TO_DO task to IN_PROGRESS")
    void updateProgress_transitionsStatusToInProgress() {
        when(taskRepository.findById(100L)).thenReturn(Optional.of(sampleTask));
        when(userRepository.findByEmailIgnoreCase("alice@empmanage.com")).thenReturn(Optional.of(sampleUser));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskProgressRequest req = new TaskProgressRequest(40, "Finished initial API scaffold.");
        TaskDto updated = taskService.updateProgress(100L, req, "alice@empmanage.com", false);

        assertNotNull(updated);
        assertEquals(40, updated.getProgress());
        assertEquals(TaskStatus.IN_PROGRESS, updated.getStatus());
        verify(progressUpdateRepository, times(1)).save(any(TaskProgressUpdate.class));
    }

    @Test
    @DisplayName("Progress Update - Reject progress percentage below 0 or above 100")
    void updateProgress_invalidBounds_throws() {
        TaskProgressRequest reqNegative = new TaskProgressRequest(-10, "Invalid progress");
        assertThrows(IllegalArgumentException.class, () -> taskService.updateProgress(100L, reqNegative, "alice@empmanage.com", false));

        TaskProgressRequest reqOverflow = new TaskProgressRequest(105, "Too high");
        assertThrows(IllegalArgumentException.class, () -> taskService.updateProgress(100L, reqOverflow, "alice@empmanage.com", false));
    }

    @Test
    @DisplayName("Progress Update - Reject update on COMPLETED or CANCELLED tasks")
    void updateProgress_completedTask_throws() {
        sampleTask.setStatus(TaskStatus.COMPLETED);
        when(taskRepository.findById(100L)).thenReturn(Optional.of(sampleTask));

        TaskProgressRequest req = new TaskProgressRequest(80, "Trying to edit completed work");
        assertThrows(IllegalStateException.class, () -> taskService.updateProgress(100L, req, "alice@empmanage.com", false));
    }

    @Test
    @DisplayName("Access Control - Employee cannot update progress on another employee's task")
    void updateProgress_unauthorizedEmployee_throwsAccessDenied() {
        when(taskRepository.findById(100L)).thenReturn(Optional.of(sampleTask));

        TaskProgressRequest req = new TaskProgressRequest(60, "Malicious update from stranger");
        // Charlie is NOT the assignee and does not have canUpdateAll
        assertThrows(AccessDeniedException.class, () -> taskService.updateProgress(100L, req, "charlie@empmanage.com", false));
    }

    @Test
    @DisplayName("Submit for Review - Assignee submits completed work, transitioning status to IN_REVIEW")
    void submitForReview_transitionsStatusToInReview() {
        sampleTask.setStatus(TaskStatus.IN_PROGRESS);
        sampleTask.setProgress(80);
        when(taskRepository.findById(100L)).thenReturn(Optional.of(sampleTask));
        when(userRepository.findByEmailIgnoreCase("alice@empmanage.com")).thenReturn(Optional.of(sampleUser));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskDto submitted = taskService.submitForReview(100L, "Code and tests completed. Ready for review.", "alice@empmanage.com", false);

        assertNotNull(submitted);
        assertEquals(TaskStatus.IN_REVIEW, submitted.getStatus());
        assertEquals(100, submitted.getProgress());
        verify(progressUpdateRepository, times(1)).save(any(TaskProgressUpdate.class));
    }

    @Test
    @DisplayName("Access Control - Non-assignee cannot submit work for another employee")
    void submitForReview_unauthorizedEmployee_throws() {
        when(taskRepository.findById(100L)).thenReturn(Optional.of(sampleTask));

        assertThrows(AccessDeniedException.class, () -> 
            taskService.submitForReview(100L, "Sneaky submit", "charlie@empmanage.com", false));
    }

    @Test
    @DisplayName("Task Review - Manager APPROVE transitions task to COMPLETED and sets completion time")
    void reviewTask_approve_completesTask() {
        sampleTask.setStatus(TaskStatus.IN_REVIEW);
        sampleTask.setProgress(100);
        when(taskRepository.findById(100L)).thenReturn(Optional.of(sampleTask));
        when(userRepository.findByEmailIgnoreCase("bob@empmanage.com")).thenReturn(Optional.of(managerUser));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskReviewRequest reviewReq = new TaskReviewRequest("APPROVE", "Great job, code meets all standards!");
        TaskDto reviewed = taskService.reviewTask(100L, reviewReq, "bob@empmanage.com");

        assertNotNull(reviewed);
        assertEquals(TaskStatus.COMPLETED, reviewed.getStatus());
        assertEquals(100, reviewed.getProgress());
        assertNotNull(reviewed.getCompletedAt());
        verify(progressUpdateRepository, times(1)).save(any(TaskProgressUpdate.class));
    }

    @Test
    @DisplayName("Task Review - Manager REQUEST_CHANGES returns task to IN_PROGRESS with feedback")
    void reviewTask_requestChanges_returnsToInProgress() {
        sampleTask.setStatus(TaskStatus.IN_REVIEW);
        sampleTask.setProgress(100);
        when(taskRepository.findById(100L)).thenReturn(Optional.of(sampleTask));
        when(userRepository.findByEmailIgnoreCase("bob@empmanage.com")).thenReturn(Optional.of(managerUser));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskReviewRequest reviewReq = new TaskReviewRequest("REQUEST_CHANGES", "Please fix edge cases in the unit test suite.");
        TaskDto reviewed = taskService.reviewTask(100L, reviewReq, "bob@empmanage.com");

        assertNotNull(reviewed);
        assertEquals(TaskStatus.IN_PROGRESS, reviewed.getStatus());
        assertNull(reviewed.getCompletedAt());
        verify(progressUpdateRepository, times(1)).save(any(TaskProgressUpdate.class));
    }

    @Test
    @DisplayName("Access Control - Employee cannot view another employee's task details")
    void getTaskById_crossEmployeeSecurity_throwsAccessDenied() {
        when(taskRepository.findById(100L)).thenReturn(Optional.of(sampleTask));

        // Alice is assigned to 100L; Charlie has canViewAll = false
        assertThrows(AccessDeniedException.class, () -> taskService.getTaskById(100L, "charlie@empmanage.com", false));
    }

    @Test
    @DisplayName("Analytics - Correctly calculates total, completion rate, status distribution, and overdue count")
    void getAnalytics_computesCorrectMetrics() {
        Task t1 = new Task("T1", "", "alice@empmanage.com", "Alice", "Engineering", "bob@empmanage.com", "Bob", TaskPriority.HIGH, LocalDate.now(), LocalDate.now().plusDays(2));
        t1.setStatus(TaskStatus.COMPLETED);

        Task t2 = new Task("T2", "", "alice@empmanage.com", "Alice", "Engineering", "bob@empmanage.com", "Bob", TaskPriority.MEDIUM, LocalDate.now(), LocalDate.now().minusDays(1));
        t2.setStatus(TaskStatus.IN_PROGRESS); // Overdue because dueDate < today and not completed!

        Task t3 = new Task("T3", "", "alice@empmanage.com", "Alice", "Engineering", "bob@empmanage.com", "Bob", TaskPriority.LOW, LocalDate.now(), LocalDate.now().plusDays(3));
        t3.setStatus(TaskStatus.TO_DO);

        Task t4 = new Task("T4", "", "alice@empmanage.com", "Alice", "Engineering", "bob@empmanage.com", "Bob", TaskPriority.URGENT, LocalDate.now(), LocalDate.now().plusDays(3));
        t4.setStatus(TaskStatus.IN_REVIEW);

        when(taskRepository.findAll()).thenReturn(List.of(t1, t2, t3, t4));

        TaskAnalyticsDto analytics = taskService.getAnalytics("bob@empmanage.com", null);

        assertNotNull(analytics);
        assertEquals(4, analytics.getTotalTasks());
        assertEquals(1, analytics.getCompletedTasks());
        assertEquals(1, analytics.getInProgressTasks());
        assertEquals(1, analytics.getToDoTasks());
        assertEquals(1, analytics.getInReviewTasks());
        assertEquals(1, analytics.getOverdueTasks());
        assertEquals(25.0, analytics.getCompletionRate()); // 1 out of 4 = 25%
        assertTrue(analytics.getStatusBreakdown().containsKey("COMPLETED"));
        assertTrue(analytics.getPriorityBreakdown().containsKey("HIGH"));
    }
}
