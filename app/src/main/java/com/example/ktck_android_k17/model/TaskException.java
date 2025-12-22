package com.example.ktck_android_k17.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Entity class đại diện cho bảng task_exceptions trong database.
 * Lưu các instance bị chỉnh sửa riêng (exception) của recurring task.
 */
@Getter
@Setter
public class TaskException {
    private int id;
    private int masterTaskId;
    private String originalOccurrenceDate; // Date của occurrence bị exception (yyyy-MM-dd)
    private String exceptionType; // 'modified' hoặc 'deleted'
    private Integer modifiedTaskId; // NULL nếu deleted, có giá trị nếu modified
    private String createdAt;

    // Exception type constants
    public static final String TYPE_MODIFIED = "modified";
    public static final String TYPE_DELETED = "deleted";

    // Default constructor
    public TaskException() {
    }

    // Constructor with all fields
    public TaskException(int id, int masterTaskId, String originalOccurrenceDate,
                        String exceptionType, Integer modifiedTaskId, String createdAt) {
        this.id = id;
        this.masterTaskId = masterTaskId;
        this.originalOccurrenceDate = originalOccurrenceDate;
        this.exceptionType = exceptionType;
        this.modifiedTaskId = modifiedTaskId;
        this.createdAt = createdAt;
    }

    // Constructor for creating new exception
    public TaskException(int masterTaskId, String originalOccurrenceDate, String exceptionType) {
        this.masterTaskId = masterTaskId;
        this.originalOccurrenceDate = originalOccurrenceDate;
        this.exceptionType = exceptionType;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMasterTaskId() {
        return masterTaskId;
    }

    public void setMasterTaskId(int masterTaskId) {
        this.masterTaskId = masterTaskId;
    }

    public String getOriginalOccurrenceDate() {
        return originalOccurrenceDate;
    }

    public void setOriginalOccurrenceDate(String originalOccurrenceDate) {
        this.originalOccurrenceDate = originalOccurrenceDate;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public Integer getModifiedTaskId() {
        return modifiedTaskId;
    }

    public void setModifiedTaskId(Integer modifiedTaskId) {
        this.modifiedTaskId = modifiedTaskId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "TaskException{" +
                "id=" + id +
                ", masterTaskId=" + masterTaskId +
                ", originalOccurrenceDate='" + originalOccurrenceDate + '\'' +
                ", exceptionType='" + exceptionType + '\'' +
                ", modifiedTaskId=" + modifiedTaskId +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}

