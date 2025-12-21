package com.example.ktck_android_k17.dto;

/**
 * Generic API Response wrapper.
 * Dùng để trả về kết quả từ các thao tác database.
 *
 * @param <T> Kiểu dữ liệu của data trả về
 */
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    // Default constructor
    public ApiResponse() {
    }

    // Constructor for success/failure without data
    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Constructor with all fields
    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // Static factory methods for common responses
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message);
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "ApiResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
