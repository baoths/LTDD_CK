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
import com.example.ktck_android_k17.service.TaskRecurrenceService;
import com.example.ktck_android_k17.dao.TaskTagDAO;
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
 * Activity để thêm công việc mới.
 */
public class AddTaskActivity extends AppCompatActivity {

    private static final String PREF_NAME = "TaskManagerPrefs";
    private static final String KEY_USER_ID = "user_id";

    private ImageButton btnBack;
    private EditText edtTitle, edtDescription, edtDueDate, edtReminder;
    private EditText edtRecurrenceInterval, edtRecurrenceEndDate;
    private Spinner spinnerStatus, spinnerPriority, spinnerCategory, spinnerRecurrenceType;
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
    private TaskTagDAO taskTagDAO;
    private ExecutorService executorService;
    private Handler mainHandler;
    private int currentUserId;
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

    // Status options
    private final String[] statusOptions = { "Chờ xử lý", "Đang thực hiện", "Hoàn thành" };
    private final String[] statusValues = { Task.STATUS_PENDING, Task.STATUS_IN_PROGRESS, Task.STATUS_COMPLETED };

    // Priority options
    private final String[] priorityOptions = { "Thấp", "Trung bình", "Cao" };
    private final String[] priorityValues = { Task.PRIORITY_LOW, Task.PRIORITY_MEDIUM, Task.PRIORITY_HIGH };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        // Lấy user ID từ session
        loadUserSession();

