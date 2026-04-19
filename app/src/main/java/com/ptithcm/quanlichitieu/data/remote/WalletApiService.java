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
 *
 * Tuân thủ Single Responsibility Principle: chỉ xử lý network calls cho Wallet.
 *
 * Endpoints:
 * - POST   /api/v1/wallets/           → tạo ví mới (gửi kèm id để server dùng đúng UUID)
 * - PATCH  /api/v1/wallets/:walletId  → cập nhật ví
 * - DELETE /api/v1/wallets/:walletId  → xóa ví (server trả 204 No Content)
 * - GET    /api/v1/wallets/           → lấy danh sách ví từ server, UPSERT vào local DB
 *
 * Lưu ý quan trọng:
 * - initial_balance PHẢI gửi dạng String vì server dùng BigInt.
 * - DELETE trả về 204 No Content, không có body — cần custom request.
 * - CREATE gửi kèm 'id' (UUID) để tránh server tự sinh ID khác với local.
 */
public class WalletApiService {

    private static final String TAG = "WalletApiService";

    private final VolleySingleton volleySingleton;
    private final TokenStorage tokenStorage;

    public WalletApiService(@NonNull Context context, @NonNull TokenStorage tokenStorage) {
        this.volleySingleton = VolleySingleton.getInstance(context);
        this.tokenStorage = tokenStorage;
    }

    // ==================== PUBLIC API METHODS ====================

