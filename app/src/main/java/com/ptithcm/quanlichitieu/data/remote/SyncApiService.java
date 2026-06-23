package com.ptithcm.quanlichitieu.data.remote;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.Response;
import com.ptithcm.quanlichitieu.data.local.token.TokenStorage;

import org.json.JSONObject;

/**
 * SyncApiService — Chịu trách nhiệm gọi endpoint /api/v1/sync/pull để đồng bộ tất cả thực thể.
 */
public class SyncApiService {

    private static final String TAG = "SyncApiService";

    private final VolleySingleton volleySingleton;
    private final TokenStorage tokenStorage;

    public SyncApiService(@NonNull Context context, @NonNull TokenStorage tokenStorage) {
        this.volleySingleton = VolleySingleton.getInstance(context);
        this.tokenStorage = tokenStorage;
    }

    /**
     * Kéo toàn bộ bản ghi có thay đổi từ server kể từ thời điểm `lastSyncTime`.
     */
    public void fetchSyncUpdates(
            @NonNull String lastSyncTime,
            @NonNull Response.Listener<JSONObject> onSuccess,
            @NonNull Response.ErrorListener onError
    ) {
        String url = ApiConfig.SYNC_PULL_URL + "?last_sync_time=" + lastSyncTime;
        Log.d(TAG, "fetchSyncUpdates: GET " + url);

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                onSuccess,
                onError,
                tokenStorage,
                null
        );
        volleySingleton.addToRequestQueue(request);
    }
}
