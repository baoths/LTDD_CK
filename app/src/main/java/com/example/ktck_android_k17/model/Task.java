package com.example.ktck_android_k17.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Entity class đại diện cho bảng tasks trong database.
 * Ánh xạ trực tiếp với cấu trúc bảng MySQL.
 */
@Getter
@Setter
public class Task {
    private int id;
    private int userId;
    private int categoryId; // Liên kết với category
    private String title;
    private String description;
    private String status; // pending, in_progress, completed
    private String priority; // low, medium, high
    private String dueDate;
    private String startDate; // Start date for recurring tasks (yyyy-MM-dd)
    private String endDate; // End date for recurring tasks (yyyy-MM-dd)
    private Integer parentTaskId; // ID of master task (NULL if master, has value if instance)
    private Boolean isMaster; // TRUE if this is a master/recurring task, FALSE if instance
    private String occurrenceDate; // Date of occurrence for this instance (NULL if master)
    private String createdAt;

    // Status constants
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_IN_PROGRESS = "in_progress";
    public static final String STATUS_COMPLETED = "completed";

    // Priority constants
    public static final String PRIORITY_LOW = "low";
    public static final String PRIORITY_MEDIUM = "medium";
    public static final String PRIORITY_HIGH = "high";

    // Default constructor
    public Task() {
    }

    // Constructor with all fields
    public Task(int id, int userId, String title, String description,
            String status, String dueDate, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.dueDate = dueDate;
        this.createdAt = createdAt;
    }

    // Constructor for creating new task
    public Task(int userId, String title, String description, String dueDate) {
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.status = STATUS_PENDING;
        this.dueDate = dueDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Integer getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(Integer parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    public Boolean getIsMaster() {
        return isMaster != null ? isMaster : false;
    }

    public void setIsMaster(Boolean isMaster) {
        this.isMaster = isMaster;
    }

    public String getOccurrenceDate() {
        return occurrenceDate;
    }

    public void setOccurrenceDate(String occurrenceDate) {
        this.occurrenceDate = occurrenceDate;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", userId=" + userId +
                ", title='" + title + '\'' +
                ", status='" + status + '\'' +
                ", dueDate='" + dueDate + '\'' +
                '}';
    }

}
