package com.example.ktck_android_k17.dao;

import android.util.Log;

import com.example.ktck_android_k17.database.DatabaseHelper;
import com.example.ktck_android_k17.model.TaskReminder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho TaskReminder entity.
 * Xử lý các thao tác CRUD với bảng task_reminders trong MySQL.
 */
public class TaskReminderDAO {

    private static final String TAG = "TaskReminderDAO";
    private DatabaseHelper dbHelper;

    public TaskReminderDAO() {
        dbHelper = DatabaseHelper.getInstance();
    }

    /**
     * Lấy tất cả reminders của một task.
     */
    public List<TaskReminder> findByTaskId(int taskId) {
        List<TaskReminder> reminders = new ArrayList<>();
        Connection conn = dbHelper.getConnection();

        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return reminders;
        }

        String sql = "SELECT * FROM task_reminders WHERE task_id = ? ORDER BY reminder_time ASC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                reminders.add(mapResultSetToTaskReminder(rs));
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi lấy danh sách reminder: " + e.getMessage());
            e.printStackTrace();
        }

        return reminders;
    }

    /**
     * Lấy tất cả reminders chưa gửi.
     */
    public List<TaskReminder> findUnsentReminders() {
        List<TaskReminder> reminders = new ArrayList<>();
        Connection conn = dbHelper.getConnection();

        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return reminders;
        }

        String sql = "SELECT * FROM task_reminders WHERE is_sent = FALSE AND reminder_time <= NOW() ORDER BY reminder_time ASC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                reminders.add(mapResultSetToTaskReminder(rs));
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi lấy unsent reminders: " + e.getMessage());
            e.printStackTrace();
        }

        return reminders;
    }

    /**
     * Thêm reminder mới.
     */
    public boolean insert(TaskReminder reminder) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return false;
        }

        String sql = "INSERT INTO task_reminders (task_id, reminder_time, type) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reminder.getTaskId());
            stmt.setString(2, reminder.getReminderTime());
            stmt.setString(3, reminder.getType() != null ? reminder.getType() : TaskReminder.TYPE_NOTIFICATION);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi thêm reminder: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Đánh dấu reminder đã gửi.
     */
    public boolean markAsSent(int reminderId) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return false;
        }

        String sql = "UPDATE task_reminders SET is_sent = TRUE WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reminderId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi update reminder: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Xóa reminder theo ID.
     */
    public boolean delete(int id) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return false;
        }

        String sql = "DELETE FROM task_reminders WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi xóa reminder: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Xóa tất cả reminders của một task.
     */
    public boolean deleteByTaskId(int taskId) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return false;
        }

        String sql = "DELETE FROM task_reminders WHERE task_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, taskId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi xóa reminders: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Map ResultSet to TaskReminder object.
     */
    private TaskReminder mapResultSetToTaskReminder(ResultSet rs) throws SQLException {
        return new TaskReminder(
                rs.getInt("id"),
                rs.getInt("task_id"),
                rs.getString("reminder_time"),
                rs.getString("type"),
                rs.getBoolean("is_sent"),
                rs.getString("created_at"));
    }
}
