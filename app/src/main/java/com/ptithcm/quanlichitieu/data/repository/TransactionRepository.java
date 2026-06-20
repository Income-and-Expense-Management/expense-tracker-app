package com.ptithcm.quanlichitieu.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ptithcm.quanlichitieu.data.model.TransactionGroup;

import java.util.List;

/**
 * TransactionRepository - Interface định nghĩa các thao tác với dữ liệu giao dịch.
 */
public interface TransactionRepository {

    List<TransactionGroup> getTransactionsByMonth(int monthOffset);

    List<TransactionGroup> getTransactionsByWalletAndMonth(@Nullable String walletId, int monthOffset);

    long getTotalExpense();

    long getTotalExpense(@Nullable String walletId, int monthOffset);

    long getTotalIncome();

    long getTotalIncome(@Nullable String walletId, int monthOffset);

    long getTotalBalance();

    long getCurrentBalance(@NonNull String walletId, long initialBalance);

    List<TransactionGroup> searchTransactions(@NonNull String keyword, @Nullable String walletId);

    @Nullable
    com.ptithcm.quanlichitieu.data.model.Transaction getById(@NonNull String transactionId);

    // ==================== LOCAL OPERATIONS ====================

    @Nullable
    String insertLocal(@NonNull com.ptithcm.quanlichitieu.data.model.Transaction transaction);

    int updateLocal(@NonNull com.ptithcm.quanlichitieu.data.model.Transaction transaction);

    int deleteLocal(@NonNull String transactionId);

    int countByCategoryId(@NonNull String categoryId);

    int countByCategoryId(@Nullable String userId, @NonNull String categoryId);

    int deleteByCategoryId(@NonNull String categoryId);

    int deleteByCategoryId(@Nullable String userId, @NonNull String categoryId);

    // ==================== REMOTE OPERATIONS (fire-and-forget) ====================

    void pushCreate(@NonNull com.ptithcm.quanlichitieu.data.model.Transaction transaction, @Nullable SyncCallback callback);

    void pushUpdate(@NonNull com.ptithcm.quanlichitieu.data.model.Transaction transaction, @Nullable SyncCallback callback);

    void pushDelete(@NonNull String transactionId, @Nullable SyncCallback callback);

    void fetchFromServer(@Nullable Runnable onDone);

    interface SyncCallback {
        void onSuccess();
        void onError(@NonNull String message);
    }
}
