package com.example.ktck_android_k17;

import android.content.Intent;
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

import com.example.ktck_android_k17.adapter.UserAdapter;
import com.example.ktck_android_k17.dao.UserDAO;
import com.example.ktck_android_k17.dto.RegisterRequest;
import com.example.ktck_android_k17.model.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity xử lý đăng ký người dùng mới.
 * Sử dụng RegisterRequest DTO để thu thập dữ liệu và UserAdapter để chuyển đổi.
 */
public class RegisterActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private EditText edtUsername, edtEmail, edtPassword, edtConfirmPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;

    private UserDAO userDAO;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        initDatabase();
        setupListeners();
    }

    /**
     * Khởi tạo các view components.
     */
    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtUsername = findViewById(R.id.edtUsername);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);
    }

    /**
     * Khởi tạo database và executor service.
     */
    private void initDatabase() {
        userDAO = new UserDAO();
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Thiết lập các event listeners.
     */
    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnRegister.setOnClickListener(v -> performRegister());

        tvLogin.setOnClickListener(v -> finish());
    }

    /**
     * Thực hiện đăng ký tài khoản mới.
     */
    private void performRegister() {
        String username = edtUsername.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString();
        String confirmPassword = edtConfirmPassword.getText().toString();

        // Tạo RegisterRequest DTO
        RegisterRequest registerRequest = new RegisterRequest(username, email, password, confirmPassword);

        // Validate input
        if (!validateInput(registerRequest)) {
            return;
        }

        // Hiển thị loading
        showLoading(true);

        // Thực hiện đăng ký trên background thread
        executorService.execute(() -> {
            try {
                // Kiểm tra email đã tồn tại chưa
                boolean emailExists = userDAO.isEmailExists(registerRequest.getEmail());

                if (emailExists) {
                    mainHandler.post(() -> {
                        showLoading(false);
                        edtEmail.setError(getString(R.string.error_email_exists));
                        edtEmail.requestFocus();
                    });
                    return;
                }

                // Chuyển đổi RegisterRequest sang User entity sử dụng Adapter
                User user = UserAdapter.toEntity(registerRequest);

                // Lưu user vào database
                boolean success = userDAO.insert(user);

                mainHandler.post(() -> {
                    showLoading(false);

                    if (success) {
                        Toast.makeText(RegisterActivity.this,
                                getString(R.string.success_register), Toast.LENGTH_SHORT).show();

                        // Quay lại màn hình đăng nhập
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this,
                                getString(R.string.error_register_failed), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(RegisterActivity.this,
                            getString(R.string.error_connection), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Validate dữ liệu đăng ký.
     *
     * @param request RegisterRequest DTO
     * @return true nếu hợp lệ
     */
    private boolean validateInput(RegisterRequest request) {
        // Validate username
        if (!request.isUsernameValid()) {
            edtUsername.setError(getString(R.string.error_empty_username));
            edtUsername.requestFocus();
            return false;
        }

        // Validate email
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

        // Validate password
        if (request.getPassword().isEmpty()) {
            edtPassword.setError(getString(R.string.error_empty_password));
            edtPassword.requestFocus();
            return false;
        }

        if (!request.isPasswordValid()) {
            edtPassword.setError(getString(R.string.error_password_short));
            edtPassword.requestFocus();
            return false;
        }

        // Validate confirm password
        if (!request.isPasswordMatch()) {
            edtConfirmPassword.setError(getString(R.string.error_password_mismatch));
            edtConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Hiển thị/ẩn loading indicator.
     *
     * @param show true để hiển thị
     */
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!show);
        edtUsername.setEnabled(!show);
        edtEmail.setEnabled(!show);
        edtPassword.setEnabled(!show);
        edtConfirmPassword.setEnabled(!show);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
