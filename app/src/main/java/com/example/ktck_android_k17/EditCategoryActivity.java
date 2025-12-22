package com.example.ktck_android_k17;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ktck_android_k17.dao.CategoryDAO;
import com.example.ktck_android_k17.model.Category;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity để chỉnh sửa danh mục.
 */
public class EditCategoryActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_ID = "category_id";

    private static final String PREF_NAME = "TaskManagerPrefs";
    private static final String KEY_USER_ID = "user_id";

    private ImageButton btnBack;
    private TextView tvTitle;
    private EditText edtName, edtDescription, edtColor;
    private Button btnSave;
    private ProgressBar progressBar;
    private View colorPreview;

    private CategoryDAO categoryDAO;
    private ExecutorService executorService;
    private Handler mainHandler;
    private int currentUserId;
    private int categoryId;
    private Category currentCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_category);

        // Lấy category ID từ intent
        categoryId = getIntent().getIntExtra(EXTRA_CATEGORY_ID, -1);
        if (categoryId == -1) {
            Toast.makeText(this, "Không tìm thấy danh mục", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Lấy user ID từ session
        loadUserSession();

        if (currentUserId == -1) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initDatabase();
        setupListeners();
        loadCategoryData();
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
        edtName = findViewById(R.id.edtName);
        edtDescription = findViewById(R.id.edtDescription);
        edtColor = findViewById(R.id.edtColor);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);
        colorPreview = findViewById(R.id.colorPreview);

        // Update button text
        if (btnSave != null) {
            btnSave.setText("Cập nhật danh mục");
        }

        // Update color preview when color text changes
        if (edtColor != null) {
            edtColor.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    updateColorPreview(s.toString());
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }
    }

    /**
     * Cập nhật màu preview.
     */
    private void updateColorPreview(String colorHex) {
        if (colorPreview != null) {
            try {
                int color;
                if (!colorHex.isEmpty() && colorHex.matches("^#[0-9A-Fa-f]{6}$")) {
                    color = android.graphics.Color.parseColor(colorHex);
                } else {
                    // Default color if invalid
                    color = android.graphics.Color.parseColor("#3498db");
                }
                colorPreview.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
            } catch (Exception e) {
                // Invalid color, use default
                try {
                    colorPreview.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#3498db")));
                } catch (Exception e2) {
                    android.util.Log.e("EditCategoryActivity", "Error setting default color: " + e2.getMessage());
                }
            }
        }
    }

    /**
     * Khởi tạo database và executor service.
     */
    private void initDatabase() {
        categoryDAO = new CategoryDAO();
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

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> updateCategory());
        }
    }

    /**
     * Load dữ liệu category từ database.
     */
    private void loadCategoryData() {
        showLoading(true);

        executorService.execute(() -> {
            Category category = categoryDAO.findById(categoryId);

            mainHandler.post(() -> {
                showLoading(false);
                if (category != null) {
                    currentCategory = category;
                    populateForm(category);
                } else {
                    Toast.makeText(EditCategoryActivity.this,
                            "Không tìm thấy danh mục", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    /**
     * Điền dữ liệu vào form.
     */
    private void populateForm(Category category) {
        if (edtName != null) {
            edtName.setText(category.getName());
        }
        if (edtDescription != null) {
            edtDescription.setText(category.getDescription() != null ? category.getDescription() : "");
        }
        if (edtColor != null) {
            String color = category.getColor() != null ? category.getColor() : "#3498db";
            edtColor.setText(color);
            updateColorPreview(color);
        }
    }

    /**
     * Validate dữ liệu đầu vào.
     */
    private boolean validateInput() {
        String name = edtName != null ? edtName.getText().toString().trim() : "";
        String color = edtColor != null ? edtColor.getText().toString().trim() : "";

        if (name.isEmpty()) {
            if (edtName != null) {
                edtName.setError("Vui lòng nhập tên danh mục");
                edtName.requestFocus();
            }
            return false;
        }

        if (name.length() > 100) {
            if (edtName != null) {
                edtName.setError("Tên danh mục không được vượt quá 100 ký tự");
                edtName.requestFocus();
            }
            return false;
        }

        // Validate color format (hex color)
        if (!color.isEmpty() && !color.matches("^#[0-9A-Fa-f]{6}$")) {
            if (edtColor != null) {
                edtColor.setError("Màu sắc không hợp lệ (ví dụ: #3498db)");
                edtColor.requestFocus();
            }
            return false;
        }

        return true;
    }

    /**
     * Cập nhật danh mục vào database.
     */
    private void updateCategory() {
        if (!validateInput()) {
            return;
        }

        String name = edtName != null ? edtName.getText().toString().trim() : "";
        String description = edtDescription != null ? edtDescription.getText().toString().trim() : "";
        String color = edtColor != null ? edtColor.getText().toString().trim() : "";

        // Set default color if empty
        if (color.isEmpty()) {
            color = "#3498db";
        }

        showLoading(true);

        // Cập nhật Category entity
        currentCategory.setName(name);
        currentCategory.setDescription(description.isEmpty() ? null : description);
        currentCategory.setColor(color);

        executorService.execute(() -> {
            try {
                boolean success = categoryDAO.update(currentCategory);

                mainHandler.post(() -> {
                    showLoading(false);
                    if (success) {
                        Toast.makeText(EditCategoryActivity.this,
                                "Cập nhật danh mục thành công!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(EditCategoryActivity.this,
                                "Không thể cập nhật danh mục. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    showLoading(false);
                    android.util.Log.e("EditCategoryActivity", "Error updating category: " + e.getMessage(), e);
                    Toast.makeText(EditCategoryActivity.this,
                            "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Hiển thị/ẩn loading indicator.
     *
     * @param show true để hiển thị
     */
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (btnSave != null) {
            btnSave.setEnabled(!show);
        }
        if (edtName != null) {
            edtName.setEnabled(!show);
        }
        if (edtDescription != null) {
            edtDescription.setEnabled(!show);
        }
        if (edtColor != null) {
            edtColor.setEnabled(!show);
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

