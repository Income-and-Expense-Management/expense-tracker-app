package com.ptithcm.quanlichitieu.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.util.Log;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.method.HideReturnsTransformationMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.ui.main.MainActivity;
import com.ptithcm.quanlichitieu.utils.ValidationUtils;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private AuthViewModel authViewModel;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Skip login screen if a valid token already exists
        if (authViewModel.isLoggedIn()) {
            Log.d(TAG, "Token exists — auto-navigating to MainActivity");
            navigateToMain(null);
            return;
        }

        setupUI();
        observeLoginState();
    }

    private void setupUI() {
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        TextView tvSignUpPrompt = findViewById(R.id.tvSignUpPrompt);


        // Make "Sign Up" part clickable and open RegisterActivity
        String full = "Không có tải khoản? Đăng kí";
        int start = full.indexOf("Đăng kí");
        SpannableString ss = new SpannableString(full);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        };
        ss.setSpan(clickableSpan, start, start + "Đăng kí".length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvSignUpPrompt.setText(ss);
        tvSignUpPrompt.setMovementMethod(LinkMovementMethod.getInstance());

        // Setup password visibility toggle
        setupPasswordToggle(etPassword);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.enter_email_password, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!ValidationUtils.isValidEmail(email)) {
                Toast.makeText(this, "Vui lòng nhập định dạng email hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            authViewModel.login(email, password);
        });
    }

    private void setupPasswordToggle(EditText etPassword) {
        etPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_END = 2;

            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                if (etPassword.getCompoundDrawables()[DRAWABLE_END] != null) {
                    if (event.getRawX() >= (etPassword.getRight() - etPassword.getCompoundDrawables()[DRAWABLE_END].getBounds().width())) {
                        // Toggle password visibility
                        if (etPassword.getTransformationMethod() instanceof PasswordTransformationMethod) {
                            // Show password
                            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                        } else {
                            // Hide password
                            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        }
                        // Keep cursor at the end
                        etPassword.setSelection(etPassword.getText().length());
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private void observeLoginState() {
        authViewModel.getLoginState().observe(this, authState -> {
            Log.d(TAG, "observeLoginState: status=" + authState.getStatus()
                    + ", data=" + authState.getData());
            switch (authState.getStatus()) {
                case LOADING:
                    btnLogin.setEnabled(false);
                    Toast.makeText(this, R.string.logging_in, Toast.LENGTH_SHORT).show();
                    break;
                case SUCCESS:
                    btnLogin.setEnabled(true);
                    Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
                    navigateToMain(authState.getData());
                    break;
                case ERROR:
                    btnLogin.setEnabled(true);
                    Toast.makeText(this,
                            getString(R.string.login_failed, authState.getData()),
                            Toast.LENGTH_SHORT).show();
                    break;
                case IDLE:
                    btnLogin.setEnabled(true);
                    break;
            }
        });
    }

    private void navigateToMain(String username) {
        Log.d(TAG, "navigateToMain: username=" + username);
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_USERNAME, username);
        startActivity(intent);
        finish();
    }
}
