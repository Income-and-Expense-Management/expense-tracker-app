package com.ptithcm.quanlichitieu.data.remote;

/**
 * Centralized API configuration constants.
 * Single Responsibility: Only holds network endpoint definitions.
 * Open/Closed: Add new endpoints here without modifying existing code.
 */
public final class ApiConfig {

    private ApiConfig() {
        // Prevent instantiation
    }

    public static final String BASE_URL = "http://10.0.2.2:3000";

    // Auth endpoints
    public static final String LOGIN_URL = BASE_URL + "/api/v1/auth/login";
    public static final String REGISTER_URL = BASE_URL + "/api/v1/auth/register";
    public static final String GOOGLE_LOGIN_URL = BASE_URL + "/api/v1/auth/google";
    public static final String LOGOUT_URL = BASE_URL + "/api/v1/auth/logout";

    // Wallet endpoints
    public static final String WALLETS_URL = BASE_URL + "/api/v1/wallets/";
    public static final String WALLET_URL  = BASE_URL + "/api/v1/wallets/"; // append walletId

    // Category endpoints
    public static final String CATEGORIES_URL = BASE_URL + "/api/v1/categories/";
    public static final String CATEGORY_URL   = BASE_URL + "/api/v1/categories/"; // append categoryId

    // Sync endpoints (batch sync — dùng cho pull/push toàn bộ dữ liệu)
    public static final String SYNC_PUSH_URL = BASE_URL + "/api/v1/sync/push";
    public static final String SYNC_PULL_URL = BASE_URL + "/api/v1/sync/pull";

    // HTTP header constants
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String CONTENT_TYPE_JSON = "application/json";
}
