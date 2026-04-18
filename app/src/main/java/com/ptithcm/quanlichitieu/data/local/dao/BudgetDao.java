package com.ptithcm.quanlichitieu.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ptithcm.quanlichitieu.data.local.BudgetDatabaseHelper;
import com.ptithcm.quanlichitieu.data.local.contract.DatabaseContract.BudgetEntry;
import com.ptithcm.quanlichitieu.data.local.util.CursorUtils;
import com.ptithcm.quanlichitieu.utils.IdGenerator;
import com.ptithcm.quanlichitieu.data.model.Budget;

import java.util.ArrayList;
import java.util.List;

public class BudgetDao {

    private static final String TAG = "BudgetDao";
    private final BudgetDatabaseHelper dbHelper;

    public BudgetDao(@NonNull BudgetDatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    @Nullable
    public String insert(@NonNull Budget budget) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        if (budget.getId() == null) {
            budget.setId(IdGenerator.generateUUID());
        }

        ContentValues values = new ContentValues();
        values.put(BudgetEntry.COLUMN_ID, budget.getId());
        values.put(BudgetEntry.COLUMN_WALLET_ID, budget.getWalletId());
        values.put(BudgetEntry.COLUMN_CATEGORY_ID, budget.getCategoryId());
        values.put(BudgetEntry.COLUMN_TARGET_AMOUNT, budget.getTargetAmount());
        values.put(BudgetEntry.COLUMN_START_DATE, budget.getStartDate());
        values.put(BudgetEntry.COLUMN_END_DATE, budget.getEndDate());
        
        long now = IdGenerator.getCurrentTimestamp();
        values.put(BudgetEntry.COLUMN_CREATED_AT, budget.getCreatedAt() == 0 ? now : budget.getCreatedAt());
        values.put(BudgetEntry.COLUMN_UPDATED_AT, budget.getUpdatedAt() == 0 ? now : budget.getUpdatedAt());
        if (budget.getDeletedAt() != null) values.put(BudgetEntry.COLUMN_DELETED_AT, budget.getDeletedAt());
        else values.putNull(BudgetEntry.COLUMN_DELETED_AT);

        long result = db.insert(BudgetEntry.TABLE_NAME, null, values);

        if (result == -1) {
            Log.e(TAG, "Failed to insert budget");
            return null;
        }

        Log.d(TAG, "Inserted budget with ID: " + budget.getId());
        return budget.getId();
    }

    @Nullable
    public Budget getById(@NonNull String budgetId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Budget budget = null;
        Cursor cursor = null;
        try {
            cursor = db.query(
                    BudgetEntry.TABLE_NAME,
                    null,
                    BudgetEntry.COLUMN_ID + " = ? AND " + BudgetEntry.COLUMN_DELETED_AT + " IS NULL",
                    new String[]{budgetId},
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                budget = cursorToBudget(cursor);
            }
        } finally {
            if (cursor != null) cursor.close();
        }

        return budget;
    }

