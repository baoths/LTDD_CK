package com.example.ktck_android_k17.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Entity class đại diện cho bảng categories trong database.
 * Dùng để phân loại công việc.
 */
@Getter
@Setter
public class Category {
    private int id;
    private int userId;
    private String name;
    private String description;
    private String color; // Hex color code (e.g., #3498db)
    private String createdAt;

    // Default constructor
    public Category() {
    }

    // Constructor with all fields
    public Category(int id, int userId, String name, String description, String color, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.color = color;
        this.createdAt = createdAt;
    }

    // Constructor for creating new category
    public Category(int userId, String name, String description, String color) {
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.color = color;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", userId=" + userId +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", color='" + color + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}
