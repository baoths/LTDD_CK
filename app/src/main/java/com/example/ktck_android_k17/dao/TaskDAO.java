package com.example.ktck_android_k17.dao;

import android.util.Log;

import com.example.ktck_android_k17.database.DatabaseHelper;
import com.example.ktck_android_k17.model.Task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho Task entity.
 * Xử lý các thao tác CRUD với bảng tasks trong MySQL.
 */
public class TaskDAO {

    private static final String TAG = "TaskDAO";
    private DatabaseHelper dbHelper;

    public TaskDAO() {
        dbHelper = DatabaseHelper.getInstance();
    }

    /**
     * Lấy tất cả tasks của một user.
     *
     * @param userId ID của user
     * @return Danh sách Task
     */
    public List<Task> findByUserId(int userId) {
        List<Task> tasks = new ArrayList<>();
        Connection conn = dbHelper.getConnection();

        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return tasks;
        }

        String sql = "SELECT * FROM tasks WHERE user_id = ? ORDER BY created_at DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                tasks.add(mapResultSetToTask(rs));
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi lấy danh sách task: " + e.getMessage());
            e.printStackTrace();
        }

        return tasks;
    }

    /**
     * Tìm task theo ID.
     *
     * @param id ID của task
     * @return Task object nếu tìm thấy, null nếu không
     */
    public Task findById(int id) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return null;
        }

        String sql = "SELECT * FROM tasks WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToTask(rs);
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi tìm task theo ID: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Thêm task mới và trả về ID của task vừa tạo.
     *
     * @param task Task entity cần thêm
     * @return Task ID nếu thành công, -1 nếu thất bại
     */
    public int insertAndGetId(Task task) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return -1;
        }

        String sql = "INSERT INTO tasks (user_id, category_id, title, description, status, priority, due_date, start_date, end_date, parent_task_id, is_master, occurrence_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, task.getUserId());
            // Chỉ set category_id nếu > 0, nếu không set NULL
            if (task.getCategoryId() > 0) {
                stmt.setInt(2, task.getCategoryId());
            } else {
                stmt.setNull(2, java.sql.Types.INTEGER);
            }
            stmt.setString(3, task.getTitle());
            stmt.setString(4, task.getDescription());
            stmt.setString(5, task.getStatus() != null ? task.getStatus() : Task.STATUS_PENDING);
            stmt.setString(6, task.getPriority() != null ? task.getPriority() : Task.PRIORITY_MEDIUM);
            stmt.setString(7, task.getDueDate());
            stmt.setString(8, task.getStartDate());
            stmt.setString(9, task.getEndDate());
            // Handle nullable fields
            if (task.getParentTaskId() != null) {
                stmt.setInt(10, task.getParentTaskId());
            } else {
                stmt.setNull(10, java.sql.Types.INTEGER);
            }
            stmt.setBoolean(11, task.getIsMaster() != null ? task.getIsMaster() : false);
            stmt.setString(12, task.getOccurrenceDate());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                java.sql.ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    return (int) generatedKeys.getLong(1);
                }
            }
            return -1;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi thêm task: " + e.getMessage());
            e.printStackTrace();
            
            // Nếu lỗi là do thiếu cột category_id, thử lại không có category_id
            if (e.getMessage() != null && e.getMessage().contains("category_id")) {
                Log.w(TAG, "Cột category_id chưa tồn tại, thử insert không có category_id...");
                return insertAndGetIdWithoutCategory(task);
            }
            
            return -1;
        }
    }
    
    /**
     * Thêm task mới không có category_id (fallback khi cột chưa tồn tại).
     */
    private int insertAndGetIdWithoutCategory(Task task) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return -1;
        }

        String sql = "INSERT INTO tasks (user_id, title, description, status, priority, due_date, start_date, end_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, task.getUserId());
            stmt.setString(2, task.getTitle());
            stmt.setString(3, task.getDescription());
            stmt.setString(4, task.getStatus() != null ? task.getStatus() : Task.STATUS_PENDING);
            stmt.setString(5, task.getPriority() != null ? task.getPriority() : Task.PRIORITY_MEDIUM);
            stmt.setString(6, task.getDueDate());
            stmt.setString(7, task.getStartDate());
            stmt.setString(8, task.getEndDate());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                java.sql.ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    return (int) generatedKeys.getLong(1);
                }
            }
            return -1;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi thêm task (không có category_id): " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
    
    /**
     * Kiểm tra xem cột có tồn tại trong bảng không.
     */
    private boolean checkColumnExists(Connection conn, String tableName, String columnName) {
        try {
            String sql = "SELECT COUNT(*) as count FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() " +
                    "AND TABLE_NAME = ? " +
                    "AND COLUMN_NAME = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName);
                stmt.setString(2, columnName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("count") > 0;
                    }
                }
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi kiểm tra cột: " + e.getMessage());
            return false; // Giả định không tồn tại nếu có lỗi
        }
        return false;
    }

    /**
     * Thêm task mới vào database.
     *
     * @param task Task entity cần thêm
     * @return true nếu thành công, false nếu thất bại
     */
    public boolean insert(Task task) {
        return insertAndGetId(task) > 0;
    }

    /**
     * Cập nhật thông tin task.
     *
     * @param task Task entity với thông tin mới
     * @return true nếu thành công, false nếu thất bại
     */
    public boolean update(Task task) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return false;
        }

        String sql = "UPDATE tasks SET title = ?, description = ?, status = ?, priority = ?, due_date = ?, start_date = ?, end_date = ?, parent_task_id = ?, is_master = ?, occurrence_date = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, task.getTitle());
            stmt.setString(2, task.getDescription());
            stmt.setString(3, task.getStatus());
            stmt.setString(4, task.getPriority());
            stmt.setString(5, task.getDueDate());
            stmt.setString(6, task.getStartDate());
            stmt.setString(7, task.getEndDate());
            // Handle nullable fields
            if (task.getParentTaskId() != null) {
                stmt.setInt(8, task.getParentTaskId());
            } else {
                stmt.setNull(8, java.sql.Types.INTEGER);
            }
            stmt.setBoolean(9, task.getIsMaster() != null ? task.getIsMaster() : false);
            stmt.setString(10, task.getOccurrenceDate());
            stmt.setInt(11, task.getId());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi cập nhật task: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cập nhật trạng thái task.
     *
     * @param taskId ID của task
     * @param status Trạng thái mới
     * @return true nếu thành công, false nếu thất bại
     */
    public boolean updateStatus(int taskId, String status) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return false;
        }

        String sql = "UPDATE tasks SET status = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, taskId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi cập nhật trạng thái task: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Xóa task theo ID.
     *
     * @param id ID của task cần xóa
     * @return true nếu thành công, false nếu thất bại
     */
    public boolean delete(int id) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return false;
        }

        String sql = "DELETE FROM tasks WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi xóa task: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lấy tasks của một user theo category.
     *
     * @param userId     ID của user
     * @param categoryId ID của category
     * @return Danh sách Task
     */
    public List<Task> findByUserIdAndCategory(int userId, int categoryId) {
        List<Task> tasks = new ArrayList<>();
        Connection conn = dbHelper.getConnection();

        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return tasks;
        }

        String sql = "SELECT * FROM tasks WHERE user_id = ? AND category_id = ? ORDER BY created_at DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, categoryId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                tasks.add(mapResultSetToTask(rs));
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi lấy task theo category: " + e.getMessage());
            e.printStackTrace();
        }

        return tasks;
    }

    /**
     * Đếm số lượng task của một user.
     *
     * @param userId ID của user
     * @return Số lượng task
     */
    public int countByUserId(int userId) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return 0;
        }

        String sql = "SELECT COUNT(*) as count FROM tasks WHERE user_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi đếm task: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Đếm số lượng task đang sử dụng một category.
     *
     * @param categoryId ID của category
     * @return Số lượng task đang sử dụng category này
     */
    public int countByCategoryId(int categoryId) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return 0;
        }

        String sql = "SELECT COUNT(*) as count FROM tasks WHERE category_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, categoryId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi đếm task theo category: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Lấy tasks theo trạng thái.
     *
     * @param userId ID của user
     * @param status Trạng thái cần lọc
     * @return Danh sách Task
     */
    public List<Task> findByUserIdAndStatus(int userId, String status) {
        List<Task> tasks = new ArrayList<>();
        Connection conn = dbHelper.getConnection();

        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return tasks;
        }

        String sql = "SELECT * FROM tasks WHERE user_id = ? AND status = ? ORDER BY created_at DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, status);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                tasks.add(mapResultSetToTask(rs));
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi lấy danh sách task theo status: " + e.getMessage());
            e.printStackTrace();
        }

        return tasks;
    }

    /**
     * Map ResultSet sang Task object.
     *
     * @param rs ResultSet từ query
     * @return Task object
     */
    private Task mapResultSetToTask(ResultSet rs) throws SQLException {
        Task task = new Task();
        task.setId(rs.getInt("id"));
        task.setUserId(rs.getInt("user_id"));
        task.setCategoryId(rs.getInt("category_id"));
        task.setTitle(rs.getString("title"));
        task.setDescription(rs.getString("description"));
        task.setStatus(rs.getString("status"));
        task.setPriority(rs.getString("priority"));
        task.setDueDate(rs.getString("due_date"));
        // Handle start_date and end_date - may be null
        try {
            task.setStartDate(rs.getString("start_date"));
        } catch (SQLException e) {
            // Column may not exist yet, set to null
            task.setStartDate(null);
        }
        try {
            task.setEndDate(rs.getString("end_date"));
        } catch (SQLException e) {
            // Column may not exist yet, set to null
            task.setEndDate(null);
        }
        // Handle new recurrence fields
        try {
            int parentTaskId = rs.getInt("parent_task_id");
            if (!rs.wasNull()) {
                task.setParentTaskId(parentTaskId);
            }
        } catch (SQLException e) {
            // Column may not exist yet, set to null
            task.setParentTaskId(null);
        }
        try {
            task.setIsMaster(rs.getBoolean("is_master"));
        } catch (SQLException e) {
            // Column may not exist yet, set to false
            task.setIsMaster(false);
        }
        try {
            task.setOccurrenceDate(rs.getString("occurrence_date"));
        } catch (SQLException e) {
            // Column may not exist yet, set to null
            task.setOccurrenceDate(null);
        }
        task.setCreatedAt(rs.getString("created_at"));
        return task;
    }

    /**
     * Lấy tất cả master tasks của một user (không phải instances).
     *
     * @param userId ID của user
     * @return Danh sách master Task
     */
    public List<Task> findMasterTasks(int userId) {
        List<Task> tasks = new ArrayList<>();
        Connection conn = dbHelper.getConnection();

        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return tasks;
        }

        String sql = "SELECT * FROM tasks WHERE user_id = ? AND (is_master = TRUE OR is_master IS NULL) AND parent_task_id IS NULL ORDER BY created_at DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                tasks.add(mapResultSetToTask(rs));
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi lấy danh sách master tasks: " + e.getMessage());
            e.printStackTrace();
        }

        return tasks;
    }

    /**
     * Lấy instances của một master task trong date range.
     *
     * @param masterTaskId ID của master task
     * @param startDate    Ngày bắt đầu (yyyy-MM-dd)
     * @param endDate      Ngày kết thúc (yyyy-MM-dd)
     * @return Danh sách Task instances
     */
    public List<Task> findInstancesByMasterId(int masterTaskId, String startDate, String endDate) {
        List<Task> tasks = new ArrayList<>();
        Connection conn = dbHelper.getConnection();

        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return tasks;
        }

        String sql = "SELECT * FROM tasks WHERE parent_task_id = ? AND occurrence_date >= ? AND occurrence_date <= ? ORDER BY occurrence_date ASC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, masterTaskId);
            stmt.setString(2, startDate);
            stmt.setString(3, endDate);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                tasks.add(mapResultSetToTask(rs));
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi lấy instances theo master task: " + e.getMessage());
            e.printStackTrace();
        }

        return tasks;
    }
}
