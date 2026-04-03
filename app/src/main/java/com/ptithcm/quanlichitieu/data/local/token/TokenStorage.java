package com.ptithcm.quanlichitieu.data.local.token;

/**
 * Interface Segregation: Defines only token-related storage operations.
 * Dependency Inversion: Upper layers depend on this abstraction, not on
 * the concrete EncryptedSharedPreferences implementation.
 */
public interface TokenStorage {

    void saveToken(String token);

    String getToken();

    void saveUserInfo(String fullName, String email);

    String getUserFullName();

    String getUserEmail();

    void clearAll();

    boolean hasToken();
}
