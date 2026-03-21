package com.ptithcm.quanlichitieu.data.repository;

import android.os.Handler;
import android.os.Looper;

/**
 * Mock implementation of AuthService.
 * Validates against hardcoded user credentials for testing purposes.
 */
public class MockAuthService implements AuthService {
    // Mock user database
    private static final String MOCK_EMAIL = "admin@test.com";
    private static final String MOCK_PASSWORD = "password123";

    @Override
    public void login(String email, String password, LoginCallback callback) {
        // Simulate network delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isValid(email, password)) {
                callback.onSuccess("Duy");
            } else {
                callback.onError("Invalid email or password");
            }
        }, 1500); // 1.5 seconds delay
    }

    private boolean isValid(String email, String password) {
        return MOCK_EMAIL.equalsIgnoreCase(email) && MOCK_PASSWORD.equals(password);
    }
}