    public List<Budget> getByWalletId(@NonNull String walletId) {
        List<Budget> budgets = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    BudgetEntry.TABLE_NAME,
                    null,
                    BudgetEntry.COLUMN_WALLET_ID + " = ? AND " + BudgetEntry.COLUMN_DELETED_AT + " IS NULL",
                    new String[]{walletId},
                    null, null,
                    BudgetEntry.COLUMN_START_DATE + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    budgets.add(cursorToBudget(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }

        return budgets;
    }

    public List<Budget> getByCategoryId(@NonNull String categoryId) {
        List<Budget> budgets = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    BudgetEntry.TABLE_NAME,
                    null,
                    BudgetEntry.COLUMN_CATEGORY_ID + " = ? AND " + BudgetEntry.COLUMN_DELETED_AT + " IS NULL",
                    new String[]{categoryId},
                    null, null,
                    BudgetEntry.COLUMN_START_DATE + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    budgets.add(cursorToBudget(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }

        return budgets;
    }

    public List<Budget> getActiveBudgets(@NonNull String walletId, long currentDate) {
        List<Budget> budgets = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    BudgetEntry.TABLE_NAME,
                    null,
                    BudgetEntry.COLUMN_WALLET_ID + " = ? AND " +
                            BudgetEntry.COLUMN_START_DATE + " <= ? AND " +
                            BudgetEntry.COLUMN_END_DATE + " >= ? AND " + 
                            BudgetEntry.COLUMN_DELETED_AT + " IS NULL",
                    new String[]{walletId, String.valueOf(currentDate), String.valueOf(currentDate)},
                    null, null,
                    BudgetEntry.COLUMN_START_DATE + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    budgets.add(cursorToBudget(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }

        return budgets;
    }

    @Nullable
    public Budget getByWalletAndCategory(@NonNull String walletId, @NonNull String categoryId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Budget budget = null;
        Cursor cursor = null;
        try {
            cursor = db.query(
                    BudgetEntry.TABLE_NAME,
                    null,
                    BudgetEntry.COLUMN_WALLET_ID + " = ? AND " + BudgetEntry.COLUMN_CATEGORY_ID + " = ? AND " + BudgetEntry.COLUMN_DELETED_AT + " IS NULL",
                    new String[]{walletId, categoryId},
                    null, null,
                    BudgetEntry.COLUMN_START_DATE + " DESC",
                    "1" 
            );

            if (cursor != null && cursor.moveToFirst()) {
                budget = cursorToBudget(cursor);
            }
        } finally {
            if (cursor != null) cursor.close();
        }

        return budget;
    }

    public int update(@NonNull Budget budget) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(BudgetEntry.COLUMN_WALLET_ID, budget.getWalletId());
        values.put(BudgetEntry.COLUMN_CATEGORY_ID, budget.getCategoryId());
        values.put(BudgetEntry.COLUMN_TARGET_AMOUNT, budget.getTargetAmount());
        values.put(BudgetEntry.COLUMN_START_DATE, budget.getStartDate());
        values.put(BudgetEntry.COLUMN_END_DATE, budget.getEndDate());
        values.put(BudgetEntry.COLUMN_UPDATED_AT, IdGenerator.getCurrentTimestamp());
        if (budget.getDeletedAt() != null) values.put(BudgetEntry.COLUMN_DELETED_AT, budget.getDeletedAt());
        else values.putNull(BudgetEntry.COLUMN_DELETED_AT);

        int rowsAffected = db.update(
                BudgetEntry.TABLE_NAME,
                values,
                BudgetEntry.COLUMN_ID + " = ?",
                new String[]{budget.getId()}
        );

        return rowsAffected;
    }

    public int delete(@NonNull String budgetId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(BudgetEntry.COLUMN_DELETED_AT, IdGenerator.getCurrentTimestamp());
        return db.update(BudgetEntry.TABLE_NAME, values, BudgetEntry.COLUMN_ID + " = ?", new String[]{budgetId});
    }

    public int deleteByWalletId(@NonNull String walletId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(BudgetEntry.COLUMN_DELETED_AT, IdGenerator.getCurrentTimestamp());
        return db.update(BudgetEntry.TABLE_NAME, values, BudgetEntry.COLUMN_WALLET_ID + " = ?", new String[]{walletId});
    }

    private Budget cursorToBudget(Cursor cursor) {
        return new Budget.Builder()
                .setId(CursorUtils.getString(cursor, BudgetEntry.COLUMN_ID))
                .setWalletId(CursorUtils.getString(cursor, BudgetEntry.COLUMN_WALLET_ID))
                .setCategoryId(CursorUtils.getString(cursor, BudgetEntry.COLUMN_CATEGORY_ID))
                .setTargetAmount(CursorUtils.getLong(cursor, BudgetEntry.COLUMN_TARGET_AMOUNT))
                .setStartDate(CursorUtils.getLong(cursor, BudgetEntry.COLUMN_START_DATE))
                .setEndDate(CursorUtils.getLong(cursor, BudgetEntry.COLUMN_END_DATE))
                .setCreatedAt(CursorUtils.getLong(cursor, BudgetEntry.COLUMN_CREATED_AT))
                .setUpdatedAt(CursorUtils.getLong(cursor, BudgetEntry.COLUMN_UPDATED_AT))
                .setDeletedAt(CursorUtils.getLong(cursor, BudgetEntry.COLUMN_DELETED_AT) == 0 ? null : CursorUtils.getLong(cursor, BudgetEntry.COLUMN_DELETED_AT))
                .build();
    }
}
