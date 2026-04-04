package com.ptithcm.quanlichitieu.data.repository;

import androidx.annotation.NonNull;

/**
 * Repository interface abstracting authentication data sources from the UI layer.
 *
 * Dependency Inversion: ViewModels depend on this interface, not concrete classes.
 * Open/Closed: Swap MockAuthService for AuthRepositoryImpl without touching UI code.
 * Interface Segregation: Only exposes auth-related operations.
 */
public interface AuthRepository {

    /**
     * Generic callback for async auth operations.
     * @param <T> The result type (String for login/register, Void for logout).
     */
    interface AuthCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    void login(@NonNull String email, @NonNull String password,
               @NonNull AuthCallback<String> callback);

    void register(@NonNull String name, @NonNull String email, @NonNull String password,
                  @NonNull AuthCallback<String> callback);

    void loginWithGoogle(@NonNull String idToken, @NonNull String displayName,
                         @NonNull String email, @NonNull AuthCallback<String> callback);

    void logout(@NonNull AuthCallback<Void> callback);

    boolean isLoggedIn();

    String getUserFullName();

    String getUserEmail();
}
