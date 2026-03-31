package com.ptithcm.quanlichitieu.data.repository;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.model.Transaction;
import com.ptithcm.quanlichitieu.data.model.TransactionGroup;

import java.util.ArrayList;
import java.util.List;

public class MockTransactionRepository implements TransactionRepository {

    @Override
    public List<TransactionGroup> getTransactionsByMonth(int monthOffset) {
        List<TransactionGroup> groups = new ArrayList<>();

        // Group 1: Saturday 18/3/2026
        List<Transaction> day1 = new ArrayList<>();
        day1.add(new Transaction(1, "Ăn uống", 50000, Transaction.TYPE_EXPENSE,
                R.drawable.ic_food, "Cash", System.currentTimeMillis()));
        day1.add(new Transaction(2, "Mua sắm", 15000, Transaction.TYPE_EXPENSE,
                R.drawable.ic_shopping, "Cash", System.currentTimeMillis()));
        day1.add(new Transaction(3, "Di chuyển", 20000, Transaction.TYPE_EXPENSE,
                R.drawable.ic_transport, "Cash", System.currentTimeMillis()));
        groups.add(new TransactionGroup("Thứ 7", "18/3/2026", -85000, day1));

        // Group 2: Friday 17/3/2026
        List<Transaction> day2 = new ArrayList<>();
        day2.add(new Transaction(4, "Ăn uống", 120000, Transaction.TYPE_EXPENSE,
                R.drawable.ic_food, "Cash", System.currentTimeMillis()));
        day2.add(new Transaction(5, "Lương", 500000, Transaction.TYPE_INCOME,
                R.drawable.ic_payment_method, "Cash", System.currentTimeMillis()));
        groups.add(new TransactionGroup("Thứ 6", "17/3/2026", 380000, day2));

        // Group 3: Thursday 16/3/2026
        List<Transaction> day3 = new ArrayList<>();
        day3.add(new Transaction(6, "Mua sắm", 350000, Transaction.TYPE_EXPENSE,
                R.drawable.ic_shopping, "Cash", System.currentTimeMillis()));
        day3.add(new Transaction(7, "Di chuyển", 45000, Transaction.TYPE_EXPENSE,
                R.drawable.ic_transport, "Cash", System.currentTimeMillis()));
        day3.add(new Transaction(8, "Ăn uống", 85000, Transaction.TYPE_EXPENSE,
                R.drawable.ic_food, "Cash", System.currentTimeMillis()));
        groups.add(new TransactionGroup("Thứ 5", "16/3/2026", -480000, day3));

        // Group 4: Wednesday 15/3/2026
        List<Transaction> day4 = new ArrayList<>();
        day4.add(new Transaction(9, "Ăn uống", 65000, Transaction.TYPE_EXPENSE,
                R.drawable.ic_food, "Cash", System.currentTimeMillis()));
        day4.add(new Transaction(10, "Mua sắm", 200000, Transaction.TYPE_EXPENSE,
                R.drawable.ic_shopping, "Cash", System.currentTimeMillis()));
        groups.add(new TransactionGroup("Thứ 4", "15/3/2026", -265000, day4));

        return groups;
    }

    @Override
    public double getTotalExpense() {
        return 1200000;
    }

    @Override
    public double getTotalIncome() {
        return 500000;
    }

    @Override
    public double getTotalBalance() {
        return 500000;
    }
}
