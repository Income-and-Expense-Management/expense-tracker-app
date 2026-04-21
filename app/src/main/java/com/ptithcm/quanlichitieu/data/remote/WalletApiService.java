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
import com.ptithcm.quanlichitieu.data.local.dao.WalletDao;
import com.ptithcm.quanlichitieu.data.local.token.TokenStorage;
import com.ptithcm.quanlichitieu.data.model.Wallet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * WalletApiService — Chịu trách nhiệm gọi tất cả Wallet-related REST API endpoints.
 */
public class WalletApiService {

    private static final String TAG = "WalletApiService";

    private final VolleySingleton volleySingleton;
    private final TokenStorage tokenStorage;

    public WalletApiService(@NonNull Context context, @NonNull TokenStorage tokenStorage) {
        this.volleySingleton = VolleySingleton.getInstance(context);
        this.tokenStorage = tokenStorage;
    }

    public void createWallet(@NonNull Wallet wallet,
                             @Nullable Response.Listener<JSONObject> onSuccess,
                             @Nullable Response.ErrorListener onError) {
        try {
            JSONObject body = buildWalletBody(wallet, true);
            AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                    Request.Method.POST,
                    ApiConfig.WALLETS_URL,
                    body,
                    onSuccess,
                    onError,
                    tokenStorage,
                    null
            );
            volleySingleton.addToRequestQueue(request);
        } catch (JSONException e) {
            if (onError != null) onError.onErrorResponse(new VolleyError(e.getMessage()));
        }
    }

    public void updateWallet(@NonNull Wallet wallet,
                             @Nullable Response.Listener<JSONObject> onSuccess,
                             @Nullable Response.ErrorListener onError) {
        try {
            String url = ApiConfig.WALLET_URL + wallet.getId();
            JSONObject body = buildWalletBody(wallet, true);
            AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                    Request.Method.PATCH,
                    url,
                    body,
                    onSuccess,
                    onError,
                    tokenStorage,
                    null
            );
            volleySingleton.addToRequestQueue(request);
        } catch (JSONException e) {
            if (onError != null) onError.onErrorResponse(new VolleyError(e.getMessage()));
        }
    }

    public void deleteWallet(@NonNull String walletId,
                             @Nullable Response.Listener<JSONObject> onSuccess,
                             @Nullable Response.ErrorListener onError) {
        String url = ApiConfig.WALLET_URL + walletId;
        NoBodyRequest request = new NoBodyRequest(
                Request.Method.DELETE,
                url,
                onSuccess,
                onError,
                tokenStorage
        );
        volleySingleton.addToRequestQueue(request);
    }

    /**
     * Kéo danh sách ví từ server và UPSERT vào local DB.
     * SỬ DỤNG endpoint /api/v1/sync/pull thay vì /api/v1/wallets/ để lấy được cả các bản ghi đã xóa (soft-deleted).
     */
    public void fetchAndUpsertWallets(@NonNull WalletDao walletDao,
                                      @Nullable String userId,
                                      @Nullable Runnable onDone) {
        String url = ApiConfig.SYNC_PULL_URL + "?last_sync_time=0";
        Log.d(TAG, "fetchAndUpsertWallets: GET " + url);

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    new Thread(() -> {
                        try {
                            if (response.optBoolean("success", false)) {
                                JSONObject data = response.optJSONObject("data");
                                if (data != null) {
                                    JSONArray walletsArray = data.optJSONArray("wallets");
                                    if (walletsArray != null) {
                                        upsertWalletsFromArray(walletsArray, walletDao, userId);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "fetchAndUpsertWallets: Parse error", e);
                        } finally {
                            if (onDone != null) onDone.run();
                        }
                    }).start();
                },
                error -> {
                    Log.e(TAG, "fetchAndUpsertWallets: Error → " + error.getMessage());
                    if (onDone != null) onDone.run();
                },
                tokenStorage,
                null
        );
        volleySingleton.addToRequestQueue(request);
    }

    private void upsertWalletsFromArray(@NonNull JSONArray walletsArray,
                                        @NonNull WalletDao walletDao,
                                        @Nullable String userId) throws JSONException {
        int upsertCount = 0;
        for (int i = 0; i < walletsArray.length(); i++) {
            JSONObject item = walletsArray.getJSONObject(i);
            Wallet serverWallet = parseWalletFromJson(item, userId);
            if (serverWallet == null) continue;

            Wallet existing = walletDao.getWalletById(serverWallet.getId());
            if (existing == null) {
                if (serverWallet.getDeletedAt() == null) {
                    walletDao.insertFromServer(serverWallet);
                    Log.d(TAG, "upsertWallet: Inserted new wallet id=" + serverWallet.getId());
                }
            } else {
                boolean serverIsDeleted = serverWallet.getDeletedAt() != null;
                boolean serverIsNewer = serverWallet.getUpdatedAt() > existing.getUpdatedAt();

                if (serverIsDeleted || serverIsNewer) {
                    walletDao.updateFromServer(serverWallet);
                    Log.d(TAG, "upsertWallet: Updated/Deleted wallet id=" + serverWallet.getId() + " (Deleted=" + serverIsDeleted + ")");
                }
            }
            upsertCount++;
        }
        Log.d(TAG, "upsertWallets: Processed " + upsertCount + " wallets");
    }

    @Nullable
    private Wallet parseWalletFromJson(@NonNull JSONObject item, @Nullable String fallbackUserId) {
        try {
            String id = item.optString("id", null);
            if (id == null || id.isEmpty()) return null;

            String userId = item.optString("user_id", null);
            if (userId == null || userId.isEmpty()) userId = fallbackUserId;

            long initialBalance = parseLongFromObject(item.opt("initial_balance"));
            Long deletedAt = item.isNull("deleted_at") ? null : parseIso8601ToMillis(item.optString("deleted_at", null));
            if (deletedAt != null && deletedAt == 0L) deletedAt = null;

            return new Wallet.Builder()
                    .setId(id)
                    .setUserId(userId)
                    .setName(item.optString("name", ""))
                    .setInitialBalance(initialBalance)
                    .setCurrency(item.optString("currency", "VND"))
                    .setIconId(item.isNull("icon_id") ? null : item.optString("icon_id", null))
                    .setCreatedAt(parseIso8601ToMillis(item.optString("created_at", null)))
                    .setUpdatedAt(parseIso8601ToMillis(item.optString("updated_at", null)))
                    .setDeletedAt(deletedAt)
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    private long parseIso8601ToMillis(@Nullable String isoString) {
        if (isoString == null || isoString.isEmpty()) return 0L;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.parse(isoString).getTime();
        } catch (ParseException e) {
            try {
                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                sdf2.setTimeZone(TimeZone.getTimeZone("UTC"));
                return sdf2.parse(isoString).getTime();
            } catch (ParseException e2) {
                return 0L;
            }
        }
    }

    private long parseLongFromObject(@Nullable Object obj) {
        if (obj == null) return 0L;
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private JSONObject buildWalletBody(@NonNull Wallet wallet, boolean includeId) throws JSONException {
        JSONObject body = new JSONObject();
        if (includeId && wallet.getId() != null) body.put("id", wallet.getId());
        body.put("name", wallet.getName());
        body.put("initial_balance", String.valueOf(wallet.getInitialBalance()));
        body.put("currency", wallet.getCurrency() != null ? wallet.getCurrency() : "VND");
        body.put("icon_id", wallet.getIconId() != null ? wallet.getIconId() : JSONObject.NULL);
        return body;
    }

    private static class NoBodyRequest extends AuthJsonObjectRequest {
        NoBodyRequest(int method, String url, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener, TokenStorage tokenStorage) {
            super(method, url, null, listener, errorListener, tokenStorage, null);
        }
        @Override
        protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
            if (response.statusCode == 204) {
                return Response.success(new JSONObject(), HttpHeaderParser.parseCacheHeaders(response));
            }
            return super.parseNetworkResponse(response);
        }
    }
}
