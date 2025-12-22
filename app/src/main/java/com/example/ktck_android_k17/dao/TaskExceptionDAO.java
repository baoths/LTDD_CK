package com.example.ktck_android_k17.dao;

import android.util.Log;

import com.example.ktck_android_k17.database.DatabaseHelper;
import com.example.ktck_android_k17.model.TaskException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho TaskException entity.
 * Xử lý các thao tác CRUD với bảng task_exceptions trong MySQL.
 */
public class TaskExceptionDAO {

    private static final String TAG = "TaskExceptionDAO";
    private DatabaseHelper dbHelper;

    public TaskExceptionDAO() {
        dbHelper = DatabaseHelper.getInstance();
    }

    /**
     * Thêm exception mới.
     *
     * @param exception TaskException entity cần thêm
     * @return true nếu thành công, false nếu thất bại
     */
    public boolean insert(TaskException exception) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return false;
        }

        String sql = "INSERT INTO task_exceptions (master_task_id, original_occurrence_date, exception_type, modified_task_id) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, exception.getMasterTaskId());
            stmt.setString(2, exception.getOriginalOccurrenceDate());
            stmt.setString(3, exception.getExceptionType());
            if (exception.getModifiedTaskId() != null) {
                stmt.setInt(4, exception.getModifiedTaskId());
            } else {
                stmt.setNull(4, java.sql.Types.INTEGER);
            }

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi thêm exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lấy tất cả exceptions của một master task.
     *
     * @param masterTaskId ID của master task
     * @return Danh sách TaskException
     */
    public List<TaskException> findByMasterTaskId(int masterTaskId) {
        List<TaskException> exceptions = new ArrayList<>();
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return exceptions;
        }

        String sql = "SELECT * FROM task_exceptions WHERE master_task_id = ? ORDER BY original_occurrence_date ASC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, masterTaskId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                exceptions.add(mapResultSetToException(rs));
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi lấy danh sách exceptions: " + e.getMessage());
            e.printStackTrace();
        }

        return exceptions;
    }

    /**
     * Tìm exception cụ thể cho một occurrence date.
     *
     * @param masterTaskId         ID của master task
     * @param originalOccurrenceDate Ngày occurrence (yyyy-MM-dd)
     * @return TaskException nếu tìm thấy, null nếu không
     */
    public TaskException findException(int masterTaskId, String originalOccurrenceDate) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return null;
        }

        String sql = "SELECT * FROM task_exceptions WHERE master_task_id = ? AND original_occurrence_date = ? LIMIT 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, masterTaskId);
            stmt.setString(2, originalOccurrenceDate);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToException(rs);
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi tìm exception: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Xóa exception theo ID.
     *
     * @param id ID của exception cần xóa
     * @return true nếu thành công, false nếu thất bại
     */
    public boolean delete(int id) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return false;
        }

        String sql = "DELETE FROM task_exceptions WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi xóa exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Xóa exception theo master task ID và occurrence date.
     *
     * @param masterTaskId         ID của master task
     * @param originalOccurrenceDate Ngày occurrence
     * @return true nếu thành công, false nếu thất bại
     */
    public boolean deleteByMasterTaskAndDate(int masterTaskId, String originalOccurrenceDate) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return false;
        }

        String sql = "DELETE FROM task_exceptions WHERE master_task_id = ? AND original_occurrence_date = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, masterTaskId);
            stmt.setString(2, originalOccurrenceDate);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi xóa exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Map ResultSet to TaskException object.
     */
    private TaskException mapResultSetToException(ResultSet rs) throws SQLException {
        TaskException exception = new TaskException();
        exception.setId(rs.getInt("id"));
        exception.setMasterTaskId(rs.getInt("master_task_id"));
        exception.setOriginalOccurrenceDate(rs.getString("original_occurrence_date"));
        exception.setExceptionType(rs.getString("exception_type"));
        
        int modifiedTaskId = rs.getInt("modified_task_id");
        if (!rs.wasNull()) {
            exception.setModifiedTaskId(modifiedTaskId);
        }
        
        exception.setCreatedAt(rs.getString("created_at"));
        return exception;
    }
}

