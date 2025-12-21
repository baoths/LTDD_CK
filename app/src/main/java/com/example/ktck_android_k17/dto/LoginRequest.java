package com.example.ktck_android_k17.dto;

/**
 * Data Transfer Object cho Login request.
 * Chứa thông tin cần thiết để đăng nhập.
 */
public class LoginRequest {
    private String email;
    private String password;

    // Default constructor
    public LoginRequest() {
    }

    // Constructor with all fields
    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // Getters and Setters
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

    /**
     * Validate login request data
     * 
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return email != null && !email.trim().isEmpty() &&
                password != null && !password.trim().isEmpty();
    }

    /**
     * Validate email format
     * 
     * @return true if email format is valid
     */
    public boolean isEmailValid() {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    @Override
    public String toString() {
        return "LoginRequest{" +
                "email='" + email + '\'' +
                '}';
    }
}
