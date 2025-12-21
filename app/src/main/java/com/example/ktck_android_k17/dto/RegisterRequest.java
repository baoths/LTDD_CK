package com.example.ktck_android_k17.dto;

/**
 * Data Transfer Object cho Register request.
 * Chứa thông tin cần thiết để đăng ký tài khoản mới.
 */
public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private String confirmPassword;

    // Default constructor
    public RegisterRequest() {
    }

    // Constructor with all fields
    public RegisterRequest(String username, String email, String password, String confirmPassword) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
    }

    // Getters and Setters
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

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    /**
     * Validate all register request data
     * 
     * @return true if all fields are valid
     */
    public boolean isValid() {
        return isUsernameValid() && isEmailValid() && isPasswordValid() && isPasswordMatch();
    }

    /**
     * Validate username
     * 
     * @return true if username is not empty
     */
    public boolean isUsernameValid() {
        return username != null && !username.trim().isEmpty() && username.length() >= 3;
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

    /**
     * Validate password length
     * 
     * @return true if password has at least 6 characters
     */
    public boolean isPasswordValid() {
        return password != null && password.length() >= 6;
    }

    /**
     * Check if password and confirm password match
     * 
     * @return true if passwords match
     */
    public boolean isPasswordMatch() {
        return password != null && password.equals(confirmPassword);
    }

    @Override
    public String toString() {
        return "RegisterRequest{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
