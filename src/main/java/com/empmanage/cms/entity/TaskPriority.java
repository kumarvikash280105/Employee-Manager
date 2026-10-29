package com.empmanage.cms.entity;

public enum TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT;

    public static TaskPriority fromString(String value) {
        if (value == null || value.isBlank()) {
            return MEDIUM;
        }
        try {
            return TaskPriority.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEDIUM;
        }
    }
}
