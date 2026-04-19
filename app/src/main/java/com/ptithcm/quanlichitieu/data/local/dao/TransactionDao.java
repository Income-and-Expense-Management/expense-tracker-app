package com.ptithcm.quanlichitieu.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ptithcm.quanlichitieu.data.local.BudgetDatabaseHelper;
import com.ptithcm.quanlichitieu.data.local.contract.DatabaseContract.CategoryEntry;
import com.ptithcm.quanlichitieu.data.local.contract.DatabaseContract.TransactionEntry;
import com.ptithcm.quanlichitieu.data.local.contract.DatabaseContract.WalletEntry;
import com.ptithcm.quanlichitieu.data.local.util.CursorUtils;
import com.ptithcm.quanlichitieu.utils.IdGenerator;
import com.ptithcm.quanlichitieu.data.model.Transaction;
import com.ptithcm.quanlichitieu.data.model.TransactionType;

import java.util.ArrayList;
import java.util.List;

public class TransactionDao {

    private static final String TAG = "TransactionDao";

    private static final String BASE_JOIN_QUERY = "SELECT " +
            "t." + TransactionEntry.COLUMN_ID + ", " +
            "t." + TransactionEntry.COLUMN_WALLET_ID + ", " +
            "t." + TransactionEntry.COLUMN_CATEGORY_ID + ", " +
            "t." + TransactionEntry.COLUMN_AMOUNT + ", " +
            "t." + TransactionEntry.COLUMN_TRANSACTION_DATE + ", " +
            "t." + TransactionEntry.COLUMN_NOTE + ", " +
            "t." + TransactionEntry.COLUMN_CREATED_AT + ", " +
            "t." + TransactionEntry.COLUMN_UPDATED_AT + ", " +
            "t." + TransactionEntry.COLUMN_DELETED_AT + ", " +
            "c." + CategoryEntry.COLUMN_NAME + " AS category_name, " +
            "c." + CategoryEntry.COLUMN_TYPE + " AS category_type, " +
            "c." + CategoryEntry.COLUMN_ICON_NAME + " AS icon_name, " +
            "w." + WalletEntry.COLUMN_NAME + " AS wallet_name " +
            "FROM " + TransactionEntry.TABLE_NAME + " t " +
            "LEFT JOIN " + CategoryEntry.TABLE_NAME + " c " +
            "ON t." + TransactionEntry.COLUMN_CATEGORY_ID + " = c." + CategoryEntry.COLUMN_ID + " " +
            "INNER JOIN " + WalletEntry.TABLE_NAME + " w " +
            "ON t." + TransactionEntry.COLUMN_WALLET_ID + " = w." + WalletEntry.COLUMN_ID + " ";

    private final BudgetDatabaseHelper dbHelper;

    public TransactionDao(@NonNull BudgetDatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    @Nullable
    public String insert(@NonNull Transaction transaction) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        if (transaction.getId() == null) {
            transaction.setId(IdGenerator.generateUUID());
        }
        long now = IdGenerator.getCurrentTimestamp();
        if (transaction.getCreatedAt() == 0) {
            transaction.setCreatedAt(now);
        }
        transaction.setUpdatedAt(now);
        if (transaction.getTransactionDate() == 0) {
            transaction.setTransactionDate(now);
        }

        ContentValues values = new ContentValues();
        values.put(TransactionEntry.COLUMN_ID, transaction.getId());
        values.put(TransactionEntry.COLUMN_WALLET_ID, transaction.getWalletId());
        values.put(TransactionEntry.COLUMN_CATEGORY_ID, transaction.getCategoryId());
        values.put(TransactionEntry.COLUMN_AMOUNT, transaction.getAmount());

        // TYPE AND ICON_ID REMOVED FROM TRANSACTION TABLE

        values.put(TransactionEntry.COLUMN_TRANSACTION_DATE, transaction.getTransactionDate());
        values.put(TransactionEntry.COLUMN_NOTE, transaction.getNote());
        values.put(TransactionEntry.COLUMN_CREATED_AT, transaction.getCreatedAt());
        values.put(TransactionEntry.COLUMN_UPDATED_AT, transaction.getUpdatedAt());
        if (transaction.getDeletedAt() != null) {
            values.put(TransactionEntry.COLUMN_DELETED_AT, transaction.getDeletedAt());
        } else {
            values.putNull(TransactionEntry.COLUMN_DELETED_AT);
        }

        long result = db.insert(TransactionEntry.TABLE_NAME, null, values);

        if (result == -1) {
            Log.e(TAG, "Failed to insert transaction");
            return null;
        }

        Log.d(TAG, "Inserted transaction with ID: " + transaction.getId());
        return transaction.getId();
    }

