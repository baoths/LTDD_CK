package com.example.ktck_android_k17.dto;

import java.util.List;

/**
 * Data Transfer Object cho Task.
 * Dùng để truyền dữ liệu Task giữa các tầng, có thể bao gồm thông tin bổ sung.
 */

public class TaskDTO {
    private int id;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String dueDate;
    private String startDate; // Start date for recurring tasks
    private String endDate; // End date for recurring tasks
    private String createdAt;
    private String ownerName; // Thông tin bổ sung: tên người sở hữu task
    private int categoryId; // ID của category
    private String categoryName; // Tên category
    private List<String> tags; // Danh sách tags
    private String reminderTime; // Thời gian nhắc nhở

    // Default constructor
    public TaskDTO() {
    }

    // Constructor with all fields
    public TaskDTO(int id, String title, String description, String status,
            String priority, String dueDate, String createdAt, String ownerName) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.createdAt = createdAt;
        this.ownerName = ownerName;
    }

    // Constructor with all fields including category, tags, reminder
    public TaskDTO(int id, String title, String description, String status,
            String priority, String dueDate, String createdAt, String ownerName,
            int categoryId, String categoryName, List<String> tags, String reminderTime) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.createdAt = createdAt;
        this.ownerName = ownerName;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.tags = tags;
        this.reminderTime = reminderTime;
    }

    // Constructor for basic task info
    public TaskDTO(int id, String title, String description, String status, String priority, String dueDate) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(String reminderTime) {
        this.reminderTime = reminderTime;
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

    @Override
    public String toString() {
        return "TaskDTO{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", status='" + status + '\'' +
                ", dueDate='" + dueDate + '\'' +
                '}';
    }
}
