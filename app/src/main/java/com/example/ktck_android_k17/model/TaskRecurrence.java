package com.example.ktck_android_k17.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Entity class đại diện cho bảng task_recurrence trong database.
 * Dùng để lưu thông tin về việc lặp lại của task.
 */
@Getter
@Setter
public class TaskRecurrence {
    private int id;
    private int taskId;
    private String recurrenceType; // 'daily', 'weekly', 'monthly', 'custom'
    private int recurrenceInterval; // e.g., every 2 days, every 3 weeks
    private String recurrenceDays; // For weekly: '1,3,5' (Mon,Wed,Fri)
    private Integer recurrenceDayOfMonth; // For monthly: day 1-31
    private String recurrenceEndDate; // Optional end date (yyyy-MM-dd)
    private Integer recurrenceCount; // Optional: number of occurrences
    private String lastGeneratedDate; // Track last time task was generated (yyyy-MM-dd)
    private boolean isActive;
    private String createdAt;

    // Recurrence type constants
    public static final String TYPE_DAILY = "daily";
    public static final String TYPE_WEEKLY = "weekly";
    public static final String TYPE_MONTHLY = "monthly";
    public static final String TYPE_CUSTOM = "custom";

    // Default constructor
    public TaskRecurrence() {
        this.isActive = true;
        this.recurrenceInterval = 1;
    }

    // Constructor with all fields
    public TaskRecurrence(int id, int taskId, String recurrenceType, int recurrenceInterval,
                         String recurrenceDays, Integer recurrenceDayOfMonth, String recurrenceEndDate,
                         Integer recurrenceCount, String lastGeneratedDate, boolean isActive, String createdAt) {
        this.id = id;
        this.taskId = taskId;
        this.recurrenceType = recurrenceType;
        this.recurrenceInterval = recurrenceInterval;
        this.recurrenceDays = recurrenceDays;
        this.recurrenceDayOfMonth = recurrenceDayOfMonth;
        this.recurrenceEndDate = recurrenceEndDate;
        this.recurrenceCount = recurrenceCount;
        this.lastGeneratedDate = lastGeneratedDate;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    // Constructor for creating new recurrence
    public TaskRecurrence(int taskId, String recurrenceType, int recurrenceInterval) {
        this.taskId = taskId;
        this.recurrenceType = recurrenceType;
        this.recurrenceInterval = recurrenceInterval;
        this.isActive = true;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public String getRecurrenceType() {
        return recurrenceType;
    }

    public void setRecurrenceType(String recurrenceType) {
        this.recurrenceType = recurrenceType;
    }

    public int getRecurrenceInterval() {
        return recurrenceInterval;
    }

    public void setRecurrenceInterval(int recurrenceInterval) {
        this.recurrenceInterval = recurrenceInterval;
    }

    public String getRecurrenceDays() {
        return recurrenceDays;
    }

    public void setRecurrenceDays(String recurrenceDays) {
        this.recurrenceDays = recurrenceDays;
    }

    public Integer getRecurrenceDayOfMonth() {
        return recurrenceDayOfMonth;
    }

    public void setRecurrenceDayOfMonth(Integer recurrenceDayOfMonth) {
        this.recurrenceDayOfMonth = recurrenceDayOfMonth;
    }

    public String getRecurrenceEndDate() {
        return recurrenceEndDate;
    }

    public void setRecurrenceEndDate(String recurrenceEndDate) {
        this.recurrenceEndDate = recurrenceEndDate;
    }

    public Integer getRecurrenceCount() {
        return recurrenceCount;
    }

    public void setRecurrenceCount(Integer recurrenceCount) {
        this.recurrenceCount = recurrenceCount;
    }

    public String getLastGeneratedDate() {
        return lastGeneratedDate;
    }

    public void setLastGeneratedDate(String lastGeneratedDate) {
        this.lastGeneratedDate = lastGeneratedDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "TaskRecurrence{" +
                "id=" + id +
                ", taskId=" + taskId +
                ", recurrenceType='" + recurrenceType + '\'' +
                ", recurrenceInterval=" + recurrenceInterval +
                ", recurrenceDays='" + recurrenceDays + '\'' +
                ", recurrenceDayOfMonth=" + recurrenceDayOfMonth +
                ", recurrenceEndDate='" + recurrenceEndDate + '\'' +
                ", recurrenceCount=" + recurrenceCount +
                ", lastGeneratedDate='" + lastGeneratedDate + '\'' +
                ", isActive=" + isActive +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}

