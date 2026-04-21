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
import com.ptithcm.quanlichitieu.data.local.dao.TransactionDao;
import com.ptithcm.quanlichitieu.data.local.token.TokenStorage;
import com.ptithcm.quanlichitieu.data.model.Transaction;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * TransactionApiService — Chịu trách nhiệm gọi tất cả Transaction-related REST API endpoints.
 */
public class TransactionApiService {

    private static final String TAG = "TransactionApiService";

    private final VolleySingleton volleySingleton;
    private final TokenStorage tokenStorage;

    public TransactionApiService(@NonNull Context context, @NonNull TokenStorage tokenStorage) {
        this.volleySingleton = VolleySingleton.getInstance(context);
        this.tokenStorage = tokenStorage;
    }

    public void createTransaction(@NonNull Transaction transaction,
                                  @Nullable Response.Listener<JSONObject> onSuccess,
                                  @Nullable Response.ErrorListener onError) {
        try {
            JSONObject body = buildTransactionBody(transaction, true);
            AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                    Request.Method.POST,
                    ApiConfig.TRANSACTIONS_URL,
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

    public void updateTransaction(@NonNull Transaction transaction,
                                  @Nullable Response.Listener<JSONObject> onSuccess,
                                  @Nullable Response.ErrorListener onError) {
        try {
            String url = ApiConfig.TRANSACTION_URL + transaction.getId();
            JSONObject body = buildTransactionBody(transaction, true);
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

    public void deleteTransaction(@NonNull String transactionId,
                                  @Nullable Response.Listener<JSONObject> onSuccess,
                                  @Nullable Response.ErrorListener onError) {
        String url = ApiConfig.TRANSACTION_URL + transactionId;
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
     * Kéo danh sách giao dịch từ server và UPSERT vào local DB.
     */
    public void fetchAndUpsertTransactions(@NonNull TransactionDao transactionDao,
                                           @Nullable Runnable onDone) {
        String url = ApiConfig.SYNC_PULL_URL + "?last_sync_time=0";
        Log.d(TAG, "fetchAndUpsertTransactions: GET " + url);

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
                                    JSONArray transactionsArray = data.optJSONArray("transactions");
                                    if (transactionsArray != null) {
                                        upsertTransactionsFromArray(transactionsArray, transactionDao);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "fetchAndUpsertTransactions: Parse error", e);
                        } finally {
                            if (onDone != null) onDone.run();
                        }
                    }).start();
                },
                error -> {
                    Log.e(TAG, "fetchAndUpsertTransactions: Error → " + error.getMessage());
                    if (onDone != null) onDone.run();
                },
                tokenStorage,
                null
        );
        volleySingleton.addToRequestQueue(request);
    }

    private void upsertTransactionsFromArray(@NonNull JSONArray transactionsArray,
                                             @NonNull TransactionDao transactionDao) throws JSONException {
        int upsertCount = 0;
        for (int i = 0; i < transactionsArray.length(); i++) {
            JSONObject item = transactionsArray.getJSONObject(i);
            Transaction serverTransaction = parseTransactionFromJson(item);
            if (serverTransaction == null) continue;

            Transaction existing = transactionDao.getById(serverTransaction.getId());
            if (existing == null) {
                if (serverTransaction.getDeletedAt() == null) {
                    transactionDao.insertFromServer(serverTransaction);
                    Log.d(TAG, "upsertTransaction: Inserted new transaction id=" + serverTransaction.getId());
                }
            } else {
                boolean serverIsDeleted = serverTransaction.getDeletedAt() != null;
                boolean serverIsNewer = serverTransaction.getUpdatedAt() > existing.getUpdatedAt();

                if (serverIsDeleted || serverIsNewer) {
                    transactionDao.updateFromServer(serverTransaction);
                    Log.d(TAG, "upsertTransaction: Updated/Deleted transaction id=" + serverTransaction.getId() + " (Deleted=" + serverIsDeleted + ")");
                }
            }
            upsertCount++;
        }
        Log.d(TAG, "upsertTransactions: Processed " + upsertCount + " transactions");
    }

    @Nullable
    private Transaction parseTransactionFromJson(@NonNull JSONObject item) {
        try {
            String id = item.optString("id", null);
            if (id == null || id.isEmpty()) return null;

            String walletId = item.optString("wallet_id", null);
            String categoryId = item.optString("category_id", null);
            
            long amount = parseLongFromObject(item.opt("amount"));
            long transactionDate = parseIso8601ToMillis(item.optString("transaction_date", null));
            if (transactionDate == 0L) transactionDate = System.currentTimeMillis();

            Long deletedAt = item.isNull("deleted_at") ? null : parseIso8601ToMillis(item.optString("deleted_at", null));
            if (deletedAt != null && deletedAt == 0L) deletedAt = null;

            return new Transaction.Builder()
                    .setId(id)
                    .setWalletId(walletId)
                    .setCategoryId(categoryId)
                    .setAmount(amount)
                    .setTransactionDate(transactionDate)
                    .setNote(item.isNull("note") ? null : item.optString("note", null))
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

    private JSONObject buildTransactionBody(@NonNull Transaction transaction, boolean includeId) throws JSONException {
        JSONObject body = new JSONObject();
        if (includeId && transaction.getId() != null) body.put("id", transaction.getId());
        body.put("wallet_id", transaction.getWalletId());
        body.put("category_id", transaction.getCategoryId());
        body.put("amount", String.valueOf(transaction.getAmount()));
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        body.put("transaction_date", sdf.format(transaction.getTransactionDate()));
        
        body.put("note", transaction.getNote() != null ? transaction.getNote() : JSONObject.NULL);
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
