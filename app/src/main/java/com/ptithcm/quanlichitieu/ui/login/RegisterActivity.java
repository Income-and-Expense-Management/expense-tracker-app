package com.ptithcm.quanlichitieu.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.repository.AuthService;
import com.ptithcm.quanlichitieu.data.repository.MockAuthService;
import com.ptithcm.quanlichitieu.ui.main.MainActivity;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registerRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupUI();
    }

    private void setupUI() {
        EditText etName = findViewById(R.id.etName);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        EditText etConfirm = findViewById(R.id.etConfirmPassword);
        android.widget.Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvLoginPrompt = findViewById(R.id.tvLoginPrompt);

        // Make "Log In" part clickable and go back to LoginActivity
        String fullText = "Already have an account? Log In";
        SpannableString ss = new SpannableString(fullText);
        int start = fullText.indexOf("Log In");
        if (start != -1) {
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    finish(); // Go back to LoginActivity
                }
            };
            ss.setSpan(clickableSpan, start, start + "Log In".length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        tvLoginPrompt.setText(ss);
        tvLoginPrompt.setMovementMethod(LinkMovementMethod.getInstance());

        AuthService auth = MockAuthService.getInstance();

        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            String confirm = etConfirm.getText().toString();

            if (name.isEmpty()) {
                Toast.makeText(this, R.string.enter_name, Toast.LENGTH_SHORT).show();
                return;
            }

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.enter_email_password, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirm)) {
                Toast.makeText(this, R.string.passwords_not_match, Toast.LENGTH_SHORT).show();
                return;
            }

            // Show loading state
            btnRegister.setEnabled(false);
            Toast.makeText(this, R.string.registering, Toast.LENGTH_SHORT).show();

            auth.register(name, email, password, new AuthService.RegisterCallback() {
                @Override
                public void onSuccess(String username) {
                    runOnUiThread(() -> {
                        btnRegister.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, R.string.register_success, Toast.LENGTH_SHORT).show();
                        // Navigate to MainActivity with username
                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                        intent.putExtra(MainActivity.EXTRA_USERNAME, username);
                        startActivity(intent);
                        finish();
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        btnRegister.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        findViewById(R.id.btnGoogleSignUp).setOnClickListener(v -> {
            Toast.makeText(this, "Google Sign Up not implemented yet", Toast.LENGTH_SHORT).show();
        });
    }
}
