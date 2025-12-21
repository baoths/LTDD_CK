package com.example.ktck_android_k17.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Entity class đại diện cho bảng task_tags trong database.
 * Dùng để gắn nhãn (tags) cho công việc.
 */
@Getter
@Setter
public class TaskTag {
    private int id;
    private int taskId;
    private String tagName;
    private String createdAt;

    // Common tags
    public static final String TAG_URGENT = "urgent";
    public static final String TAG_IMPORTANT = "important";
    public static final String TAG_BUG = "bug";
    public static final String TAG_FEATURE = "feature";
    public static final String TAG_DOCUMENTATION = "documentation";
    public static final String TAG_REVIEW = "review";

    // Default constructor
    public TaskTag() {
    }

    // Constructor with all fields
    public TaskTag(int id, int taskId, String tagName, String createdAt) {
        this.id = id;
        this.taskId = taskId;
        this.tagName = tagName;
        this.createdAt = createdAt;
    }

    // Constructor for creating new tag
    public TaskTag(int taskId, String tagName) {
        this.taskId = taskId;
        this.tagName = tagName;
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

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "TaskTag{" +
                "id=" + id +
                ", taskId=" + taskId +
                ", tagName='" + tagName + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}
