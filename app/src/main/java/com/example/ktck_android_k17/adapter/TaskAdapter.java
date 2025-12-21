package com.example.ktck_android_k17.adapter;

import com.example.ktck_android_k17.dao.CategoryDAO;
import com.example.ktck_android_k17.dao.TaskReminderDAO;
import com.example.ktck_android_k17.dao.TaskTagDAO;
import com.example.ktck_android_k17.dto.TaskDTO;
import com.example.ktck_android_k17.model.Category;
import com.example.ktck_android_k17.model.Task;
import com.example.ktck_android_k17.model.TaskReminder;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter class để chuyển đổi giữa Task entity và TaskDTO.
 * Hỗ trợ thêm thông tin bổ sung như tên owner khi chuyển sang DTO.
 */
public class TaskAdapter {

    /**
     * Chuyển đổi Task entity sang TaskDTO với đầy đủ thông tin (category, tags, reminder).
     *
     * @param task      Task entity từ database
     * @param ownerName Tên người sở hữu task
     * @return TaskDTO với thông tin đầy đủ
     */
    public static TaskDTO toDTO(Task task, String ownerName) {
        if (task == null) {
            return null;
        }

        return toDTO(task, ownerName, null, null, null);
    }

    /**
     * Chuyển đổi Task entity sang TaskDTO với đầy đủ thông tin (category, tags, reminder).
     *
     * @param task           Task entity từ database
     * @param ownerName      Tên người sở hữu task
     * @param categoryDAO    DAO để load category (có thể null)
     * @param taskTagDAO     DAO để load tags (có thể null)
     * @param reminderDAO    DAO để load reminder (có thể null)
     * @return TaskDTO với thông tin đầy đủ
     */
    public static TaskDTO toDTO(Task task, String ownerName,
            CategoryDAO categoryDAO, TaskTagDAO taskTagDAO, TaskReminderDAO reminderDAO) {
        if (task == null) {
            return null;
        }

        // Load category name
        String categoryName = null;
        int categoryId = task.getCategoryId();
        if (categoryDAO != null && categoryId > 0) {
            Category category = categoryDAO.findById(categoryId);
            if (category != null) {
                categoryName = category.getName();
            }
        }

        // Load tags
        List<String> tags = new ArrayList<>();
        if (taskTagDAO != null) {
            tags = taskTagDAO.findTagNamesByTaskId(task.getId());
        }

        // Load reminder
        String reminderTime = null;
        if (reminderDAO != null) {
            List<TaskReminder> reminders = reminderDAO.findByTaskId(task.getId());
            if (reminders != null && !reminders.isEmpty()) {
                // Lấy reminder đầu tiên (có thể có nhiều reminders)
                reminderTime = reminders.get(0).getReminderTime();
            }
        }

        TaskDTO dto = new TaskDTO(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getCreatedAt(),
                ownerName,
                categoryId,
                categoryName,
                tags,
                reminderTime);
        
        // Set startDate and endDate
        dto.setStartDate(task.getStartDate());
        dto.setEndDate(task.getEndDate());
        
        return dto;
    }

    /**
     * Chuyển đổi Task entity sang TaskDTO (không có ownerName).
     *
     * @param task Task entity từ database
     * @return TaskDTO cơ bản
     */
    public static TaskDTO toDTO(Task task) {
        if (task == null) {
            return null;
        }

        TaskDTO dto = new TaskDTO(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate());
        dto.setStartDate(task.getStartDate());
        dto.setEndDate(task.getEndDate());
        return dto;
    }

    /**
     * Chuyển đổi TaskDTO sang Task entity.
     *
     * @param dto    TaskDTO từ form
     * @param userId ID của user sở hữu task
     * @return Task entity để lưu vào database
     */
    public static Task toEntity(TaskDTO dto, int userId) {
        if (dto == null) {
            return null;
        }

        Task task = new Task();
        task.setId(dto.getId());
        task.setUserId(userId);
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatus(dto.getStatus());
        task.setPriority(dto.getPriority());
        task.setDueDate(dto.getDueDate());
        task.setCreatedAt(dto.getCreatedAt());

        return task;
    }

    /**
     * Chuyển đổi danh sách Task entities sang danh sách TaskDTOs.
     *
     * @param tasks Danh sách Task entities
     * @return Danh sách TaskDTOs
     */
    public static List<TaskDTO> toDTOList(List<Task> tasks) {
        if (tasks == null) {
            return new ArrayList<>();
        }

        List<TaskDTO> dtos = new ArrayList<>();
        for (Task task : tasks) {
            dtos.add(toDTO(task));
        }
        return dtos;
    }

    /**
     * Chuyển đổi danh sách Task entities sang danh sách TaskDTOs với ownerName.
     *
     * @param tasks     Danh sách Task entities
     * @param ownerName Tên người sở hữu
     * @return Danh sách TaskDTOs với ownerName
     */
    public static List<TaskDTO> toDTOList(List<Task> tasks, String ownerName) {
        if (tasks == null) {
            return new ArrayList<>();
        }

        // Tạo DAOs để load thông tin bổ sung
        CategoryDAO categoryDAO = new CategoryDAO();
        TaskTagDAO taskTagDAO = new TaskTagDAO();
        TaskReminderDAO reminderDAO = new TaskReminderDAO();

        List<TaskDTO> dtos = new ArrayList<>();
        for (Task task : tasks) {
            dtos.add(toDTO(task, ownerName, categoryDAO, taskTagDAO, reminderDAO));
        }
        return dtos;
    }

    /**
     * Chuyển đổi danh sách Task entities sang danh sách TaskDTOs với đầy đủ thông tin.
     *
     * @param tasks        Danh sách Task entities
     * @param ownerName    Tên người sở hữu
     * @param categoryDAO  DAO để load category
     * @param taskTagDAO   DAO để load tags
     * @param reminderDAO  DAO để load reminder
     * @return Danh sách TaskDTOs với thông tin đầy đủ
     */
    public static List<TaskDTO> toDTOList(List<Task> tasks, String ownerName,
            CategoryDAO categoryDAO, TaskTagDAO taskTagDAO, TaskReminderDAO reminderDAO) {
        if (tasks == null) {
            return new ArrayList<>();
        }

        List<TaskDTO> dtos = new ArrayList<>();
        for (Task task : tasks) {
            dtos.add(toDTO(task, ownerName, categoryDAO, taskTagDAO, reminderDAO));
        }
        return dtos;
    }
}
