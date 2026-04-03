package com.ptithcm.quanlichitieu.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.util.Log;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.ui.main.MainActivity;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private AuthViewModel authViewModel;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registerRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupUI();
        observeRegisterState();
    }

    private void setupUI() {
        EditText etName = findViewById(R.id.etName);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        EditText etConfirm = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        TextView tvLoginPrompt = findViewById(R.id.tvLoginPrompt);

        // Make "Log In" part clickable and go back to LoginActivity
        String fullText = "Đã có tài khoản? Đăng nhập";
        SpannableString ss = new SpannableString(fullText);
        int start = fullText.indexOf("Đăng nhập");
        if (start != -1) {
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    finish();
                }
            };
            ss.setSpan(clickableSpan, start, start + "Đăng nhập".length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        tvLoginPrompt.setText(ss);
        tvLoginPrompt.setMovementMethod(LinkMovementMethod.getInstance());

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

            authViewModel.register(name, email, password);
        });

        findViewById(R.id.btnGoogleSignUp).setOnClickListener(v -> {
            Toast.makeText(this, "Google Sign Up not implemented yet", Toast.LENGTH_SHORT).show();
        });
    }

    private void observeRegisterState() {
        authViewModel.getRegisterState().observe(this, authState -> {
            Log.d(TAG, "observeRegisterState: status=" + authState.getStatus()
                    + ", data=" + authState.getData());
            switch (authState.getStatus()) {
                case LOADING:
                    btnRegister.setEnabled(false);
                    Toast.makeText(this, R.string.registering, Toast.LENGTH_SHORT).show();
                    break;
                case SUCCESS:
                    btnRegister.setEnabled(true);
                    Toast.makeText(this, R.string.register_success, Toast.LENGTH_SHORT).show();
                    navigateToMain(authState.getData());
                    break;
                case ERROR:
                    btnRegister.setEnabled(true);
                    Toast.makeText(this, authState.getData(), Toast.LENGTH_SHORT).show();
                    break;
                case IDLE:
                    btnRegister.setEnabled(true);
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
