package com.ptithcm.quanlichitieu.data.local.token;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Secure token storage backed by EncryptedSharedPreferences.
 * Uses AES256-SIV for key encryption and AES256-GCM for value encryption.
 *
 * Thread-safe Singleton with double-checked locking (project convention).
 * Single Responsibility: Only handles encrypted persistence of auth tokens.
 */
public class EncryptedTokenStorage implements TokenStorage {

    private static final String TAG = "EncryptedTokenStorage";
    private static final String PREFS_FILE_NAME = "secure_auth_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_USER_FULL_NAME = "user_full_name";
    private static final String KEY_USER_EMAIL = "user_email";

    private static volatile EncryptedTokenStorage instance;
    private final SharedPreferences sharedPreferences;

    private EncryptedTokenStorage(Context context) {
        Log.d(TAG, "Initializing EncryptedSharedPreferences");
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            sharedPreferences = EncryptedSharedPreferences.create(
                    PREFS_FILE_NAME,
                    masterKeyAlias,
                    context.getApplicationContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            Log.d(TAG, "EncryptedSharedPreferences created successfully");
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Failed to create EncryptedSharedPreferences", e);
            throw new RuntimeException("Failed to create EncryptedSharedPreferences", e);
        }
    }

    public static EncryptedTokenStorage getInstance(Context context) {
        if (instance == null) {
            synchronized (EncryptedTokenStorage.class) {
                if (instance == null) {
                    instance = new EncryptedTokenStorage(context);
                }
            }
        }
        return instance;
    }

    @Override
    public void saveToken(String token) {
        Log.d(TAG, "Saving token (length=" + (token != null ? token.length() : 0) + ")");
        sharedPreferences.edit().putString(KEY_ACCESS_TOKEN, token).apply();
    }

    @Override
    public String getToken() {
        String token = sharedPreferences.getString(KEY_ACCESS_TOKEN, null);
        Log.d(TAG, "getToken: " + (token != null ? "present" : "null"));
        return token;
    }

    @Override
    public void saveUserInfo(String fullName, String email) {
        Log.d(TAG, "Saving user info: fullName=" + fullName + ", email=" + email);
        sharedPreferences.edit()
                .putString(KEY_USER_FULL_NAME, fullName)
                .putString(KEY_USER_EMAIL, email)
                .apply();
    }

    @Override
    public String getUserFullName() {
        return sharedPreferences.getString(KEY_USER_FULL_NAME, null);
    }

    @Override
    public String getUserEmail() {
        return sharedPreferences.getString(KEY_USER_EMAIL, null);
    }

    @Override
    public void clearAll() {
        Log.d(TAG, "Clearing all auth data");
        sharedPreferences.edit().clear().apply();
    }

    @Override
    public boolean hasToken() {
        boolean has = getToken() != null;
        Log.d(TAG, "hasToken: " + has);
        return has;
    }
}
