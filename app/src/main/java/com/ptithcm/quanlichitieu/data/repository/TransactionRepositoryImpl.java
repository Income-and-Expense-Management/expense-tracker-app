package com.ptithcm.quanlichitieu.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ptithcm.quanlichitieu.data.local.DatabaseManager;
import com.ptithcm.quanlichitieu.data.local.dao.TransactionDao;
import com.ptithcm.quanlichitieu.data.model.Transaction;
import com.ptithcm.quanlichitieu.data.model.TransactionGroup;
import com.ptithcm.quanlichitieu.data.model.TransactionType;
import com.ptithcm.quanlichitieu.utils.DateUtils;
import com.ptithcm.quanlichitieu.data.local.token.TokenStorage;
import com.ptithcm.quanlichitieu.data.remote.TransactionApiService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TransactionRepositoryImpl - Implementation thực của TransactionRepository.
 */
public class TransactionRepositoryImpl implements TransactionRepository {

    private static final String TAG = "TransactionRepositoryImpl";

    private final TransactionDao transactionDao;
    private final TransactionApiService apiService;

    public TransactionRepositoryImpl(@NonNull Context context, @NonNull TokenStorage tokenStorage) {
        DatabaseManager dbManager = DatabaseManager.getInstance(context);
        this.transactionDao = dbManager.getTransactionDao();
        this.apiService = new TransactionApiService(context, tokenStorage);
    }

    public TransactionRepositoryImpl(@NonNull TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
        this.apiService = null;
    }

    @Override
    public List<TransactionGroup> getTransactionsByMonth(int monthOffset) {
        return getTransactionsByWalletAndMonth(null, monthOffset);
    }

    @Override
    public List<TransactionGroup> getTransactionsByWalletAndMonth(@Nullable String walletId, int monthOffset) {
        long startDate = DateUtils.getMonthStartTimestamp(monthOffset);
        long endDate = DateUtils.getMonthEndTimestamp(monthOffset);
        List<Transaction> transactions = transactionDao.getByDateRangeWithDetails(walletId, startDate, endDate);
        return groupTransactionsByDate(transactions);
    }

    @Override
    public long getTotalExpense() {
        return 0L;
    }

    @Override
    public long getTotalExpense(@Nullable String walletId, int monthOffset) {
        if (walletId == null) {
            return 0L;
        }
        long startDate = DateUtils.getMonthStartTimestamp(monthOffset);
        long endDate = DateUtils.getMonthEndTimestamp(monthOffset);
        return transactionDao.getTotalAmountByType(walletId, TransactionType.EXPENSE, startDate, endDate);
    }

    @Override
    public long getTotalIncome() {
        return 0L;
    }

    @Override
    public long getTotalIncome(@Nullable String walletId, int monthOffset) {
        if (walletId == null) {
            return 0L;
        }
        long startDate = DateUtils.getMonthStartTimestamp(monthOffset);
        long endDate = DateUtils.getMonthEndTimestamp(monthOffset);
        return transactionDao.getTotalAmountByType(walletId, TransactionType.INCOME, startDate, endDate);
    }

    @Override
    public long getTotalBalance() {
        return 0L;
    }

    @Override
    public long getCurrentBalance(@NonNull String walletId, long initialBalance) {
        long startDate = 0L;
        long endDate = Long.MAX_VALUE;
        long allIncome = transactionDao.getTotalAmountByType(walletId, TransactionType.INCOME, startDate, endDate);
        long allExpense = transactionDao.getTotalAmountByType(walletId, TransactionType.EXPENSE, startDate, endDate);
        return initialBalance + allIncome - allExpense;
    }

    @Override
    public List<TransactionGroup> searchTransactions(@NonNull String keyword, @Nullable String walletId) {
        if (keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<Transaction> results = transactionDao.searchWithKeyword(keyword.trim(), walletId);
        return groupTransactionsByDate(results);
    }

    private List<TransactionGroup> groupTransactionsByDate(List<Transaction> transactions) {
        Map<String, List<Transaction>> groupedMap = new LinkedHashMap<>();
        for (Transaction transaction : transactions) {
            String dateKey = DateUtils.getDateKey(transaction.getTransactionDate());
            if (!groupedMap.containsKey(dateKey)) {
                groupedMap.put(dateKey, new ArrayList<>());
            }
            groupedMap.get(dateKey).add(transaction);
        }
        List<TransactionGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<Transaction>> entry : groupedMap.entrySet()) {
            List<Transaction> dayTransactions = entry.getValue();
            if (dayTransactions.isEmpty()) continue;
            long timestamp = dayTransactions.get(0).getTransactionDate();
            String dayOfWeek = DateUtils.formatDayOfWeek(timestamp);
            String date = DateUtils.formatDate(timestamp);
            long dayTotal = calculateDayTotal(dayTransactions);
            groups.add(new TransactionGroup(dayOfWeek, date, dayTotal, dayTransactions));
        }
        return groups;
    }

    private long calculateDayTotal(List<Transaction> transactions) {
        long total = 0L;
        for (Transaction transaction : transactions) {
            if (transaction.getType() == TransactionType.INCOME) {
                total += transaction.getAmount();
            } else if (transaction.getType() == TransactionType.EXPENSE) {
                total -= transaction.getAmount();
            }
        }
        return total;
    }

    @Override
    @Nullable
    public Transaction getById(@NonNull String transactionId) {
        return transactionDao.getById(transactionId);
    }

    @Override
    @Nullable
    public String insertLocal(@NonNull Transaction transaction) {
        return transactionDao.insert(transaction);
    }

    @Override
    public int updateLocal(@NonNull Transaction transaction) {
        return transactionDao.update(transaction);
    }

    @Override
    public int deleteLocal(@NonNull String transactionId) {
        return transactionDao.delete(transactionId);
    }

    @Override
    public int countByCategoryId(@NonNull String categoryId) {
        return transactionDao.countByCategoryId(categoryId);
    }

    @Override
    public int countByCategoryId(@Nullable String userId, @NonNull String categoryId) {
        return transactionDao.countByCategoryId(userId, categoryId);
    }

    @Override
    public int deleteByCategoryId(@NonNull String categoryId) {
        return transactionDao.deleteByCategoryId(categoryId);
    }

    @Override
    public int deleteByCategoryId(@Nullable String userId, @NonNull String categoryId) {
        return transactionDao.deleteByCategoryId(userId, categoryId);
    }

    @Override
    public void pushCreate(@NonNull Transaction transaction, @Nullable SyncCallback callback) {
        if (apiService == null) return;
        apiService.createTransaction(transaction, response -> { if (callback != null) callback.onSuccess(); }, error -> { if (callback != null) callback.onError(error.getMessage()); });
    }

    @Override
    public void pushUpdate(@NonNull Transaction transaction, @Nullable SyncCallback callback) {
        if (apiService == null) return;
        apiService.updateTransaction(transaction, response -> { if (callback != null) callback.onSuccess(); }, error -> { if (callback != null) callback.onError(error.getMessage()); });
    }

    @Override
    public void pushDelete(@NonNull String transactionId, @Nullable SyncCallback callback) {
        if (apiService == null) return;
        apiService.deleteTransaction(transactionId, response -> { if (callback != null) callback.onSuccess(); }, error -> { if (callback != null) callback.onError(error.getMessage()); });
    }

    @Override
    public void fetchFromServer(@Nullable Runnable onDone) {
        if (apiService == null) { if (onDone != null) onDone.run(); return; }
        apiService.fetchAndUpsertTransactions(transactionDao, onDone);
    }
}
