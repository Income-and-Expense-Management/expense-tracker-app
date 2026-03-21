package com.ptithcm.quanlichitieu.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.repository.AuthService;
import com.ptithcm.quanlichitieu.data.repository.MockAuthService;
import com.ptithcm.quanlichitieu.ui.home.HomeActivity;

/**
 * LoginActivity serves as the Login Screen.
 * It handles user authentication using MockAuthService.
 */
public class LoginActivity extends AppCompatActivity {

    private AuthService authService; // Dependency Injection (Manual for now)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize dependencies
        // Ideally handled via DI framework (Dagger/Hilt/Koin)
        authService = new MockAuthService();

        setupUI();
    }

    private void setupUI() {
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);

        // Pre-fill for convenience/testing
        etEmail.setText("admin@test.com");

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            performLogin(email, password);
        });
    }

    private void performLogin(String email, String password) {
        // Show loading indicator
        Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();

        authService.login(email, password, new AuthService.LoginCallback() {
            @Override
            public void onSuccess(String username) {
                Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                navigateToHome(username);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(LoginActivity.this, "Login Failed: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToHome(String username) {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        intent.putExtra(HomeActivity.EXTRA_USERNAME, username);
        startActivity(intent);
        finish(); // Close Login screen so back button exits app or returns to expected state
    }
}
