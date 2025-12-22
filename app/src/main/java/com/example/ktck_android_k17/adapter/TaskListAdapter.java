package com.example.ktck_android_k17.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ktck_android_k17.R;
import com.example.ktck_android_k17.dto.TaskDTO;
import com.example.ktck_android_k17.model.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter để hiển thị danh sách Task.
 * Sử dụng TaskDTO để hiển thị dữ liệu.
 */
public class TaskListAdapter extends RecyclerView.Adapter<TaskListAdapter.TaskViewHolder> {

    private List<TaskDTO> taskList;
    private OnTaskClickListener listener;

    /**
     * Interface để xử lý sự kiện click trên task item.
     */
    public interface OnTaskClickListener {
        void onTaskClick(TaskDTO task);

        void onTaskLongClick(TaskDTO task);
    }

    public TaskListAdapter() {
        this.taskList = new ArrayList<>();
    }

    public TaskListAdapter(List<TaskDTO> taskList) {
        this.taskList = taskList != null ? taskList : new ArrayList<>();
    }

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.listener = listener;
    }

    /**
     * Cập nhật danh sách task và refresh RecyclerView.
     *
     * @param tasks Danh sách TaskDTO mới
     */
    public void setTaskList(List<TaskDTO> tasks) {
        this.taskList = tasks != null ? tasks : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * Thêm một task mới vào đầu danh sách.
     *
     * @param task TaskDTO mới
     */
    public void addTask(TaskDTO task) {
        if (task != null) {
            taskList.add(0, task);
            notifyItemInserted(0);
        }
    }

    /**
     * Xóa một task khỏi danh sách.
     *
     * @param position Vị trí của task cần xóa
     */
    public void removeTask(int position) {
        if (position >= 0 && position < taskList.size()) {
            taskList.remove(position);
            notifyItemRemoved(position);
        }
    }

    /**
     * Cập nhật một task trong danh sách.
     *
     * @param position Vị trí của task
     * @param task     TaskDTO đã cập nhật
     */
    public void updateTask(int position, TaskDTO task) {
        if (position >= 0 && position < taskList.size() && task != null) {
            taskList.set(position, task);
            notifyItemChanged(position);
        }
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskDTO task = taskList.get(position);
        holder.bind(task);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    /**
     * ViewHolder class cho task item.
     */
    class TaskViewHolder extends RecyclerView.ViewHolder {
        private View viewStatusIndicator;
        private TextView tvTaskTitle;
        private TextView tvTaskDescription;
        private TextView tvDueDate;
        private TextView tvEndDate;
        private TextView tvOverdueWarning;
        private TextView tvStatus;
        private TextView tvPriority;
        private TextView tvCategory;
        private TextView tvReminder;
        private LinearLayout llTags;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);

            viewStatusIndicator = itemView.findViewById(R.id.viewStatusIndicator);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvTaskDescription = itemView.findViewById(R.id.tvTaskDescription);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);
            tvEndDate = itemView.findViewById(R.id.tvEndDate);
            tvOverdueWarning = itemView.findViewById(R.id.tvOverdueWarning);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvReminder = itemView.findViewById(R.id.tvReminder);
            llTags = itemView.findViewById(R.id.llTags);

            // Set click listeners
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTaskClick(taskList.get(position));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTaskLongClick(taskList.get(position));
                    return true;
                }
                return false;
            });
        }

        /**
         * Bind data từ TaskDTO vào views.
         *
         * @param task TaskDTO cần hiển thị
         */
        public void bind(TaskDTO task) {
            tvTaskTitle.setText(task.getTitle());
            tvTaskDescription.setText(task.getDescription());

            // Format due date - convert from MySQL format to user format if needed
            String dueDate = task.getDueDate();
            if (dueDate != null && !dueDate.isEmpty()) {
                try {
                    // Try to parse MySQL format (yyyy-MM-dd) and convert to user format (dd/MM/yyyy)
                    if (dueDate.contains("-") && dueDate.length() == 10) {
                        java.text.SimpleDateFormat dbFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                        java.text.SimpleDateFormat displayFormat = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                        java.util.Date date = dbFormat.parse(dueDate);
                        if (date != null) {
                            dueDate = displayFormat.format(date);
                        }
                    }
                } catch (Exception e) {
                    // Keep original format if parsing fails
                }
                tvDueDate.setText("Bắt đầu: " + dueDate);
                tvDueDate.setVisibility(View.VISIBLE);
            } else {
                tvDueDate.setVisibility(View.GONE);
            }

            // Format end date - convert from MySQL format to user format if needed
            String endDate = task.getEndDate();
            if (tvEndDate != null) {
                if (endDate != null && !endDate.isEmpty()) {
                    String endDateDisplay = endDate;
                    try {
                        // Try to parse MySQL format (yyyy-MM-dd) and convert to user format (dd/MM/yyyy)
                        if (endDate.contains("-") && endDate.length() == 10) {
                            java.text.SimpleDateFormat dbFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                            java.text.SimpleDateFormat displayFormat = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                            java.util.Date date = dbFormat.parse(endDate);
                            if (date != null) {
                                endDateDisplay = displayFormat.format(date);
                            }
                        }
                    } catch (Exception e) {
                        // Keep original format if parsing fails
                    }
                    tvEndDate.setText("Kết thúc: " + endDateDisplay);
                    tvEndDate.setVisibility(View.VISIBLE);
                    
                    // Check if overdue
                    try {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                        java.util.Date endDateObj = sdf.parse(endDate);
                        java.util.Date today = new java.util.Date();
                        if (endDateObj != null && endDateObj.before(today)) {
                            // Task is overdue
                            tvOverdueWarning.setVisibility(View.VISIBLE);
                            tvEndDate.setTextColor(Color.parseColor("#E53935")); // Red color
                        } else {
                            tvOverdueWarning.setVisibility(View.GONE);
                            tvEndDate.setTextColor(Color.parseColor("#666666")); // Default gray
                        }
                    } catch (Exception e) {
                        // Error parsing date, hide warning
                        tvOverdueWarning.setVisibility(View.GONE);
                    }
                } else {
                    tvEndDate.setVisibility(View.GONE);
                    if (tvOverdueWarning != null) {
                        tvOverdueWarning.setVisibility(View.GONE);
                    }
                }
            }

            // Display recurrence indicator for master task
            if (task.getIsRecurringMaster() != null && task.getIsRecurringMaster()) {
                // Show recurrence badge
                if (tvCategory != null) {
                    String recurrenceInfo = task.getRecurrenceInfo();
                    if (recurrenceInfo != null && !recurrenceInfo.isEmpty()) {
                        tvCategory.setText("🔄 " + recurrenceInfo);
                    } else {
                        tvCategory.setText("🔄 Lặp lại");
                    }
                    tvCategory.setVisibility(View.VISIBLE);
                }
            } else if (task.getParentTaskId() != null && task.getParentTaskId() > 0) {
                // Show instance indicator
                if (tvCategory != null) {
                    tvCategory.setText("📋 Instance");
                    tvCategory.setVisibility(View.VISIBLE);
                }
            } else {
                // Display category for normal tasks
                if (tvCategory != null) {
                    if (task.getCategoryName() != null && !task.getCategoryName().isEmpty()) {
                        tvCategory.setText("📁 " + task.getCategoryName());
                        tvCategory.setVisibility(View.VISIBLE);
                    } else {
                        tvCategory.setVisibility(View.GONE);
                    }
                }
            }

            // Display reminder
            if (tvReminder != null) {
                String reminderTime = task.getReminderTime();
                if (reminderTime != null && !reminderTime.isEmpty()) {
                    try {
                        // Try to parse MySQL format (yyyy-MM-dd HH:mm:ss) and convert to user format (dd/MM/yyyy HH:mm)
                        if (reminderTime.contains("-") && reminderTime.length() >= 16) {
                            java.text.SimpleDateFormat dbFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                            java.text.SimpleDateFormat displayFormat = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
                            java.util.Date date = dbFormat.parse(reminderTime);
                            if (date != null) {
                                reminderTime = displayFormat.format(date);
                            }
                        }
                    } catch (Exception e) {
                        // Keep original format if parsing fails
                    }
                    tvReminder.setText("⏰ " + reminderTime);
                    tvReminder.setVisibility(View.VISIBLE);
                } else {
                    tvReminder.setVisibility(View.GONE);
                }
            }

            // Display tags
            if (llTags != null) {
                if (task.getTags() != null && !task.getTags().isEmpty()) {
                    llTags.removeAllViews();
                    llTags.setVisibility(View.VISIBLE);
                    for (String tag : task.getTags()) {
                        if (tag != null && !tag.trim().isEmpty()) {
                            TextView tagView = new TextView(itemView.getContext());
                            tagView.setText("#" + tag);
                            tagView.setTextSize(10);
                            tagView.setTextColor(Color.parseColor("#666666"));
                            tagView.setPadding(6, 2, 6, 2);
                            tagView.setBackgroundColor(Color.parseColor("#E0E0E0"));
                            android.view.ViewGroup.MarginLayoutParams params = new android.view.ViewGroup.MarginLayoutParams(
                                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                            params.setMargins(0, 0, 4, 0);
                            tagView.setLayoutParams(params);
                            llTags.addView(tagView);
                        }
                    }
                } else {
                    llTags.setVisibility(View.GONE);
                }
            }

            // Set status color and text
            String status = task.getStatus();
            int statusColor;
            String statusText;

            switch (status) {
                case Task.STATUS_IN_PROGRESS:
                    statusColor = Color.parseColor("#2196F3"); // Blue
                    statusText = "Đang thực hiện";
                    break;
                case Task.STATUS_COMPLETED:
                    statusColor = Color.parseColor("#4CAF50"); // Green
                    statusText = "Hoàn thành";
                    break;
                case Task.STATUS_PENDING:
                default:
                    statusColor = Color.parseColor("#FF9800"); // Orange
                    statusText = "Chờ xử lý";
                    break;
            }

            viewStatusIndicator.setBackgroundColor(statusColor);
            tvStatus.setText(statusText);
            tvStatus.setTextColor(statusColor);

            // Set priority badge
            String priority = task.getPriority();
            int priorityColor;
            String priorityText;

            if (Task.PRIORITY_HIGH.equals(priority)) {
                priorityColor = Color.parseColor("#E53935"); // Red
                priorityText = "Cao";
            } else if (Task.PRIORITY_LOW.equals(priority)) {
                priorityColor = Color.parseColor("#43A047"); // Green
                priorityText = "Thấp";
            } else {
                priorityColor = Color.parseColor("#FFC107"); // Yellow
                priorityText = "TB";
            }

            tvPriority.setText(priorityText);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(priorityColor);
            bg.setCornerRadius(16);
            tvPriority.setBackground(bg);
        }
    }
}