    @Nullable
    public Transaction getById(@NonNull String transactionId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Transaction transaction = null;

        String query = BASE_JOIN_QUERY + "WHERE t." + TransactionEntry.COLUMN_ID + " = ? AND t." + TransactionEntry.COLUMN_DELETED_AT + " IS NULL";

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{transactionId});
            if (cursor != null && cursor.moveToFirst()) {
                transaction = cursorToTransactionWithDetails(cursor);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return transaction;
    }

    public List<Transaction> getWithDetails(@Nullable String walletId, int limit) {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = BASE_JOIN_QUERY + "WHERE t." + TransactionEntry.COLUMN_DELETED_AT + " IS NULL";

        List<String> argsList = new ArrayList<>();
        if (walletId != null) {
            query += " AND t." + TransactionEntry.COLUMN_WALLET_ID + " = ?";
            argsList.add(walletId);
        }

        query += " ORDER BY t." + TransactionEntry.COLUMN_TRANSACTION_DATE + " DESC";
        if (limit > 0) {
            query += " LIMIT " + limit;
        }

        String[] selectionArgs = argsList.isEmpty() ? null : argsList.toArray(new String[0]);
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, selectionArgs);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    transactions.add(cursorToTransactionWithDetails(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return transactions;
    }

    public List<Transaction> getByDateRange(@Nullable String walletId, long startDate, long endDate) {
        return getByDateRangeWithDetails(walletId, startDate, endDate);
    }

    public List<Transaction> getByDateRangeWithDetails(@Nullable String walletId, long startDate, long endDate) {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = BASE_JOIN_QUERY +
                "WHERE t." + TransactionEntry.COLUMN_TRANSACTION_DATE + " >= ? " +
                "AND t." + TransactionEntry.COLUMN_TRANSACTION_DATE + " <= ? " +
                "AND t." + TransactionEntry.COLUMN_DELETED_AT + " IS NULL ";

        List<String> argsList = new ArrayList<>();
        argsList.add(String.valueOf(startDate));
        argsList.add(String.valueOf(endDate));

        if (walletId != null) {
            query += "AND t." + TransactionEntry.COLUMN_WALLET_ID + " = ? ";
            argsList.add(walletId);
        }

        query += "ORDER BY t." + TransactionEntry.COLUMN_TRANSACTION_DATE + " DESC";

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, argsList.toArray(new String[0]));
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    transactions.add(cursorToTransactionWithDetails(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return transactions;
    }

    public List<Transaction> getByCategoryId(@NonNull String categoryId) {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = BASE_JOIN_QUERY +
                "WHERE t." + TransactionEntry.COLUMN_CATEGORY_ID + " = ? " +
                "AND t." + TransactionEntry.COLUMN_DELETED_AT + " IS NULL " +
                "ORDER BY t." + TransactionEntry.COLUMN_TRANSACTION_DATE + " DESC";

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{categoryId});
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    transactions.add(cursorToTransactionWithDetails(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return transactions;
    }

    public int update(@NonNull Transaction transaction) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        transaction.setUpdatedAt(IdGenerator.getCurrentTimestamp());

        ContentValues values = new ContentValues();
        values.put(TransactionEntry.COLUMN_CATEGORY_ID, transaction.getCategoryId());
        values.put(TransactionEntry.COLUMN_AMOUNT, transaction.getAmount());
        values.put(TransactionEntry.COLUMN_TRANSACTION_DATE, transaction.getTransactionDate());
        values.put(TransactionEntry.COLUMN_NOTE, transaction.getNote());
        values.put(TransactionEntry.COLUMN_UPDATED_AT, transaction.getUpdatedAt());
        if (transaction.getDeletedAt() != null) {
            values.put(TransactionEntry.COLUMN_DELETED_AT, transaction.getDeletedAt());
        } else {
            values.putNull(TransactionEntry.COLUMN_DELETED_AT);
        }

        return db.update(TransactionEntry.TABLE_NAME, values, TransactionEntry.COLUMN_ID + " = ?", new String[]{transaction.getId()});
    }

    public int delete(@NonNull String transactionId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TransactionEntry.COLUMN_DELETED_AT, IdGenerator.getCurrentTimestamp());
        return db.update(TransactionEntry.TABLE_NAME, values, TransactionEntry.COLUMN_ID + " = ?", new String[]{transactionId});
    }

    public int deleteByWalletId(@NonNull String walletId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TransactionEntry.COLUMN_DELETED_AT, IdGenerator.getCurrentTimestamp());
        return db.update(TransactionEntry.TABLE_NAME, values, TransactionEntry.COLUMN_WALLET_ID + " = ?", new String[]{walletId});
    }

