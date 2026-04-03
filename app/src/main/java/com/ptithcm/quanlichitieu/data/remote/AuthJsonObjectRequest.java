package com.ptithcm.quanlichitieu.data.remote;

import android.util.Log;

import androidx.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.ptithcm.quanlichitieu.data.local.token.TokenStorage;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom Volley request that automatically attaches the JWT token from
 * {@link TokenStorage} to every outgoing request's Authorization header.
 *
 * Gracefully handles HTTP 401 Unauthorized by clearing the local token
 * and notifying the app via {@link SessionExpiredListener} to trigger logout.
 *
 * Open/Closed Principle: Extends JsonObjectRequest without modifying it;
 * new header requirements can be added by subclassing further.
 */
public class AuthJsonObjectRequest extends JsonObjectRequest {

    private static final String TAG = "AuthJsonObjectRequest";
    private static final int HTTP_UNAUTHORIZED = 401;

    private final TokenStorage tokenStorage;
    @Nullable
    private final SessionExpiredListener sessionExpiredListener;

    /**
     * Callback for session expiry events (401 responses).
     * Allows the app layer to react (e.g., navigate to LoginActivity).
     */
    public interface SessionExpiredListener {
        void onSessionExpired();
    }

    public AuthJsonObjectRequest(
            int method,
            String url,
            @Nullable JSONObject jsonRequest,
            Response.Listener<JSONObject> listener,
            @Nullable Response.ErrorListener errorListener,
            TokenStorage tokenStorage,
            @Nullable SessionExpiredListener sessionExpiredListener) {
        super(method, url, jsonRequest, listener, errorListener);
        this.tokenStorage = tokenStorage;
        this.sessionExpiredListener = sessionExpiredListener;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", ApiConfig.CONTENT_TYPE_JSON);

        String token = tokenStorage.getToken();
        if (token != null && !token.isEmpty()) {
            headers.put(ApiConfig.HEADER_AUTHORIZATION, ApiConfig.TOKEN_PREFIX + token);
            Log.d(TAG, "getHeaders: Authorization header attached");
        } else {
            Log.d(TAG, "getHeaders: No token available, skipping Authorization header");
        }

        return headers;
    }

    @Override
    protected VolleyError parseNetworkError(VolleyError volleyError) {
        NetworkResponse response = volleyError.networkResponse;
        if (response != null) {
            Log.e(TAG, "parseNetworkError: HTTP " + response.statusCode + " for " + getUrl());
            if (response.statusCode == HTTP_UNAUTHORIZED) {
                Log.w(TAG, "401 Unauthorized detected — clearing auth data and notifying listener");
                tokenStorage.clearAll();
                if (sessionExpiredListener != null) {
                    sessionExpiredListener.onSessionExpired();
                }
            }
        } else {
            Log.e(TAG, "parseNetworkError: No network response (timeout/connection error)");
        }
        return super.parseNetworkError(volleyError);
    }
}
