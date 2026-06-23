package com.ptithcm.quanlichitieu.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.ptithcm.quanlichitieu.data.local.DatabaseManager;
import com.ptithcm.quanlichitieu.data.local.dao.BudgetDao;
import com.ptithcm.quanlichitieu.data.local.dao.CategoryDao;
import com.ptithcm.quanlichitieu.data.local.dao.TransactionDao;
import com.ptithcm.quanlichitieu.data.local.dao.WalletDao;
import com.ptithcm.quanlichitieu.data.local.token.EncryptedTokenStorage;
import com.ptithcm.quanlichitieu.data.local.token.TokenStorage;
import com.ptithcm.quanlichitieu.data.model.Budget;
import com.ptithcm.quanlichitieu.data.model.Category;
import com.ptithcm.quanlichitieu.data.model.Transaction;
import com.ptithcm.quanlichitieu.data.model.TransactionType;
import com.ptithcm.quanlichitieu.data.model.Wallet;
import com.ptithcm.quanlichitieu.data.remote.SyncApiService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * SyncRepository — Chịu trách nhiệm đồng bộ tập trung tất cả thực thể.
 * Tải 1 request duy nhất chứa cả 4 thực thể và lưu vào SQLite local.
 */
public class SyncRepository {

    private static final String TAG = "SyncRepository";
    private static volatile SyncRepository instance;

    private final Context context;
    private final WalletDao walletDao;
    private final CategoryDao categoryDao;
    private final TransactionDao transactionDao;
    private final BudgetDao budgetDao;
    private final SyncApiService apiService;

    private SyncRepository(@NonNull Context context) {
        this.context = context.getApplicationContext();
        DatabaseManager dbManager = DatabaseManager.getInstance(this.context);
        this.walletDao = dbManager.getWalletDao();
        this.categoryDao = dbManager.getCategoryDao();
        this.transactionDao = dbManager.getTransactionDao();
        this.budgetDao = dbManager.getBudgetDao();
        TokenStorage tokenStorage = EncryptedTokenStorage.getInstance(this.context);
        this.apiService = new SyncApiService(this.context, tokenStorage);
    }