    public long getTotalAmountByType(@NonNull String walletId, @NonNull TransactionType type, long startDate, long endDate) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        long total = 0;

        String query = "SELECT SUM(t." + TransactionEntry.COLUMN_AMOUNT + ") AS total " +
                "FROM " + TransactionEntry.TABLE_NAME + " t " +
                "INNER JOIN " + CategoryEntry.TABLE_NAME + " c " +
                "ON t." + TransactionEntry.COLUMN_CATEGORY_ID + " = c." + CategoryEntry.COLUMN_ID + " " +
                "WHERE t." + TransactionEntry.COLUMN_WALLET_ID + " = ? " +
                "AND c." + CategoryEntry.COLUMN_TYPE + " = ? " +
                "AND t." + TransactionEntry.COLUMN_TRANSACTION_DATE + " >= ? " +
                "AND t." + TransactionEntry.COLUMN_TRANSACTION_DATE + " <= ? " +
                "AND t." + TransactionEntry.COLUMN_DELETED_AT + " IS NULL";

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{walletId, type.getValue(), String.valueOf(startDate), String.valueOf(endDate)});
            if (cursor != null && cursor.moveToFirst()) {
                total = cursor.getLong(0);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return total;
    }

    public long getTotalAmountByCategory(@NonNull String walletId, @NonNull String categoryId, long startDate, long endDate) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        long total = 0;

        String query = "SELECT SUM(t." + TransactionEntry.COLUMN_AMOUNT + ") AS total " +
                "FROM " + TransactionEntry.TABLE_NAME + " t " +
                "INNER JOIN " + CategoryEntry.TABLE_NAME + " c " +
                "ON t." + TransactionEntry.COLUMN_CATEGORY_ID + " = c." + CategoryEntry.COLUMN_ID + " " +
                "WHERE t." + TransactionEntry.COLUMN_WALLET_ID + " = ? " +
                "AND t." + TransactionEntry.COLUMN_CATEGORY_ID + " = ? " +
                "AND c." + CategoryEntry.COLUMN_TYPE + " = ? " +
                "AND t." + TransactionEntry.COLUMN_TRANSACTION_DATE + " >= ? " +
                "AND t." + TransactionEntry.COLUMN_TRANSACTION_DATE + " <= ? " +
                "AND t." + TransactionEntry.COLUMN_DELETED_AT + " IS NULL";

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{walletId, categoryId, TransactionType.EXPENSE.getValue(), String.valueOf(startDate), String.valueOf(endDate)});
            if (cursor != null && cursor.moveToFirst()) {
                total = cursor.getLong(0);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return total;
    }

    public int countByWalletId(@NonNull String walletId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int count = 0;
        String query = "SELECT COUNT(*) FROM " + TransactionEntry.TABLE_NAME + " WHERE " + TransactionEntry.COLUMN_WALLET_ID + " = ? AND " + TransactionEntry.COLUMN_DELETED_AT + " IS NULL";
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{walletId});
            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return count;
    }

    private Transaction cursorToTransactionWithDetails(Cursor cursor) {
        String typeString = CursorUtils.getString(cursor, "category_type");
        TransactionType type = typeString != null ? TransactionType.fromValue(typeString) : TransactionType.EXPENSE;
        return new Transaction.Builder()
                .setId(CursorUtils.getString(cursor, TransactionEntry.COLUMN_ID))
                .setWalletId(CursorUtils.getString(cursor, TransactionEntry.COLUMN_WALLET_ID))
                .setCategoryId(CursorUtils.getString(cursor, TransactionEntry.COLUMN_CATEGORY_ID))
                .setAmount(CursorUtils.getLong(cursor, TransactionEntry.COLUMN_AMOUNT))
                .setTransactionDate(CursorUtils.getLong(cursor, TransactionEntry.COLUMN_TRANSACTION_DATE))
                .setNote(CursorUtils.getString(cursor, TransactionEntry.COLUMN_NOTE))
                .setCreatedAt(CursorUtils.getLong(cursor, TransactionEntry.COLUMN_CREATED_AT))
                .setUpdatedAt(CursorUtils.getLong(cursor, TransactionEntry.COLUMN_UPDATED_AT))
                .setDeletedAt(CursorUtils.getLong(cursor, TransactionEntry.COLUMN_DELETED_AT) == 0 ? null : CursorUtils.getLong(cursor, TransactionEntry.COLUMN_DELETED_AT))
                .setCategoryName(CursorUtils.getString(cursor, "category_name"))
                .setCategoryType(type)
                .setIconId(CursorUtils.getString(cursor, "icon_name"))
                .setWalletName(CursorUtils.getString(cursor, "wallet_name"))
                .build();
    }
}
