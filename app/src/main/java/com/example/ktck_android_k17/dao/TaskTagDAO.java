package com.example.ktck_android_k17.dao;

import android.util.Log;

import com.example.ktck_android_k17.database.DatabaseHelper;
import com.example.ktck_android_k17.model.TaskTag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho TaskTag entity.
 * Xử lý các thao tác CRUD với bảng task_tags trong MySQL.
 */
public class TaskTagDAO {

    private static final String TAG = "TaskTagDAO";
    private DatabaseHelper dbHelper;

    public TaskTagDAO() {
        dbHelper = DatabaseHelper.getInstance();
    }

    /**
     * Lấy tất cả tags của một task.
     */
    public List<TaskTag> findByTaskId(int taskId) {
        List<TaskTag> tags = new ArrayList<>();
        Connection conn = dbHelper.getConnection();

        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return tags;
        }

        String sql = "SELECT * FROM task_tags WHERE task_id = ? ORDER BY tag_name ASC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                tags.add(mapResultSetToTaskTag(rs));
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi lấy danh sách tag: " + e.getMessage());
            e.printStackTrace();
        }

        return tags;
    }

    /**
     * Lấy tất cả tag names của một task.
     */
    public List<String> findTagNamesByTaskId(int taskId) {
        List<String> tagNames = new ArrayList<>();
        Connection conn = dbHelper.getConnection();

        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return tagNames;
        }

        String sql = "SELECT tag_name FROM task_tags WHERE task_id = ? ORDER BY tag_name ASC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                tagNames.add(rs.getString("tag_name"));
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi lấy tag names: " + e.getMessage());
            e.printStackTrace();
        }

        return tagNames;
    }

    /**
     * Thêm tag mới.
     */
    public boolean insert(TaskTag tag) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return false;
        }

        String sql = "INSERT INTO task_tags (task_id, tag_name) VALUES (?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, tag.getTaskId());
            stmt.setString(2, tag.getTagName());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi thêm tag: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Xóa tag theo ID.
     */
    public boolean delete(int id) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return false;
        }

        String sql = "DELETE FROM task_tags WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi xóa tag: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Xóa tất cả tags của một task.
     */
    public boolean deleteByTaskId(int taskId) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return false;
        }

        String sql = "DELETE FROM task_tags WHERE task_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, taskId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi xóa tags: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Xóa tag cụ thể của task.
     */
    public boolean deleteByTaskIdAndTagName(int taskId, String tagName) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return false;
        }

        String sql = "DELETE FROM task_tags WHERE task_id = ? AND tag_name = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            stmt.setString(2, tagName);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi xóa tag: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Map ResultSet to TaskTag object.
     */
    private TaskTag mapResultSetToTaskTag(ResultSet rs) throws SQLException {
        return new TaskTag(
                rs.getInt("id"),
                rs.getInt("task_id"),
                rs.getString("tag_name"),
                rs.getString("created_at"));
    }
}
