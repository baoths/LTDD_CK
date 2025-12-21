-- Script SQL để tạo các bảng cho ứng dụng Task Manager
-- Database: task_manager
-- Chạy script này trong MySQL để đảm bảo tất cả các bảng được tạo

-- Tạo database nếu chưa tồn tại
CREATE DATABASE IF NOT EXISTS task_manager;
USE task_manager;

-- Xóa các bảng cũ nếu cần (CẨN THẬN: Sẽ xóa toàn bộ dữ liệu!)
-- DROP TABLE IF EXISTS task_tags;
-- DROP TABLE IF EXISTS task_reminders;
-- DROP TABLE IF EXISTS tasks;
-- DROP TABLE IF EXISTS categories;
-- DROP TABLE IF EXISTS users;

-- 1. Tạo bảng users (phải tạo đầu tiên vì các bảng khác phụ thuộc)
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Tạo bảng categories (phải tạo trước tasks)
CREATE TABLE IF NOT EXISTS categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    color VARCHAR(7) DEFAULT '#3498db',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_category (user_id, name),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Tạo bảng tasks (phải tạo trước task_reminders và task_tags)
CREATE TABLE IF NOT EXISTS tasks (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    category_id INT,
    title VARCHAR(255) NOT NULL,
    description LONGTEXT,
    status VARCHAR(50) DEFAULT 'pending',
    priority VARCHAR(50) DEFAULT 'medium',
    due_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Tạo bảng task_reminders
CREATE TABLE IF NOT EXISTS task_reminders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_id INT NOT NULL,
    reminder_time DATETIME NOT NULL,
    type VARCHAR(50) DEFAULT 'notification',
    is_sent BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Tạo bảng task_tags
CREATE TABLE IF NOT EXISTS task_tags (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_id INT NOT NULL,
    tag_name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_task_tag (task_id, tag_name),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Kiểm tra các bảng đã được tạo
SHOW TABLES;

-- Hiển thị cấu trúc các bảng
DESCRIBE users;
DESCRIBE categories;
DESCRIBE tasks;
DESCRIBE task_reminders;
DESCRIBE task_tags;

