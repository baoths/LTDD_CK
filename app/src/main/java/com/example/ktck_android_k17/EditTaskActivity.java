package com.example.ktck_android_k17;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ktck_android_k17.dao.CategoryDAO;
import com.example.ktck_android_k17.dao.TaskDAO;
import com.example.ktck_android_k17.dao.TaskRecurrenceDAO;
import com.example.ktck_android_k17.dao.TaskReminderDAO;
import com.example.ktck_android_k17.dialog.RecurrenceEditDialog;
import com.example.ktck_android_k17.service.TaskRecurrenceService;
// TaskTagDAO removed - tags no longer supported
import com.example.ktck_android_k17.model.Category;
import com.example.ktck_android_k17.model.Task;
import com.example.ktck_android_k17.model.TaskRecurrence;
import com.example.ktck_android_k17.model.TaskReminder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity để chỉnh sửa công việc.
 */
public class EditTaskActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "task_id";

    private ImageButton btnBack;
    private EditText edtTitle, edtDescription, edtDueDate, edtReminder;
    private EditText edtRecurrenceInterval, edtRecurrenceEndDate;
    private Spinner spinnerPriority, spinnerStatus, spinnerCategory, spinnerRecurrenceType;
    private android.widget.Switch switchRecurrence;
    private android.widget.LinearLayout recurrenceSection, layoutRecurrenceInterval, layoutWeeklyDays;
    private android.widget.TextView tvRecurrenceUnit;
    private android.widget.CheckBox chkMonday, chkTuesday, chkWednesday, chkThursday, chkFriday, chkSaturday, chkSunday;
    private Button btnSave;
    private ProgressBar progressBar;

    private TaskDAO taskDAO;
    private CategoryDAO categoryDAO;
    private TaskRecurrenceDAO taskRecurrenceDAO;
    private TaskReminderDAO taskReminderDAO;
    // TaskTagDAO removed - tags no longer supported
    private ExecutorService executorService;
    private Handler mainHandler;
    private int taskId;
    private Task currentTask;
    private Calendar selectedDate;
    private Calendar selectedReminderTime;
    private Calendar selectedRecurrenceEndDate;
    private List<Category> categories;
    
    // Recurrence type options
    private final String[] recurrenceTypeOptions = { "Hàng ngày", "Hàng tuần", "Hàng tháng", "Tùy chỉnh" };
    private final String[] recurrenceTypeValues = { 
        TaskRecurrence.TYPE_DAILY, 
        TaskRecurrence.TYPE_WEEKLY, 
        TaskRecurrence.TYPE_MONTHLY, 
        TaskRecurrence.TYPE_CUSTOM 
    };

    // Priority options
    private final String[] priorityOptions = { "Thấp", "Trung bình", "Cao" };
    private final String[] priorityValues = { Task.PRIORITY_LOW, Task.PRIORITY_MEDIUM, Task.PRIORITY_HIGH };

    // Status options
    private final String[] statusOptions = { "Chờ xử lý", "Đang thực hiện", "Hoàn thành" };
    private final String[] statusValues = { Task.STATUS_PENDING, Task.STATUS_IN_PROGRESS, Task.STATUS_COMPLETED };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        taskId = getIntent().getIntExtra(EXTRA_TASK_ID, -1);
        if (taskId == -1) {
            Toast.makeText(this, "Không tìm thấy công việc", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initDatabase();
        setupRecurrenceSpinner();
        setupListeners();
        loadCategories(); // Load categories first, then setup spinners in callback
        loadTaskData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtTitle = findViewById(R.id.edtTitle);
        edtDescription = findViewById(R.id.edtDescription);
        edtDueDate = findViewById(R.id.edtDueDate);
        // Tags removed - no longer supported
        edtReminder = findViewById(R.id.edtReminder);
        spinnerPriority = findViewById(R.id.spinnerPriority);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);
        
        // Recurrence views
        switchRecurrence = findViewById(R.id.switchRecurrence);
        recurrenceSection = findViewById(R.id.recurrenceSection);
        spinnerRecurrenceType = findViewById(R.id.spinnerRecurrenceType);
        edtRecurrenceInterval = findViewById(R.id.edtRecurrenceInterval);
        edtRecurrenceEndDate = findViewById(R.id.edtRecurrenceEndDate);
        layoutRecurrenceInterval = findViewById(R.id.layoutRecurrenceInterval);
        layoutWeeklyDays = findViewById(R.id.layoutWeeklyDays);
        tvRecurrenceUnit = findViewById(R.id.tvRecurrenceUnit);
        chkMonday = findViewById(R.id.chkMonday);
        chkTuesday = findViewById(R.id.chkTuesday);
        chkWednesday = findViewById(R.id.chkWednesday);
        chkThursday = findViewById(R.id.chkThursday);
        chkFriday = findViewById(R.id.chkFriday);
        chkSaturday = findViewById(R.id.chkSaturday);
        chkSunday = findViewById(R.id.chkSunday);

        selectedDate = Calendar.getInstance();
        selectedReminderTime = Calendar.getInstance();
        selectedRecurrenceEndDate = Calendar.getInstance();
        categories = new ArrayList<>();
    }

    private void initDatabase() {
        taskDAO = new TaskDAO();
        categoryDAO = new CategoryDAO();
        taskRecurrenceDAO = new TaskRecurrenceDAO();
        taskReminderDAO = new TaskReminderDAO();
        // TaskTagDAO removed
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    private void setupSpinners() {
        // Priority Spinner
        if (spinnerPriority != null) {
            try {
                ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(
                        this, android.R.layout.simple_spinner_item, priorityOptions);
                priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerPriority.setAdapter(priorityAdapter);
            } catch (Exception e) {
                android.util.Log.e("EditTaskActivity", "Error setting up priority spinner: " + e.getMessage(), e);
            }
        }

        // Status Spinner
        if (spinnerStatus != null) {
            try {
                ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                        this, android.R.layout.simple_spinner_item, statusOptions);
                statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerStatus.setAdapter(statusAdapter);
            } catch (Exception e) {
                android.util.Log.e("EditTaskActivity", "Error setting up status spinner: " + e.getMessage(), e);
            }
        }

        // Category Spinner - only setup if categories are loaded
        if (categories != null) {
            setupCategorySpinner();
        }
    }

    private void setupCategorySpinner() {
        if (spinnerCategory == null) {
            android.util.Log.w("EditTaskActivity", "spinnerCategory is null, cannot setup adapter");
            return;
        }

        try {
            List<String> categoryNames = new ArrayList<>();
            categoryNames.add("-- Không có danh mục --"); // Option mặc định
            if (categories != null) {
                for (Category cat : categories) {
                    categoryNames.add(cat.getName());
                }
            }

            ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, categoryNames);
            categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategory.setAdapter(categoryAdapter);
        } catch (Exception e) {
            android.util.Log.e("EditTaskActivity", "Error setting up category spinner: " + e.getMessage(), e);
        }
    }

    private void loadCategories() {
        executorService.execute(() -> {
            SharedPreferences prefs = getSharedPreferences("TaskManagerPrefs", MODE_PRIVATE);
            int userId = prefs.getInt("user_id", -1);
            List<Category> loadedCategories = categoryDAO.findByUserId(userId);

            mainHandler.post(() -> {
                if (loadedCategories != null) {
                    categories = loadedCategories;
                } else {
                    categories = new ArrayList<>();
                }
                // Setup spinners after categories are loaded
                setupSpinners();
                // Also update category spinner in form if task data is already loaded
                if (currentTask != null) {
                    updateCategorySelection();
                }
            });
        });
    }
    
    /**
     * Update category selection in spinner after categories are loaded.
     */
    private void updateCategorySelection() {
        if (spinnerCategory != null && currentTask != null && categories != null) {
            if (currentTask.getCategoryId() > 0) {
                for (int i = 0; i < categories.size(); i++) {
                    if (categories.get(i).getId() == currentTask.getCategoryId()) {
                        spinnerCategory.setSelection(i + 1); // +1 because position 0 is "Không có danh mục"
                        return;
                    }
                }
            } else {
                spinnerCategory.setSelection(0); // "Không có danh mục"
            }
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        if (edtDueDate != null) {
            edtDueDate.setOnClickListener(v -> showDatePicker());
        }

        if (edtReminder != null) {
            edtReminder.setOnClickListener(v -> showReminderPicker());
        }
        
        // Recurrence listeners
        if (switchRecurrence != null) {
            switchRecurrence.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (recurrenceSection != null) {
                    recurrenceSection.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
            });
        }
        
        if (spinnerRecurrenceType != null) {
            spinnerRecurrenceType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    updateRecurrenceUI(position);
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {
                }
            });
        }
        
        if (edtRecurrenceEndDate != null) {
            edtRecurrenceEndDate.setOnClickListener(v -> showRecurrenceEndDatePicker());
        }

        btnSave.setOnClickListener(v -> saveTask());
    }
    
    /**
     * Cập nhật UI dựa trên recurrence type được chọn.
     */
    private void updateRecurrenceUI(int position) {
        if (layoutRecurrenceInterval == null || tvRecurrenceUnit == null || layoutWeeklyDays == null) {
            return;
        }
        
        String type = recurrenceTypeValues[position];
        switch (type) {
            case TaskRecurrence.TYPE_DAILY:
                layoutRecurrenceInterval.setVisibility(View.VISIBLE);
                layoutWeeklyDays.setVisibility(View.GONE);
                tvRecurrenceUnit.setText("ngày");
                break;
            case TaskRecurrence.TYPE_WEEKLY:
                layoutRecurrenceInterval.setVisibility(View.VISIBLE);
                layoutWeeklyDays.setVisibility(View.VISIBLE);
                tvRecurrenceUnit.setText("tuần");
                break;
            case TaskRecurrence.TYPE_MONTHLY:
                layoutRecurrenceInterval.setVisibility(View.VISIBLE);
                layoutWeeklyDays.setVisibility(View.GONE);
                tvRecurrenceUnit.setText("tháng");
                break;
            case TaskRecurrence.TYPE_CUSTOM:
                layoutRecurrenceInterval.setVisibility(View.VISIBLE);
                layoutWeeklyDays.setVisibility(View.GONE);
                tvRecurrenceUnit.setText("ngày");
                break;
            default:
                layoutRecurrenceInterval.setVisibility(View.GONE);
                layoutWeeklyDays.setVisibility(View.GONE);
        }
    }
    
    /**
     * Hiển thị DatePicker cho recurrence end date.
     */
    private void showRecurrenceEndDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedRecurrenceEndDate.set(Calendar.YEAR, year);
                    selectedRecurrenceEndDate.set(Calendar.MONTH, month);
                    selectedRecurrenceEndDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    edtRecurrenceEndDate.setText(sdf.format(selectedRecurrenceEndDate.getTime()));
                },
                selectedRecurrenceEndDate.get(Calendar.YEAR),
                selectedRecurrenceEndDate.get(Calendar.MONTH),
                selectedRecurrenceEndDate.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }
    
    /**
     * Setup recurrence spinner.
     */
    private void setupRecurrenceSpinner() {
        if (spinnerRecurrenceType != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    recurrenceTypeOptions);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerRecurrenceType.setAdapter(adapter);
        }
    }

    private void loadTaskData() {
        showLoading(true);

        executorService.execute(() -> {
            Task task = taskDAO.findById(taskId);

            mainHandler.post(() -> {
                showLoading(false);
                if (task != null) {
                    currentTask = task;
                    populateForm(task);
                } else {
                    Toast.makeText(this, "Không tìm thấy công việc", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    private void populateForm(Task task) {
        edtTitle.setText(task.getTitle());
        edtDescription.setText(task.getDescription());

        // Due Date - Parse from MySQL format (yyyy-MM-dd) to user format (dd/MM/yyyy)
        String dueDate = task.getDueDate();
        if (dueDate != null && !dueDate.isEmpty()) {
            try {
                SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                java.util.Date date = dbFormat.parse(dueDate);
                if (date != null) {
                    edtDueDate.setText(displayFormat.format(date));
                    selectedDate.setTime(date);
                } else {
                    edtDueDate.setText(dueDate);
                }
            } catch (Exception e) {
                edtDueDate.setText(dueDate);
            }
        }

        // Priority
        int priorityIndex = getPriorityIndex(task.getPriority());
        spinnerPriority.setSelection(priorityIndex);

        // Status
        int statusIndex = getStatusIndex(task.getStatus());
        spinnerStatus.setSelection(statusIndex);

        // Category - Adjust for "Không có danh mục" option at position 0
        if (spinnerCategory != null && categories != null) {
            if (task.getCategoryId() > 0) {
                for (int i = 0; i < categories.size(); i++) {
                    if (categories.get(i).getId() == task.getCategoryId()) {
                        spinnerCategory.setSelection(i + 1); // +1 because position 0 is "Không có danh mục"
                        break;
                    }
                }
            } else {
                spinnerCategory.setSelection(0); // "Không có danh mục"
            }
        }

        // Load existing reminders and recurrence
        loadExistingReminders(task.getId());
        loadExistingRecurrence(task.getId());
    }

    // loadExistingTags removed - tags no longer supported

    private void loadExistingReminders(int taskId) {
        executorService.execute(() -> {
            // If this is an instance task, load reminders from master task (parentTaskId)
            int taskIdToLoad = taskId;
            if (currentTask != null && currentTask.getParentTaskId() != null && currentTask.getParentTaskId() > 0) {
                taskIdToLoad = currentTask.getParentTaskId();
            }
            
            List<TaskReminder> reminders = taskReminderDAO.findByTaskId(taskIdToLoad);
            mainHandler.post(() -> {
                if (reminders != null && !reminders.isEmpty()) {
                    TaskReminder reminder = reminders.get(0); // Get first reminder
                    String reminderTime = reminder.getReminderTime();
                    if (reminderTime != null && !reminderTime.isEmpty() && edtReminder != null) {
                        try {
                            // Parse from MySQL format (yyyy-MM-dd HH:mm:ss) to user format (dd/MM/yyyy HH:mm)
                            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                            SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                            java.util.Date date = dbFormat.parse(reminderTime);
                            if (date != null) {
                                edtReminder.setText(displayFormat.format(date));
                                selectedReminderTime.setTime(date);
                            } else {
                                edtReminder.setText(reminderTime);
                            }
                        } catch (Exception e) {
                            edtReminder.setText(reminderTime);
                        }
                    }
                }
            });
        });
    }
    
    /**
     * Load existing recurrence pattern for the task.
     */
    private void loadExistingRecurrence(int taskId) {
        executorService.execute(() -> {
            // If this is an instance task, load recurrence from master task (parentTaskId)
            int taskIdToLoad = taskId;
            if (currentTask != null && currentTask.getParentTaskId() != null && currentTask.getParentTaskId() > 0) {
                taskIdToLoad = currentTask.getParentTaskId();
            }
            
            TaskRecurrence recurrence = taskRecurrenceDAO.findByTaskId(taskIdToLoad);
            mainHandler.post(() -> {
                if (recurrence != null && recurrence.isActive()) {
                    // Enable recurrence section
                    if (switchRecurrence != null) {
                        switchRecurrence.setChecked(true);
                        if (recurrenceSection != null) {
                            recurrenceSection.setVisibility(View.VISIBLE);
                        }
                    }
                    
                    // Set recurrence type
                    if (spinnerRecurrenceType != null) {
                        String type = recurrence.getRecurrenceType();
                        for (int i = 0; i < recurrenceTypeValues.length; i++) {
                            if (recurrenceTypeValues[i].equals(type)) {
                                spinnerRecurrenceType.setSelection(i);
                                updateRecurrenceUI(i);
                                break;
                            }
                        }
                    }
                    
                    // Set interval
                    if (edtRecurrenceInterval != null) {
                        edtRecurrenceInterval.setText(String.valueOf(recurrence.getRecurrenceInterval()));
                    }
                    
                    // Set weekly days if weekly type
                    if (TaskRecurrence.TYPE_WEEKLY.equals(recurrence.getRecurrenceType()) && recurrence.getRecurrenceDays() != null) {
                        String daysStr = recurrence.getRecurrenceDays();
                        String[] days = daysStr.split(",");
                        for (String dayStr : days) {
                            try {
                                int dbDay = Integer.parseInt(dayStr.trim());
                                // Convert: 1=Monday -> Calendar.MONDAY=2, 7=Sunday -> Calendar.SUNDAY=1
                                int calendarDay = (dbDay == 7) ? Calendar.SUNDAY : dbDay + 1;
                                
                                if (calendarDay == Calendar.MONDAY && chkMonday != null) chkMonday.setChecked(true);
                                else if (calendarDay == Calendar.TUESDAY && chkTuesday != null) chkTuesday.setChecked(true);
                                else if (calendarDay == Calendar.WEDNESDAY && chkWednesday != null) chkWednesday.setChecked(true);
                                else if (calendarDay == Calendar.THURSDAY && chkThursday != null) chkThursday.setChecked(true);
                                else if (calendarDay == Calendar.FRIDAY && chkFriday != null) chkFriday.setChecked(true);
                                else if (calendarDay == Calendar.SATURDAY && chkSaturday != null) chkSaturday.setChecked(true);
                                else if (calendarDay == Calendar.SUNDAY && chkSunday != null) chkSunday.setChecked(true);
                            } catch (NumberFormatException e) {
                                // Skip invalid day
                            }
                        }
                    }
                    
                    // Set end date
                    if (edtRecurrenceEndDate != null) {
                        String endDate = recurrence.getRecurrenceEndDate();
                        if (endDate != null && !endDate.isEmpty()) {
                            try {
                                // Parse from MySQL format (yyyy-MM-dd) to user format (dd/MM/yyyy)
                                SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                java.util.Date date = dbFormat.parse(endDate);
                                if (date != null) {
                                    edtRecurrenceEndDate.setText(displayFormat.format(date));
                                    selectedRecurrenceEndDate.setTime(date);
                                } else {
                                    edtRecurrenceEndDate.setText(endDate);
                                }
                            } catch (Exception e) {
                                edtRecurrenceEndDate.setText(endDate);
                            }
                        }
                    }
                } else {
                    // No recurrence, ensure section is hidden
                    if (switchRecurrence != null) {
                        switchRecurrence.setChecked(false);
                    }
                    if (recurrenceSection != null) {
                        recurrenceSection.setVisibility(View.GONE);
                    }
                }
            });
        });
    }

    private int getPriorityIndex(String priority) {
        for (int i = 0; i < priorityValues.length; i++) {
            if (priorityValues[i].equals(priority)) {
                return i;
            }
        }
        return 1; // Default: medium
    }

    private int getStatusIndex(String status) {
        for (int i = 0; i < statusValues.length; i++) {
            if (statusValues[i].equals(status)) {
                return i;
            }
        }
        return 0; // Default: pending
    }

    private void showReminderPicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedReminderTime.set(Calendar.YEAR, year);
                    selectedReminderTime.set(Calendar.MONTH, month);
                    selectedReminderTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            EditTaskActivity.this,
                            (view2, hourOfDay, minute) -> {
                                selectedReminderTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                selectedReminderTime.set(Calendar.MINUTE, minute);

                                // Display in user-friendly format (dd/MM/yyyy HH:mm)
                                if (edtReminder != null) {
                                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                                    edtReminder.setText(sdf.format(selectedReminderTime.getTime()));
                                }
                            },
                            selectedReminderTime.get(Calendar.HOUR_OF_DAY),
                            selectedReminderTime.get(Calendar.MINUTE),
                            true);
                    timePickerDialog.show();
                },
                selectedReminderTime.get(Calendar.YEAR),
                selectedReminderTime.get(Calendar.MONTH),
                selectedReminderTime.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    // Display in user-friendly format (dd/MM/yyyy)
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    edtDueDate.setText(sdf.format(selectedDate.getTime()));
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void saveTask() {
        // Check if this is a recurring master task
        if (currentTask != null && currentTask.getIsMaster() != null && currentTask.getIsMaster()) {
            // Show dialog to choose edit option
            showRecurrenceEditDialog();
            return;
        }
        
        // Not a recurring master, proceed with normal save
        performSaveTask(RecurrenceEditDialog.OPTION_ALL_OCCURRENCES);
    }

    /**
     * Show dialog to choose how to edit recurring task.
     */
    private void showRecurrenceEditDialog() {
        RecurrenceEditDialog dialog = new RecurrenceEditDialog(this);
        dialog.setOnOptionSelectedListener(option -> {
            performSaveTask(option);
        });
        dialog.show();
    }

    /**
     * Perform the actual save based on the selected option.
     */
    private void performSaveTask(int editOption) {
        String title = edtTitle.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();
        String dueDate = edtDueDate != null ? edtDueDate.getText().toString().trim() : "";
        // Tags removed - no longer supported
        String reminderInput = edtReminder != null ? edtReminder.getText().toString().trim() : "";
        int priorityIndex = spinnerPriority != null ? spinnerPriority.getSelectedItemPosition() : 1;
        int statusIndex = spinnerStatus != null ? spinnerStatus.getSelectedItemPosition() : 0;
        int categoryIndex = spinnerCategory != null ? spinnerCategory.getSelectedItemPosition() : 0;

        // Validate
        if (title.isEmpty()) {
            edtTitle.setError("Vui lòng nhập tiêu đề");
            edtTitle.requestFocus();
            return;
        }

        if (title.length() < 3) {
            edtTitle.setError("Tiêu đề phải có ít nhất 3 ký tự");
            edtTitle.requestFocus();
            return;
        }

        // Validate recurrence end date if recurrence is enabled
        if (switchRecurrence != null && switchRecurrence.isChecked()) {
            if (!dueDate.isEmpty() && edtRecurrenceEndDate != null) {
                String endDateStr = edtRecurrenceEndDate.getText().toString().trim();
                if (!endDateStr.isEmpty()) {
                    try {
                        SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        java.util.Date dueDateObj = displayFormat.parse(dueDate);
                        java.util.Date endDateObj = displayFormat.parse(endDateStr);
                        if (dueDateObj != null && endDateObj != null && dueDateObj.compareTo(endDateObj) >= 0) {
                            edtRecurrenceEndDate.setError("Ngày kết thúc phải sau ngày hết hạn");
                            edtRecurrenceEndDate.requestFocus();
                            return;
                        }
                    } catch (Exception e) {
                        // Ignore parsing errors, will be handled later
                    }
                }
            }
        }

        showLoading(true);

        // Update task
        currentTask.setTitle(title);
        currentTask.setDescription(description.isEmpty() ? null : description);
        currentTask.setPriority(priorityValues[priorityIndex]);
        currentTask.setStatus(statusValues[statusIndex]);

        // Parse due date from user format (dd/MM/yyyy) to MySQL format (yyyy-MM-dd)
        final String[] dueDateForDb = {null};
        if (!dueDate.isEmpty()) {
            try {
                // Check if already in MySQL format
                if (dueDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    dueDateForDb[0] = dueDate;
                } else {
                    // Try to parse from user format (dd/MM/yyyy)
                    SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    java.util.Date date = displayFormat.parse(dueDate);
                    if (date != null) {
                        dueDateForDb[0] = dbFormat.format(date);
                        selectedDate.setTime(date);
                    } else {
                        dueDateForDb[0] = dueDate; // Fallback to original
                    }
                }
            } catch (Exception e) {
                // If parsing fails, try to use selectedDate if it was set
                if (selectedDate != null) {
                    SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    dueDateForDb[0] = dbFormat.format(selectedDate.getTime());
                } else {
                    dueDateForDb[0] = dueDate; // Fallback to original
                }
            }
        }
        currentTask.setDueDate(dueDateForDb[0]);

        // Set startDate and endDate if recurrence is enabled
        final String[] startDateForDb = {null};
        final String[] endDateForDb = {null};
        
        if (switchRecurrence != null && switchRecurrence.isChecked()) {
            // startDate = dueDate (hoặc ngày hiện tại nếu không có dueDate)
            startDateForDb[0] = dueDateForDb[0];
            if (startDateForDb[0] == null || startDateForDb[0].isEmpty()) {
                SimpleDateFormat sdfDb = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                startDateForDb[0] = sdfDb.format(Calendar.getInstance().getTime());
            }
            currentTask.setStartDate(startDateForDb[0]);

            // endDate = recurrenceEndDate (nếu có) hoặc startDate + 1 năm
            if (edtRecurrenceEndDate != null) {
                String endDateStr = edtRecurrenceEndDate.getText().toString().trim();
                if (!endDateStr.isEmpty()) {
                    try {
                        SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        java.util.Date date = displayFormat.parse(endDateStr);
                        if (date != null) {
                            endDateForDb[0] = dbFormat.format(date);
                        }
                    } catch (Exception e) {
                        android.util.Log.e("EditTaskActivity", "Error parsing recurrence end date: " + e.getMessage());
                    }
                }
            }
            
            // Nếu không có endDate, set mặc định = startDate + 1 năm
            if (endDateForDb[0] == null || endDateForDb[0].isEmpty()) {
                try {
                    SimpleDateFormat sdfDb = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(sdfDb.parse(startDateForDb[0]));
                    cal.add(Calendar.YEAR, 1);
                    endDateForDb[0] = sdfDb.format(cal.getTime());
                } catch (Exception e) {
                    endDateForDb[0] = startDateForDb[0]; // Fallback
                }
            }
            currentTask.setEndDate(endDateForDb[0]);
        } else {
            // Nếu không có recurrence, clear startDate và endDate
            currentTask.setStartDate(null);
            currentTask.setEndDate(null);
        }

        // Handle category - categoryIndex 0 is "Không có danh mục"
        if (categoryIndex > 0 && categoryIndex <= categories.size()) {
            currentTask.setCategoryId(categories.get(categoryIndex - 1).getId());
        } else {
            currentTask.setCategoryId(0); // No category
        }

        executorService.execute(() -> {
            final boolean[] success = {false};
            TaskRecurrenceService recurrenceService = new TaskRecurrenceService();
            
            // Handle based on edit option
            if (currentTask.getIsMaster() != null && currentTask.getIsMaster()) {
                // This is a recurring master task
                switch (editOption) {
                    case RecurrenceEditDialog.OPTION_THIS_OCCURRENCE:
                        // Create exception for this occurrence
                        String occurrenceDate = currentTask.getDueDate(); // Use dueDate as occurrence date
                        Task modifiedTask = new Task();
                        modifiedTask.setUserId(currentTask.getUserId());
                        modifiedTask.setCategoryId(currentTask.getCategoryId());
                        modifiedTask.setTitle(title);
                        modifiedTask.setDescription(description.isEmpty() ? null : description);
                        modifiedTask.setStatus(statusValues[statusIndex]);
                        modifiedTask.setPriority(priorityValues[priorityIndex]);
                        modifiedTask.setDueDate(currentTask.getDueDate());
                        modifiedTask.setStartDate(currentTask.getStartDate());
                        modifiedTask.setEndDate(currentTask.getEndDate());
                        
                        // Parse dueDate if changed
                        if (!dueDate.isEmpty()) {
                            try {
                                SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                java.util.Date date = displayFormat.parse(dueDate);
                                if (date != null) {
                                    modifiedTask.setDueDate(dbFormat.format(date));
                                    occurrenceDate = dbFormat.format(date);
                                }
                            } catch (Exception e) {
                                // Keep original
                            }
                        }
                        
                        success[0] = recurrenceService.createException(currentTask.getId(), occurrenceDate, modifiedTask);
                        break;
                        
                    case RecurrenceEditDialog.OPTION_FROM_THIS_FORWARD:
                        // Split recurrence
                        String splitDate = currentTask.getDueDate();
                        if (!dueDate.isEmpty()) {
                            try {
                                SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                java.util.Date date = displayFormat.parse(dueDate);
                                if (date != null) {
                                    splitDate = dbFormat.format(date);
                                }
                            } catch (Exception e) {
                                // Keep original
                            }
                        }
                        
                        // Update master task first
                        currentTask.setTitle(title);
                        currentTask.setDescription(description.isEmpty() ? null : description);
                        currentTask.setPriority(priorityValues[priorityIndex]);
                        currentTask.setStatus(statusValues[statusIndex]);
                        success[0] = taskDAO.update(currentTask);
                        
                        if (success[0]) {
                            success[0] = recurrenceService.splitRecurrence(currentTask.getId(), splitDate);
                        }
                        break;
                        
                    case RecurrenceEditDialog.OPTION_ALL_OCCURRENCES:
                    default:
                        // Update master task and recurrence rule
                        currentTask.setTitle(title);
                        currentTask.setDescription(description.isEmpty() ? null : description);
                        currentTask.setPriority(priorityValues[priorityIndex]);
                        currentTask.setStatus(statusValues[statusIndex]);
                        success[0] = taskDAO.update(currentTask);
                        
                        if (success[0] && switchRecurrence != null && switchRecurrence.isChecked()) {
                            // Delete old recurrence and save new one
                            taskRecurrenceDAO.deleteByTaskId(currentTask.getId());
                            saveRecurrence(currentTask.getId());
                        }
                        break;
                }
            } else {
                // Normal task or instance, just update
                currentTask.setTitle(title);
                currentTask.setDescription(description.isEmpty() ? null : description);
                currentTask.setPriority(priorityValues[priorityIndex]);
                currentTask.setStatus(statusValues[statusIndex]);
                currentTask.setDueDate(dueDateForDb[0]);
                success[0] = taskDAO.update(currentTask);
                
                if (success[0] && switchRecurrence != null && switchRecurrence.isChecked()) {
                    // Make it a master task
                    currentTask.setIsMaster(true);
                    taskDAO.update(currentTask);
                    saveRecurrence(currentTask.getId());
                }
            }
            
            // Handle reminders (for all cases)
            if (success[0]) {
                // Tags removed - no longer supported

                // Delete old reminders and insert new one
                taskReminderDAO.deleteByTaskId(currentTask.getId());
                if (!reminderInput.isEmpty()) {
                    // Parse reminder from user format (dd/MM/yyyy HH:mm) to MySQL format (yyyy-MM-dd HH:mm:ss)
                    String reminderTimeForDb = null;
                    try {
                        // Check if already in MySQL format
                        if (reminderInput.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
                            reminderTimeForDb = reminderInput;
                        } else {
                            // Try to parse from user format (dd/MM/yyyy HH:mm)
                            SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                            java.util.Date date = displayFormat.parse(reminderInput);
                            if (date != null) {
                                reminderTimeForDb = dbFormat.format(date);
                            } else {
                                reminderTimeForDb = reminderInput; // Fallback to original
                            }
                        }
                    } catch (Exception e) {
                        // If parsing fails, try to use selectedReminderTime if it was set
                        if (selectedReminderTime != null) {
                            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                            reminderTimeForDb = dbFormat.format(selectedReminderTime.getTime());
                        } else {
                            reminderTimeForDb = reminderInput; // Fallback to original
                        }
                    }
                    TaskReminder reminder = new TaskReminder(currentTask.getId(), reminderTimeForDb,
                            TaskReminder.TYPE_NOTIFICATION);
                    taskReminderDAO.insert(reminder);
                }
            }

            mainHandler.post(() -> {
                showLoading(false);

                if (success[0]) {
                    Toast.makeText(this, getString(R.string.success_task_updated), Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(this, "Không thể cập nhật. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * Lưu recurrence pattern cho task.
     */
    private void saveRecurrence(int taskId) {
        if (spinnerRecurrenceType == null) {
            return;
        }
        
        int typeIndex = spinnerRecurrenceType.getSelectedItemPosition();
        if (typeIndex < 0 || typeIndex >= recurrenceTypeValues.length) {
            return;
        }
        
        String recurrenceType = recurrenceTypeValues[typeIndex];
        int interval = 1;
        
        if (edtRecurrenceInterval != null) {
            String intervalStr = edtRecurrenceInterval.getText().toString().trim();
            if (!intervalStr.isEmpty()) {
                try {
                    interval = Integer.parseInt(intervalStr);
                    if (interval < 1) interval = 1;
                } catch (NumberFormatException e) {
                    interval = 1;
                }
            }
        }
        
        TaskRecurrence recurrence = new TaskRecurrence(taskId, recurrenceType, interval);
        
        // Set recurrence days for weekly type
        if (TaskRecurrence.TYPE_WEEKLY.equals(recurrenceType) && layoutWeeklyDays != null) {
            java.util.List<Integer> selectedDays = new java.util.ArrayList<>();
            if (chkMonday != null && chkMonday.isChecked()) selectedDays.add(Calendar.MONDAY);
            if (chkTuesday != null && chkTuesday.isChecked()) selectedDays.add(Calendar.TUESDAY);
            if (chkWednesday != null && chkWednesday.isChecked()) selectedDays.add(Calendar.WEDNESDAY);
            if (chkThursday != null && chkThursday.isChecked()) selectedDays.add(Calendar.THURSDAY);
            if (chkFriday != null && chkFriday.isChecked()) selectedDays.add(Calendar.FRIDAY);
            if (chkSaturday != null && chkSaturday.isChecked()) selectedDays.add(Calendar.SATURDAY);
            if (chkSunday != null && chkSunday.isChecked()) selectedDays.add(Calendar.SUNDAY);
            
            if (!selectedDays.isEmpty()) {
                // Convert Calendar day constants to database format (1=Monday, 7=Sunday)
                StringBuilder daysStr = new StringBuilder();
                for (int i = 0; i < selectedDays.size(); i++) {
                    int day = selectedDays.get(i);
                    // Convert: Calendar.MONDAY=2 -> 1, Calendar.SUNDAY=1 -> 7
                    int dbDay = (day == Calendar.SUNDAY) ? 7 : day - 1;
                    daysStr.append(dbDay);
                    if (i < selectedDays.size() - 1) {
                        daysStr.append(",");
                    }
                }
                recurrence.setRecurrenceDays(daysStr.toString());
            }
        }
        
        // Set end date if provided
        if (edtRecurrenceEndDate != null) {
            String endDateStr = edtRecurrenceEndDate.getText().toString().trim();
            if (!endDateStr.isEmpty()) {
                try {
                    SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    java.util.Date date = displayFormat.parse(endDateStr);
                    if (date != null) {
                        recurrence.setRecurrenceEndDate(dbFormat.format(date));
                    }
                } catch (Exception e) {
                    android.util.Log.e("EditTaskActivity", "Error parsing recurrence end date: " + e.getMessage());
                }
            }
        }
        
        taskRecurrenceDAO.insert(recurrence);
        
        // Note: Không còn generate instances ngay lập tức
        // Instances sẽ được generate động khi hiển thị trong MainActivity
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!show);
        edtTitle.setEnabled(!show);
        edtDescription.setEnabled(!show);
        edtDueDate.setEnabled(!show);
        spinnerPriority.setEnabled(!show);
        spinnerStatus.setEnabled(!show);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
