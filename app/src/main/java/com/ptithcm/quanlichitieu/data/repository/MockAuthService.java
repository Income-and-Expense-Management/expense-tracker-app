package com.ptithcm.quanlichitieu.data.repository;

import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.Map;

/**
 * Mock implementation of AuthService.
 * Validates against hardcoded user credentials for testing purposes.
 */
public class MockAuthService implements AuthService {
    // Simple in-memory user store: email -> {name,password}
    private final Map<String, User> users = new HashMap<>();

    private static final long DELAY_MS = 1500;

    private static MockAuthService instance;

    private MockAuthService() {
        // Pre-populate with a default user
        users.put("admin@test.com", new User("Duy", "password123"));
    }

    public static synchronized MockAuthService getInstance() {
        if (instance == null) {
            instance = new MockAuthService();
        }
        return instance;
    }

    @Override
    public void login(String email, String password, LoginCallback callback) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            User u = users.get(email.toLowerCase());
            if (u != null && u.password.equals(password)) {
                callback.onSuccess(u.name);
            } else {
                callback.onError("Invalid email or password");
            }
        }, DELAY_MS);
    }

    @Override
    public void register(String name, String email, String password, RegisterCallback callback) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            String key = email.toLowerCase();
            if (users.containsKey(key)) {
                callback.onError("Email already registered");
                return;
            }
            users.put(key, new User(name, password));
            callback.onSuccess(name);
        }, DELAY_MS);
    }

    private static class User {
        final String name;
        final String password;

        User(String name, String password) {
            this.name = name;
            this.password = password;
        }
    }
}
