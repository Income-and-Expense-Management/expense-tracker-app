package com.ptithcm.quanlichitieu.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ptithcm.quanlichitieu.data.local.dao.WalletDao;
import com.ptithcm.quanlichitieu.data.local.token.TokenStorage;
import com.ptithcm.quanlichitieu.data.model.Wallet;
import com.ptithcm.quanlichitieu.data.remote.WalletApiService;

import java.util.ArrayList;
import java.util.List;

/**
 * WalletRepositoryImpl — Concrete implementation của WalletRepository.
 *
 * Chiến lược: LOCAL-FIRST + REAL-TIME PUSH
 *
 * Mọi thao tác write (tạo / sửa / xóa) thực hiện theo 2 bước:
 *   1. Lưu vào SQLite local NGAY LẬP TỨC (đồng bộ, trên background thread).
 *   2. Đẩy lên server BẤT ĐỒNG BỘ qua WalletApiService (Volley, fire-and-forget).
 *
 * Nếu server lỗi hoặc offline → app vẫn hoạt động bình thường với dữ liệu local.
 * Lỗi sync được thông báo về ViewModel qua SyncCallback, không block UI.
 */
public class WalletRepositoryImpl implements WalletRepository {

    private static final String TAG = "WalletRepositoryImpl";

    private final WalletDao walletDao;
    private final WalletApiService apiService;
    @Nullable
    private final String userId; // userId hiện tại — dùng khi UPSERT từ server

    /**
     * Constructor chính — dùng trong ViewModel.
     *
     * @param context     Application context (cho Volley)
     * @param walletDao   WalletDao từ DatabaseManager
     * @param tokenStorage TokenStorage để đính JWT vào request
     */
    public WalletRepositoryImpl(@NonNull Context context,
                                @NonNull WalletDao walletDao,
                                @NonNull TokenStorage tokenStorage) {
        this(context, walletDao, tokenStorage, null);
    }

    /**
     * Constructor đầy đủ — truyền userId để lọc khi UPSERT từ server.
     */
    public WalletRepositoryImpl(@NonNull Context context,
                                @NonNull WalletDao walletDao,
                                @NonNull TokenStorage tokenStorage,
                                @Nullable String userId) {
        this.walletDao = walletDao;
        this.apiService = new WalletApiService(context, tokenStorage);
        this.userId = userId;
    }

    // ==================== LOCAL OPERATIONS ====================

    @Override
    @Nullable
    public String insertLocal(@NonNull Wallet wallet) {
        String id = walletDao.insert(wallet);
        if (id != null) {
            Log.d(TAG, "insertLocal: Wallet saved locally with id=" + id);
        } else {
            Log.e(TAG, "insertLocal: Failed to save wallet locally");
        }
        return id;
    }

    @Override
    public int updateLocal(@NonNull Wallet wallet) {
        int rows = walletDao.update(wallet);
        Log.d(TAG, "updateLocal: Updated " + rows + " row(s) for wallet id=" + wallet.getId());
        return rows;
    }

    @Override
    public int deleteLocal(@NonNull String walletId) {
        int rows = walletDao.delete(walletId);
        Log.d(TAG, "deleteLocal: Soft-deleted " + rows + " row(s) for wallet id=" + walletId);
        return rows;
    }

    @Override
    @NonNull
    public List<Wallet> getByUserId(@Nullable String userId) {
        List<Wallet> wallets = walletDao.getByUserId(userId);
        return wallets != null ? wallets : new ArrayList<>();
    }

    // ==================== REMOTE OPERATIONS (fire-and-forget) ====================

    @Override
    public void pushCreate(@NonNull Wallet wallet, @Nullable SyncCallback callback) {
        Log.d(TAG, "pushCreate: Pushing wallet id=" + wallet.getId() + " to server");
        apiService.createWallet(
                wallet,
                response -> {
                    Log.d(TAG, "pushCreate: Server confirmed wallet creation");
                    if (callback != null) callback.onSuccess();
                },
                error -> {
                    String msg = error != null && error.getMessage() != null
                            ? error.getMessage() : "Network error";
                    Log.e(TAG, "pushCreate: Server error — " + msg);
                    if (callback != null) callback.onError(msg);
                }
        );
    }

    @Override
    public void pushUpdate(@NonNull Wallet wallet, @Nullable SyncCallback callback) {
        Log.d(TAG, "pushUpdate: Pushing update for wallet id=" + wallet.getId());
        apiService.updateWallet(
                wallet,
                response -> {
                    Log.d(TAG, "pushUpdate: Server confirmed wallet update");
                    if (callback != null) callback.onSuccess();
                },
                error -> {
                    String msg = error != null && error.getMessage() != null
                            ? error.getMessage() : "Network error";
                    Log.e(TAG, "pushUpdate: Server error — " + msg);
                    if (callback != null) callback.onError(msg);
                }
        );
    }

    @Override
    public void pushDelete(@NonNull String walletId, @Nullable SyncCallback callback) {
        Log.d(TAG, "pushDelete: Pushing delete for wallet id=" + walletId);
        apiService.deleteWallet(
                walletId,
                response -> {
                    Log.d(TAG, "pushDelete: Server confirmed wallet deletion (204)");
                    if (callback != null) callback.onSuccess();
                },
                error -> {
                    String msg = error != null && error.getMessage() != null
                            ? error.getMessage() : "Network error";
                    Log.e(TAG, "pushDelete: Server error — " + msg);
                    if (callback != null) callback.onError(msg);
                }
        );
    }

    @Override
    public void fetchFromServer(@Nullable Runnable onDone) {
        Log.d(TAG, "fetchFromServer: Pulling wallets from server for userId=" + userId);
        // WalletApiService.fetchAndUpsertWallets() phải chạy trên main thread vì Volley
        // yêu cầu được gọi từ main thread (RequestQueue được tạo từ main thread).
        // Phần UPSERT SQLite bên trong apiService sẽ tự chạy trên background thread.
        apiService.fetchAndUpsertWallets(walletDao, userId, onDone);
    }
}
