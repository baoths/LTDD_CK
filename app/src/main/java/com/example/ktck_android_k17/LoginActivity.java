package com.example.ktck_android_k17;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ktck_android_k17.adapter.UserAdapter;
import com.example.ktck_android_k17.dao.UserDAO;
import com.example.ktck_android_k17.database.DatabaseHelper;
import com.example.ktck_android_k17.dto.LoginRequest;
import com.example.ktck_android_k17.dto.UserDTO;
import com.example.ktck_android_k17.model.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity xử lý đăng nhập người dùng.
 * Sử dụng LoginRequest DTO để thu thập dữ liệu và UserAdapter để chuyển đổi.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String PREF_NAME = "TaskManagerPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";

    private EditText edtEmail, edtPassword;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword;
    private ProgressBar progressBar;

    private UserDAO userDAO;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            android.util.Log.d("LoginActivity", "onCreate started");
            setContentView(R.layout.activity_login);
            android.util.Log.d("LoginActivity", "Layout loaded successfully");
        } catch (Exception e) {
            android.util.Log.e("LoginActivity", "CRITICAL: Error loading layout: " + e.getMessage(), e);
            e.printStackTrace();
            // Không thể hiển thị Toast nếu layout chưa load được
            try {
                Toast.makeText(this, "Lỗi tải giao diện: " + e.getMessage(), Toast.LENGTH_LONG).show();
            } catch (Exception e2) {
                android.util.Log.e("LoginActivity", "Cannot show toast: " + e2.getMessage());
            }
            finish();
            return;
        }

        // Kiểm tra nếu đã đăng nhập trước đó
        try {
            if (isLoggedIn()) {
                android.util.Log.d("LoginActivity", "User already logged in, navigating to MainActivity");
                navigateToMain();
                return;
            }
        } catch (Exception e) {
            android.util.Log.e("LoginActivity", "Error checking login status: " + e.getMessage(), e);
            // Tiếp tục với login screen
        }

        try {
            initViews();
            android.util.Log.d("LoginActivity", "Views initialized");

            // Khởi tạo database trong background để không block UI
            initDatabase();
            android.util.Log.d("LoginActivity", "Database initialization started");

            setupListeners();
            android.util.Log.d("LoginActivity", "Listeners setup complete");
        } catch (Exception e) {
            android.util.Log.e("LoginActivity", "CRITICAL: Error in onCreate: " + e.getMessage(), e);
            e.printStackTrace();
            try {
                Toast.makeText(this, "Lỗi khởi tạo: " + e.getMessage(), Toast.LENGTH_LONG).show();
            } catch (Exception e2) {
                android.util.Log.e("LoginActivity", "Cannot show error toast: " + e2.getMessage());
            }
            // Không finish() để user có thể thử lại
        }
    }

    /**
     * Khởi tạo các view components.
     */
    private void initViews() {
        try {
            edtEmail = findViewById(R.id.edtEmail);
            edtPassword = findViewById(R.id.edtPassword);
            btnLogin = findViewById(R.id.btnLogin);
            tvRegister = findViewById(R.id.tvRegister);
            tvForgotPassword = findViewById(R.id.tvForgotPassword);
            progressBar = findViewById(R.id.progressBar);
            
            // Kiểm tra null để tránh crash
            if (edtEmail == null || edtPassword == null || btnLogin == null) {
                android.util.Log.e("LoginActivity", "Some views are null!");
                throw new RuntimeException("Không tìm thấy các view cần thiết trong layout");
            }
        } catch (Exception e) {
            android.util.Log.e("LoginActivity", "Error initializing views: " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Khởi tạo database và executor service.
     */
    private void initDatabase() {
        try {
            android.util.Log.d("LoginActivity", "Initializing database components...");
            executorService = Executors.newSingleThreadExecutor();
            android.util.Log.d("LoginActivity", "ExecutorService created");
            
            mainHandler = new Handler(Looper.getMainLooper());
            android.util.Log.d("LoginActivity", "MainHandler created");
            
            // Khởi tạo UserDAO - chỉ lưu reference, không kết nối database ngay
            userDAO = new UserDAO();
            android.util.Log.d("LoginActivity", "UserDAO created");

            // Chạy initializeDatabase trong background thread để không block UI
            // Delay một chút để đảm bảo UI đã load xong
            mainHandler.postDelayed(() -> {
                try {
                    if (executorService != null && !executorService.isShutdown()) {
                        executorService.execute(() -> {
                            try {
                                android.util.Log.d("LoginActivity", "Starting database initialization in background...");
                                DatabaseHelper.getInstance().initializeDatabase();
                                android.util.Log.d("LoginActivity", "Database initialized successfully");
                            } catch (Exception e) {
                                android.util.Log.e("LoginActivity", "Error initializing database: " + e.getMessage(), e);
                                e.printStackTrace();
                                if (mainHandler != null) {
                                    mainHandler.post(() -> {
                                        // Không hiển thị toast ngay để tránh làm gián đoạn UI
                                        android.util.Log.e("LoginActivity", "Database init failed, will retry on first use");
                                    });
                                }
                            }
                        });
                    } else {
                        android.util.Log.w("LoginActivity", "ExecutorService is null or shutdown, cannot initialize database");
                    }
                } catch (Exception e) {
                    android.util.Log.e("LoginActivity", "Error posting database init task: " + e.getMessage(), e);
                }
            }, 1000); // Delay 1s để UI load xong trước
        } catch (Exception e) {
            android.util.Log.e("LoginActivity", "CRITICAL: Error setting up database: " + e.getMessage(), e);
            e.printStackTrace();
            // Không crash app nếu database setup fail - app vẫn có thể hoạt động
        }
    }

    /**
     * Thiết lập các event listeners.
     */
    private void setupListeners() {
        try {
            if (btnLogin != null) {
                btnLogin.setOnClickListener(v -> performLogin());
            } else {
                android.util.Log.e("LoginActivity", "btnLogin is null, cannot set listener");
            }

            if (tvRegister != null) {
                tvRegister.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        android.util.Log.e("LoginActivity", "Error navigating to RegisterActivity: " + e.getMessage(), e);
                        Toast.makeText(this, "Lỗi mở màn hình đăng ký", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            if (tvForgotPassword != null) {
                tvForgotPassword.setOnClickListener(v -> {
                    Toast.makeText(this, "Chức năng đang phát triển", Toast.LENGTH_SHORT).show();
                });
            }
        } catch (Exception e) {
            android.util.Log.e("LoginActivity", "Error setting up listeners: " + e.getMessage(), e);
        }
    }

    /**
     * Thực hiện đăng nhập.
     */
    private void performLogin() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        // Tạo LoginRequest DTO
        LoginRequest loginRequest = new LoginRequest(email, password);

        // Validate input
        if (!validateInput(loginRequest)) {
            return;
        }

        // Hiển thị loading
        showLoading(true);

        // Thực hiện đăng nhập trên background thread
        executorService.execute(() -> {
            try {
                // Bước 1: Kiểm tra email có tồn tại không
                User userByEmail = userDAO.findByEmail(loginRequest.getEmail());

                if (userByEmail == null) {
                    // Email không tồn tại
                    mainHandler.post(() -> {
                        showLoading(false);
                        edtEmail.setError("Email không tồn tại");
                        edtEmail.requestFocus();
                        Toast.makeText(LoginActivity.this,
                                "Email không tồn tại trong hệ thống", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Bước 2: Kiểm tra mật khẩu
                User user = userDAO.validateLogin(loginRequest.getEmail(), loginRequest.getPassword());

                mainHandler.post(() -> {
                    showLoading(false);

                    if (user != null) {
                        // Chuyển đổi User sang UserDTO (loại bỏ password)
                        UserDTO userDTO = UserAdapter.toDTO(user);

                        // Lưu thông tin đăng nhập
                        saveUserSession(userDTO);

                        // Thông báo thành công
                        Toast.makeText(LoginActivity.this,
                                getString(R.string.success_login), Toast.LENGTH_SHORT).show();

                        // Chuyển đến MainActivity
                        navigateToMain();
                    } else {
                        // Email đúng nhưng mật khẩu sai
                        edtPassword.setError("Mật khẩu không đúng");
                        edtPassword.requestFocus();
                        Toast.makeText(LoginActivity.this,
                                "Mật khẩu không đúng", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this,
                            getString(R.string.error_connection) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Validate dữ liệu đăng nhập.
     *
     * @param request LoginRequest DTO
     * @return true nếu hợp lệ
     */
    private boolean validateInput(LoginRequest request) {
        if (request.getEmail().isEmpty()) {
            edtEmail.setError(getString(R.string.error_empty_email));
            edtEmail.requestFocus();
            return false;
        }

        if (!request.isEmailValid()) {
            edtEmail.setError(getString(R.string.error_invalid_email));
            edtEmail.requestFocus();
            return false;
        }

        if (request.getPassword().isEmpty()) {
            edtPassword.setError(getString(R.string.error_empty_password));
            edtPassword.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Lưu thông tin user vào SharedPreferences.
     *
     * @param userDTO UserDTO đã đăng nhập
     */
    private void saveUserSession(UserDTO userDTO) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_USER_ID, userDTO.getId());
        editor.putString(KEY_USERNAME, userDTO.getUsername());
        editor.putString(KEY_EMAIL, userDTO.getEmail());
        editor.apply();
    }

    /**
     * Kiểm tra đã đăng nhập chưa.
     *
     * @return true nếu đã đăng nhập
     */
    private boolean isLoggedIn() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        return prefs.getInt(KEY_USER_ID, -1) != -1;
    }

    /**
     * Chuyển đến MainActivity.
     */
    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
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
        btnLogin.setEnabled(!show);
        edtEmail.setEnabled(!show);
        edtPassword.setEnabled(!show);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
