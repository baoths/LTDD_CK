package com.example.ktck_android_k17.dao;

import android.util.Log;

import com.example.ktck_android_k17.database.DatabaseHelper;
import com.example.ktck_android_k17.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object cho User entity.
 * Xử lý các thao tác CRUD với bảng users trong MySQL.
 */
public class UserDAO {

    private static final String TAG = "UserDAO";
    private DatabaseHelper dbHelper;

    public UserDAO() {
        dbHelper = DatabaseHelper.getInstance();
    }

    /**
     * Tìm user theo email.
     *
     * @param email Email cần tìm
     * @return User object nếu tìm thấy, null nếu không
     */
    public User findByEmail(String email) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return null;
        }

        String sql = "SELECT * FROM users WHERE email = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi tìm user theo email: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Tìm user theo ID.
     *
     * @param id ID của user
     * @return User object nếu tìm thấy, null nếu không
     */
    public User findById(int id) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return null;
        }

        String sql = "SELECT * FROM users WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi tìm user theo ID: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Thêm user mới vào database.
     *
     * @param user User entity cần thêm
     * @return true nếu thành công, false nếu thất bại
     */
    public boolean insert(User user) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return false;
        }

        String sql = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi thêm user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cập nhật thông tin user.
     *
     * @param user User entity với thông tin mới
     * @return true nếu thành công, false nếu thất bại
     */
    public boolean update(User user) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return false;
        }

        String sql = "UPDATE users SET username = ?, email = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setInt(3, user.getId());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi cập nhật user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Xóa user theo ID.
     *
     * @param id ID của user cần xóa
     * @return true nếu thành công, false nếu thất bại
     */
    public boolean delete(int id) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return false;
        }

        String sql = "DELETE FROM users WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            Log.e(TAG, "Lỗi xóa user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Kiểm tra đăng nhập.
     *
     * @param email    Email đăng nhập
     * @param password Mật khẩu đăng nhập
     * @return User object nếu đăng nhập thành công, null nếu thất bại
     */
    public User validateLogin(String email, String password) {
        Connection conn = dbHelper.getConnection();
        if (conn == null) {
            Log.e(TAG, "Không thể kết nối database");
            return null;
        }

        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            Log.e(TAG, "Lỗi kiểm tra đăng nhập: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Kiểm tra email đã tồn tại chưa.
     *
     * @param email Email cần kiểm tra
     * @return true nếu email đã tồn tại
     */
    public boolean isEmailExists(String email) {
        return findByEmail(email) != null;
    }

    /**
     * Map ResultSet sang User object.
     *
     * @param rs ResultSet từ query
     * @return User object
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setCreatedAt(rs.getString("created_at"));
        return user;
    }
}
