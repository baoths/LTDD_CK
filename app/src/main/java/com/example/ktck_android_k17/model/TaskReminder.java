package com.example.ktck_android_k17.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Entity class đại diện cho bảng task_reminders trong database.
 * Lưu thông tin nhắc nhở cho công việc.
 */
@Getter
@Setter
public class TaskReminder {
    private int id;
    private int taskId;
    private String reminderTime; // DateTime format: yyyy-MM-dd HH:mm:ss
    private String type; // notification, email, etc.
    private boolean isSent;
    private String createdAt;

    // Reminder types
    public static final String TYPE_NOTIFICATION = "notification";
    public static final String TYPE_EMAIL = "email";
    public static final String TYPE_SMS = "sms";

    // Default constructor
    public TaskReminder() {
    }

    // Constructor with all fields
    public TaskReminder(int id, int taskId, String reminderTime, String type, boolean isSent, String createdAt) {
        this.id = id;
        this.taskId = taskId;
        this.reminderTime = reminderTime;
        this.type = type;
        this.isSent = isSent;
        this.createdAt = createdAt;
    }

    // Constructor for creating new reminder
    public TaskReminder(int taskId, String reminderTime, String type) {
        this.taskId = taskId;
        this.reminderTime = reminderTime;
        this.type = type;
        this.isSent = false;
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

    public String getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(String reminderTime) {
        this.reminderTime = reminderTime;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isSent() {
        return isSent;
    }

    public void setSent(boolean sent) {
        isSent = sent;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "TaskReminder{" +
                "id=" + id +
                ", taskId=" + taskId +
                ", reminderTime='" + reminderTime + '\'' +
                ", type='" + type + '\'' +
                ", isSent=" + isSent +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}
