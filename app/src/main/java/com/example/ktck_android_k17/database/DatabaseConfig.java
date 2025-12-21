package com.example.ktck_android_k17.database;

/**
 * Configuration class cho kết nối MySQL.
 * Chứa các thông tin cấu hình kết nối database.
 */
public class DatabaseConfig {

    // Địa chỉ MySQL server
    // 10.0.2.2 là địa chỉ localhost từ Android Emulator
    // Thay đổi thành IP thực của MySQL server khi chạy trên thiết bị thật
    public static final String DB_HOST = "10.0.2.2";

    // Port mặc định của MySQL
    public static final String DB_PORT = "3306";

    // Tên database
    public static final String DB_NAME = "task_manager";

    // Thông tin đăng nhập MySQL
    public static final String DB_USER = "root";
    public static final String DB_PASSWORD = "123456";

    // JDBC URL
    public static final String JDBC_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;

    // JDBC Driver class
    public static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";

    // Connection timeout (milliseconds)
    public static final int CONNECTION_TIMEOUT = 5000;

    /**
     * Lấy JDBC URL đầy đủ với các tham số.
     * 
     * @return JDBC URL string
     */
    public static String getJdbcUrl() {
        return JDBC_URL + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }
}
