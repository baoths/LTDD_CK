package com.example.ktck_android_k17.dao;

import android.util.Log;

import com.example.ktck_android_k17.database.DatabaseHelper;
import com.example.ktck_android_k17.model.TaskRecurrence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho TaskRecurrence entity.
 * Xử lý các thao tác CRUD với bảng task_recurrence trong MySQL.
 */
public class TaskRecurrenceDAO {

    private static final String TAG = "TaskRecurrenceDAO";
    private DatabaseHelper dbHelper;

    public TaskRecurrenceDAO() {
        dbHelper = DatabaseHelper.getInstance();
    }

    /**
     * Tìm recurrence theo task ID.
     */
    public TaskRecurrence findByTaskId(int taskId) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return null;
        }

        String sql = "SELECT * FROM task_recurrence WHERE task_id = ? AND is_active = TRUE LIMIT 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToRecurrence(rs);
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi tìm recurrence theo task ID: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Lấy tất cả recurrences đang active cần được generate.
     */
    public List<TaskRecurrence> findActiveRecurrences() {
        List<TaskRecurrence> recurrences = new ArrayList<>();
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return recurrences;
        }

        String sql = "SELECT * FROM task_recurrence WHERE is_active = TRUE";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                recurrences.add(mapResultSetToRecurrence(rs));
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi lấy danh sách recurrences: " + e.getMessage());
            e.printStackTrace();
        }

        return recurrences;
    }

    /**
     * Thêm recurrence mới.
     */
    public boolean insert(TaskRecurrence recurrence) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return false;
        }

        String sql = "INSERT INTO task_recurrence (task_id, recurrence_type, recurrence_interval, " +
                "recurrence_days, recurrence_day_of_month, recurrence_end_date, recurrence_count, " +
                "last_generated_date, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, recurrence.getTaskId());
            stmt.setString(2, recurrence.getRecurrenceType());
            stmt.setInt(3, recurrence.getRecurrenceInterval());
            stmt.setString(4, recurrence.getRecurrenceDays());
            stmt.setObject(5, recurrence.getRecurrenceDayOfMonth());
            stmt.setString(6, recurrence.getRecurrenceEndDate());
            stmt.setObject(7, recurrence.getRecurrenceCount());
            stmt.setString(8, recurrence.getLastGeneratedDate());
            stmt.setBoolean(9, recurrence.isActive());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi thêm recurrence: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cập nhật recurrence.
     */
    public boolean update(TaskRecurrence recurrence) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return false;
        }

        String sql = "UPDATE task_recurrence SET recurrence_type = ?, recurrence_interval = ?, " +
                "recurrence_days = ?, recurrence_day_of_month = ?, recurrence_end_date = ?, " +
                "recurrence_count = ?, last_generated_date = ?, is_active = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, recurrence.getRecurrenceType());
            stmt.setInt(2, recurrence.getRecurrenceInterval());
            stmt.setString(3, recurrence.getRecurrenceDays());
            stmt.setObject(4, recurrence.getRecurrenceDayOfMonth());
            stmt.setString(5, recurrence.getRecurrenceEndDate());
            stmt.setObject(6, recurrence.getRecurrenceCount());
            stmt.setString(7, recurrence.getLastGeneratedDate());
            stmt.setBoolean(8, recurrence.isActive());
            stmt.setInt(9, recurrence.getId());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi cập nhật recurrence: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Xóa recurrence theo ID.
     */
    public boolean delete(int id) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return false;
        }

        String sql = "DELETE FROM task_recurrence WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi xóa recurrence: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Xóa recurrence theo task ID.
     */
    public boolean deleteByTaskId(int taskId) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return false;
        }

        String sql = "DELETE FROM task_recurrence WHERE task_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, taskId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi xóa recurrence theo task ID: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Vô hiệu hóa recurrence (set is_active = FALSE).
     */
    public boolean deactivate(int id) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return false;
        }

        String sql = "UPDATE task_recurrence SET is_active = FALSE WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi vô hiệu hóa recurrence: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Map ResultSet to TaskRecurrence object.
     */
    private TaskRecurrence mapResultSetToRecurrence(ResultSet rs) throws SQLException {
        TaskRecurrence recurrence = new TaskRecurrence();
        recurrence.setId(rs.getInt("id"));
        recurrence.setTaskId(rs.getInt("task_id"));
        recurrence.setRecurrenceType(rs.getString("recurrence_type"));
        recurrence.setRecurrenceInterval(rs.getInt("recurrence_interval"));
        recurrence.setRecurrenceDays(rs.getString("recurrence_days"));
        
        int dayOfMonth = rs.getInt("recurrence_day_of_month");
        if (!rs.wasNull()) {
            recurrence.setRecurrenceDayOfMonth(dayOfMonth);
        }
        
        recurrence.setRecurrenceEndDate(rs.getString("recurrence_end_date"));
        
        int count = rs.getInt("recurrence_count");
        if (!rs.wasNull()) {
            recurrence.setRecurrenceCount(count);
        }
        
        recurrence.setLastGeneratedDate(rs.getString("last_generated_date"));
        recurrence.setActive(rs.getBoolean("is_active"));
        recurrence.setCreatedAt(rs.getString("created_at"));
        
        return recurrence;
    }
}

