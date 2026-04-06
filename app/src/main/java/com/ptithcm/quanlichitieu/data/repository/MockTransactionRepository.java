package com.ptithcm.quanlichitieu.data.repository;

import androidx.annotation.Nullable;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.model.Transaction;
import com.ptithcm.quanlichitieu.data.model.TransactionGroup;
import com.ptithcm.quanlichitieu.data.model.TransactionType;

import java.util.ArrayList;
import java.util.List;

/**
 * MockTransactionRepository - Mock implementation để testing và development.
 * Implement đầy đủ interface mới với các method hỗ trợ wallet filtering.
 */
public class MockTransactionRepository implements TransactionRepository {

    /**
     * Helper method để tạo mock Transaction với model mới.
     */
    private Transaction createMockTransaction(String id, String categoryName, long amount,
                                               TransactionType type, String iconId, 
                                               String walletName, long timestamp) {
        Transaction t = new Transaction.Builder()
                .setId(id)
                .setCategoryName(categoryName)
                .setAmount(amount)
                .setType(type)
                .setIconId(iconId)
                .setWalletName(walletName)
                .setTransactionDate(timestamp)
                .setCreatedAt(timestamp)
                .setUpdatedAt(timestamp)
                .build();
        return t;
    }

    @Override
    public List<TransactionGroup> getTransactionsByMonth(int monthOffset) {
        return getTransactionsByWalletAndMonth(null, monthOffset);
    }

    @Override
    public List<TransactionGroup> getTransactionsByWalletAndMonth(@Nullable String walletId, int monthOffset) {
        // Mock data - không filter theo wallet
        List<TransactionGroup> groups = new ArrayList<>();
        long now = System.currentTimeMillis();

        // Group 1: Saturday 18/3/2026
        List<Transaction> day1 = new ArrayList<>();
        day1.add(createMockTransaction("1", "Ăn uống", 50000, TransactionType.EXPENSE,
                "ic_food", "Cash", now));
        day1.add(createMockTransaction("2", "Mua sắm", 15000, TransactionType.EXPENSE,
                "ic_shopping", "Cash", now));
        day1.add(createMockTransaction("3", "Di chuyển", 20000, TransactionType.EXPENSE,
                "ic_transport", "Cash", now));
        groups.add(new TransactionGroup("Thứ 7", "18/3/2026", -85000, day1));

        // Group 2: Friday 17/3/2026
        List<Transaction> day2 = new ArrayList<>();
        day2.add(createMockTransaction("4", "Ăn uống", 120000, TransactionType.EXPENSE,
                "ic_food", "Cash", now));
        day2.add(createMockTransaction("5", "Lương", 500000, TransactionType.INCOME,
                "ic_payment_method", "Cash", now));
        groups.add(new TransactionGroup("Thứ 6", "17/3/2026", 380000, day2));

        // Group 3: Thursday 16/3/2026
        List<Transaction> day3 = new ArrayList<>();
        day3.add(createMockTransaction("6", "Mua sắm", 350000, TransactionType.EXPENSE,
                "ic_shopping", "Cash", now));
        day3.add(createMockTransaction("7", "Di chuyển", 45000, TransactionType.EXPENSE,
                "ic_transport", "Cash", now));
        day3.add(createMockTransaction("8", "Ăn uống", 85000, TransactionType.EXPENSE,
                "ic_food", "Cash", now));
        groups.add(new TransactionGroup("Thứ 5", "16/3/2026", -480000, day3));

        // Group 4: Wednesday 15/3/2026
        List<Transaction> day4 = new ArrayList<>();
        day4.add(createMockTransaction("9", "Ăn uống", 65000, TransactionType.EXPENSE,
                "ic_food", "Cash", now));
        day4.add(createMockTransaction("10", "Mua sắm", 200000, TransactionType.EXPENSE,
                "ic_shopping", "Cash", now));
        groups.add(new TransactionGroup("Thứ 4", "15/3/2026", -265000, day4));

        return groups;
    }

    @Override
    public double getTotalExpense() {
        return 1200000;
    }

    @Override
    public double getTotalExpense(@Nullable String walletId, int monthOffset) {
        return 1200000;
    }

    @Override
    public double getTotalIncome() {
        return 500000;
    }

    @Override
    public double getTotalIncome(@Nullable String walletId, int monthOffset) {
        return 500000;
    }

    @Override
    public double getTotalBalance() {
        return 500000;
    }
}
