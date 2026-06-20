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

    // Thống nhất điều kiện lọc Soft Delete (NULL hoặc 0)
    private static final String NOT_DELETED_SIMPLE = "(" + TransactionEntry.COLUMN_DELETED_AT + " IS NULL OR " + TransactionEntry.COLUMN_DELETED_AT + " = 0)";
    private static final String NOT_DELETED_JOINED = "(t." + TransactionEntry.COLUMN_DELETED_AT + " IS NULL OR t." + TransactionEntry.COLUMN_DELETED_AT + " = 0)";

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
            "LEFT JOIN " + CategoryEntry.TABLE_NAME + " c ON t." + TransactionEntry.COLUMN_CATEGORY_ID + " = c." + CategoryEntry.COLUMN_ID + " " +
            "LEFT JOIN " + WalletEntry.TABLE_NAME + " w ON t." + TransactionEntry.COLUMN_WALLET_ID + " = w." + WalletEntry.COLUMN_ID + " ";

    private final BudgetDatabaseHelper dbHelper;

    public TransactionDao(@NonNull BudgetDatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Lấy danh sách giao dịch chi tiết (Dùng cho DbExpenseRepository và TransactionFragment).
     */
    public List<Transaction> getWithDetails(@Nullable String walletId, int limit) {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        StringBuilder query = new StringBuilder(BASE_JOIN_QUERY);
        query.append("WHERE ").append(NOT_DELETED_JOINED);
        List<String> argsList = new ArrayList<>();
        if (walletId != null) {
            query.append(" AND t.").append(TransactionEntry.COLUMN_WALLET_ID).append(" = ?");
            argsList.add(walletId);
        }
        query.append(" ORDER BY t.").append(TransactionEntry.COLUMN_TRANSACTION_DATE).append(" DESC");
        if (limit > 0) query.append(" LIMIT ").append(limit);
        String[] args = argsList.isEmpty() ? null : argsList.toArray(new String[0]);
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query.toString(), args);
            if (cursor != null && cursor.moveToFirst()) {
                do { transactions.add(cursorToTransactionWithDetails(cursor)); } while (cursor.moveToNext());
            }
        } finally { if (cursor != null) cursor.close(); }
        return transactions;
    }

    /**
     * Đếm số lượng giao dịch của một danh mục (chưa xóa).
     */
    public int countByCategoryId(@NonNull String categoryId) {
        return countByCategoryId(null, categoryId);
    }

    /**
     * Đếm số lượng giao dịch của một danh mục (chưa xóa) thuộc ví của user cụ thể.
     */
    public int countByCategoryId(@Nullable String userId, @NonNull String categoryId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int count = 0;
        String query;
        String[] args;
        if (userId != null) {
            query = "SELECT COUNT(*) FROM " + TransactionEntry.TABLE_NAME + " t " +
                    "INNER JOIN wallets w ON t." + TransactionEntry.COLUMN_WALLET_ID + " = w.id " +
                    "WHERE t." + TransactionEntry.COLUMN_CATEGORY_ID + " = ? " +
                    "AND (t." + TransactionEntry.COLUMN_DELETED_AT + " IS NULL OR t." + TransactionEntry.COLUMN_DELETED_AT + " = 0) " +
                    "AND (w." + WalletEntry.COLUMN_DELETED_AT + " IS NULL OR w." + WalletEntry.COLUMN_DELETED_AT + " = 0) " +
                    "AND w." + WalletEntry.COLUMN_USER_ID + " = ?";
            args = new String[]{categoryId, userId};
        } else {
            query = "SELECT COUNT(*) FROM " + TransactionEntry.TABLE_NAME + " t " +
                    "INNER JOIN wallets w ON t." + TransactionEntry.COLUMN_WALLET_ID + " = w.id " +
                    "WHERE t." + TransactionEntry.COLUMN_CATEGORY_ID + " = ? " +
                    "AND (t." + TransactionEntry.COLUMN_DELETED_AT + " IS NULL OR t." + TransactionEntry.COLUMN_DELETED_AT + " = 0) " +
                    "AND (w." + WalletEntry.COLUMN_DELETED_AT + " IS NULL OR w." + WalletEntry.COLUMN_DELETED_AT + " = 0) " +
                    "AND w." + WalletEntry.COLUMN_USER_ID + " IS NULL";
            args = new String[]{categoryId};
        }
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, args);
            if (cursor != null && cursor.moveToFirst()) count = cursor.getInt(0);
        } finally { if (cursor != null) cursor.close(); }
        return count;
    }

    /**
     * Xóa mềm tất cả giao dịch thuộc danh mục.
     */
    public int deleteByCategoryId(@NonNull String categoryId) {
        return deleteByCategoryId(null, categoryId);
    }

    /**
     * Xóa mềm tất cả giao dịch thuộc danh mục và thuộc ví của user cụ thể.
     */
    public int deleteByCategoryId(@Nullable String userId, @NonNull String categoryId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long now = IdGenerator.getCurrentTimestamp();
        ContentValues values = new ContentValues();
        values.put(TransactionEntry.COLUMN_DELETED_AT, now);
        values.put(TransactionEntry.COLUMN_UPDATED_AT, now);
        
        String whereClause;
        String[] whereArgs;
        if (userId != null) {
            whereClause = TransactionEntry.COLUMN_CATEGORY_ID + " = ? " +
                    "AND " + NOT_DELETED_SIMPLE + " " +
                    "AND " + TransactionEntry.COLUMN_WALLET_ID + " IN (SELECT id FROM wallets WHERE user_id = ? AND (deleted_at IS NULL OR deleted_at = 0))";
            whereArgs = new String[]{categoryId, userId};
        } else {
            whereClause = TransactionEntry.COLUMN_CATEGORY_ID + " = ? " +
                    "AND " + NOT_DELETED_SIMPLE + " " +
                    "AND " + TransactionEntry.COLUMN_WALLET_ID + " IN (SELECT id FROM wallets WHERE user_id IS NULL AND (deleted_at IS NULL OR deleted_at = 0))";
            whereArgs = new String[]{categoryId};
        }
        return db.update(TransactionEntry.TABLE_NAME, values, whereClause, whereArgs);
    }

    /**
     * Lấy tổng tiền theo danh mục (Dùng cho BudgetRepository).
     */
    public long getTotalAmountByCategory(@NonNull String walletId, @NonNull String categoryId, long startDate, long endDate) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        long total = 0;
        String query = "SELECT SUM(" + TransactionEntry.COLUMN_AMOUNT + ") FROM " + TransactionEntry.TABLE_NAME +
                " WHERE " + TransactionEntry.COLUMN_WALLET_ID + " = ? AND " + TransactionEntry.COLUMN_CATEGORY_ID + " = ?" +
                " AND " + TransactionEntry.COLUMN_TRANSACTION_DATE + " >= ? AND " + TransactionEntry.COLUMN_TRANSACTION_DATE + " <= ?" +
                " AND " + NOT_DELETED_SIMPLE;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{walletId, categoryId, String.valueOf(startDate), String.valueOf(endDate)});
            if (cursor != null && cursor.moveToFirst()) total = cursor.getLong(0);
        } finally { if (cursor != null) cursor.close(); }
        return total;
    }

    /**
     * Tìm kiếm giao dịch.
     */
    public List<Transaction> searchWithKeyword(@NonNull String keyword, @Nullable String walletId) {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String likePattern = "%" + keyword + "%";
        String query = BASE_JOIN_QUERY + "WHERE " + NOT_DELETED_JOINED + " AND (t." + TransactionEntry.COLUMN_NOTE + " LIKE ? OR c." + CategoryEntry.COLUMN_NAME + " LIKE ?) ";
        List<String> argsList = new ArrayList<>();
        argsList.add(likePattern);
        argsList.add(likePattern);
        if (walletId != null) {
            query += "AND t." + TransactionEntry.COLUMN_WALLET_ID + " = ? ";
            argsList.add(walletId);
        }
        query += "ORDER BY t." + TransactionEntry.COLUMN_TRANSACTION_DATE + " DESC";
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, argsList.toArray(new String[0]));
            if (cursor != null && cursor.moveToFirst()) {
                do { transactions.add(cursorToTransactionWithDetails(cursor)); } while (cursor.moveToNext());
            }
        } finally { if (cursor != null) cursor.close(); }
        return transactions;
    }

    /**
     * Lấy danh sách thô (không Join) để sync Server.
     */
    public List<Transaction> getByCategoryIdSimple(@NonNull String categoryId) {
        return getByCategoryIdSimple(null, categoryId);
    }

    /**
     * Lấy danh sách thô (không Join) thuộc ví của user cụ thể để sync Server.
     */
    public List<Transaction> getByCategoryIdSimple(@Nullable String userId, @NonNull String categoryId) {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String query;
        String[] args;
        if (userId != null) {
            query = "SELECT t.* FROM " + TransactionEntry.TABLE_NAME + " t " +
                    "INNER JOIN wallets w ON t." + TransactionEntry.COLUMN_WALLET_ID + " = w.id " +
                    "WHERE t." + TransactionEntry.COLUMN_CATEGORY_ID + " = ? " +
                    "AND (t." + TransactionEntry.COLUMN_DELETED_AT + " IS NULL OR t." + TransactionEntry.COLUMN_DELETED_AT + " = 0) " +
                    "AND (w." + WalletEntry.COLUMN_DELETED_AT + " IS NULL OR w." + WalletEntry.COLUMN_DELETED_AT + " = 0) " +
                    "AND w." + WalletEntry.COLUMN_USER_ID + " = ?";
            args = new String[]{categoryId, userId};
        } else {
            query = "SELECT t.* FROM " + TransactionEntry.TABLE_NAME + " t " +
                    "INNER JOIN wallets w ON t." + TransactionEntry.COLUMN_WALLET_ID + " = w.id " +
                    "WHERE t." + TransactionEntry.COLUMN_CATEGORY_ID + " = ? " +
                    "AND (t." + TransactionEntry.COLUMN_DELETED_AT + " IS NULL OR t." + TransactionEntry.COLUMN_DELETED_AT + " = 0) " +
                    "AND (w." + WalletEntry.COLUMN_DELETED_AT + " IS NULL OR w." + WalletEntry.COLUMN_DELETED_AT + " = 0) " +
                    "AND w." + WalletEntry.COLUMN_USER_ID + " IS NULL";
            args = new String[]{categoryId};
        }
        
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, args);
            if (cursor != null && cursor.moveToFirst()) {
                do { transactions.add(cursorToTransaction(cursor)); } while (cursor.moveToNext());
            }
        } finally { if (cursor != null) cursor.close(); }
        return transactions;
    }

    @Nullable
    public String insert(@NonNull Transaction transaction) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (transaction.getId() == null) transaction.setId(IdGenerator.generateUUID());
        long now = IdGenerator.getCurrentTimestamp();
        ContentValues values = new ContentValues();
        values.put(TransactionEntry.COLUMN_ID, transaction.getId());
        values.put(TransactionEntry.COLUMN_WALLET_ID, transaction.getWalletId());
        values.put(TransactionEntry.COLUMN_CATEGORY_ID, transaction.getCategoryId());
        values.put(TransactionEntry.COLUMN_AMOUNT, transaction.getAmount());
        values.put(TransactionEntry.COLUMN_TRANSACTION_DATE, transaction.getTransactionDate() > 0 ? transaction.getTransactionDate() : now);
        values.put(TransactionEntry.COLUMN_NOTE, transaction.getNote());
        values.put(TransactionEntry.COLUMN_CREATED_AT, transaction.getCreatedAt() > 0 ? transaction.getCreatedAt() : now);
        values.put(TransactionEntry.COLUMN_UPDATED_AT, now);
        if (transaction.getDeletedAt() != null) values.put(TransactionEntry.COLUMN_DELETED_AT, transaction.getDeletedAt());
        else values.putNull(TransactionEntry.COLUMN_DELETED_AT);
        long result = db.insert(TransactionEntry.TABLE_NAME, null, values);
        return result != -1 ? transaction.getId() : null;
    }

    @Nullable
    public Transaction getById(@NonNull String transactionId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Transaction transaction = null;
        String query = BASE_JOIN_QUERY + "WHERE t." + TransactionEntry.COLUMN_ID + " = ? AND " + NOT_DELETED_JOINED;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{transactionId});
            if (cursor != null && cursor.moveToFirst()) transaction = cursorToTransactionWithDetails(cursor);
        } finally { if (cursor != null) cursor.close(); }
        return transaction;
    }

    public List<Transaction> getByDateRangeWithDetails(@Nullable String walletId, long startDate, long endDate) {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        StringBuilder query = new StringBuilder(BASE_JOIN_QUERY);
        query.append("WHERE t.").append(TransactionEntry.COLUMN_TRANSACTION_DATE).append(" >= ? AND t.").append(TransactionEntry.COLUMN_TRANSACTION_DATE).append(" <= ? AND ").append(NOT_DELETED_JOINED);
        List<String> argsList = new ArrayList<>();
        argsList.add(String.valueOf(startDate));
        argsList.add(String.valueOf(endDate));
        if (walletId != null) {
            query.append(" AND t.").append(TransactionEntry.COLUMN_WALLET_ID).append(" = ?");
            argsList.add(walletId);
        }
        query.append(" ORDER BY t.").append(TransactionEntry.COLUMN_TRANSACTION_DATE).append(" DESC");
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query.toString(), argsList.toArray(new String[0]));
            if (cursor != null && cursor.moveToFirst()) {
                do { transactions.add(cursorToTransactionWithDetails(cursor)); } while (cursor.moveToNext());
            }
        } finally { if (cursor != null) cursor.close(); }
        return transactions;
    }

    public long getTotalAmountByType(@NonNull String walletId, @NonNull TransactionType type, long startDate, long endDate) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        long total = 0;
        String query = "SELECT SUM(t." + TransactionEntry.COLUMN_AMOUNT + ") FROM " + TransactionEntry.TABLE_NAME + " t " +
                "INNER JOIN " + CategoryEntry.TABLE_NAME + " c ON t." + TransactionEntry.COLUMN_CATEGORY_ID + " = c." + CategoryEntry.COLUMN_ID + " " +
                "WHERE t." + TransactionEntry.COLUMN_WALLET_ID + " = ? AND c." + CategoryEntry.COLUMN_TYPE + " = ? " +
                "AND t." + TransactionEntry.COLUMN_TRANSACTION_DATE + " >= ? AND t." + TransactionEntry.COLUMN_TRANSACTION_DATE + " <= ? " +
                "AND " + NOT_DELETED_JOINED;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{walletId, type.getValue(), String.valueOf(startDate), String.valueOf(endDate)});
            if (cursor != null && cursor.moveToFirst()) total = cursor.getLong(0);
        } finally { if (cursor != null) cursor.close(); }
        return total;
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
        return db.update(TransactionEntry.TABLE_NAME, values, TransactionEntry.COLUMN_ID + " = ?", new String[]{transaction.getId()});
    }

    public int delete(@NonNull String transactionId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long now = IdGenerator.getCurrentTimestamp();
        ContentValues values = new ContentValues();
        values.put(TransactionEntry.COLUMN_DELETED_AT, now);
        values.put(TransactionEntry.COLUMN_UPDATED_AT, now);
        return db.update(TransactionEntry.TABLE_NAME, values, TransactionEntry.COLUMN_ID + " = ?", new String[]{transactionId});
    }

    public void insertFromServer(@NonNull Transaction t) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(TransactionEntry.COLUMN_ID, t.getId());
        v.put(TransactionEntry.COLUMN_WALLET_ID, t.getWalletId());
        v.put(TransactionEntry.COLUMN_CATEGORY_ID, t.getCategoryId());
        v.put(TransactionEntry.COLUMN_AMOUNT, t.getAmount());
        v.put(TransactionEntry.COLUMN_TRANSACTION_DATE, t.getTransactionDate());
        v.put(TransactionEntry.COLUMN_NOTE, t.getNote());
        v.put(TransactionEntry.COLUMN_CREATED_AT, t.getCreatedAt());
        v.put(TransactionEntry.COLUMN_UPDATED_AT, t.getUpdatedAt());
        if (t.getDeletedAt() != null) v.put(TransactionEntry.COLUMN_DELETED_AT, t.getDeletedAt());
        else v.putNull(TransactionEntry.COLUMN_DELETED_AT);
        db.insertWithOnConflict(TransactionEntry.TABLE_NAME, null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void updateFromServer(@NonNull Transaction t) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(TransactionEntry.COLUMN_WALLET_ID, t.getWalletId());
        v.put(TransactionEntry.COLUMN_CATEGORY_ID, t.getCategoryId());
        v.put(TransactionEntry.COLUMN_AMOUNT, t.getAmount());
        v.put(TransactionEntry.COLUMN_TRANSACTION_DATE, t.getTransactionDate());
        v.put(TransactionEntry.COLUMN_NOTE, t.getNote());
        v.put(TransactionEntry.COLUMN_UPDATED_AT, t.getUpdatedAt());
        if (t.getDeletedAt() != null) v.put(TransactionEntry.COLUMN_DELETED_AT, t.getDeletedAt());
        else v.putNull(TransactionEntry.COLUMN_DELETED_AT);
        db.update(TransactionEntry.TABLE_NAME, v, TransactionEntry.COLUMN_ID + " = ?", new String[]{t.getId()});
    }

    public int deleteByWalletId(@NonNull String walletId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long now = IdGenerator.getCurrentTimestamp();
        ContentValues values = new ContentValues();
        values.put(TransactionEntry.COLUMN_DELETED_AT, now);
        values.put(TransactionEntry.COLUMN_UPDATED_AT, now);
        return db.update(TransactionEntry.TABLE_NAME, values, TransactionEntry.COLUMN_WALLET_ID + " = ?", new String[]{walletId});
    }

    public int countByWalletId(@NonNull String walletId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int count = 0;
        String query = "SELECT COUNT(*) FROM " + TransactionEntry.TABLE_NAME + " WHERE " + TransactionEntry.COLUMN_WALLET_ID + " = ? AND " + NOT_DELETED_SIMPLE;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{walletId});
            if (cursor != null && cursor.moveToFirst()) count = cursor.getInt(0);
        } finally { if (cursor != null) cursor.close(); }
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

    private Transaction cursorToTransaction(Cursor cursor) {
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
                .build();
    }
}
