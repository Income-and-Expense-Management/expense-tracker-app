package com.ptithcm.quanlichitieu.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.ptithcm.quanlichitieu.data.local.token.TokenStorage;
import com.ptithcm.quanlichitieu.data.remote.ApiConfig;
import com.ptithcm.quanlichitieu.data.remote.AuthJsonObjectRequest;
import com.ptithcm.quanlichitieu.data.remote.VolleySingleton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Concrete AuthRepository implementation backed by the Node.js REST API via Volley.
 *
 * Responsibilities:
 * - Constructs HTTP requests with the correct payload.
 * - Delegates token persistence to {@link TokenStorage} (SRP).
 * - Parses JSON responses and maps Volley callbacks to {@link AuthCallback}.
 *
 * Expected server response format for login/register:
 * {
 *   "token": "jwt_string",
 *   "user": { "id": "...", "fullName": "...", "email": "..." }
 * }
 */
public class AuthRepositoryImpl implements AuthRepository {

    private static final String TAG = "AuthRepositoryImpl";
    private final VolleySingleton volleySingleton;
    private final TokenStorage tokenStorage;

    @Nullable
    private AuthJsonObjectRequest.SessionExpiredListener sessionExpiredListener;

    public AuthRepositoryImpl(Context context, TokenStorage tokenStorage) {
        this.volleySingleton = VolleySingleton.getInstance(context);
        this.tokenStorage = tokenStorage;
    }

    /**
     * Sets a listener to be invoked when a 401 response is detected.
     * Typically wired by the ViewModel to expose session-expired events via LiveData.
     */
    public void setSessionExpiredListener(
            @Nullable AuthJsonObjectRequest.SessionExpiredListener listener) {
        this.sessionExpiredListener = listener;
    }

    @Override
    public void login(@NonNull String email, @NonNull String password,
                      @NonNull AuthCallback<String> callback) {
        Log.d(TAG, "login: Sending request to " + ApiConfig.LOGIN_URL + " for email=" + email);
        try {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("password", password);

            AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                    Request.Method.POST,
                    ApiConfig.LOGIN_URL,
                    body,
                    response -> {
                        Log.d(TAG, "login: Success response received");
                        handleAuthSuccess(response, callback);
                    },
                    error -> {
                        Log.e(TAG, "login: Error response received", error);
                        handleError(error, callback);
                    },
                    tokenStorage,
                    sessionExpiredListener
            );

            volleySingleton.addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "login: Failed to build JSON body", e);
            callback.onError("Failed to create login request");
        }
    }

    @Override
    public void register(@NonNull String full_name, @NonNull String email,
                         @NonNull String password, @NonNull AuthCallback<String> callback) {
        Log.d(TAG, "register: Sending request to " + ApiConfig.REGISTER_URL + " for email=" + email);
        try {
            JSONObject body = new JSONObject();
            body.put("full_name", full_name);
            body.put("email", email);
            body.put("password", password);
            body.put("avatar_url", null);

            AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                    Request.Method.POST,
                    ApiConfig.REGISTER_URL,
                    body,
                    response -> {
                        Log.d(TAG, "[REGISTER]: Success response received");
                        handleAuthSuccess(response, callback);
                    },
                    error -> {
                        Log.e(TAG, "register: Error response received", error);
                        handleError(error, callback);
                    },
                    tokenStorage,
                    sessionExpiredListener
            );

            volleySingleton.addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "register: Failed to build JSON body", e);
            callback.onError("Failed to create register request");
        }
    }

    @Override
    public void logout(@NonNull AuthCallback<Void> callback) {
        Log.d(TAG, "logout: Sending request to " + ApiConfig.LOGOUT_URL);
        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.POST,
                ApiConfig.LOGOUT_URL,
                null,
                response -> {
                    Log.d(TAG, "logout: Server confirmed logout, clearing all auth data");
                    tokenStorage.clearAll();
                    callback.onSuccess(null);
                },
                error -> {
                    Log.w(TAG, "logout: Server error, clearing auth data locally anyway", error);
                    tokenStorage.clearAll();
                    callback.onSuccess(null);
                },
                tokenStorage,
                sessionExpiredListener
        );

        volleySingleton.addToRequestQueue(request);
    }

    @Override
    public boolean isLoggedIn() {
        boolean loggedIn = tokenStorage.hasToken();
        Log.d(TAG, "isLoggedIn: " + loggedIn);
        return loggedIn;
    }

    @Override
    public String getUserId() {
        return tokenStorage.getUserId();
    }

    @Override
    public String getUserFullName() {
        return tokenStorage.getUserFullName();
    }

    @Override
    public String getUserEmail() {
        return tokenStorage.getUserEmail();
    }

    // --- Private helpers --------------------------------------------------------

    private void handleAuthSuccess(JSONObject response, AuthCallback<String> callback) {
        try {
            JSONObject data = response.getJSONObject("data");

            String token = data.getString("token");
            Log.d(TAG, "handleAuthSuccess: Token received (length=" + token.length() + ")");
            tokenStorage.saveToken(token);

            Log.d(TAG, "handleAuthSuccess: Token received (length=" + token.length() + ")");

            JSONObject user = data.getJSONObject("user");

            String fullName = user.isNull("full_name") ? null : user.optString("full_name", null);
            if (fullName == null || fullName.isEmpty()) {
                fullName = user.isNull("name") ? null : user.optString("name", null);
            }
            String email = user.isNull("email") ? null : user.optString("email", null);
            String userId = user.isNull("id") ? null : user.optString("id", null);
            Log.d(TAG, "handleAuthSuccess: User fullName=" + fullName + ", email=" + email);
            tokenStorage.saveUserInfo(userId, fullName, email);

            callback.onSuccess(fullName);
        } catch (JSONException e) {
            Log.e(TAG, "handleAuthSuccess: Failed to parse response", e);
            callback.onError("Invalid server response");
        }
    }

    private void handleError(VolleyError error, AuthCallback<?> callback) {
        String message = "Network error. Please try again.";

        if (error.networkResponse != null && error.networkResponse.data != null) {
            Log.e(TAG, "handleError: HTTP " + error.networkResponse.statusCode);
            try {
                String body = new String(error.networkResponse.data, "UTF-8");
                Log.e(TAG, "handleError: Response body=" + body);
                JSONObject json = new JSONObject(body);
                message = json.optString("message", message);
            } catch (UnsupportedEncodingException | JSONException ignored) {
                Log.e(TAG, "handleError: Could not parse error body");
            }
        } else {
            Log.e(TAG, "handleError: No network response — " + error.getMessage());
        }

        callback.onError(message);
    }
}
