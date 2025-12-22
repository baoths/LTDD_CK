package com.example.ktck_android_k17.database;

import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton class quản lý kết nối MySQL.
 * Cung cấp phương thức để lấy và đóng kết nối database.
 */
public class DatabaseHelper {

    private static final String TAG = "DatabaseHelper";
    private static DatabaseHelper instance;
    private Connection connection;

    // Private constructor để đảm bảo Singleton
    private DatabaseHelper() {
    }

    /**
     * Lấy instance duy nhất của DatabaseHelper.
     * 
     * @return DatabaseHelper instance
     */
    public static synchronized DatabaseHelper getInstance() {
        if (instance == null) {
            instance = new DatabaseHelper();
        }
        return instance;
    }

    /**
     * Lấy kết nối đến MySQL database.
     * Nếu chưa có kết nối hoặc kết nối đã đóng, tạo kết nối mới.
     * 
     * @return Connection object hoặc null nếu lỗi
     */
    public Connection getConnection() {
        try {
            // Kiểm tra nếu kết nối hiện tại còn valid
            if (connection != null && !connection.isClosed()) {
                return connection;
            }

            // Load JDBC driver với error handling tốt hơn
            try {
                Class.forName(DatabaseConfig.JDBC_DRIVER);
                Log.d(TAG, "JDBC Driver loaded successfully");
            } catch (ClassNotFoundException e) {
                // Thử load driver mới hơn nếu driver cũ không tìm thấy
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    Log.d(TAG, "JDBC Driver (cj) loaded successfully");
                } catch (ClassNotFoundException e2) {
                    Log.e(TAG, "Không tìm thấy JDBC Driver: " + e.getMessage());
                    Log.e(TAG, "Cũng không tìm thấy cj driver: " + e2.getMessage());
                    e.printStackTrace();
                    e2.printStackTrace();
                    return null;
                }
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi load JDBC Driver: " + e.getMessage());
                e.printStackTrace();
                return null;
            }

            // Tạo kết nối mới
            connection = DriverManager.getConnection(
                    DatabaseConfig.getJdbcUrl(),
                    DatabaseConfig.DB_USER,
                    DatabaseConfig.DB_PASSWORD);

            Log.d(TAG, "Kết nối MySQL thành công!");
            return connection;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi kết nối MySQL: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Lỗi không mong đợi khi kết nối database: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Đóng kết nối database.
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                Log.d(TAG, "Đã đóng kết nối MySQL");
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi khi đóng kết nối: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Kiểm tra kết nối có hoạt động không.
     * 
     * @return true nếu kết nối OK
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Test kết nối đến database.
     * 
     * @return true nếu kết nối thành công
     */
    public boolean testConnection() {
        Connection conn = getConnection();
        if (conn != null) {
            try {
                return conn.isValid(DatabaseConfig.CONNECTION_TIMEOUT / 1000);
            } catch (SQLException e) {
                Log.e(TAG, "Lỗi test kết nối: " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    /**
     * Khởi tạo database: tạo các bảng nếu chưa tồn tại.
     * Phương thức này nên được gọi khi app khởi động.
     */
    public void initializeDatabase() {
        Log.d(TAG, "Bắt đầu khởi tạo database...");
        Connection conn = getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database để khởi tạo");
            return;
        }

        try {
            // Đảm bảo auto-commit được bật
            conn.setAutoCommit(true);
            Log.d(TAG, "Auto-commit đã được bật");

            // Tạo bảng users nếu chưa tồn tại (phải tạo đầu tiên vì các bảng khác phụ thuộc)
            Log.d(TAG, "Đang tạo bảng users...");
            createUsersTableIfNotExists(conn);
            verifyTableExists(conn, "users");
            Log.d(TAG, "✓ Bảng users OK");

            // Tạo bảng categories nếu chưa tồn tại (phải tạo trước tasks)
            Log.d(TAG, "Đang tạo bảng categories...");
            createCategoriesTableIfNotExists(conn);
            verifyTableExists(conn, "categories");
            Log.d(TAG, "✓ Bảng categories OK");

            // Tạo bảng tasks nếu chưa tồn tại (phải tạo trước task_reminders và task_tags)
            Log.d(TAG, "Đang tạo bảng tasks...");
            createTasksTableIfNotExists(conn);
            verifyTableExists(conn, "tasks");
            // Kiểm tra và thêm cột category_id nếu chưa có (migration)
            migrateTasksTableIfNeeded(conn);
            // Kiểm tra và thêm cột start_date và end_date nếu chưa có (migration)
            migrateTasksTableAddStartEndDate(conn);
            Log.d(TAG, "✓ Bảng tasks OK");

            // Tạo bảng task_reminders nếu chưa tồn tại
            Log.d(TAG, "Đang tạo bảng task_reminders...");
            createTaskRemindersTableIfNotExists(conn);
            verifyTableExists(conn, "task_reminders");
            Log.d(TAG, "✓ Bảng task_reminders OK");

            // Tạo bảng task_tags nếu chưa tồn tại
            Log.d(TAG, "Đang tạo bảng task_tags...");
            createTaskTagsTableIfNotExists(conn);
            verifyTableExists(conn, "task_tags");
            Log.d(TAG, "✓ Bảng task_tags OK");

            // Tạo bảng task_recurrence nếu chưa tồn tại
            Log.d(TAG, "Đang tạo bảng task_recurrence...");
            createTaskRecurrenceTableIfNotExists(conn);
            verifyTableExists(conn, "task_recurrence");
            Log.d(TAG, "✓ Bảng task_recurrence OK");

            // Tạo bảng task_exceptions nếu chưa tồn tại
            Log.d(TAG, "Đang tạo bảng task_exceptions...");
            createTaskExceptionsTableIfNotExists(conn);
            verifyTableExists(conn, "task_exceptions");
            Log.d(TAG, "✓ Bảng task_exceptions OK");

            // Migration: Thêm các cột mới vào bảng tasks
            migrateTasksTableAddRecurrenceFields(conn);

            // Run recurrence migration (one-time, checks if needed)
            runRecurrenceMigrationIfNeeded(conn);

            Log.d(TAG, "✓✓✓ Tất cả các bảng đã được khởi tạo thành công! ✓✓✓");

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi khởi tạo database: " + e.getMessage());
            Log.e(TAG, "SQL State: " + e.getSQLState());
            Log.e(TAG, "Error Code: " + e.getErrorCode());
            e.printStackTrace();
        } catch (Exception e) {
            Log.e(TAG, "Lỗi không mong đợi khi khởi tạo database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Kiểm tra xem bảng có tồn tại trong database không.
     */
    private void verifyTableExists(Connection conn, String tableName) {
        try {
            String sql = "SHOW TABLES LIKE ?";
            try (java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, tableName);
                try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Log.d(TAG, "✓ Bảng '" + tableName + "' đã tồn tại trong database");
                    } else {
                        Log.w(TAG, "⚠ CẢNH BÁO: Bảng '" + tableName + "' không tồn tại sau khi tạo!");
                    }
                }
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi khi kiểm tra bảng '" + tableName + "': " + e.getMessage());
        }
    }

    /**
     * Tạo bảng users nếu chưa tồn tại.
     */
    private void createUsersTableIfNotExists(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "username VARCHAR(100) UNIQUE NOT NULL," +
                "email VARCHAR(150) UNIQUE NOT NULL," +
                "password VARCHAR(255) NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        try (java.sql.Statement stmt = conn.createStatement()) {
            boolean result = stmt.execute(sql);
            Log.d(TAG, "CREATE TABLE users executed. Result: " + result);
            Log.d(TAG, "Bảng users đã được tạo hoặc đã tồn tại");
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi khi tạo bảng users: " + e.getMessage());
            Log.e(TAG, "SQL State: " + e.getSQLState());
            throw e;
        }
    }

    /**
     * Tạo bảng tasks nếu chưa tồn tại.
     */
    private void createTasksTableIfNotExists(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS tasks (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "user_id INT NOT NULL," +
                "category_id INT," +
                "title VARCHAR(255) NOT NULL," +
                "description LONGTEXT," +
                "status VARCHAR(50) DEFAULT 'pending'," +
                "priority VARCHAR(50) DEFAULT 'medium'," +
                "due_date DATE," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE," +
                "FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL" +
                ")";

        try (java.sql.Statement stmt = conn.createStatement()) {
            boolean result = stmt.execute(sql);
            Log.d(TAG, "CREATE TABLE tasks executed. Result: " + result);
            Log.d(TAG, "Bảng tasks đã được tạo hoặc đã tồn tại");
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi khi tạo bảng tasks: " + e.getMessage());
            Log.e(TAG, "SQL State: " + e.getSQLState());
            throw e;
        }
    }

    /**
     * Tạo bảng categories nếu chưa tồn tại.
     */
    private void createCategoriesTableIfNotExists(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS categories (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "user_id INT NOT NULL," +
                "name VARCHAR(100) NOT NULL," +
                "description VARCHAR(255)," +
                "color VARCHAR(7) DEFAULT '#3498db'," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "UNIQUE KEY unique_category (user_id, name)," +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")";

        try (java.sql.Statement stmt = conn.createStatement()) {
            boolean result = stmt.execute(sql);
            Log.d(TAG, "CREATE TABLE categories executed. Result: " + result);
            Log.d(TAG, "Bảng categories đã được tạo hoặc đã tồn tại");
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi khi tạo bảng categories: " + e.getMessage());
            Log.e(TAG, "SQL State: " + e.getSQLState());
            throw e;
        }
    }

    /**
     * Tạo bảng task_reminders nếu chưa tồn tại.
     */
    private void createTaskRemindersTableIfNotExists(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS task_reminders (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "task_id INT NOT NULL," +
                "reminder_time DATETIME NOT NULL," +
                "type VARCHAR(50) DEFAULT 'notification'," +
                "is_sent BOOLEAN DEFAULT FALSE," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE" +
                ")";

        try (java.sql.Statement stmt = conn.createStatement()) {
            boolean result = stmt.execute(sql);
            Log.d(TAG, "CREATE TABLE task_reminders executed. Result: " + result);
            Log.d(TAG, "Bảng task_reminders đã được tạo hoặc đã tồn tại");
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi khi tạo bảng task_reminders: " + e.getMessage());
            Log.e(TAG, "SQL State: " + e.getSQLState());
            throw e;
        }
    }

    /**
     * Tạo bảng task_tags nếu chưa tồn tại.
     */
    private void createTaskTagsTableIfNotExists(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS task_tags (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "task_id INT NOT NULL," +
                "tag_name VARCHAR(50) NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "UNIQUE KEY unique_task_tag (task_id, tag_name)," +
                "FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE" +
                ")";

        try (java.sql.Statement stmt = conn.createStatement()) {
            boolean result = stmt.execute(sql);
            Log.d(TAG, "CREATE TABLE task_tags executed. Result: " + result);
            Log.d(TAG, "Bảng task_tags đã được tạo hoặc đã tồn tại");
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi khi tạo bảng task_tags: " + e.getMessage());
            Log.e(TAG, "SQL State: " + e.getSQLState());
            throw e;
        }
    }

    /**
     * Tạo bảng task_recurrence nếu chưa tồn tại.
     */
    private void createTaskRecurrenceTableIfNotExists(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS task_recurrence (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "task_id INT NOT NULL," +
                "recurrence_type VARCHAR(50) NOT NULL," +
                "recurrence_interval INT DEFAULT 1," +
                "recurrence_days VARCHAR(20)," +
                "recurrence_day_of_month INT," +
                "recurrence_end_date DATE," +
                "recurrence_count INT," +
                "last_generated_date DATE," +
                "is_active BOOLEAN DEFAULT TRUE," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE" +
                ")";

        try (java.sql.Statement stmt = conn.createStatement()) {
            boolean result = stmt.execute(sql);
            Log.d(TAG, "CREATE TABLE task_recurrence executed. Result: " + result);
            Log.d(TAG, "Bảng task_recurrence đã được tạo hoặc đã tồn tại");
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi khi tạo bảng task_recurrence: " + e.getMessage());
            Log.e(TAG, "SQL State: " + e.getSQLState());
            throw e;
        }
    }

    /**
     * Migration: Kiểm tra và thêm các cột cần thiết vào bảng tasks nếu chưa có.
     * Phương thức này xử lý trường hợp bảng đã tồn tại nhưng thiếu các cột mới.
     */
    private void migrateTasksTableIfNeeded(Connection conn) {
        try {
            // Kiểm tra xem cột category_id đã tồn tại chưa
            String checkColumnSql = "SELECT COUNT(*) as count FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() " +
                    "AND TABLE_NAME = 'tasks' " +
                    "AND COLUMN_NAME = 'category_id'";

            try (java.sql.Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery(checkColumnSql)) {
                
                if (rs.next() && rs.getInt("count") == 0) {
                    // Cột category_id chưa tồn tại, cần thêm vào
                    Log.d(TAG, "⚠ Cột category_id chưa tồn tại, đang thêm vào bảng tasks...");
                    
                    // Thêm cột category_id
                    String alterSql = "ALTER TABLE tasks ADD COLUMN category_id INT";
                    try (java.sql.Statement alterStmt = conn.createStatement()) {
                        alterStmt.execute(alterSql);
                        Log.d(TAG, "✓ Đã thêm cột category_id vào bảng tasks");
                    } catch (SQLException alterError) {
                        Log.e(TAG, "Lỗi khi thêm cột category_id: " + alterError.getMessage());
                        Log.e(TAG, "SQL State: " + alterError.getSQLState());
                        Log.e(TAG, "Error Code: " + alterError.getErrorCode());
                        // Tiếp tục thử thêm foreign key dù có lỗi
                    }
                    
                    // Thêm foreign key constraint nếu chưa có
                    try {
                        // Kiểm tra xem foreign key đã tồn tại chưa
                        String checkFkSql = "SELECT COUNT(*) as count FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                                "WHERE TABLE_SCHEMA = DATABASE() " +
                                "AND TABLE_NAME = 'tasks' " +
                                "AND CONSTRAINT_NAME = 'fk_tasks_category'";
                        
                        try (java.sql.Statement checkFkStmt = conn.createStatement();
                             java.sql.ResultSet fkRs = checkFkStmt.executeQuery(checkFkSql)) {
                            
                            if (fkRs.next() && fkRs.getInt("count") == 0) {
                                String addFkSql = "ALTER TABLE tasks " +
                                        "ADD CONSTRAINT fk_tasks_category " +
                                        "FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL";
                                
                                try (java.sql.Statement fkStmt = conn.createStatement()) {
                                    fkStmt.execute(addFkSql);
                                    Log.d(TAG, "✓ Đã thêm foreign key constraint cho category_id");
                                }
                            } else {
                                Log.d(TAG, "Foreign key constraint đã tồn tại, bỏ qua");
                            }
                        }
                    } catch (SQLException fkError) {
                        // Foreign key có thể đã tồn tại hoặc có lỗi khác
                        String errorMsg = fkError.getMessage().toLowerCase();
                        if (errorMsg.contains("duplicate key") || 
                            errorMsg.contains("already exists") ||
                            errorMsg.contains("cannot add foreign key")) {
                            Log.d(TAG, "Foreign key constraint đã tồn tại hoặc không thể thêm: " + fkError.getMessage());
                        } else {
                            Log.w(TAG, "Không thể thêm foreign key constraint: " + fkError.getMessage());
                            Log.w(TAG, "SQL State: " + fkError.getSQLState());
                        }
                    }
                } else {
                    Log.d(TAG, "✓ Cột category_id đã tồn tại trong bảng tasks");
                }
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi khi migration bảng tasks: " + e.getMessage());
            Log.e(TAG, "SQL State: " + e.getSQLState());
            Log.e(TAG, "Error Code: " + e.getErrorCode());
            e.printStackTrace();
            // Không throw exception để không làm gián đoạn quá trình khởi tạo
            // App vẫn có thể hoạt động nếu migration thất bại
        } catch (Exception e) {
            Log.e(TAG, "Lỗi không mong đợi khi migration bảng tasks: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Migration: Thêm cột start_date và end_date vào bảng tasks nếu chưa có.
     */
    private void migrateTasksTableAddStartEndDate(Connection conn) {
        try {
            // Kiểm tra xem cột start_date đã tồn tại chưa
            String checkStartDateSql = "SELECT COUNT(*) as count FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() " +
                    "AND TABLE_NAME = 'tasks' " +
                    "AND COLUMN_NAME = 'start_date'";

            try (java.sql.Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery(checkStartDateSql)) {
                
                if (rs.next() && rs.getInt("count") == 0) {
                    // Cột start_date chưa tồn tại, cần thêm vào
                    Log.d(TAG, "⚠ Cột start_date chưa tồn tại, đang thêm vào bảng tasks...");
                    
                    String alterSql = "ALTER TABLE tasks ADD COLUMN start_date DATE";
                    try (java.sql.Statement alterStmt = conn.createStatement()) {
                        alterStmt.execute(alterSql);
                        Log.d(TAG, "✓ Đã thêm cột start_date vào bảng tasks");
                    } catch (SQLException alterError) {
                        Log.e(TAG, "Lỗi khi thêm cột start_date: " + alterError.getMessage());
                    }
                } else {
                    Log.d(TAG, "✓ Cột start_date đã tồn tại trong bảng tasks");
                }
            }

            // Kiểm tra xem cột end_date đã tồn tại chưa
            String checkEndDateSql = "SELECT COUNT(*) as count FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() " +
                    "AND TABLE_NAME = 'tasks' " +
                    "AND COLUMN_NAME = 'end_date'";

            try (java.sql.Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery(checkEndDateSql)) {
                
                if (rs.next() && rs.getInt("count") == 0) {
                    // Cột end_date chưa tồn tại, cần thêm vào
                    Log.d(TAG, "⚠ Cột end_date chưa tồn tại, đang thêm vào bảng tasks...");
                    
                    String alterSql = "ALTER TABLE tasks ADD COLUMN end_date DATE";
                    try (java.sql.Statement alterStmt = conn.createStatement()) {
                        alterStmt.execute(alterSql);
                        Log.d(TAG, "✓ Đã thêm cột end_date vào bảng tasks");
                    } catch (SQLException alterError) {
                        Log.e(TAG, "Lỗi khi thêm cột end_date: " + alterError.getMessage());
                    }
                } else {
                    Log.d(TAG, "✓ Cột end_date đã tồn tại trong bảng tasks");
                }
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi khi migration bảng tasks (start_date/end_date): " + e.getMessage());
            Log.e(TAG, "SQL State: " + e.getSQLState());
            Log.e(TAG, "Error Code: " + e.getErrorCode());
            e.printStackTrace();
        } catch (Exception e) {
            Log.e(TAG, "Lỗi không mong đợi khi migration bảng tasks (start_date/end_date): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Tạo bảng task_exceptions nếu chưa tồn tại.
     */
    private void createTaskExceptionsTableIfNotExists(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS task_exceptions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "master_task_id INT NOT NULL," +
                "original_occurrence_date DATE NOT NULL," +
                "exception_type VARCHAR(20) NOT NULL," +
                "modified_task_id INT," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (master_task_id) REFERENCES tasks(id) ON DELETE CASCADE," +
                "FOREIGN KEY (modified_task_id) REFERENCES tasks(id) ON DELETE SET NULL," +
                "UNIQUE KEY unique_exception (master_task_id, original_occurrence_date)" +
                ")";

        try (java.sql.Statement stmt = conn.createStatement()) {
            boolean result = stmt.execute(sql);
            Log.d(TAG, "CREATE TABLE task_exceptions executed. Result: " + result);
            Log.d(TAG, "Bảng task_exceptions đã được tạo hoặc đã tồn tại");
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi khi tạo bảng task_exceptions: " + e.getMessage());
            Log.e(TAG, "SQL State: " + e.getSQLState());
            throw e;
        }
    }

    /**
     * Migration: Thêm các cột parent_task_id, is_master, occurrence_date vào bảng tasks nếu chưa có.
     */
    private void migrateTasksTableAddRecurrenceFields(Connection conn) {
        try {
            // Kiểm tra và thêm parent_task_id
            String checkParentTaskIdSql = "SELECT COUNT(*) as count FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() " +
                    "AND TABLE_NAME = 'tasks' " +
                    "AND COLUMN_NAME = 'parent_task_id'";

            try (java.sql.Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery(checkParentTaskIdSql)) {
                
                if (rs.next() && rs.getInt("count") == 0) {
                    Log.d(TAG, "⚠ Cột parent_task_id chưa tồn tại, đang thêm vào bảng tasks...");
                    
                    String alterSql = "ALTER TABLE tasks ADD COLUMN parent_task_id INT NULL";
                    try (java.sql.Statement alterStmt = conn.createStatement()) {
                        alterStmt.execute(alterSql);
                        Log.d(TAG, "✓ Đã thêm cột parent_task_id vào bảng tasks");
                    } catch (SQLException alterError) {
                        Log.e(TAG, "Lỗi khi thêm cột parent_task_id: " + alterError.getMessage());
                    }
                    
                    // Thêm foreign key constraint
                    try {
                        String addFkSql = "ALTER TABLE tasks " +
                                "ADD CONSTRAINT fk_tasks_parent_task " +
                                "FOREIGN KEY (parent_task_id) REFERENCES tasks(id) ON DELETE CASCADE";
                        try (java.sql.Statement fkStmt = conn.createStatement()) {
                            fkStmt.execute(addFkSql);
                            Log.d(TAG, "✓ Đã thêm foreign key constraint cho parent_task_id");
                        }
                    } catch (SQLException fkError) {
                        String errorMsg = fkError.getMessage().toLowerCase();
                        if (!errorMsg.contains("duplicate key") && 
                            !errorMsg.contains("already exists")) {
                            Log.w(TAG, "Không thể thêm foreign key constraint cho parent_task_id: " + fkError.getMessage());
                        }
                    }
                } else {
                    Log.d(TAG, "✓ Cột parent_task_id đã tồn tại trong bảng tasks");
                }
            }

            // Kiểm tra và thêm is_master
            String checkIsMasterSql = "SELECT COUNT(*) as count FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() " +
                    "AND TABLE_NAME = 'tasks' " +
                    "AND COLUMN_NAME = 'is_master'";

            try (java.sql.Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery(checkIsMasterSql)) {
                
                if (rs.next() && rs.getInt("count") == 0) {
                    Log.d(TAG, "⚠ Cột is_master chưa tồn tại, đang thêm vào bảng tasks...");
                    
                    String alterSql = "ALTER TABLE tasks ADD COLUMN is_master BOOLEAN DEFAULT FALSE";
                    try (java.sql.Statement alterStmt = conn.createStatement()) {
                        alterStmt.execute(alterSql);
                        Log.d(TAG, "✓ Đã thêm cột is_master vào bảng tasks");
                    } catch (SQLException alterError) {
                        Log.e(TAG, "Lỗi khi thêm cột is_master: " + alterError.getMessage());
                    }
                } else {
                    Log.d(TAG, "✓ Cột is_master đã tồn tại trong bảng tasks");
                }
            }

            // Kiểm tra và thêm occurrence_date
            String checkOccurrenceDateSql = "SELECT COUNT(*) as count FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() " +
                    "AND TABLE_NAME = 'tasks' " +
                    "AND COLUMN_NAME = 'occurrence_date'";

            try (java.sql.Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery(checkOccurrenceDateSql)) {
                
                if (rs.next() && rs.getInt("count") == 0) {
                    Log.d(TAG, "⚠ Cột occurrence_date chưa tồn tại, đang thêm vào bảng tasks...");
                    
                    String alterSql = "ALTER TABLE tasks ADD COLUMN occurrence_date DATE NULL";
                    try (java.sql.Statement alterStmt = conn.createStatement()) {
                        alterStmt.execute(alterSql);
                        Log.d(TAG, "✓ Đã thêm cột occurrence_date vào bảng tasks");
                    } catch (SQLException alterError) {
                        Log.e(TAG, "Lỗi khi thêm cột occurrence_date: " + alterError.getMessage());
                    }
                } else {
                    Log.d(TAG, "✓ Cột occurrence_date đã tồn tại trong bảng tasks");
                }
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi khi migration bảng tasks (recurrence fields): " + e.getMessage());
            Log.e(TAG, "SQL State: " + e.getSQLState());
            e.printStackTrace();
        } catch (Exception e) {
            Log.e(TAG, "Lỗi không mong đợi khi migration bảng tasks (recurrence fields): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Chạy migration recurring tasks một lần nếu cần.
     * Kiểm tra xem đã migration chưa bằng cách kiểm tra một master task có is_master = TRUE.
     */
    private void runRecurrenceMigrationIfNeeded(Connection conn) {
        try {
            // Kiểm tra xem đã có master task nào chưa
            String checkSql = "SELECT COUNT(*) as count FROM tasks WHERE is_master = TRUE LIMIT 1";
            try (java.sql.Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery(checkSql)) {
                
                if (rs.next() && rs.getInt("count") > 0) {
                    Log.d(TAG, "Recurrence migration đã được chạy trước đó, bỏ qua");
                    return;
                }
            }

            // Chưa migration, chạy migration
            Log.d(TAG, "Chạy recurrence migration...");
            com.example.ktck_android_k17.service.RecurrenceMigrationService migrationService = 
                new com.example.ktck_android_k17.service.RecurrenceMigrationService();
            migrationService.migrateExistingRecurringTasks();
            Log.d(TAG, "Recurrence migration hoàn tất");

        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi chạy recurrence migration: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
