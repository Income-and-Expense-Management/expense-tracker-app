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
import com.ptithcm.quanlichitieu.data.local.dao.BudgetDao;
import com.ptithcm.quanlichitieu.data.local.token.TokenStorage;
import com.ptithcm.quanlichitieu.data.model.Budget;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class BudgetApiService {

    private static final String TAG = "BudgetApiService";

    private final VolleySingleton volleySingleton;
    private final TokenStorage tokenStorage;

    public BudgetApiService(@NonNull Context context, @NonNull TokenStorage tokenStorage) {
        this.volleySingleton = VolleySingleton.getInstance(context);
        this.tokenStorage = tokenStorage;
    }

    public void createBudget(@NonNull Budget budget,
                             @Nullable Response.Listener<JSONObject> onSuccess,
                             @Nullable Response.ErrorListener onError) {
        try {
            JSONObject body = buildBudgetBody(budget, true);
            AuthJsonObjectRequest request = new AuthJsonObjectRequest(
                    Request.Method.POST,
                    ApiConfig.BUDGETS_URL,
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

    public void updateBudget(@NonNull Budget budget,
                             @Nullable Response.Listener<JSONObject> onSuccess,
                             @Nullable Response.ErrorListener onError) {
        try {
            String url = ApiConfig.BUDGET_URL + budget.getId();
            JSONObject body = buildBudgetBody(budget, true);
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

    public void deleteBudget(@NonNull String budgetId,
                             @Nullable Response.Listener<JSONObject> onSuccess,
                             @Nullable Response.ErrorListener onError) {
        String url = ApiConfig.BUDGET_URL + budgetId;
        NoBodyRequest request = new NoBodyRequest(
                Request.Method.DELETE,
                url,
                onSuccess,
                onError,
                tokenStorage
        );
        volleySingleton.addToRequestQueue(request);
    }

    public void fetchAndUpsertBudgets(@NonNull BudgetDao budgetDao,
                                      @Nullable Runnable onDone) {
        String url = ApiConfig.SYNC_PULL_URL + "?last_sync_time=0";
        Log.d(TAG, "fetchAndUpsertBudgets: GET " + url);

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
                                    JSONArray arrays = data.optJSONArray("budgets");
                                    if (arrays != null) {
                                        upsertBudgetsFromArray(arrays, budgetDao);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "fetchAndUpsertBudgets: Parse error", e);
                        } finally {
                            if (onDone != null) onDone.run();
                        }
                    }).start();
                },
                error -> {
                    Log.e(TAG, "fetchAndUpsertBudgets: Error → " + error.getMessage());
                    if (onDone != null) onDone.run();
                },
                tokenStorage,
                null
        );
        volleySingleton.addToRequestQueue(request);
    }

    private void upsertBudgetsFromArray(@NonNull JSONArray array,
                                        @NonNull BudgetDao budgetDao) throws JSONException {
        int upsertCount = 0;
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            Budget serverBudget = parseBudgetFromJson(item);
            if (serverBudget == null) continue;

            Budget existing = budgetDao.getById(serverBudget.getId());
            if (existing == null) {
                if (serverBudget.getDeletedAt() == null) {
                    budgetDao.insertFromServer(serverBudget);
                    Log.d(TAG, "upsertBudget: Inserted new budget id=" + serverBudget.getId());
                }
            } else {
                boolean serverIsDeleted = serverBudget.getDeletedAt() != null;
                boolean serverIsNewer = serverBudget.getUpdatedAt() > existing.getUpdatedAt();

                if (serverIsDeleted || serverIsNewer) {
                    budgetDao.updateFromServer(serverBudget);
                    Log.d(TAG, "upsertBudget: Updated/Deleted budget id=" + serverBudget.getId() + " (Deleted=" + serverIsDeleted + ")");
                }
            }
            upsertCount++;
        }
        Log.d(TAG, "upsertBudgets: Processed " + upsertCount + " budgets");
    }

    @Nullable
    private Budget parseBudgetFromJson(@NonNull JSONObject item) {
        try {
            String id = item.optString("id", null);
            if (id == null || id.isEmpty()) return null;

            String walletId = item.optString("wallet_id", null);
            if (walletId == null || walletId.isEmpty()) return null;

            String categoryId = item.optString("category_id", null);
            if (categoryId == null || categoryId.isEmpty()) return null;

            long targetAmount = parseLongFromObject(item.opt("target_amount"));
            long startDate = parseIso8601ToMillis(item.optString("start_date", null));
            long endDate = parseIso8601ToMillis(item.optString("end_date", null));
            
            Long deletedAt = item.isNull("deleted_at") ? null : parseIso8601ToMillis(item.optString("deleted_at", null));
            if (deletedAt != null && deletedAt == 0L) deletedAt = null;

            return new Budget.Builder()
                    .setId(id)
                    .setWalletId(walletId)
                    .setCategoryId(categoryId)
                    .setTargetAmount(targetAmount)
                    .setStartDate(startDate)
                    .setEndDate(endDate)
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

    private JSONObject buildBudgetBody(@NonNull Budget budget, boolean includeId) throws JSONException {
        JSONObject body = new JSONObject();
        if (includeId && budget.getId() != null) body.put("id", budget.getId());
        body.put("wallet_id", budget.getWalletId());
        body.put("category_id", budget.getCategoryId());
        body.put("target_amount", String.valueOf(budget.getTargetAmount()));
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        if (budget.getStartDate() > 0) {
            body.put("start_date", sdf.format(budget.getStartDate()));
        }
        if (budget.getEndDate() > 0) {
            body.put("end_date", sdf.format(budget.getEndDate()));
        }
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
