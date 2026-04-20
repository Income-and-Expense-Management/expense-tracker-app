package com.ptithcm.quanlichitieu.data.remote;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.ptithcm.quanlichitieu.data.local.token.TokenStorage;
import com.ptithcm.quanlichitieu.data.model.Category;
import com.ptithcm.quanlichitieu.data.model.TransactionType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class CategoryApiService {

    private static final String TAG = "CategoryApiService";

    private final VolleySingleton volleySingleton;
    private final TokenStorage tokenStorage;

    public CategoryApiService(@NonNull Context context, @NonNull TokenStorage tokenStorage) {
        this.volleySingleton = VolleySingleton.getInstance(context);
        this.tokenStorage = tokenStorage;
    }

    private String getAuthToken() {
        return tokenStorage.getToken();
    }

    public void createCategory(
            @NonNull Category category,
            @Nullable Response.Listener<JSONObject> onSuccess,
            @Nullable Response.ErrorListener onError
    ) {
        try {
            JSONObject body = buildCategoryBody(category, true);
            AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                    Request.Method.POST,
                    ApiConfig.CATEGORIES_URL,
                    body,
                    response -> {
                        Log.d(TAG, "createCategory: Success");
                        if (onSuccess != null) onSuccess.onResponse(response);
                    },
                    error -> {
                        Log.e(TAG, "createCategory: Error " + errorMessage(error));
                        if (onError != null) onError.onErrorResponse(error);
                    },
                    tokenStorage,
                    null
            );
            volleySingleton.addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "createCategory: Failed to build request body", e);
            if (onError != null) {
                onError.onErrorResponse(new VolleyError("Failed to build request body: " + e.getMessage()));
            }
        }
    }

    public void updateCategory(
            @NonNull Category category,
            @Nullable Response.Listener<JSONObject> onSuccess,
            @Nullable Response.ErrorListener onError
    ) {
        try {
            JSONObject body = buildCategoryBody(category, false);
            String url = ApiConfig.CATEGORY_URL + category.getId();

            AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                    Request.Method.PATCH,
                    url,
                    body,
                    response -> {
                        Log.d(TAG, "updateCategory: Success");
                        if (onSuccess != null) onSuccess.onResponse(response);
                    },
                    error -> {
                        Log.e(TAG, "updateCategory: Error " + errorMessage(error));
                        if (onError != null) onError.onErrorResponse(error);
                    },
                    tokenStorage,
                    null
            );
            volleySingleton.addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "updateCategory: Failed to build request body", e);
            if (onError != null) {
                onError.onErrorResponse(new VolleyError("Failed to build request body: " + e.getMessage()));
            }
        }
    }

    public void deleteCategory(
            @NonNull String categoryId,
            @Nullable Response.Listener<JSONObject> onSuccess,
            @Nullable Response.ErrorListener onError
    ) {
        String url = ApiConfig.CATEGORY_URL + categoryId;
        NoBodyRequest request = new NoBodyRequest(
                Request.Method.DELETE,
                url,
                response -> {
                    Log.d(TAG, "deleteCategory: Success (204)");
                    if (onSuccess != null) onSuccess.onResponse(response);
                },
                error -> {
                    Log.e(TAG, "deleteCategory: Error " + errorMessage(error));
                    if (onError != null) onError.onErrorResponse(error);
                },
                tokenStorage
        );
        volleySingleton.addToRequestQueue(request);
    }

    public void fetchCategories(
            @NonNull Context context,
            @NonNull final Response.Listener<List<Category>> successListener,
            @NonNull final Response.ErrorListener errorListener
    ) {
        String token = getAuthToken();
        if (token == null || token.isEmpty()) {
            errorListener.onErrorResponse(new VolleyError("Unauthorized: Token is missing"));
            return;
        }

        String url = ApiConfig.CATEGORIES_URL + "?include_inactive=true";
        Log.d(TAG, "Fetching categories: " + url);

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.has("success") && response.getBoolean("success")) {
                                JSONArray dataArray = response.getJSONArray("data");
                                List<Category> apiCategoryList = new ArrayList<>();

                                for (int i = 0; i < dataArray.length(); i++) {
                                    JSONObject categoryObj = dataArray.getJSONObject(i);

                                    Category category = new Category();
                                    category.setId(categoryObj.optString("id", null));

                                    if(categoryObj.has("user_id") && !categoryObj.isNull("user_id")) {
                                        category.setUserId(categoryObj.optString("user_id", null));
                                    }

                                    category.setName(categoryObj.optString("name", ""));
                                    String typeStr = categoryObj.optString("type", "").toUpperCase();
                                    category.setType(TransactionType.fromValue(typeStr));
                                    category.setIconName(categoryObj.optString("icon_name", ""));
                                    category.setActive(categoryObj.optBoolean("is_active", true));

                                    apiCategoryList.add(category);
                                }
                                successListener.onResponse(apiCategoryList);
                            } else {
                                String errorMsg = response.optString("message", "Unknown API Error");
                                errorListener.onErrorResponse(new VolleyError(errorMsg));
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON Parsing error", e);
                            errorListener.onErrorResponse(new VolleyError("JSON parsing error", e));
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "API Error: " + error.getMessage(), error);
                        errorListener.onErrorResponse(error);
                    }
                },
                tokenStorage,
                null
        );

        volleySingleton.addToRequestQueue(request);
    }

    private JSONObject buildCategoryBody(@NonNull Category category, boolean includeId) throws JSONException {
        JSONObject body = new JSONObject();
        if (includeId && category.getId() != null && !category.getId().isEmpty()) {
            body.put("id", category.getId());
        }
        body.put("name", category.getName());
        if (category.getType() != null) {
            body.put("type", category.getType().getValue());
        }
        if (category.getIconName() == null) {
            body.put("icon_name", JSONObject.NULL);
        } else {
            body.put("icon_name", category.getIconName());
        }
        body.put("is_active", category.isActive());
        return body;
    }

    private String errorMessage(@Nullable VolleyError error) {
        if (error == null) return "null";
        if (error.networkResponse != null) {
            try {
                String body = new String(error.networkResponse.data, "UTF-8");
                return "HTTP " + error.networkResponse.statusCode + " - " + body;
            } catch (UnsupportedEncodingException e) {
                return "HTTP " + error.networkResponse.statusCode;
            }
        }
        return error.getMessage() != null ? error.getMessage() : "Unknown error";
    }

    private static class NoBodyRequest extends AuthJsonObjectRequest {
        private static final int HTTP_NO_CONTENT = 204;

        NoBodyRequest(int method,
                      String url,
                      Response.Listener<JSONObject> listener,
                      Response.ErrorListener errorListener,
                      TokenStorage tokenStorage) {
            super(method, url, null, listener, errorListener, tokenStorage, null);
        }

        @Override
        protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
            if (response.statusCode == HTTP_NO_CONTENT) {
                return Response.success(new JSONObject(), HttpHeaderParser.parseCacheHeaders(response));
            }
            return super.parseNetworkResponse(response);
        }
    }
}
