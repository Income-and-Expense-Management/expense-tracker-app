package com.ptithcm.quanlichitieu.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.ui.login.LoginActivity;

/**
 * SplashActivity: The initial screen displayed when the app starts.
 * It shows for a few seconds before navigating to the Login screen.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 2000; // 2 seconds delay

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Using a Handler to delay the transition to the next screen
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Navigate to LoginActivity (Login Screen)
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Close SplashActivity so the user can't go back to it
        }, SPLASH_DELAY_MS);
    }
}
