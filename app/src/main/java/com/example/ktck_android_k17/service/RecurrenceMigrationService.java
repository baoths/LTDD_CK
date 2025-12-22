package com.example.ktck_android_k17.service;

import android.util.Log;

import com.example.ktck_android_k17.dao.TaskDAO;
import com.example.ktck_android_k17.dao.TaskRecurrenceDAO;
import com.example.ktck_android_k17.model.Task;
import com.example.ktck_android_k17.model.TaskRecurrence;

import java.util.List;

/**
 * Service để migrate dữ liệu recurring tasks từ cách cũ (tạo sẵn instances) 
 * sang cách mới (chỉ lưu master task, generate động).
 */
public class RecurrenceMigrationService {

    private static final String TAG = "RecurrenceMigrationService";
    private TaskDAO taskDAO;
    private TaskRecurrenceDAO recurrenceDAO;

    public RecurrenceMigrationService() {
        taskDAO = new TaskDAO();
        recurrenceDAO = new TaskRecurrenceDAO();
    }

    /**
     * Migrate existing recurring tasks:
     * 1. Tìm tất cả tasks có recurrence
     * 2. Đánh dấu là master (is_master = TRUE)
     * 3. Xóa tất cả instances đã tạo sẵn (có parent_task_id hoặc is_master = FALSE)
     * 4. Giữ lại recurrence rules
     */
    public void migrateExistingRecurringTasks() {
        Log.d(TAG, "Bắt đầu migration recurring tasks...");

        try {
            // Lấy tất cả recurrence rules
            List<TaskRecurrence> recurrences = recurrenceDAO.findActiveRecurrences();
            Log.d(TAG, "Tìm thấy " + recurrences.size() + " recurrence rules");

            int masterCount = 0;
            int deletedCount = 0;

            for (TaskRecurrence recurrence : recurrences) {
                Task masterTask = taskDAO.findById(recurrence.getTaskId());
                if (masterTask == null) {
                    Log.w(TAG, "Master task không tồn tại: " + recurrence.getTaskId());
                    continue;
                }

                // Đánh dấu master task
                masterTask.setIsMaster(true);
                masterTask.setParentTaskId(null);
                masterTask.setOccurrenceDate(null);
                
                if (taskDAO.update(masterTask)) {
                    masterCount++;
                    Log.d(TAG, "Đã đánh dấu master task: " + masterTask.getId());
                }

                // Xóa tất cả instances đã tạo sẵn của master task này
                // Tìm các tasks có parent_task_id = masterTaskId hoặc có cùng title và user_id nhưng không phải master
                List<Task> allUserTasks = taskDAO.findByUserId(masterTask.getUserId());
                for (Task task : allUserTasks) {
                    // Kiểm tra xem có phải instance đã tạo sẵn không
                    // (có cùng title, user_id nhưng khác ID và có startDate/endDate giống recurrence)
                    if (task.getId() != masterTask.getId() &&
                        task.getTitle().equals(masterTask.getTitle()) &&
                        task.getUserId() == masterTask.getUserId()) {
                        
                        // Có thể là instance đã tạo sẵn, xóa nó
                        if (taskDAO.delete(task.getId())) {
                            deletedCount++;
                            Log.d(TAG, "Đã xóa instance: " + task.getId());
                        }
                    }
                }
            }

            Log.d(TAG, "Migration hoàn tất:");
            Log.d(TAG, "  - Đã đánh dấu " + masterCount + " master tasks");
            Log.d(TAG, "  - Đã xóa " + deletedCount + " instances cũ");

        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi migration: " + e.getMessage(), e);
        }
    }
}

