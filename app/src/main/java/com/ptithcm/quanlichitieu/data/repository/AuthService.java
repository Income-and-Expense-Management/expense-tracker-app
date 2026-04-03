package com.ptithcm.quanlichitieu.data.repository;

/**
 * Interface defining authentication operations.
 * Allows switching between Mock and Real implementations.
 */
public interface AuthService {
    interface LoginCallback {
        void onSuccess(String username);
        void onError(String message);
    }

    interface RegisterCallback {
        void onSuccess(String username);
        void onError(String message);
    }

    void login(String email, String password, LoginCallback callback);
    void register(String name, String email, String password, RegisterCallback callback);
}
