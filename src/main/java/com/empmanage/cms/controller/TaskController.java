package com.empmanage.cms.controller;

import com.empmanage.cms.dto.*;
import com.empmanage.cms.entity.TaskPriority;
import com.empmanage.cms.entity.TaskStatus;
import com.empmanage.cms.entity.User;
import com.empmanage.cms.repository.UserRepository;
import com.empmanage.cms.service.TaskService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<TaskDto>> createTask(
            @Valid @RequestBody TaskCreateRequest req,
            HttpServletRequest request) {

        String authEmail = getAuthenticatedEmail(request);
        if (authEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required to create tasks."));
        }

        if (!hasAnyAuthority(request, "ROLE_ADMIN", "ROLE_MANAGER", "ROLE_HR_MANAGER", "TASK_CREATE", "TASK_ASSIGN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You do not have permission to create or assign tasks."));
        }

        TaskDto created = taskService.createTask(req, authEmail);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Task created and assigned successfully.", created));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<TaskDto>>> getMyTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String query,
            HttpServletRequest request) {

        String authEmail = getAuthenticatedEmail(request);
        if (authEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required to view assigned tasks."));
        }

        TaskStatus taskStatus = (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status))
                ? TaskStatus.fromString(status) : null;
        TaskPriority taskPriority = (priority != null && !priority.isBlank() && !"ALL".equalsIgnoreCase(priority))
                ? TaskPriority.fromString(priority) : null;

        List<TaskDto> tasks = taskService.getMyTasks(authEmail, taskStatus, taskPriority, query);
        return ResponseEntity.ok(ApiResponse.ok("Assigned tasks retrieved.", tasks));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<TaskDto>>> getAllTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) String query,
            HttpServletRequest request) {

        String authEmail = getAuthenticatedEmail(request);
        if (authEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required."));
        }

        if (!hasAnyAuthority(request, "ROLE_ADMIN", "ROLE_MANAGER", "ROLE_HR_MANAGER", "ROLE_VIEWER", "TASK_VIEW_ALL")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You do not have permission to view team tasks."));
        }

        TaskStatus taskStatus = (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status))
                ? TaskStatus.fromString(status) : null;
        TaskPriority taskPriority = (priority != null && !priority.isBlank() && !"ALL".equalsIgnoreCase(priority))
                ? TaskPriority.fromString(priority) : null;

        List<TaskDto> tasks = taskService.getAllTasks(authEmail, taskStatus, taskPriority, department, assignee, query);
        return ResponseEntity.ok(ApiResponse.ok("Team tasks retrieved.", tasks));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskDto>> getTaskDetails(
            @PathVariable Long id,
            HttpServletRequest request) {

        String authEmail = getAuthenticatedEmail(request);
        if (authEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required."));
        }

        boolean canViewAll = hasAnyAuthority(request, "ROLE_ADMIN", "ROLE_MANAGER", "ROLE_HR_MANAGER", "ROLE_VIEWER", "TASK_VIEW_ALL");
        TaskDto task = taskService.getTaskById(id, authEmail, canViewAll);
        return ResponseEntity.ok(ApiResponse.ok("Task details retrieved.", task));
    }

    @PostMapping("/{id}/progress")
    public ResponseEntity<ApiResponse<TaskDto>> updateProgress(
            @PathVariable Long id,
            @Valid @RequestBody TaskProgressRequest req,
            HttpServletRequest request) {

        String authEmail = getAuthenticatedEmail(request);
        if (authEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required."));
        }

        boolean canUpdateAll = hasAnyAuthority(request, "ROLE_ADMIN", "ROLE_MANAGER", "TASK_UPDATE_ALL");
        TaskDto updated = taskService.updateProgress(id, req, authEmail, canUpdateAll);
        return ResponseEntity.ok(ApiResponse.ok("Task progress updated successfully.", updated));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<TaskDto>> submitForReview(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {

        String authEmail = getAuthenticatedEmail(request);
        if (authEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required."));
        }

        String notes = body != null ? body.get("notes") : null;
        boolean canUpdateAll = hasAnyAuthority(request, "ROLE_ADMIN", "ROLE_MANAGER", "TASK_UPDATE_ALL");
        TaskDto submitted = taskService.submitForReview(id, notes, authEmail, canUpdateAll);
        return ResponseEntity.ok(ApiResponse.ok("Task submitted for manager review.", submitted));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<ApiResponse<TaskDto>> reviewTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskReviewRequest req,
            HttpServletRequest request) {

        String authEmail = getAuthenticatedEmail(request);
        if (authEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required."));
        }

        if (!hasAnyAuthority(request, "ROLE_ADMIN", "ROLE_MANAGER", "TASK_REVIEW")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You do not have permission to review tasks."));
        }

        TaskDto reviewed = taskService.reviewTask(id, req, authEmail);
        String actionName = "APPROVE".equalsIgnoreCase(req.getAction()) ? "approved" : "updated with revision requests";
        return ResponseEntity.ok(ApiResponse.ok("Task successfully " + actionName + ".", reviewed));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskDto>> updateTask(
            @PathVariable Long id,
            @RequestBody TaskUpdateRequest req,
            HttpServletRequest request) {

        String authEmail = getAuthenticatedEmail(request);
        if (authEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required."));
        }

        if (!hasAnyAuthority(request, "ROLE_ADMIN", "ROLE_MANAGER", "TASK_UPDATE_ALL")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You do not have permission to modify task settings."));
        }

        TaskDto updated = taskService.updateTask(id, req, authEmail);
        return ResponseEntity.ok(ApiResponse.ok("Task updated successfully.", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @PathVariable Long id,
            HttpServletRequest request) {

        String authEmail = getAuthenticatedEmail(request);
        if (authEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required."));
        }

        if (!hasAnyAuthority(request, "ROLE_ADMIN", "ROLE_MANAGER", "TASK_DELETE")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You do not have permission to delete tasks."));
        }

        taskService.deleteTask(id, authEmail);
        return ResponseEntity.ok(ApiResponse.ok("Task deleted successfully."));
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<TaskAnalyticsDto>> getAnalytics(
            @RequestParam(required = false) String department,
            HttpServletRequest request) {

        String authEmail = getAuthenticatedEmail(request);
        if (authEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required."));
        }

        if (!hasAnyAuthority(request, "ROLE_ADMIN", "ROLE_MANAGER", "ROLE_HR_MANAGER", "ROLE_VIEWER", "TASK_ANALYTICS_VIEW")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You do not have permission to view task analytics."));
        }

        TaskAnalyticsDto analytics = taskService.getAnalytics(authEmail, department);
        return ResponseEntity.ok(ApiResponse.ok("Task analytics metrics calculated.", analytics));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<TaskProgressDto>>> getTaskHistory(
            @PathVariable Long id,
            HttpServletRequest request) {

        String authEmail = getAuthenticatedEmail(request);
        if (authEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required."));
        }

        boolean canViewAll = hasAnyAuthority(request, "ROLE_ADMIN", "ROLE_MANAGER", "ROLE_HR_MANAGER", "ROLE_VIEWER", "TASK_VIEW_ALL");
        List<TaskProgressDto> history = taskService.getTaskHistory(id, authEmail, canViewAll);
        return ResponseEntity.ok(ApiResponse.ok("Task progress history fetched.", history));
    }

    private String getAuthenticatedEmail(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("CURRENT_USER") != null) {
            User sessionUser = (User) session.getAttribute("CURRENT_USER");
            return sessionUser.getEmail();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return null;
    }

    private User getAuthenticatedUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("CURRENT_USER") != null) {
            return (User) session.getAttribute("CURRENT_USER");
        }
        String email = getAuthenticatedEmail(request);
        if (email != null) {
            return userRepository.findByEmailIgnoreCase(email).orElse(null);
        }
        return null;
    }

    private boolean hasAnyAuthority(HttpServletRequest request, String... authoritiesToCheck) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            Set<String> granted = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());
            for (String authName : authoritiesToCheck) {
                if (granted.contains(authName)) {
                    return true;
                }
            }
        }
        User u = getAuthenticatedUser(request);
        if (u != null) {
            if (u.isAdmin()) return true;
            for (String authName : authoritiesToCheck) {
                if (authName.equalsIgnoreCase(u.getRole())) return true;
                if (u.hasPermission(authName)) return true;
            }
        }
        return false;
    }
}
