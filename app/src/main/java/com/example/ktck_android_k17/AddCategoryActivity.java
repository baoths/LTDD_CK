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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ktck_android_k17.dao.CategoryDAO;
import com.example.ktck_android_k17.model.Category;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity để thêm danh mục mới.
 */
public class AddCategoryActivity extends AppCompatActivity {

    private static final String PREF_NAME = "TaskManagerPrefs";
    private static final String KEY_USER_ID = "user_id";

    private ImageButton btnBack;
    private EditText edtName, edtDescription, edtColor;
    private Button btnSave;
    private ProgressBar progressBar;
    private View colorPreview;

    private CategoryDAO categoryDAO;
    private ExecutorService executorService;
    private Handler mainHandler;
    private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_category);

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

        // Set default color
        edtColor.setText("#3498db");
        
        // Update color preview when color text changes
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
        
        // Initial color preview
        updateColorPreview("#3498db");
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
                    android.util.Log.e("AddCategoryActivity", "Error setting default color: " + e2.getMessage());
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
        btnBack.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> saveCategory());
    }

    /**
     * Validate dữ liệu đầu vào.
     */
    private boolean validateInput() {
        String name = edtName.getText().toString().trim();
        String color = edtColor.getText().toString().trim();

        if (name.isEmpty()) {
            edtName.setError("Vui lòng nhập tên danh mục");
            edtName.requestFocus();
            return false;
        }

        if (name.length() > 100) {
            edtName.setError("Tên danh mục không được vượt quá 100 ký tự");
            edtName.requestFocus();
            return false;
        }

        // Validate color format (hex color)
        if (!color.isEmpty() && !color.matches("^#[0-9A-Fa-f]{6}$")) {
            edtColor.setError("Màu sắc không hợp lệ (ví dụ: #3498db)");
            edtColor.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Lưu danh mục mới vào database.
     */
    private void saveCategory() {
        if (!validateInput()) {
            return;
        }

        String name = edtName.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();
        String color = edtColor.getText().toString().trim();

        // Set default color if empty
        if (color.isEmpty()) {
            color = "#3498db";
        }

        showLoading(true);

        // Tạo Category entity
        Category category = new Category(
                currentUserId,
                name,
                description.isEmpty() ? null : description,
                color
        );

        executorService.execute(() -> {
            try {
                boolean success = categoryDAO.insert(category);

                mainHandler.post(() -> {
                    showLoading(false);
                    if (success) {
                        Toast.makeText(AddCategoryActivity.this,
                                "Tạo danh mục thành công!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(AddCategoryActivity.this,
                                "Không thể tạo danh mục. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    showLoading(false);
                    android.util.Log.e("AddCategoryActivity", "Error saving category: " + e.getMessage(), e);
                    Toast.makeText(AddCategoryActivity.this,
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
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!show);
        edtName.setEnabled(!show);
        edtDescription.setEnabled(!show);
        edtColor.setEnabled(!show);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}

