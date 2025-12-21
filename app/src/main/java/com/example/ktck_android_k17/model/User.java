package com.example.ktck_android_k17.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Entity class đại diện cho bảng users trong database.
 * Ánh xạ trực tiếp với cấu trúc bảng MySQL.
 */

public class User {
    private int id;
    private String username;
    private String email;
    private String password;
    private String createdAt;

    // Default constructor
    public User() {
    }

    // Constructor with all fields
    public User(int id, String username, String email, String password, String createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.createdAt = createdAt;
    }

    // Constructor for creating new user (without id and createdAt)
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}