    public static SyncRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (SyncRepository.class) {
                if (instance == null) {
                    instance = new SyncRepository(context);
                }
            }
        }
        return instance;
    }

    /**
     * Thực hiện đồng bộ tất cả thực thể thay đổi từ server.
     */
    public void syncAll(@Nullable String userId, @Nullable Runnable onDone) {
        String userKey = (userId != null && !userId.trim().isEmpty()) ? userId : "default";
        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String lastSyncTime = prefs.getString("last_sync_time_" + userKey, "0");

        Log.d(TAG, "syncAll: Starting sync for user=" + userKey + ", lastSyncTime=" + lastSyncTime);

        apiService.fetchSyncUpdates(
                lastSyncTime,
                response -> {
                    new Thread(() -> {
                        try {
                            if (response.optBoolean("success", false)) {
                                JSONObject data = response.optJSONObject("data");
                                if (data != null) {
                                    // 1. Đồng bộ Ví
                                    JSONArray walletsArray = data.optJSONArray("wallets");
                                    if (walletsArray != null) {
                                        upsertWallets(walletsArray, userId);
                                    }

                                    // 2. Đồng bộ Danh mục
                                    JSONArray categoriesArray = data.optJSONArray("categories");
                                    if (categoriesArray != null) {
                                        upsertCategories(categoriesArray, userId);
                                    }

                                    // 3. Đồng bộ Giao dịch
                                    JSONArray transactionsArray = data.optJSONArray("transactions");
                                    if (transactionsArray != null) {
                                        upsertTransactions(transactionsArray);
                                    }

                                    // 4. Đồng bộ Ngân sách
                                    JSONArray budgetsArray = data.optJSONArray("budgets");
                                    if (budgetsArray != null) {
                                        upsertBudgets(budgetsArray);
                                    }

                                    // Lưu server_sync_time để làm last_sync_time cho lần tiếp theo
                                    String serverSyncTime = data.optString("server_sync_time", null);
                                    if (serverSyncTime != null && !serverSyncTime.isEmpty()) {
                                        prefs.edit().putString("last_sync_time_" + userKey, serverSyncTime).apply();
                                        Log.d(TAG, "syncAll: Saved new last_sync_time=" + serverSyncTime);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "syncAll: Processing error", e);
                        } finally {
                            if (onDone != null) {
                                onDone.run();
                            }
                        }
                    }).start();
                },
                error -> {
                    Log.e(TAG, "syncAll: Volley error -> " + error.getMessage());
                    if (onDone != null) {
                        onDone.run();
                    }
                }
        );
    }

    private void upsertWallets(JSONArray array, String userId) throws JSONException {
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            Wallet serverWallet = parseWalletFromJson(item, userId);
            if (serverWallet == null) continue;

            Wallet existing = walletDao.getWalletById(serverWallet.getId());
            if (existing == null) {
                if (serverWallet.getDeletedAt() == null) {
                    walletDao.insertFromServer(serverWallet);
                }
            } else {
                boolean serverIsDeleted = serverWallet.getDeletedAt() != null;
                boolean serverIsNewer = serverWallet.getUpdatedAt() > existing.getUpdatedAt();
                if (serverIsDeleted || serverIsNewer) {
                    walletDao.updateFromServer(serverWallet);
                }
            }
        }
    }

    private void upsertCategories(JSONArray array, String userId) throws JSONException {
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            Category serverCategory = parseCategoryFromJson(item, userId);
            if (serverCategory == null) continue;

            Category existing = categoryDao.getById(serverCategory.getId());
            if (existing == null) {
                if (serverCategory.getDeletedAt() == null) {
                    categoryDao.insertFromServer(serverCategory);
                }
            } else {
                boolean serverIsDeleted = serverCategory.getDeletedAt() != null;
                boolean serverIsNewer = serverCategory.getUpdatedAt() > existing.getUpdatedAt();
                if (serverIsDeleted || serverIsNewer) {
                    categoryDao.updateFromServer(serverCategory);
                }
            }
        }
    }

    private void upsertTransactions(JSONArray array) throws JSONException {
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            Transaction serverTransaction = parseTransactionFromJson(item);
            if (serverTransaction == null) continue;

            Transaction existing = transactionDao.getById(serverTransaction.getId());
            if (existing == null) {
                if (serverTransaction.getDeletedAt() == null) {
                    transactionDao.insertFromServer(serverTransaction);
                }
            } else {
                boolean serverIsDeleted = serverTransaction.getDeletedAt() != null;
                boolean serverIsNewer = serverTransaction.getUpdatedAt() > existing.getUpdatedAt();
                if (serverIsDeleted || serverIsNewer) {
                    transactionDao.updateFromServer(serverTransaction);
                }
            }
        }
    }

    private void upsertBudgets(JSONArray array) throws JSONException {
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            Budget serverBudget = parseBudgetFromJson(item);
            if (serverBudget == null) continue;

            Budget existing = budgetDao.getById(serverBudget.getId());
            if (existing == null) {
                if (serverBudget.getDeletedAt() == null) {
                    budgetDao.insertFromServer(serverBudget);
                }
            } else {
                boolean serverIsDeleted = serverBudget.getDeletedAt() != null;
                boolean serverIsNewer = serverBudget.getUpdatedAt() > existing.getUpdatedAt();
                if (serverIsDeleted || serverIsNewer) {
                    budgetDao.updateFromServer(serverBudget);
                }
            }
        }
    }

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

    private Category parseCategoryFromJson(@NonNull JSONObject categoryObj, @Nullable String fallbackUserId) {
        try {
            String id = categoryObj.optString("id", null);
            if (id == null || id.isEmpty()) {
                return null;
            }

            Category category = new Category();
            category.setId(id);

            String userId = categoryObj.isNull("user_id") ? null : categoryObj.optString("user_id", null);
            category.setUserId(userId);

            category.setName(categoryObj.optString("name", ""));
            String typeStr = categoryObj.optString("type", "").toUpperCase();
            category.setType(TransactionType.fromValue(typeStr));
            category.setIconName(categoryObj.isNull("icon_name") ? null : categoryObj.optString("icon_name", null));
            category.setActive(categoryObj.optBoolean("is_active", true));
            category.setCreatedAt(parseIso8601ToMillis(categoryObj.optString("created_at", null)));
            category.setUpdatedAt(parseIso8601ToMillis(categoryObj.optString("updated_at", null)));
            Long deletedAt = categoryObj.isNull("deleted_at") ? null : parseIso8601ToMillis(categoryObj.optString("deleted_at", null));
            if (deletedAt != null && deletedAt == 0L) {
                deletedAt = null;
            }
            category.setDeletedAt(deletedAt);
            return category;
        } catch (Exception e) {
            return null;
        }
    }

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
}