        initViews();
        initDatabase();
        setupSpinner();
        setupRecurrenceSpinner();
        setupListeners();
    }

    /**
     * Lấy user ID từ SharedPreferences.
     */
    private void loadUserSession() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        currentUserId = prefs.getInt(KEY_USER_ID, -1);
    }

    /**
     * Khởi tạo các view components.
     */
    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtTitle = findViewById(R.id.edtTitle);
        edtDescription = findViewById(R.id.edtDescription);
        edtDueDate = findViewById(R.id.edtDueDate);
        // Tags removed - no longer needed
        edtReminder = findViewById(R.id.edtReminder);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        spinnerPriority = findViewById(R.id.spinnerPriority);
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

    /**
     * Khởi tạo database và executor service.
     */
    private void initDatabase() {
        taskDAO = new TaskDAO();
        categoryDAO = new CategoryDAO();
        taskRecurrenceDAO = new TaskRecurrenceDAO();
        taskReminderDAO = new TaskReminderDAO();
        taskTagDAO = new TaskTagDAO();
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        // Load categories
        loadCategories();
    }

    /**
     * Load danh sách categories từ database.
     */
    private void loadCategories() {
        executorService.execute(() -> {
            categories = categoryDAO.findByUserId(currentUserId);
            mainHandler.post(this::setupCategorySpinner);
        });
    }

    /**
     * Setup category spinner.
     */
    private void setupCategorySpinner() {
        List<String> categoryNames = new ArrayList<>();
        categoryNames.add("-- Chọn danh mục --"); // Lựa chọn mặc định
        for (Category cat : categories) {
            categoryNames.add(cat.getName());
        }

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categoryNames);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);
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
            spinnerRecurrenceType.setSelection(0); // Default: Daily
        }
    }

    /**
     * Setup status spinner.
     */
    private void setupSpinner() {
        // Status Spinner
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                statusOptions);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);

        // Priority Spinner
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                priorityOptions);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(priorityAdapter);
        spinnerPriority.setSelection(1); // Default: Trung bình
    }

    /**
     * Thiết lập các event listeners.
     */
    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        edtDueDate.setOnClickListener(v -> showDatePicker());

        edtReminder.setOnClickListener(v -> showReminderPicker());
        
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
     * Hiển thị DatePicker dialog.
     */
    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    // Format và hiển thị ngày đã chọn
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    edtDueDate.setText(sdf.format(selectedDate.getTime()));
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH));

        // Không cho chọn ngày trong quá khứ
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    /**
     * Hiển thị Reminder picker (ngày + giờ).
     */
    private void showReminderPicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedReminderTime.set(Calendar.YEAR, year);
                    selectedReminderTime.set(Calendar.MONTH, month);
                    selectedReminderTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    // Hiển thị TimePicker
                    new TimePickerDialog(
                            this,
                            (timePicker, hour, minute) -> {
                                selectedReminderTime.set(Calendar.HOUR_OF_DAY, hour);
                                selectedReminderTime.set(Calendar.MINUTE, minute);

                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                                edtReminder.setText(sdf.format(selectedReminderTime.getTime()));
                            },
                            selectedReminderTime.get(Calendar.HOUR_OF_DAY),
                            selectedReminderTime.get(Calendar.MINUTE),
                            true).show();
                },
                selectedReminderTime.get(Calendar.YEAR),
                selectedReminderTime.get(Calendar.MONTH),
                selectedReminderTime.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    /**
     * Lưu task mới vào database.
     */
    private void saveTask() {
        String title = edtTitle.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();
        String dueDate = edtDueDate.getText().toString().trim();
        String reminderInput = edtReminder.getText().toString().trim();

        int statusIndex = spinnerStatus.getSelectedItemPosition();
        String status = statusValues[statusIndex];
        int priorityIndex = spinnerPriority.getSelectedItemPosition();
        String priority = priorityValues[priorityIndex];
        int categoryIndex = spinnerCategory.getSelectedItemPosition();
        int categoryId = categoryIndex > 0 ? categories.get(categoryIndex - 1).getId() : 0;

        // Validate
        if (!validateInput(title)) {
            return;
        }

        // Convert due date format for MySQL (yyyy-MM-dd)
        String dueDateForDb = null;
        if (!dueDate.isEmpty()) {
            SimpleDateFormat sdfDb = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            dueDateForDb = sdfDb.format(selectedDate.getTime());
        }

        // Convert reminder time to MySQL format (yyyy-MM-dd HH:mm:ss)
        String reminderTimeForDb = null;
        if (!reminderInput.isEmpty()) {
            SimpleDateFormat sdfReminder = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            reminderTimeForDb = sdfReminder.format(selectedReminderTime.getTime());
        }

        showLoading(true);

        // Tạo Task entity
        Task task = new Task();
        task.setUserId(currentUserId);
        task.setCategoryId(categoryId);
        task.setTitle(title);
        task.setDescription(description.isEmpty() ? null : description);
        task.setStatus(status);
        task.setPriority(priority);
        task.setDueDate(dueDateForDb);

        String finalReminderTimeForDb = reminderTimeForDb;
        executorService.execute(() -> {
            try {
                // Insert task and get ID
                int taskId = taskDAO.insertAndGetId(task);

                mainHandler.post(() -> {
                    if (taskId > 0) {
                        // Save reminder if provided
                        if (finalReminderTimeForDb != null && !finalReminderTimeForDb.isEmpty()) {
                            taskReminderDAO.insert(
                                    new TaskReminder(taskId, finalReminderTimeForDb, TaskReminder.TYPE_NOTIFICATION));
                        }
                        
                        // Save recurrence if enabled
                        if (switchRecurrence != null && switchRecurrence.isChecked()) {
                            saveRecurrence(taskId);
                        }

                        showLoading(false);
                        Toast.makeText(AddTaskActivity.this,
                                getString(R.string.success_task_created), Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        showLoading(false);
                        Toast.makeText(AddTaskActivity.this,
                                "Không thể tạo công việc. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(AddTaskActivity.this,
                            "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Validate input.
     */
    private boolean validateInput(String title) {
        if (title.isEmpty()) {
            edtTitle.setError("Vui lòng nhập tiêu đề");
            edtTitle.requestFocus();
            return false;
        }

        if (title.length() < 3) {
            edtTitle.setError("Tiêu đề phải có ít nhất 3 ký tự");
            edtTitle.requestFocus();
            return false;
        }

        // Validate recurrence end date if recurrence is enabled
        if (switchRecurrence != null && switchRecurrence.isChecked()) {
            String dueDate = edtDueDate.getText().toString().trim();
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
                            return false;
                        }
                    } catch (Exception e) {
                        // Ignore parsing errors, will be handled later
                    }
                }
            }
        }

        return true;
    }

    /**
     * Hiển thị/ẩn loading indicator.
     */
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
                    android.util.Log.e("AddTaskActivity", "Error parsing recurrence end date: " + e.getMessage());
                }
            }
        }
        
        taskRecurrenceDAO.insert(recurrence);
        
        // Generate all recurring instances immediately
        TaskRecurrenceService recurrenceService = new TaskRecurrenceService();
        recurrenceService.generateAllRecurringInstances(recurrence);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!show);
        edtTitle.setEnabled(!show);
        edtDescription.setEnabled(!show);
        edtDueDate.setEnabled(!show);
        // Tags removed
        edtReminder.setEnabled(!show);
        spinnerStatus.setEnabled(!show);
        spinnerPriority.setEnabled(!show);
        spinnerCategory.setEnabled(!show);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
