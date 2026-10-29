package com.empmanage.cms.entity;

public enum TaskStatus {
    TO_DO,
    IN_PROGRESS,
    IN_REVIEW,
    COMPLETED,
    CANCELLED;

    public static TaskStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return TO_DO;
        }
        try {
            return TaskStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TO_DO;
        }
    }
}
