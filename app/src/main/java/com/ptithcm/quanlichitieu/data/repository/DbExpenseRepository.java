package com.ptithcm.quanlichitieu.data.repository;

import android.content.Context;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.local.DatabaseManager;
import com.ptithcm.quanlichitieu.data.local.dao.TransactionDao;
import com.ptithcm.quanlichitieu.data.model.Expense;
import com.ptithcm.quanlichitieu.data.model.Transaction;
import com.ptithcm.quanlichitieu.data.model.TransactionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DbExpenseRepository implements ExpenseRepository {
    private final TransactionDao transactionDao;
    private final Context context;

    public DbExpenseRepository(Context context) {
        this.context = context;
        DatabaseManager dbManager = DatabaseManager.getInstance(context);
        this.transactionDao = dbManager.getTransactionDao();
    }

    @Override
    public List<Expense> getTopExpenses(int limit) {
        List<Transaction> allTxs = transactionDao.getWithDetails(null, 0);
        Map<String, ExpenseAggr> map = new HashMap<>();

        for (Transaction t : allTxs) {
            if (t.getType() == TransactionType.EXPENSE) {
                String cat = t.getCategoryName() != null ? t.getCategoryName() : "Khác";
                ExpenseAggr aggr = map.get(cat);
                if (aggr == null) {
                    int resId = R.drawable.ic_payment_method;
                    if (t.getIconId() != null && !t.getIconId().isEmpty()) {
                        resId = context.getResources().getIdentifier(t.getIconId(), "drawable", context.getPackageName());
                        if (resId == 0) resId = R.drawable.ic_payment_method;
                    }
                    aggr = new ExpenseAggr(cat, t.getNote() != null ? t.getNote() : "", 0, resId, t.getTransactionDate());
                    map.put(cat, aggr);
                }
                aggr.amount += t.getAmount();
            }
        }

        List<Expense> res = new ArrayList<>();
        long idCounter = 1;
        for (ExpenseAggr a : map.values()) {
            res.add(new Expense(idCounter++, a.category, a.description, a.amount, a.iconResId, a.timestamp));
        }

        Collections.sort(res, (e1, e2) -> Double.compare(e2.getAmount(), e1.getAmount()));
        return res.subList(0, Math.min(limit, res.size()));
    }

    @Override
    public List<Expense> getCurrentMonthExpenses() {
        return new ArrayList<>();
    }

    @Override
    public double getTotalExpenseAmount() {
        return 0;
    }

    private static class ExpenseAggr {
        String category;
        String description;
        double amount;
        int iconResId;
        long timestamp;

        ExpenseAggr(String c, String d, double a, int i, long t) {
            category = c; description = d; amount = a; iconResId = i; timestamp = t;
        }
    }
}

