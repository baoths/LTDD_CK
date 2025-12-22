package com.example.ktck_android_k17.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ktck_android_k17.R;
import com.example.ktck_android_k17.dto.TaskDTO;
import com.example.ktck_android_k17.model.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter cho Kanban board columns.
 * Hiển thị tasks dưới dạng cards có thể kéo thả.
 */
public class KanbanTaskAdapter extends RecyclerView.Adapter<KanbanTaskAdapter.KanbanTaskViewHolder> {

    private List<TaskDTO> taskList;
    private String columnStatus; // Status của column này (pending, in_progress, completed)
    private OnTaskStatusChangeListener statusChangeListener;
    private OnTaskLongClickListener longClickListener;
    
    /**
     * Interface để xử lý khi task được long press.
     */
    public interface OnTaskLongClickListener {
        boolean onTaskLongClick(TaskDTO task);
    }

    /**
     * Interface để xử lý khi task được kéo sang column khác (status thay đổi).
     */
    public interface OnTaskStatusChangeListener {
        void onTaskStatusChanged(TaskDTO task, String newStatus);
    }

    public KanbanTaskAdapter(String columnStatus) {
        this.columnStatus = columnStatus;
        this.taskList = new ArrayList<>();
    }

    public void setOnTaskStatusChangeListener(OnTaskStatusChangeListener listener) {
        this.statusChangeListener = listener;
    }
    
    public void setOnTaskLongClickListener(OnTaskLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setTaskList(List<TaskDTO> tasks) {
        this.taskList = tasks != null ? tasks : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addTask(TaskDTO task) {
        if (task != null) {
            taskList.add(task);
            notifyItemInserted(taskList.size() - 1);
        }
    }

    public void removeTask(TaskDTO task) {
        int position = taskList.indexOf(task);
        if (position >= 0) {
            taskList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public TaskDTO getTaskAt(int position) {
        if (position >= 0 && position < taskList.size()) {
            return taskList.get(position);
        }
        return null;
    }

    @NonNull
    @Override
    public KanbanTaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_kanban_task, parent, false);
        return new KanbanTaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull KanbanTaskViewHolder holder, int position) {
        TaskDTO task = taskList.get(position);
        if (task != null) {
            holder.bind(task);
        }
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    /**
     * ViewHolder cho Kanban task card.
     */
    class KanbanTaskViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTitle, tvPriority, tvCategory, tvDueDate, tvEndDate, tvOverdueWarning;

        public KanbanTaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);
            tvEndDate = itemView.findViewById(R.id.tvEndDate);
            tvOverdueWarning = itemView.findViewById(R.id.tvOverdueWarning);
            
            // Setup click listener để mở task detail
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && longClickListener != null) {
                    TaskDTO task = taskList.get(position);
                    longClickListener.onTaskLongClick(task);
                }
            });
            
            // Setup long press listener để enable drag
            itemView.setOnLongClickListener(v -> {
                // Return false để allow ItemTouchHelper handle the drag
                return false;
            });
        }

        public void bind(TaskDTO task) {
            if (tvTitle != null) {
                tvTitle.setText(task.getTitle());
            }

            // Priority badge
            if (tvPriority != null) {
                String priority = task.getPriority();
                if (priority != null) {
                    String priorityText = "";
                    int priorityColor = Color.parseColor("#3498db");
                    
                    switch (priority) {
                        case Task.PRIORITY_HIGH:
                            priorityText = "Cao";
                            priorityColor = Color.parseColor("#e74c3c");
                            break;
                        case Task.PRIORITY_MEDIUM:
                            priorityText = "TB";
                            priorityColor = Color.parseColor("#f39c12");
                            break;
                        case Task.PRIORITY_LOW:
                            priorityText = "Thấp";
                            priorityColor = Color.parseColor("#3498db");
                            break;
                    }
                    
                    tvPriority.setText(priorityText);
                    tvPriority.setBackgroundColor(priorityColor);
                    tvPriority.setVisibility(View.VISIBLE);
                } else {
                    tvPriority.setVisibility(View.GONE);
                }
            }

            // Category badge or recurrence indicator
            if (tvCategory != null) {
                if (task.getIsRecurringMaster() != null && task.getIsRecurringMaster()) {
                    // Show recurrence indicator for master task
                    String recurrenceInfo = task.getRecurrenceInfo();
                    if (recurrenceInfo != null && !recurrenceInfo.isEmpty()) {
                        tvCategory.setText("🔄 " + recurrenceInfo);
                    } else {
                        tvCategory.setText("🔄 Lặp lại");
                    }
                    tvCategory.setVisibility(View.VISIBLE);
                } else if (task.getParentTaskId() != null && task.getParentTaskId() > 0) {
                    // Show instance indicator
                    tvCategory.setText("📋 Instance");
                    tvCategory.setVisibility(View.VISIBLE);
                } else {
                    // Show category for normal tasks
                    String categoryName = task.getCategoryName();
                    if (categoryName != null && !categoryName.isEmpty()) {
                        tvCategory.setText(categoryName);
                        tvCategory.setVisibility(View.VISIBLE);
                    } else {
                        tvCategory.setVisibility(View.GONE);
                    }
                }
            }

            // Due date
            if (tvDueDate != null) {
                String dueDate = task.getDueDate();
                if (dueDate != null && !dueDate.isEmpty()) {
                    try {
                        // Parse from MySQL format (yyyy-MM-dd) to user format (dd/MM/yyyy)
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
                    tvDueDate.setText("Bắt đầu: " + dueDate);
                    tvDueDate.setVisibility(View.VISIBLE);
                } else {
                    tvDueDate.setVisibility(View.GONE);
                }
            }

            // End date
            if (tvEndDate != null) {
                String endDate = task.getEndDate();
                if (endDate != null && !endDate.isEmpty()) {
                    try {
                        // Parse from MySQL format (yyyy-MM-dd) to user format (dd/MM/yyyy)
                        if (endDate.contains("-") && endDate.length() == 10) {
                            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                            java.util.Date date = dbFormat.parse(endDate);
                            if (date != null) {
                                endDate = displayFormat.format(date);
                            }
                        }
                    } catch (Exception e) {
                        // Keep original format if parsing fails
                    }
                    tvEndDate.setText("Kết thúc: " + endDate);
                    tvEndDate.setVisibility(View.VISIBLE);
                    
                    // Check if overdue
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        java.util.Date endDateObj = sdf.parse(task.getEndDate());
                        java.util.Date today = new java.util.Date();
                        if (endDateObj != null && endDateObj.before(today)) {
                            // Task is overdue
                            tvOverdueWarning.setVisibility(View.VISIBLE);
                            tvEndDate.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_red_dark));
                        } else {
                            tvOverdueWarning.setVisibility(View.GONE);
                            tvEndDate.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
                        }
                    } catch (Exception e) {
                        // Error parsing date, hide warning
                        tvOverdueWarning.setVisibility(View.GONE);
                    }
                } else {
                    tvEndDate.setVisibility(View.GONE);
                    tvOverdueWarning.setVisibility(View.GONE);
                }
            }
        }
    }
}

