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

/**
 * BudgetDao - Data Access Object cho bảng budgets.
 * 
 * Chịu trách nhiệm duy nhất: CRUD operations cho Budget entity.
 * Tuân thủ Single Responsibility Principle (SRP).
 */
public class BudgetDao {

    private static final String TAG = "BudgetDao";

    private final BudgetDatabaseHelper dbHelper;

    public BudgetDao(@NonNull BudgetDatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // ==================== CREATE ====================

    /**
     * Thêm Budget mới vào database.
     * Tự động tạo UUID nếu chưa có.
     * 
     * @param budget Budget object cần thêm
     * @return ID của budget mới, hoặc null nếu thất bại
     */
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

        long result = db.insert(BudgetEntry.TABLE_NAME, null, values);

        if (result == -1) {
            Log.e(TAG, "Failed to insert budget");
            return null;
        }

        Log.d(TAG, "Inserted budget with ID: " + budget.getId());
        return budget.getId();
    }

    // ==================== READ ====================

    /**
     * Lấy Budget theo ID.
     * 
     * @param budgetId ID của budget cần tìm
     * @return Budget object, hoặc null nếu không tìm thấy
     */
    @Nullable
    public Budget getById(@NonNull String budgetId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Budget budget = null;

        Cursor cursor = null;
        try {
            cursor = db.query(
                    BudgetEntry.TABLE_NAME,
                    null,
                    BudgetEntry.COLUMN_ID + " = ?",
                    new String[]{budgetId},
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                budget = cursorToBudget(cursor);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return budget;
    }

    /**
     * Lấy tất cả Budget của một Wallet.
     * 
     * @param walletId ID của wallet
     * @return Danh sách Budget của wallet
     */
    public List<Budget> getByWalletId(@NonNull String walletId) {
        List<Budget> budgets = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = null;
        try {
            cursor = db.query(
                    BudgetEntry.TABLE_NAME,
                    null,
                    BudgetEntry.COLUMN_WALLET_ID + " = ?",
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
            if (cursor != null) {
                cursor.close();
            }
        }

        return budgets;
    }

    /**
     * Lấy Budget theo category.
     * 
     * @param categoryId ID của category
     * @return Danh sách Budget theo category
     */
    public List<Budget> getByCategoryId(@NonNull String categoryId) {
        List<Budget> budgets = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = null;
        try {
            cursor = db.query(
                    BudgetEntry.TABLE_NAME,
                    null,
                    BudgetEntry.COLUMN_CATEGORY_ID + " = ?",
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
            if (cursor != null) {
                cursor.close();
            }
        }

        return budgets;
    }

    /**
     * Lấy các Budget đang active (trong khoảng thời gian hiện tại).
     * 
     * @param walletId ID của wallet
     * @param currentDate Ngày hiện tại (timestamp)
     * @return Danh sách Budget đang active
     */
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
                            BudgetEntry.COLUMN_END_DATE + " >= ?",
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
            if (cursor != null) {
                cursor.close();
            }
        }

        return budgets;
    }

    /**
     * Lấy Budget theo wallet và category.
     * 
     * @param walletId ID của wallet
     * @param categoryId ID của category
     * @return Budget object, hoặc null nếu không tìm thấy
     */
    @Nullable
    public Budget getByWalletAndCategory(@NonNull String walletId, @NonNull String categoryId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Budget budget = null;

        Cursor cursor = null;
        try {
            cursor = db.query(
                    BudgetEntry.TABLE_NAME,
                    null,
                    BudgetEntry.COLUMN_WALLET_ID + " = ? AND " + BudgetEntry.COLUMN_CATEGORY_ID + " = ?",
                    new String[]{walletId, categoryId},
                    null, null,
                    BudgetEntry.COLUMN_START_DATE + " DESC",
                    "1" // LIMIT 1
            );

            if (cursor != null && cursor.moveToFirst()) {
                budget = cursorToBudget(cursor);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return budget;
    }

    // ==================== UPDATE ====================

    /**
     * Cập nhật thông tin Budget.
     * 
     * @param budget Budget object với thông tin mới
     * @return Số dòng bị ảnh hưởng
     */
    public int update(@NonNull Budget budget) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(BudgetEntry.COLUMN_TARGET_AMOUNT, budget.getTargetAmount());
        values.put(BudgetEntry.COLUMN_START_DATE, budget.getStartDate());
        values.put(BudgetEntry.COLUMN_END_DATE, budget.getEndDate());

        return db.update(
                BudgetEntry.TABLE_NAME,
                values,
                BudgetEntry.COLUMN_ID + " = ?",
                new String[]{budget.getId()}
        );
    }

    // ==================== DELETE ====================

    /**
     * Xóa Budget theo ID.
     * 
     * @param budgetId ID của budget cần xóa
     * @return Số dòng bị xóa
     */
    public int delete(@NonNull String budgetId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
                BudgetEntry.TABLE_NAME,
                BudgetEntry.COLUMN_ID + " = ?",
                new String[]{budgetId}
        );
    }

    /**
     * Xóa tất cả budgets của một wallet.
     * 
     * @param walletId ID của wallet
     * @return Số dòng bị xóa
     */
    public int deleteByWalletId(@NonNull String walletId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
                BudgetEntry.TABLE_NAME,
                BudgetEntry.COLUMN_WALLET_ID + " = ?",
                new String[]{walletId}
        );
    }

    // ==================== CURSOR MAPPING ====================

    /**
     * Parse Cursor thành Budget object.
     */
    private Budget cursorToBudget(Cursor cursor) {
        return new Budget.Builder()
                .setId(CursorUtils.getString(cursor, BudgetEntry.COLUMN_ID))
                .setWalletId(CursorUtils.getString(cursor, BudgetEntry.COLUMN_WALLET_ID))
                .setCategoryId(CursorUtils.getString(cursor, BudgetEntry.COLUMN_CATEGORY_ID))
                .setTargetAmount(CursorUtils.getLong(cursor, BudgetEntry.COLUMN_TARGET_AMOUNT))
                .setStartDate(CursorUtils.getLong(cursor, BudgetEntry.COLUMN_START_DATE))
                .setEndDate(CursorUtils.getLong(cursor, BudgetEntry.COLUMN_END_DATE))
                .build();
    }
}
