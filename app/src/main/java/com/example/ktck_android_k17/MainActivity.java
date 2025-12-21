package com.example.ktck_android_k17;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ktck_android_k17.adapter.KanbanTaskAdapter;
import com.example.ktck_android_k17.adapter.TaskAdapter;
import com.example.ktck_android_k17.adapter.TaskListAdapter;
import com.example.ktck_android_k17.dao.CategoryDAO;
import com.example.ktck_android_k17.dao.TaskDAO;
import com.example.ktck_android_k17.dao.TaskTagDAO;
import com.example.ktck_android_k17.database.DatabaseHelper;
import com.example.ktck_android_k17.service.TaskRecurrenceService;
import com.example.ktck_android_k17.dto.TaskDTO;
import com.example.ktck_android_k17.dto.UserDTO;
import com.example.ktck_android_k17.model.Category;
import com.example.ktck_android_k17.model.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity chính hiển thị danh sách công việc.
 * Sử dụng TaskListAdapter để hiển thị danh sách TaskDTO.
 */
public class MainActivity extends AppCompatActivity implements TaskListAdapter.OnTaskClickListener {

    private static final String PREF_NAME = "TaskManagerPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";

    private TextView tvWelcome, tvTaskCount;
    private ImageButton btnLogout, btnAddCategory, btnToggleView;
    private RecyclerView rvTasks;
    private RecyclerView rvKanbanPending, rvKanbanInProgress, rvKanbanCompleted;
    private android.widget.HorizontalScrollView kanbanScrollView;
    private LinearLayout emptyStateLayout;
    private FloatingActionButton fabAddTask;
    private ProgressBar progressBar;
    private Spinner spinnerCategory;
    
    private boolean isKanbanView = false; // false = list view, true = kanban view

    private TaskListAdapter taskListAdapter;
    private KanbanTaskAdapter kanbanPendingAdapter, kanbanInProgressAdapter, kanbanCompletedAdapter;
    private TaskDAO taskDAO;
    private CategoryDAO categoryDAO;
    private TaskTagDAO taskTagDAO;
    private TaskRecurrenceService recurrenceService;
    private UserDTO currentUser;
    private ExecutorService executorService;
    private Handler mainHandler;
    private List<Category> categories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Lấy thông tin user từ session
        loadUserSession();

        if (currentUser == null) {
            navigateToLogin();
            return;
        }

