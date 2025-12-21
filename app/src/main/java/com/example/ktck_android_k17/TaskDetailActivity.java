package com.example.ktck_android_k17;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.ktck_android_k17.adapter.TaskAdapter;
import com.example.ktck_android_k17.dao.CategoryDAO;
import com.example.ktck_android_k17.dao.TaskDAO;
import com.example.ktck_android_k17.dao.TaskRecurrenceDAO;
import com.example.ktck_android_k17.dao.TaskReminderDAO;
import com.example.ktck_android_k17.dao.TaskTagDAO;
import com.example.ktck_android_k17.dto.TaskDTO;
import com.example.ktck_android_k17.model.Category;
import com.example.ktck_android_k17.model.Task;
import com.example.ktck_android_k17.model.TaskReminder;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.widget.LinearLayout;

/**
 * Activity hiển thị chi tiết công việc.
 */
public class TaskDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "task_id";
    private static final int EDIT_TASK_REQUEST = 100;

    private ImageButton btnBack;
    private TextView tvTitle, tvPriority, tvStatus, tvDescription, tvDueDate, tvCreatedAt;
    private TextView tvCategory, tvReminder, tvRecurrence;
    private LinearLayout llTags;
    private Button btnEdit, btnMarkComplete, btnDelete;
    private ProgressBar progressBar;

    private TaskDAO taskDAO;
    private CategoryDAO categoryDAO;
    private TaskTagDAO taskTagDAO;
    private TaskRecurrenceDAO taskRecurrenceDAO;
    private TaskReminderDAO taskReminderDAO;
    private ExecutorService executorService;
    private Handler mainHandler;
    private int taskId;
    private TaskDTO currentTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        taskId = getIntent().getIntExtra(EXTRA_TASK_ID, -1);
        if (taskId == -1) {
            Toast.makeText(this, "Không tìm thấy công việc", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initDatabase();
        setupListeners();
        loadTaskDetail();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        tvPriority = findViewById(R.id.tvPriority);
        tvStatus = findViewById(R.id.tvStatus);
        tvDescription = findViewById(R.id.tvDescription);
        tvDueDate = findViewById(R.id.tvDueDate);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        tvCategory = findViewById(R.id.tvCategory);
        tvReminder = findViewById(R.id.tvReminder);
        tvRecurrence = findViewById(R.id.tvRecurrence);
        llTags = findViewById(R.id.llTags);
        btnEdit = findViewById(R.id.btnEdit);
        btnMarkComplete = findViewById(R.id.btnMarkComplete);
        btnDelete = findViewById(R.id.btnDelete);
        progressBar = findViewById(R.id.progressBar);
    }

    private void initDatabase() {
        taskDAO = new TaskDAO();
        categoryDAO = new CategoryDAO();
        taskTagDAO = new TaskTagDAO();
        taskRecurrenceDAO = new TaskRecurrenceDAO();
        taskReminderDAO = new TaskReminderDAO();
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditTaskActivity.class);
            intent.putExtra(EditTaskActivity.EXTRA_TASK_ID, taskId);
            startActivityForResult(intent, EDIT_TASK_REQUEST);
        });

        btnMarkComplete.setOnClickListener(v -> markAsComplete());

        btnDelete.setOnClickListener(v -> showDeleteConfirmDialog());
    }

    private void loadTaskDetail() {
        showLoading(true);

        executorService.execute(() -> {
            Task task = taskDAO.findById(taskId);
            if (task == null) {
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(this, "Không tìm thấy công việc", Toast.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }

            // Load full task details with category, tags, reminder
            TaskDTO dto = TaskAdapter.toDTO(task, null, categoryDAO, taskTagDAO, taskReminderDAO);

            mainHandler.post(() -> {
                showLoading(false);
                if (dto != null) {
                    currentTask = dto;
                    displayTaskDetail(dto);
                } else {
                    Toast.makeText(this, "Không tìm thấy công việc", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    private void displayTaskDetail(TaskDTO task) {
        tvTitle.setText(task.getTitle());

        // Description
        String description = task.getDescription();
        if (description != null && !description.isEmpty()) {
            tvDescription.setText(description);
        } else {
            tvDescription.setText(getString(R.string.no_description));
            tvDescription.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }

        // Due Date - Format from MySQL to user format
        String dueDate = task.getDueDate();
        if (dueDate != null && !dueDate.isEmpty()) {
            try {
                // Try to parse MySQL format (yyyy-MM-dd) and convert to user format (dd/MM/yyyy)
                if (dueDate.contains("-") && dueDate.length() == 10) {
                    SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    java.util.Date date = dbFormat.parse(dueDate);
                    if (date != null) {
                        dueDate = displayFormat.format(date);
                    }
                }
            } catch (Exception e) {
                // Keep original format if parsing fails
            }
            tvDueDate.setText(dueDate);
        } else {
            tvDueDate.setText(getString(R.string.no_due_date));
            tvDueDate.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }

        // Category
        String categoryName = task.getCategoryName();
        if (categoryName != null && !categoryName.isEmpty()) {
            tvCategory.setText(categoryName);
            tvCategory.setVisibility(View.VISIBLE);
        } else {
            tvCategory.setText("Không có danh mục");
            tvCategory.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvCategory.setVisibility(View.VISIBLE);
        }

        // Tags
        if (llTags != null) {
            List<String> tags = task.getTags();
            if (tags != null && !tags.isEmpty()) {
                llTags.removeAllViews();
                llTags.setVisibility(View.VISIBLE);
                for (String tag : tags) {
                    if (tag != null && !tag.trim().isEmpty()) {
                        TextView tagView = new TextView(this);
                        tagView.setText("#" + tag);
                        tagView.setTextSize(14);
                        tagView.setTextColor(android.graphics.Color.parseColor("#666666"));
                        tagView.setPadding(12, 6, 12, 6);
                        tagView.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"));
                        android.view.ViewGroup.MarginLayoutParams params = new android.view.ViewGroup.MarginLayoutParams(
                                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                        params.setMargins(0, 0, 8, 0);
                        tagView.setLayoutParams(params);
                        llTags.addView(tagView);
                    }
                }
            } else {
                llTags.setVisibility(View.GONE);
            }
        }

        // Reminder
        String reminderTime = task.getReminderTime();
        if (reminderTime != null && !reminderTime.isEmpty()) {
            try {
                // Try to parse MySQL format (yyyy-MM-dd HH:mm:ss) and convert to user format (dd/MM/yyyy HH:mm)
                if (reminderTime.contains("-") && reminderTime.length() >= 16) {
                    SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    java.util.Date date = dbFormat.parse(reminderTime);
                    if (date != null) {
                        reminderTime = displayFormat.format(date);
                    }
                }
            } catch (Exception e) {
                // Keep original format if parsing fails
            }
            tvReminder.setText(reminderTime);
            tvReminder.setVisibility(View.VISIBLE);
        } else {
            tvReminder.setText("Không có nhắc nhở");
            tvReminder.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvReminder.setVisibility(View.VISIBLE);
        }

        // Recurrence
        loadAndDisplayRecurrence(task.getId());

        // Created At - Format if needed
        String createdAt = task.getCreatedAt();
        if (createdAt != null) {
            try {
                // Try to parse MySQL format and convert to user format
                if (createdAt.contains("-") && createdAt.length() >= 16) {
                    SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    java.util.Date date = dbFormat.parse(createdAt);
                    if (date != null) {
                        createdAt = displayFormat.format(date);
                    }
                }
            } catch (Exception e) {
                // Keep original format if parsing fails
            }
            tvCreatedAt.setText(createdAt);
        }

        // Priority Badge
        setPriorityBadge(task.getPriority());

        // Status Badge
        setStatusBadge(task.getStatus());

        // Hide mark complete button if already completed
        if (Task.STATUS_COMPLETED.equals(task.getStatus())) {
            btnMarkComplete.setVisibility(View.GONE);
        } else {
            btnMarkComplete.setVisibility(View.VISIBLE);
        }
    }

    private void setPriorityBadge(String priority) {
        int colorRes;
        String text;

        if (Task.PRIORITY_HIGH.equals(priority)) {
            colorRes = R.color.priority_high;
            text = getString(R.string.priority_high);
        } else if (Task.PRIORITY_LOW.equals(priority)) {
            colorRes = R.color.priority_low;
            text = getString(R.string.priority_low);
        } else {
            colorRes = R.color.priority_medium;
            text = getString(R.string.priority_medium);
        }

        tvPriority.setText(text);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ContextCompat.getColor(this, colorRes));
        bg.setCornerRadius(16);
        tvPriority.setBackground(bg);
    }

    private void setStatusBadge(String status) {
        int colorRes;
        String text;

        if (Task.STATUS_COMPLETED.equals(status)) {
            colorRes = R.color.status_completed;
            text = getString(R.string.status_completed);
        } else if (Task.STATUS_IN_PROGRESS.equals(status)) {
            colorRes = R.color.status_in_progress;
            text = getString(R.string.status_in_progress);
        } else {
            colorRes = R.color.status_pending;
            text = getString(R.string.status_pending);
        }

        tvStatus.setText(text);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ContextCompat.getColor(this, colorRes));
        bg.setCornerRadius(16);
        tvStatus.setBackground(bg);
    }

    private void markAsComplete() {
        showLoading(true);

        executorService.execute(() -> {
            boolean success = taskDAO.updateStatus(taskId, Task.STATUS_COMPLETED);

            mainHandler.post(() -> {
                showLoading(false);
                if (success) {
                    Toast.makeText(this, getString(R.string.success_task_updated), Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    loadTaskDetail(); // Reload to update UI
                } else {
                    Toast.makeText(this, "Không thể cập nhật", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_confirm))
                .setMessage(getString(R.string.dialog_delete_task))
                .setPositiveButton(getString(R.string.btn_delete), (dialog, which) -> deleteTask())
                .setNegativeButton(getString(R.string.dialog_cancel), null)
                .show();
    }

    private void deleteTask() {
        showLoading(true);

        executorService.execute(() -> {
            boolean success = taskDAO.delete(taskId);

            mainHandler.post(() -> {
                showLoading(false);
                if (success) {
                    Toast.makeText(this, getString(R.string.success_task_deleted), Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(this, "Không thể xóa", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_TASK_REQUEST && resultCode == RESULT_OK) {
            loadTaskDetail(); // Reload task after edit
            setResult(RESULT_OK);
        }
    }

    /**
     * Load and display recurrence information.
     */
    private void loadAndDisplayRecurrence(int taskId) {
        if (tvRecurrence == null) {
            return;
        }
        
        executorService.execute(() -> {
            com.example.ktck_android_k17.model.TaskRecurrence recurrence = taskRecurrenceDAO.findByTaskId(taskId);
            mainHandler.post(() -> {
                if (recurrence != null && recurrence.isActive()) {
                    String recurrenceText = formatRecurrenceText(recurrence);
                    tvRecurrence.setText(recurrenceText);
                    tvRecurrence.setVisibility(View.VISIBLE);
                } else {
                    tvRecurrence.setText("Không lặp lại");
                    tvRecurrence.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                    tvRecurrence.setVisibility(View.VISIBLE);
                }
            });
        });
    }
    
    /**
     * Format recurrence text for display.
     */
    private String formatRecurrenceText(com.example.ktck_android_k17.model.TaskRecurrence recurrence) {
        String type = recurrence.getRecurrenceType();
        int interval = recurrence.getRecurrenceInterval();
        
        String text = "";
        switch (type) {
            case com.example.ktck_android_k17.model.TaskRecurrence.TYPE_DAILY:
                if (interval == 1) {
                    text = "Hàng ngày";
                } else {
                    text = "Mỗi " + interval + " ngày";
                }
                break;
            case com.example.ktck_android_k17.model.TaskRecurrence.TYPE_WEEKLY:
                if (interval == 1) {
                    text = "Hàng tuần";
                } else {
                    text = "Mỗi " + interval + " tuần";
                }
                break;
            case com.example.ktck_android_k17.model.TaskRecurrence.TYPE_MONTHLY:
                if (interval == 1) {
                    text = "Hàng tháng";
                } else {
                    text = "Mỗi " + interval + " tháng";
                }
                break;
            case com.example.ktck_android_k17.model.TaskRecurrence.TYPE_CUSTOM:
                text = "Mỗi " + interval + " ngày";
                break;
            default:
                text = "Lặp lại";
        }
        
        if (recurrence.getRecurrenceEndDate() != null && !recurrence.getRecurrenceEndDate().isEmpty()) {
            try {
                SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                java.util.Date date = dbFormat.parse(recurrence.getRecurrenceEndDate());
                if (date != null) {
                    text += " (đến " + displayFormat.format(date) + ")";
                }
            } catch (Exception e) {
                // Ignore parsing error
            }
        }
        
        return text;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
