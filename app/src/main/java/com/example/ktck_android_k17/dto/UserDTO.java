package com.example.ktck_android_k17.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object cho User.
 * Dùng để truyền dữ liệu giữa các tầng mà không chứa thông tin nhạy cảm
 * (password).
 */

public class UserDTO {
    private int id;
    private String username;
    private String email;
    private String createdAt;

    // Default constructor
    public UserDTO() {
    }

    // Constructor with all fields
    public UserDTO(int id, String username, String email, String createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.createdAt = createdAt;
    }

    // Constructor without createdAt
    public UserDTO(int id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "UserDTO{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
