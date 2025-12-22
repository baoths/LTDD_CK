package com.example.ktck_android_k17;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ktck_android_k17.dao.CategoryDAO;
import com.example.ktck_android_k17.dao.TaskDAO;
import com.example.ktck_android_k17.model.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity để quản lý danh sách categories (xem, sửa, xóa).
 */
public class CategoryManagementActivity extends AppCompatActivity {

    private static final String PREF_NAME = "TaskManagerPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final int REQUEST_CODE_EDIT_CATEGORY = 100;
    private static final int REQUEST_CODE_ADD_CATEGORY = 200;

    private ImageButton btnBack, btnAddCategory;
    private RecyclerView rvCategories;
    private LinearLayout emptyStateLayout;
    private ProgressBar progressBar;

    private CategoryDAO categoryDAO;
    private TaskDAO taskDAO;
    private ExecutorService executorService;
    private Handler mainHandler;
    private int currentUserId;
    private CategoryAdapter adapter;
    private List<Category> categories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_management);

        loadUserSession();

        if (currentUserId == -1) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initDatabase();
        setupListeners();
        loadCategories();
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
        btnAddCategory = findViewById(R.id.btnAddCategory);
        rvCategories = findViewById(R.id.rvCategories);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        progressBar = findViewById(R.id.progressBar);

        categories = new ArrayList<>();
        adapter = new CategoryAdapter();
        rvCategories.setLayoutManager(new LinearLayoutManager(this));
        rvCategories.setAdapter(adapter);
    }

    /**
     * Khởi tạo database và executor service.
     */
    private void initDatabase() {
        categoryDAO = new CategoryDAO();
        taskDAO = new TaskDAO();
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Thiết lập các event listeners.
     */
    private void setupListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnAddCategory != null) {
            btnAddCategory.setOnClickListener(v -> {
                Intent intent = new Intent(CategoryManagementActivity.this, AddCategoryActivity.class);
                startActivityForResult(intent, REQUEST_CODE_ADD_CATEGORY);
            });
        }
    }

    /**
     * Load danh sách categories từ database.
     */
    private void loadCategories() {
        showLoading(true);

        executorService.execute(() -> {
            List<Category> loadedCategories = categoryDAO.findByUserId(currentUserId);

            mainHandler.post(() -> {
                showLoading(false);
                categories.clear();
                if (loadedCategories != null && !loadedCategories.isEmpty()) {
                    categories.addAll(loadedCategories);
                    adapter.notifyDataSetChanged();
                    showEmptyState(false);
                } else {
                    showEmptyState(true);
                }
            });
        });
    }

    /**
     * Hiển thị/ẩn empty state.
     */
    private void showEmptyState(boolean show) {
        if (emptyStateLayout != null) {
            emptyStateLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (rvCategories != null) {
            rvCategories.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Hiển thị/ẩn loading indicator.
     */
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Mở EditCategoryActivity để chỉnh sửa category.
     */
    private void editCategory(Category category) {
        Intent intent = new Intent(CategoryManagementActivity.this, EditCategoryActivity.class);
        intent.putExtra(EditCategoryActivity.EXTRA_CATEGORY_ID, category.getId());
        startActivityForResult(intent, REQUEST_CODE_EDIT_CATEGORY);
    }

    /**
     * Xóa category với validation.
     */
    private void deleteCategory(Category category) {
        // Hiển thị dialog xác nhận
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa danh mục \"" + category.getName() + "\"?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    performDeleteCategory(category);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Thực hiện xóa category sau khi đã xác nhận.
     */
    private void performDeleteCategory(Category category) {
        showLoading(true);

        executorService.execute(() -> {
            // Kiểm tra xem category có đang được sử dụng không
            int taskCount = taskDAO.countByCategoryId(category.getId());

            mainHandler.post(() -> {
                showLoading(false);

                if (taskCount > 0) {
                    // Category đang được sử dụng, không cho phép xóa
                    Toast.makeText(CategoryManagementActivity.this,
                            "Không thể xóa danh mục này vì đang được sử dụng bởi " + taskCount + " task",
                            Toast.LENGTH_LONG).show();
                } else {
                    // Không có task nào sử dụng, thực hiện xóa
                    executorService.execute(() -> {
                        boolean success = categoryDAO.delete(category.getId());

                        mainHandler.post(() -> {
                            if (success) {
                                Toast.makeText(CategoryManagementActivity.this,
                                        "Xóa danh mục thành công!", Toast.LENGTH_SHORT).show();
                                loadCategories(); // Reload danh sách
                            } else {
                                Toast.makeText(CategoryManagementActivity.this,
                                        "Không thể xóa danh mục. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                }
            });
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            // Reload danh sách sau khi thêm/sửa category
            loadCategories();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    /**
     * RecyclerView Adapter cho danh sách categories.
     */
    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

        @NonNull
        @Override
        public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category, parent, false);
            return new CategoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
            Category category = categories.get(position);
            holder.bind(category);
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        /**
         * ViewHolder cho category item.
         */
        class CategoryViewHolder extends RecyclerView.ViewHolder {
            private View colorIndicator;
            private TextView tvCategoryName, tvCategoryDescription;
            private ImageButton btnEdit, btnDelete;

            public CategoryViewHolder(@NonNull View itemView) {
                super(itemView);
                colorIndicator = itemView.findViewById(R.id.colorIndicator);
                tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
                tvCategoryDescription = itemView.findViewById(R.id.tvCategoryDescription);
                btnEdit = itemView.findViewById(R.id.btnEdit);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }

            public void bind(Category category) {
                // Set category name
                if (tvCategoryName != null) {
                    tvCategoryName.setText(category.getName());
                }

                // Set description
                if (tvCategoryDescription != null) {
                    String description = category.getDescription();
                    if (description != null && !description.isEmpty()) {
                        tvCategoryDescription.setText(description);
                        tvCategoryDescription.setVisibility(View.VISIBLE);
                    } else {
                        tvCategoryDescription.setVisibility(View.GONE);
                    }
                }

                // Set color indicator
                if (colorIndicator != null) {
                    try {
                        String colorHex = category.getColor() != null ? category.getColor() : "#3498db";
                        int color = android.graphics.Color.parseColor(colorHex);
                        colorIndicator.setBackgroundTintList(
                                android.content.res.ColorStateList.valueOf(color));
                    } catch (Exception e) {
                        // Invalid color, use default
                        colorIndicator.setBackgroundTintList(
                                android.content.res.ColorStateList.valueOf(
                                        android.graphics.Color.parseColor("#3498db")));
                    }
                }

                // Set edit button listener
                if (btnEdit != null) {
                    btnEdit.setOnClickListener(v -> editCategory(category));
                }

                // Set delete button listener
                if (btnDelete != null) {
                    btnDelete.setOnClickListener(v -> deleteCategory(category));
                }
            }
        }
    }
}

