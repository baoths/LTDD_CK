-- Migration Script: Thêm cột category_id vào bảng tasks
-- Chạy script này nếu bảng tasks đã tồn tại nhưng thiếu cột category_id
-- Database: task_manager

USE task_manager;

-- Kiểm tra xem cột category_id đã tồn tại chưa
-- Nếu chưa có, thêm cột vào bảng tasks
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'task_manager' 
    AND TABLE_NAME = 'tasks' 
    AND COLUMN_NAME = 'category_id'
);

-- Nếu cột chưa tồn tại, thêm vào
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE tasks ADD COLUMN category_id INT',
    'SELECT "Cột category_id đã tồn tại, không cần migration" AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Thêm foreign key constraint nếu chưa có
-- Lưu ý: Có thể sẽ báo lỗi nếu constraint đã tồn tại, đó là bình thường
SET @fk_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
    WHERE TABLE_SCHEMA = 'task_manager' 
    AND TABLE_NAME = 'tasks' 
    AND CONSTRAINT_NAME = 'fk_tasks_category'
);

SET @fk_sql = IF(@fk_exists = 0,
    'ALTER TABLE tasks ADD CONSTRAINT fk_tasks_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL',
    'SELECT "Foreign key constraint đã tồn tại" AS message'
);

PREPARE fk_stmt FROM @fk_sql;
EXECUTE fk_stmt;
DEALLOCATE PREPARE fk_stmt;

-- Kiểm tra kết quả
DESCRIBE tasks;

