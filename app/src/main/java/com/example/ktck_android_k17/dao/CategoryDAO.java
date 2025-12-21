package com.example.ktck_android_k17.dao;

import android.util.Log;

import com.example.ktck_android_k17.database.DatabaseHelper;
import com.example.ktck_android_k17.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho Category entity.
 * Xử lý các thao tác CRUD với bảng categories trong MySQL.
 */
public class CategoryDAO {

    private static final String TAG = "CategoryDAO";
    private DatabaseHelper dbHelper;

    public CategoryDAO() {
        dbHelper = DatabaseHelper.getInstance();
    }

    /**
     * Lấy tất cả categories của một user.
     */
    public List<Category> findByUserId(int userId) {
        List<Category> categories = new ArrayList<>();
        Connection conn = dbHelper.getConnection();

        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return categories;
        }

        String sql = "SELECT * FROM categories WHERE user_id = ? ORDER BY name ASC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                categories.add(mapResultSetToCategory(rs));
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi lấy danh sách category: " + e.getMessage());
            e.printStackTrace();
        }

        return categories;
    }

    /**
     * Tìm category theo ID.
     */
    public Category findById(int id) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return null;
        }

        String sql = "SELECT * FROM categories WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToCategory(rs);
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi tìm category theo ID: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Thêm category mới.
     */
    public boolean insert(Category category) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return false;
        }

        String sql = "INSERT INTO categories (user_id, name, description, color) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, category.getUserId());
            stmt.setString(2, category.getName());
            stmt.setString(3, category.getDescription());
            stmt.setString(4, category.getColor() != null ? category.getColor() : "#3498db");

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi thêm category: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cập nhật category.
     */
    public boolean update(Category category) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return false;
        }

        String sql = "UPDATE categories SET name = ?, description = ?, color = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category.getName());
            stmt.setString(2, category.getDescription());
            stmt.setString(3, category.getColor());
            stmt.setInt(4, category.getId());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi cập nhật category: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Xóa category theo ID.
     */
    public boolean delete(int id) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return false;
        }

        String sql = "DELETE FROM categories WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi xóa category: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Map ResultSet to Category object.
     */
    private Category mapResultSetToCategory(ResultSet rs) throws SQLException {
        return new Category(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("color"),
                rs.getString("created_at"));
    }
}
