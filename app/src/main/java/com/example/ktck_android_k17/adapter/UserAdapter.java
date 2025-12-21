package com.example.ktck_android_k17.adapter;

import com.example.ktck_android_k17.dto.RegisterRequest;
import com.example.ktck_android_k17.dto.UserDTO;
import com.example.ktck_android_k17.model.User;

/**
 * Adapter class để chuyển đổi giữa User entity và UserDTO.
 * Đảm bảo dữ liệu nhạy cảm (password) không bị truyền đi ngoài tầng cần thiết.
 */
public class UserAdapter {

    /**
     * Chuyển đổi User entity sang UserDTO.
     * Loại bỏ thông tin password khi truyền lên tầng View.
     *
     * @param user User entity từ database
     * @return UserDTO không chứa password
     */
    public static UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }

        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt());
    }

    /**
     * Chuyển đổi RegisterRequest sang User entity.
     * Dùng khi tạo user mới từ form đăng ký.
     *
     * @param request RegisterRequest từ form
     * @return User entity để lưu vào database
     */
    public static User toEntity(RegisterRequest request) {
        if (request == null) {
            return null;
        }

        return new User(
                request.getUsername(),
                request.getEmail(),
                request.getPassword());
    }

    /**
     * Chuyển đổi UserDTO sang User entity (chỉ dùng cho update).
     * Lưu ý: không có password, cần set riêng nếu cần update password.
     *
     * @param dto UserDTO
     * @return User entity (không có password)
     */
    public static User toEntity(UserDTO dto) {
        if (dto == null) {
            return null;
        }

        User user = new User();
        user.setId(dto.getId());
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setCreatedAt(dto.getCreatedAt());

        return user;
    }
}
