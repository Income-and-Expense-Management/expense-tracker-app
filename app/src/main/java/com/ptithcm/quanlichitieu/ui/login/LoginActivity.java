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
import androidx.lifecycle.ViewModelProvider;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.ui.main.MainActivity;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel loginViewModel;
    private Button btnLogin;

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

        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        setupUI();
        observeLoginState();
    }

    private void setupUI() {
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        etEmail.setText("admin@test.com");

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.enter_email_password, Toast.LENGTH_SHORT).show();
                return;
            }

            loginViewModel.login(email, password);
        });
    }

    private void observeLoginState() {
        loginViewModel.getLoginState().observe(this, loginState -> {
            switch (loginState.getState()) {
                case LOADING:
                    btnLogin.setEnabled(false);
                    Toast.makeText(this, R.string.logging_in, Toast.LENGTH_SHORT).show();
                    break;
                case SUCCESS:
                    btnLogin.setEnabled(true);
                    Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
                    navigateToMain(loginState.getData());
                    break;
                case ERROR:
                    btnLogin.setEnabled(true);
                    Toast.makeText(this,
                            getString(R.string.login_failed, loginState.getData()),
                            Toast.LENGTH_SHORT).show();
                    break;
                case IDLE:
                    btnLogin.setEnabled(true);
                    break;
            }
        });
    }

    private void navigateToMain(String username) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_USERNAME, username);
        startActivity(intent);
        finish();
    }
}
