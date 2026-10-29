package com.empmanage.cms.dto;

import java.util.HashMap;
import java.util.Map;

public class TaskAnalyticsDto {

    private long totalTasks;
    private long toDoTasks;
    private long inProgressTasks;
    private long inReviewTasks;
    private long completedTasks;
    private long cancelledTasks;
    private long overdueTasks;
    private double completionRate;

    private Map<String, Long> statusBreakdown = new HashMap<>();
    private Map<String, Long> priorityBreakdown = new HashMap<>();
    private Map<String, Map<String, Object>> departmentBreakdown = new HashMap<>();

    public TaskAnalyticsDto() {}

    public long getTotalTasks() { return totalTasks; }
    public void setTotalTasks(long totalTasks) { this.totalTasks = totalTasks; }

    public long getToDoTasks() { return toDoTasks; }
    public void setToDoTasks(long toDoTasks) { this.toDoTasks = toDoTasks; }

    public long getInProgressTasks() { return inProgressTasks; }
    public void setInProgressTasks(long inProgressTasks) { this.inProgressTasks = inProgressTasks; }

    public long getInReviewTasks() { return inReviewTasks; }
    public void setInReviewTasks(long inReviewTasks) { this.inReviewTasks = inReviewTasks; }

    public long getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(long completedTasks) { this.completedTasks = completedTasks; }

    public long getCancelledTasks() { return cancelledTasks; }
    public void setCancelledTasks(long cancelledTasks) { this.cancelledTasks = cancelledTasks; }

    public long getOverdueTasks() { return overdueTasks; }
    public void setOverdueTasks(long overdueTasks) { this.overdueTasks = overdueTasks; }

    public double getCompletionRate() { return completionRate; }
    public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }

    public Map<String, Long> getStatusBreakdown() { return statusBreakdown; }
    public void setStatusBreakdown(Map<String, Long> statusBreakdown) { this.statusBreakdown = statusBreakdown; }

    public Map<String, Long> getPriorityBreakdown() { return priorityBreakdown; }
    public void setPriorityBreakdown(Map<String, Long> priorityBreakdown) { this.priorityBreakdown = priorityBreakdown; }

    public Map<String, Map<String, Object>> getDepartmentBreakdown() { return departmentBreakdown; }
    public void setDepartmentBreakdown(Map<String, Map<String, Object>> departmentBreakdown) { this.departmentBreakdown = departmentBreakdown; }
}