    /**
     * Tạo ví mới trên server.
     * POST /api/v1/wallets/
     *
     * QUAN TRỌNG: Gửi kèm 'id' (UUID do client tạo) để server dùng chính ID này.
     * Nếu không gửi id, server tự sinh UUID khác → PATCH sau đó sẽ lỗi 404.
     *
     * Response: { "success": true, "data": { wallet_object } }
     */
    public void createWallet(@NonNull Wallet wallet,
                             @Nullable Response.Listener<JSONObject> onSuccess,
                             @Nullable Response.ErrorListener onError) {
        try {
            JSONObject body = buildWalletBody(wallet, true); // true = include id
            Log.d(TAG, "createWallet: POST to " + ApiConfig.WALLETS_URL + " body=" + body);

            AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                    Request.Method.POST,
                    ApiConfig.WALLETS_URL,
                    body,
                    response -> {
                        Log.d(TAG, "createWallet: Success → " + response);
                        if (onSuccess != null) onSuccess.onResponse(response);
                    },
                    error -> {
                        Log.e(TAG, "createWallet: Error → " + errorMessage(error));
                        if (onError != null) onError.onErrorResponse(error);
                    },
                    tokenStorage,
                    null
            );
            volleySingleton.addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "createWallet: Failed to build request body", e);
            if (onError != null) {
                onError.onErrorResponse(new VolleyError("Failed to build request body: " + e.getMessage()));
            }
        }
    }

    /**
     * Cập nhật ví trên server.
     * PATCH /api/v1/wallets/:walletId
     */
    public void updateWallet(@NonNull Wallet wallet,
                             @Nullable Response.Listener<JSONObject> onSuccess,
                             @Nullable Response.ErrorListener onError) {
        try {
            String url = ApiConfig.WALLET_URL + wallet.getId();
            JSONObject body = buildWalletBody(wallet, true);
            Log.d(TAG, "updateWallet: PATCH to " + url + " body=" + body);

            AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                    Request.Method.PATCH,
                    url,
                    body,
                    response -> {
                        Log.d(TAG, "updateWallet: Success → " + response);
                        if (onSuccess != null) onSuccess.onResponse(response);
                    },
                    error -> {
                        Log.e(TAG, "updateWallet: Error → " + errorMessage(error));
                        if (onError != null) onError.onErrorResponse(error);
                    },
                    tokenStorage,
                    null
            );
            volleySingleton.addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "updateWallet: Failed to build request body", e);
            if (onError != null) {
                onError.onErrorResponse(new VolleyError("Failed to build request body: " + e.getMessage()));
            }
        }
    }

    /**
     * Xóa ví trên server (server thực hiện soft-delete).
     * DELETE /api/v1/wallets/:walletId
     *
     * Server trả về 204 No Content (không có body).
     * Dùng NoBodyRequest để xử lý đúng 204.
     */
    public void deleteWallet(@NonNull String walletId,
                             @Nullable Response.Listener<JSONObject> onSuccess,
                             @Nullable Response.ErrorListener onError) {
        String url = ApiConfig.WALLET_URL + walletId;
        Log.d(TAG, "deleteWallet: DELETE to " + url);

        NoBodyRequest request = new NoBodyRequest(
                Request.Method.DELETE,
                url,
                response -> {
                    Log.d(TAG, "deleteWallet: Success (204)");
                    if (onSuccess != null) onSuccess.onResponse(response);
                },
                error -> {
                    Log.e(TAG, "deleteWallet: Error → " + errorMessage(error));
                    if (onError != null) onError.onErrorResponse(error);
                },
                tokenStorage
        );
        volleySingleton.addToRequestQueue(request);
    }

    // ==================== PRIVATE HELPERS ====================

    /**
     * Lấy danh sách ví từ server và UPSERT vào local DB.
     * GET /api/v1/wallets/
     *
     * Giải quyết vấn đề: Ví tạo từ web không xuất hiện trên app vì app chỉ
     * đọc SQLite local. Method này kéo dữ liệu mới nhất từ server về.
     *
     * Logic UPSERT:
     * - Ví đã có id trong local → update (giữ nguyên)
     * - Ví chưa có id trong local → insert mới
     * - Ví có deleted_at != null trên server → soft-delete local
     *
     * @param walletDao WalletDao để thực hiện UPSERT (chạy trên BG thread)
     * @param userId    userId hiện tại để lọc kết quả
     * @param onDone    Callback khi hoàn thành (có thể null)
     */
    public void fetchAndUpsertWallets(@NonNull WalletDao walletDao,
                                      @Nullable String userId,
                                      @Nullable Runnable onDone) {
        Log.d(TAG, "fetchAndUpsertWallets: GET " + ApiConfig.WALLETS_URL);

        AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                Request.Method.GET,
                ApiConfig.WALLETS_URL,
                null,
                response -> {
                    Log.d(TAG, "fetchAndUpsertWallets: Response received");
                    // Xử lý UPSERT trên background thread (tránh block Volley main thread)
                    new Thread(() -> {
                        try {
                            upsertWalletsFromResponse(response, walletDao, userId);
                        } catch (JSONException e) {
                            Log.e(TAG, "fetchAndUpsertWallets: Parse error", e);
                        } finally {
                            if (onDone != null) onDone.run();
                        }
                    }).start();
                },
                error -> {
                    Log.e(TAG, "fetchAndUpsertWallets: Error → " + errorMessage(error));
                    // Lỗi kéo dữ liệu không nghiêm trọng — vẫn hiển thị local data
                    if (onDone != null) onDone.run();
                },
                tokenStorage,
                null
        );
        volleySingleton.addToRequestQueue(request);
    }

    /**
     * Parse response từ GET /api/v1/wallets/ và thực hiện UPSERT vào local DB.
     *
     * Response format: { "success": true, "data": [ wallet_array ] }
     */
    private void upsertWalletsFromResponse(@NonNull JSONObject response,
                                           @NonNull WalletDao walletDao,
                                           @Nullable String userId) throws JSONException {
        if (!response.optBoolean("success", false)) {
            Log.w(TAG, "upsertWalletsFromResponse: Server returned success=false");
            return;
        }

        Object dataObj = response.opt("data");
        JSONArray dataArray;

        if (dataObj instanceof JSONArray) {
            dataArray = (JSONArray) dataObj;
        } else {
            Log.w(TAG, "upsertWalletsFromResponse: 'data' is not an array or is null");
            return;
        }

        int upsertCount = 0;
        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject item = dataArray.getJSONObject(i);
            Wallet serverWallet = parseWalletFromJson(item, userId);
            if (serverWallet == null) continue;

            // UPSERT: kiểm tra ví đã tồn tại local chưa
            Wallet existing = walletDao.getWalletById(serverWallet.getId());
            if (existing == null) {
                // Ví mới từ server (vd: tạo từ web) → insert vào local
                walletDao.insertFromServer(serverWallet);
                Log.d(TAG, "upsertWallet: Inserted new wallet id=" + serverWallet.getId());
            } else {
                // Ví đã có local → cập nhật nếu server mới hơn
                if (serverWallet.getUpdatedAt() > existing.getUpdatedAt()) {
                    walletDao.updateFromServer(serverWallet);
                    Log.d(TAG, "upsertWallet: Updated wallet id=" + serverWallet.getId());
                }
            }
            upsertCount++;
        }
        Log.d(TAG, "upsertWalletsFromResponse: Processed " + upsertCount + " wallets from server");
    }

    /**
     * Parse một JSONObject từ server thành Wallet model.
     * Timestamp server trả về dạng ISO8601 string → convert sang long (ms).
     */
    @Nullable
    private Wallet parseWalletFromJson(@NonNull JSONObject item, @Nullable String fallbackUserId) {
        try {
            String id = item.optString("id", null);
            if (id == null || id.isEmpty()) return null;

            String userId = item.optString("user_id", null);
            if (userId == null || userId.isEmpty()) userId = fallbackUserId;

            String name = item.optString("name", "");
            // initial_balance từ server có thể là string (BigInt) hoặc number
            long initialBalance = parseLongFromObject(item.opt("initial_balance"));
            String currency = item.optString("currency", "VND");
            String iconId = item.isNull("icon_id") ? null : item.optString("icon_id", null);

            long createdAt = parseIso8601ToMillis(item.optString("created_at", null));
            long updatedAt = parseIso8601ToMillis(item.optString("updated_at", null));
            Long deletedAt = item.isNull("deleted_at") ? null
                    : parseIso8601ToMillis(item.optString("deleted_at", null));
            // deletedAt = 0 có nghĩa là parseIso8601ToMillis thất bại → treat as null
            if (deletedAt != null && deletedAt == 0L) deletedAt = null;

            return new Wallet.Builder()
                    .setId(id)
                    .setUserId(userId)
                    .setName(name)
                    .setInitialBalance(initialBalance)
                    .setCurrency(currency)
                    .setIconId(iconId)
                    .setCreatedAt(createdAt)
                    .setUpdatedAt(updatedAt)
                    .setDeletedAt(deletedAt)
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "parseWalletFromJson: Error parsing wallet", e);
            return null;
        }
    }

    /** Parse ISO8601 datetime string → milliseconds since epoch. Returns 0 on failure. */
    private long parseIso8601ToMillis(@Nullable String isoString) {
        if (isoString == null || isoString.isEmpty()) return System.currentTimeMillis();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.parse(isoString).getTime();
        } catch (ParseException e) {
            try {
                // Fallback: without milliseconds
                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                sdf2.setTimeZone(TimeZone.getTimeZone("UTC"));
                return sdf2.parse(isoString).getTime();
            } catch (ParseException e2) {
                Log.w(TAG, "parseIso8601ToMillis: Cannot parse '" + isoString + "'");
                return 0L;
            }
        }
    }

    /** Parse BigInt-compatible value (String hoặc Number) thành long. */
    private long parseLongFromObject(@Nullable Object obj) {
        if (obj == null) return 0L;
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    // ==================== PRIVATE HELPERS ====================

    /**
     * Xây dựng JSON body gửi lên server cho tạo/sửa ví.
     *
     * @param wallet    Wallet object
     * @param includeId true = gửi kèm 'id' (dùng khi CREATE)
     */
    private JSONObject buildWalletBody(@NonNull Wallet wallet, boolean includeId) throws JSONException {
        JSONObject body = new JSONObject();
        // Gửi id khi tạo mới để server dùng đúng UUID của client
        if (includeId && wallet.getId() != null) {
            body.put("id", wallet.getId());
        }
        body.put("name", wallet.getName());
        // initial_balance phải là String theo ràng buộc BigInt của server
        body.put("initial_balance", String.valueOf(wallet.getInitialBalance()));
        body.put("currency", wallet.getCurrency() != null ? wallet.getCurrency() : "VND");
        body.put("icon_id", wallet.getIconId() != null ? wallet.getIconId() : JSONObject.NULL);
        return body;
    }

    /**
     * Trích xuất message từ VolleyError để logging.
     */
    private String errorMessage(VolleyError error) {
        if (error == null) return "null";
        if (error.networkResponse != null) {
            try {
                String body = new String(error.networkResponse.data, "UTF-8");
                return "HTTP " + error.networkResponse.statusCode + " — " + body;
            } catch (UnsupportedEncodingException e) {
                return "HTTP " + error.networkResponse.statusCode;
            }
        }
        return error.getMessage() != null ? error.getMessage() : "Unknown error";
    }

    // ==================== INNER CLASS: NoBodyRequest ====================

    /**
     * Custom Volley request chấp nhận response không có body (204 No Content).
     *
     * Volley mặc định báo lỗi khi response body rỗng vì ParseError.
     * Class này override parseNetworkResponse() để xử lý 204 là thành công.
     */
    private static class NoBodyRequest extends AuthJsonObjectRequest {

        private static final int HTTP_NO_CONTENT = 204;

        NoBodyRequest(int method, String url,
                      Response.Listener<JSONObject> listener,
                      Response.ErrorListener errorListener,
                      TokenStorage tokenStorage) {
            super(method, url, null, listener, errorListener, tokenStorage, null);
        }

        @Override
        protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
            // 204 No Content — không có body, nhưng vẫn là thành công
            if (response.statusCode == HTTP_NO_CONTENT) {
                Log.d("NoBodyRequest", "Received 204 No Content — treating as success");
                return Response.success(new JSONObject(), HttpHeaderParser.parseCacheHeaders(response));
            }
            return super.parseNetworkResponse(response);
        }
    }
}