        initDatabase(); // Initialize database and executorService first
        initViews(); // Then initialize views (which may call methods that need executorService)
        setupRecyclerView();
        setupKanbanView();
        loadCategories();
        setupListeners();
        checkRecurringTasks(); // Check and generate recurring tasks on startup
        loadTasks();
    }

    /**
     * Lấy thông tin user từ SharedPreferences.
     */
    private void loadUserSession() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        int userId = prefs.getInt(KEY_USER_ID, -1);
        String username = prefs.getString(KEY_USERNAME, "");
        String email = prefs.getString(KEY_EMAIL, "");

        if (userId != -1) {
            currentUser = new UserDTO(userId, username, email);
        }
    }

    /**
     * Khởi tạo các view components.
     */
    private void initViews() {
        try {
            tvWelcome = findViewById(R.id.tvWelcome);
            tvTaskCount = findViewById(R.id.tvTaskCount);
            btnLogout = findViewById(R.id.btnLogout);
            btnAddCategory = findViewById(R.id.btnAddCategory);
            btnToggleView = findViewById(R.id.btnToggleView);
            rvTasks = findViewById(R.id.rvTasks);
            kanbanScrollView = findViewById(R.id.kanbanScrollView);
            rvKanbanPending = findViewById(R.id.rvKanbanPending);
            rvKanbanInProgress = findViewById(R.id.rvKanbanInProgress);
            rvKanbanCompleted = findViewById(R.id.rvKanbanCompleted);
            emptyStateLayout = findViewById(R.id.emptyStateLayout);
            fabAddTask = findViewById(R.id.fabAddTask);
            progressBar = findViewById(R.id.progressBar);
            spinnerCategory = findViewById(R.id.spinnerCategory);
            
            // Load view preference
            loadViewPreference();

            categories = new ArrayList<>();

            // Kiểm tra các view quan trọng
            if (tvWelcome == null || rvTasks == null) {
                android.util.Log.e("MainActivity", "Critical views are null!");
                throw new RuntimeException("Không tìm thấy các view cần thiết trong layout");
            }

            // Hiển thị lời chào với tên user
            if (currentUser != null && currentUser.getUsername() != null) {
                String welcomeText = getString(R.string.text_welcome, currentUser.getUsername());
                tvWelcome.setText(welcomeText);
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error initializing views: " + e.getMessage(), e);
            Toast.makeText(this, "Lỗi khởi tạo giao diện: " + e.getMessage(), Toast.LENGTH_LONG).show();
            throw e; // Re-throw để onCreate có thể xử lý
        }
    }

    /**
     * Khởi tạo database và executor service.
     */
    private void initDatabase() {
        taskDAO = new TaskDAO();
        categoryDAO = new CategoryDAO();
        taskTagDAO = new TaskTagDAO();
        recurrenceService = new TaskRecurrenceService();
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        
        // Đảm bảo database schema được khởi tạo và migration được chạy
        executorService.execute(() -> {
            try {
                android.util.Log.d("MainActivity", "Đang khởi tạo database schema...");
                DatabaseHelper.getInstance().initializeDatabase();
                android.util.Log.d("MainActivity", "Database schema đã được khởi tạo");
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Lỗi khởi tạo database schema: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Kiểm tra và tạo các recurring tasks.
     */
    private void checkRecurringTasks() {
        executorService.execute(() -> {
            try {
                recurrenceService.checkAndGenerateRecurringTasks();
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Lỗi kiểm tra recurring tasks: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Setup RecyclerView với adapter.
     */
    private void setupRecyclerView() {
        taskListAdapter = new TaskListAdapter();
        taskListAdapter.setOnTaskClickListener(this);

        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(taskListAdapter);
    }
    
    /**
     * Setup Kanban board với 3 columns.
     */
    private void setupKanbanView() {
        // Setup adapters for each column
        kanbanPendingAdapter = new KanbanTaskAdapter(Task.STATUS_PENDING);
        kanbanInProgressAdapter = new KanbanTaskAdapter(Task.STATUS_IN_PROGRESS);
        kanbanCompletedAdapter = new KanbanTaskAdapter(Task.STATUS_COMPLETED);
        
        // Set status change listeners
        kanbanPendingAdapter.setOnTaskStatusChangeListener((task, newStatus) -> {
            updateTaskStatus(task, newStatus);
        });
        kanbanInProgressAdapter.setOnTaskStatusChangeListener((task, newStatus) -> {
            updateTaskStatus(task, newStatus);
        });
        kanbanCompletedAdapter.setOnTaskStatusChangeListener((task, newStatus) -> {
            updateTaskStatus(task, newStatus);
        });
        
        // Setup RecyclerViews
        if (rvKanbanPending != null) {
            rvKanbanPending.setLayoutManager(new LinearLayoutManager(this));
            rvKanbanPending.setAdapter(kanbanPendingAdapter);
            setupDragAndDrop(rvKanbanPending, kanbanPendingAdapter, kanbanInProgressAdapter, kanbanCompletedAdapter, Task.STATUS_PENDING);
        }
        
        if (rvKanbanInProgress != null) {
            rvKanbanInProgress.setLayoutManager(new LinearLayoutManager(this));
            rvKanbanInProgress.setAdapter(kanbanInProgressAdapter);
            setupDragAndDrop(rvKanbanInProgress, kanbanInProgressAdapter, kanbanPendingAdapter, kanbanCompletedAdapter, Task.STATUS_IN_PROGRESS);
        }
        
        if (rvKanbanCompleted != null) {
            rvKanbanCompleted.setLayoutManager(new LinearLayoutManager(this));
            rvKanbanCompleted.setAdapter(kanbanCompletedAdapter);
            setupDragAndDrop(rvKanbanCompleted, kanbanCompletedAdapter, kanbanPendingAdapter, kanbanInProgressAdapter, Task.STATUS_COMPLETED);
        }
        
        // Load view preference
        loadViewPreference();
    }
    
    /**
     * Setup drag and drop cho Kanban column với khả năng kéo sang các column khác.
     */
    private void setupDragAndDrop(RecyclerView recyclerView, KanbanTaskAdapter currentAdapter,
                                   KanbanTaskAdapter targetAdapter1, KanbanTaskAdapter targetAdapter2,
                                   String currentStatus) {
        // Store reference to dragged task
        final TaskDTO[] draggedTask = {null};
        final RecyclerView[] sourceRecyclerView = {null};
        
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                // Allow reordering within the same column
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                
                if (fromPosition < 0 || toPosition < 0 || fromPosition >= currentAdapter.getItemCount() || 
                    toPosition >= currentAdapter.getItemCount()) {
                    return false;
                }
                
                currentAdapter.notifyItemMoved(fromPosition, toPosition);
                return true;
            }
            
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Not used for kanban
            }
            
            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    if (viewHolder != null) {
                        int position = viewHolder.getAdapterPosition();
                        if (position >= 0 && position < currentAdapter.getItemCount()) {
                            draggedTask[0] = currentAdapter.getTaskAt(position);
                            sourceRecyclerView[0] = recyclerView;
                            viewHolder.itemView.setAlpha(0.5f);
                            viewHolder.itemView.setElevation(8f);
                            // Highlight other columns
                            highlightDropTargets(true);
                        }
                    }
                } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    highlightDropTargets(false);
                    draggedTask[0] = null;
                    sourceRecyclerView[0] = null;
                }
            }
            
            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                viewHolder.itemView.setAlpha(1.0f);
                viewHolder.itemView.setElevation(0f);
            }
        };
        
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemTouchHelperCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        
        // Setup drop listener for cross-column dragging
        setupDropListener(recyclerView, currentAdapter, currentStatus, draggedTask, sourceRecyclerView);
        
        // Setup long press listener để cho phép kéo task sang column khác (fallback)
        currentAdapter.setOnTaskLongClickListener((task) -> {
            showMoveTaskDialog(task, currentStatus, currentAdapter, targetAdapter1, targetAdapter2);
            return true;
        });
    }
    
    /**
     * Setup drop listener để detect khi task được drop vào column khác.
     */
    private void setupDropListener(RecyclerView recyclerView, KanbanTaskAdapter adapter, String columnStatus,
                                   TaskDTO[] draggedTask, RecyclerView[] sourceRecyclerView) {
        recyclerView.setOnDragListener(new android.view.View.OnDragListener() {
            @Override
            public boolean onDrag(android.view.View v, android.view.DragEvent event) {
                switch (event.getAction()) {
                    case android.view.DragEvent.ACTION_DROP:
                        if (draggedTask[0] != null && sourceRecyclerView[0] != null && 
                            sourceRecyclerView[0] != recyclerView) {
                            // Task dropped on different column
                            String sourceStatus = getStatusForRecyclerView(sourceRecyclerView[0]);
                            if (sourceStatus != null && !sourceStatus.equals(columnStatus)) {
                                moveTaskBetweenColumns(draggedTask[0], sourceStatus, columnStatus);
                            }
                        }
                        return true;
                    case android.view.DragEvent.ACTION_DRAG_ENTERED:
                        recyclerView.setBackgroundColor(0x3300FF00); // Green tint
                        return true;
                    case android.view.DragEvent.ACTION_DRAG_EXITED:
                        recyclerView.setBackgroundColor(0x00000000); // Clear
                        return true;
                }
                return false;
            }
        });
    }
    
    /**
     * Highlight drop target columns.
     */
    private void highlightDropTargets(boolean highlight) {
        if (highlight) {
            if (rvKanbanPending != null) {
                rvKanbanPending.setBackgroundColor(0x2200FF00);
            }
            if (rvKanbanInProgress != null) {
                rvKanbanInProgress.setBackgroundColor(0x2200FF00);
            }
            if (rvKanbanCompleted != null) {
                rvKanbanCompleted.setBackgroundColor(0x2200FF00);
            }
        } else {
            if (rvKanbanPending != null) {
                rvKanbanPending.setBackgroundColor(0x00000000);
            }
            if (rvKanbanInProgress != null) {
                rvKanbanInProgress.setBackgroundColor(0x00000000);
            }
            if (rvKanbanCompleted != null) {
                rvKanbanCompleted.setBackgroundColor(0x00000000);
            }
        }
    }
    
    /**
     * Get status for a RecyclerView.
     */
    private String getStatusForRecyclerView(RecyclerView recyclerView) {
        if (recyclerView == rvKanbanPending) {
            return Task.STATUS_PENDING;
        } else if (recyclerView == rvKanbanInProgress) {
            return Task.STATUS_IN_PROGRESS;
        } else if (recyclerView == rvKanbanCompleted) {
            return Task.STATUS_COMPLETED;
        }
        return null;
    }
    
    /**
     * Move task between columns.
     */
    private void moveTaskBetweenColumns(TaskDTO task, String oldStatus, String newStatus) {
        // Remove from source adapter
        KanbanTaskAdapter sourceAdapter = getAdapterForStatus(oldStatus);
        KanbanTaskAdapter targetAdapter = getAdapterForStatus(newStatus);
        
        if (sourceAdapter != null && targetAdapter != null) {
            sourceAdapter.removeTask(task);
            task.setStatus(newStatus);
            targetAdapter.addTask(task);
            updateTaskStatus(task, newStatus);
        }
    }
    
    /**
     * Get adapter for status.
     */
    private KanbanTaskAdapter getAdapterForStatus(String status) {
        if (Task.STATUS_PENDING.equals(status)) {
            return kanbanPendingAdapter;
        } else if (Task.STATUS_IN_PROGRESS.equals(status)) {
            return kanbanInProgressAdapter;
        } else if (Task.STATUS_COMPLETED.equals(status)) {
            return kanbanCompletedAdapter;
        }
        return null;
    }
    
    /**
     * Hiển thị quick action dialog cho task trong Kanban.
     */
    private void showQuickActionDialog(TaskDTO task, String currentStatus, KanbanTaskAdapter currentAdapter,
                                      KanbanTaskAdapter targetAdapter1, KanbanTaskAdapter targetAdapter2) {
        String[] actions = { "Chỉnh sửa", "Di chuyển" };
        new AlertDialog.Builder(this)
                .setTitle(task.getTitle())
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        // Edit task
                        Intent editIntent = new Intent(MainActivity.this, EditTaskActivity.class);
                        editIntent.putExtra(EditTaskActivity.EXTRA_TASK_ID, task.getId());
                        startActivityForResult(editIntent, 100);
                    } else if (which == 1) {
                        // Move task
                        showMoveTaskDialog(task, currentStatus, currentAdapter, targetAdapter1, targetAdapter2);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    /**
     * Hiển thị dialog để chọn column đích khi di chuyển task.
     */
    private void showMoveTaskDialog(TaskDTO task, String currentStatus, KanbanTaskAdapter currentAdapter,
                                     KanbanTaskAdapter targetAdapter1, KanbanTaskAdapter targetAdapter2) {
        List<String> options = new ArrayList<>();
        List<String> statuses = new ArrayList<>();
        
        // Determine target statuses based on current status
        if (Task.STATUS_PENDING.equals(currentStatus)) {
            options.add("Đang thực hiện");
            statuses.add(Task.STATUS_IN_PROGRESS);
            options.add("Hoàn thành");
            statuses.add(Task.STATUS_COMPLETED);
        } else if (Task.STATUS_IN_PROGRESS.equals(currentStatus)) {
            options.add("Chờ xử lý");
            statuses.add(Task.STATUS_PENDING);
            options.add("Hoàn thành");
            statuses.add(Task.STATUS_COMPLETED);
        } else { // COMPLETED
            options.add("Chờ xử lý");
            statuses.add(Task.STATUS_PENDING);
            options.add("Đang thực hiện");
            statuses.add(Task.STATUS_IN_PROGRESS);
        }
        
        new AlertDialog.Builder(this)
                .setTitle("Di chuyển: " + task.getTitle())
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    String newStatus = statuses.get(which);
                    // Create updated task with new status
                    task.setStatus(newStatus);
                    
                    // Remove from current adapter
                    currentAdapter.removeTask(task);
                    // Add to target adapter
                    if (Task.STATUS_PENDING.equals(newStatus)) {
                        kanbanPendingAdapter.addTask(task);
                    } else if (Task.STATUS_IN_PROGRESS.equals(newStatus)) {
                        kanbanInProgressAdapter.addTask(task);
                    } else {
                        kanbanCompletedAdapter.addTask(task);
                    }
                    // Update status in database
                    updateTaskStatus(task, newStatus);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    /**
     * Load view preference from SharedPreferences.
     */
    private void loadViewPreference() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        isKanbanView = prefs.getBoolean("is_kanban_view", false);
        toggleView(isKanbanView);
    }
    
    /**
     * Save view preference to SharedPreferences.
     */
    private void saveViewPreference(boolean isKanban) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean("is_kanban_view", isKanban).apply();
    }
    
    /**
     * Toggle between list and kanban view.
     */
    private void toggleView(boolean showKanban) {
        isKanbanView = showKanban;
        
        if (showKanban) {
            if (rvTasks != null) rvTasks.setVisibility(View.GONE);
            if (kanbanScrollView != null) kanbanScrollView.setVisibility(View.VISIBLE);
        } else {
            if (rvTasks != null) rvTasks.setVisibility(View.VISIBLE);
            if (kanbanScrollView != null) kanbanScrollView.setVisibility(View.GONE);
        }
        
        saveViewPreference(isKanbanView);
        
        // Reload tasks to update the current view (only if executorService is initialized)
        if (executorService != null) {
            loadTasks();
        }
    }
    
    /**
     * Update task status when moved between columns.
     */
    private void updateTaskStatus(TaskDTO task, String newStatus) {
        executorService.execute(() -> {
            boolean success = taskDAO.updateStatus(task.getId(), newStatus);
            mainHandler.post(() -> {
                if (success) {
                    Toast.makeText(this, "Đã cập nhật trạng thái", Toast.LENGTH_SHORT).show();
                    loadTasks(); // Reload to refresh both views
                } else {
                    Toast.makeText(this, "Không thể cập nhật trạng thái", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * Thiết lập các event listeners.
     */
    private void setupListeners() {
        try {
            if (btnLogout != null) {
                btnLogout.setOnClickListener(v -> showLogoutDialog());
            } else {
                android.util.Log.e("MainActivity", "btnLogout is null");
            }

            if (fabAddTask != null) {
                fabAddTask.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        android.util.Log.e("MainActivity", "Error navigating to AddTaskActivity: " + e.getMessage(), e);
                        Toast.makeText(this, "Lỗi mở màn hình thêm công việc", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                android.util.Log.e("MainActivity", "fabAddTask is null");
            }

            if (spinnerCategory != null) {
                spinnerCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                        // Reload tasks with selected category filter
                        loadTasks();
                    }

                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {
                    }
                });
            } else {
                android.util.Log.w("MainActivity", "spinnerCategory is null - category filter will not work");
            }

            if (btnAddCategory != null) {
                btnAddCategory.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(MainActivity.this, AddCategoryActivity.class);
                        startActivityForResult(intent, 200); // Request code 200 for category
                    } catch (Exception e) {
                        android.util.Log.e("MainActivity", "Error navigating to AddCategoryActivity: " + e.getMessage(), e);
                        Toast.makeText(this, "Lỗi mở màn hình thêm danh mục", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            if (btnToggleView != null) {
                btnToggleView.setOnClickListener(v -> {
                    toggleView(!isKanbanView);
                });
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error setting up listeners: " + e.getMessage(), e);
            Toast.makeText(this, "Lỗi khởi tạo giao diện", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Load danh sách categories từ database.
     */
    private void loadCategories() {
        executorService.execute(() -> {
            List<Category> loadedCategories = categoryDAO.findByUserId(currentUser.getId());

            mainHandler.post(() -> {
                if (loadedCategories != null) {
                    categories = loadedCategories;
                    setupCategorySpinner();
                }
            });
        });
    }

    /**
     * Setup category spinner adapter.
     */
    private void setupCategorySpinner() {
        if (spinnerCategory == null) {
            android.util.Log.w("MainActivity", "spinnerCategory is null, cannot setup adapter");
            return;
        }

        try {
            List<String> categoryNames = new ArrayList<>();
            categoryNames.add("Tất cả danh mục"); // Default option

            for (Category cat : categories) {
                categoryNames.add(cat.getName());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, categoryNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategory.setAdapter(adapter);
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error setting up category spinner: " + e.getMessage(), e);
        }
    }

    /**
     * Get selected category ID from spinner (-1 for all categories).
     */
    private int getSelectedCategoryId() {
        if (spinnerCategory == null || categories == null || categories.isEmpty()) {
            return -1; // All categories if spinner not available
        }
        
        try {
            int position = spinnerCategory.getSelectedItemPosition();
            if (position <= 0) {
                return -1; // All categories
            }
            if (position - 1 < categories.size()) {
                return categories.get(position - 1).getId(); // Adjusted for "All" option at position 0
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error getting selected category: " + e.getMessage(), e);
        }
        
        return -1; // Default to all categories on error
    }

    /**
     * Load danh sách tasks từ database.
     */
    private void loadTasks() {
        showLoading(true);

        executorService.execute(() -> {
            try {
                // Lấy danh sách tasks của user hiện tại
                List<Task> tasks = taskDAO.findByUserId(currentUser.getId());

                // Filter by category if selected
                int selectedCategoryId = getSelectedCategoryId();
                if (selectedCategoryId > 0) {
                    List<Task> filteredTasks = new ArrayList<>();
                    for (Task task : tasks) {
                        if (task.getCategoryId() == selectedCategoryId) {
                            filteredTasks.add(task);
                        }
                    }
                    tasks = filteredTasks;
                }

                // Chuyển đổi sang DTOs sử dụng TaskAdapter
                List<TaskDTO> taskDTOs = TaskAdapter.toDTOList(tasks, currentUser.getUsername());

                mainHandler.post(() -> {
                    showLoading(false);
                    updateUI(taskDTOs);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(MainActivity.this,
                            getString(R.string.error_connection), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Cập nhật UI với danh sách tasks.
     *
     * @param tasks Danh sách TaskDTO
     */
    private void updateUI(List<TaskDTO> tasks) {
        if (tasks.isEmpty()) {
            if (isKanbanView) {
                kanbanScrollView.setVisibility(View.GONE);
            } else {
                rvTasks.setVisibility(View.GONE);
            }
            emptyStateLayout.setVisibility(View.VISIBLE);
            tvTaskCount.setText("Bạn có 0 công việc");
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            tvTaskCount.setText("Bạn có " + tasks.size() + " công việc");
            
            if (isKanbanView) {
                updateKanbanView(tasks);
            } else {
                rvTasks.setVisibility(View.VISIBLE);
                taskListAdapter.setTaskList(tasks);
            }
        }
    }
    
    /**
     * Cập nhật Kanban view với tasks được phân loại theo status.
     */
    private void updateKanbanView(List<TaskDTO> tasks) {
        List<TaskDTO> pendingTasks = new ArrayList<>();
        List<TaskDTO> inProgressTasks = new ArrayList<>();
        List<TaskDTO> completedTasks = new ArrayList<>();
        
        for (TaskDTO task : tasks) {
            String status = task.getStatus();
            if (Task.STATUS_PENDING.equals(status)) {
                pendingTasks.add(task);
            } else if (Task.STATUS_IN_PROGRESS.equals(status)) {
                inProgressTasks.add(task);
            } else if (Task.STATUS_COMPLETED.equals(status)) {
                completedTasks.add(task);
            }
        }
        
        if (kanbanPendingAdapter != null) {
            kanbanPendingAdapter.setTaskList(pendingTasks);
        }
        if (kanbanInProgressAdapter != null) {
            kanbanInProgressAdapter.setTaskList(inProgressTasks);
        }
        if (kanbanCompletedAdapter != null) {
            kanbanCompletedAdapter.setTaskList(completedTasks);
        }
        
        kanbanScrollView.setVisibility(View.VISIBLE);
    }
    

    /**
     * Xử lý click vào task.
     */
    @Override
    public void onTaskClick(TaskDTO task) {
        Intent intent = new Intent(this, TaskDetailActivity.class);
        intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.getId());
        startActivityForResult(intent, 100);
    }

    /**
     * Xử lý long click vào task.
     */
    @Override
    public void onTaskLongClick(TaskDTO task) {
        showTaskOptionsDialog(task);
    }

    /**
     * Hiển thị dialog tùy chọn cho task.
     */
    private void showTaskOptionsDialog(TaskDTO task) {
        String[] options = { "Đánh dấu hoàn thành", "Chỉnh sửa", "Xóa" };

        new AlertDialog.Builder(this)
                .setTitle(task.getTitle())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Đánh dấu hoàn thành
                            updateTaskStatus(task, Task.STATUS_COMPLETED);
                            break;
                        case 1: // Chỉnh sửa
                            Intent editIntent = new Intent(MainActivity.this, EditTaskActivity.class);
                            editIntent.putExtra(EditTaskActivity.EXTRA_TASK_ID, task.getId());
                            startActivityForResult(editIntent, 100);
                            break;
                        case 2: // Xóa
                            showDeleteConfirmDialog(task);
                            break;
                    }
                })
                .show();
    }


    /**
     * Hiển thị dialog xác nhận xóa task.
     */
    private void showDeleteConfirmDialog(TaskDTO task) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_confirm))
                .setMessage(getString(R.string.dialog_delete_task))
                .setPositiveButton(getString(R.string.dialog_confirm), (dialog, which) -> deleteTask(task))
                .setNegativeButton(getString(R.string.dialog_cancel), null)
                .show();
    }

    /**
     * Xóa task.
     */
    private void deleteTask(TaskDTO task) {
        executorService.execute(() -> {
            boolean success = taskDAO.delete(task.getId());

            mainHandler.post(() -> {
                if (success) {
                    Toast.makeText(this, getString(R.string.success_task_deleted), Toast.LENGTH_SHORT).show();
                    loadTasks(); // Reload danh sách
                }
            });
        });
    }

    /**
     * Hiển thị dialog xác nhận đăng xuất.
     */
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_confirm))
                .setMessage(getString(R.string.dialog_logout))
                .setPositiveButton(getString(R.string.btn_logout), (dialog, which) -> performLogout())
                .setNegativeButton(getString(R.string.dialog_cancel), null)
                .show();
    }

    /**
     * Thực hiện đăng xuất.
     */
    private void performLogout() {
        // Xóa thông tin session
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().clear().apply();

        navigateToLogin();
    }

    /**
     * Chuyển đến màn hình đăng nhập.
     */
    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Hiển thị/ẩn loading indicator.
     *
     * @param show true để hiển thị
     */
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Nếu thêm hoặc chỉnh sửa task thành công, reload danh sách
        if (requestCode == 100 && resultCode == RESULT_OK) {
            loadTasks();
        }
        // Nếu thêm category thành công, reload categories và tasks
        if (requestCode == 200 && resultCode == RESULT_OK) {
            loadCategories();
            loadTasks();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload tasks khi quay lại màn hình
        if (currentUser != null) {
            loadTasks();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
