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
    public static final String LOGIN_URL = BASE_URL + "/api/auth/login";
    public static final String REGISTER_URL = BASE_URL + "/api/auth/register";
    public static final String GOOGLE_LOGIN_URL = BASE_URL + "/api/auth/google";
    public static final String LOGOUT_URL = BASE_URL + "/api/auth/logout";

    // HTTP header constants
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String CONTENT_TYPE_JSON = "application/json";
}
