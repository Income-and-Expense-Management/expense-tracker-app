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

/**
 * TransactionDao - Data Access Object cho bảng transactions.
 * 
 * Chịu trách nhiệm duy nhất: CRUD operations cho Transaction entity.
 * Tuân thủ Single Responsibility Principle (SRP).
 * 
 * Xử lý đặc biệt:
 * - TransactionType enum được lưu dạng String trong SQLite
 * - Hỗ trợ JOIN query để lấy tên category và wallet
 * - Tính tổng tiền theo loại giao dịch và khoảng thời gian
 */
public class TransactionDao {

    private static final String TAG = "TransactionDao";

    private final BudgetDatabaseHelper dbHelper;

    public TransactionDao(@NonNull BudgetDatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // ==================== CREATE ====================

    /**
     * Thêm Transaction mới vào database.
     * Tự động tạo UUID và timestamps nếu chưa có.
     * 
     * @param transaction Transaction object cần thêm
     * @return ID của transaction mới, hoặc null nếu thất bại
     */
    @Nullable
    public String insert(@NonNull Transaction transaction) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Tạo UUID và timestamps nếu chưa có
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

        // Tạo ContentValues từ Transaction object
        ContentValues values = new ContentValues();
        values.put(TransactionEntry.COLUMN_ID, transaction.getId());
        values.put(TransactionEntry.COLUMN_WALLET_ID, transaction.getWalletId());
        values.put(TransactionEntry.COLUMN_CATEGORY_ID, transaction.getCategoryId());
        values.put(TransactionEntry.COLUMN_AMOUNT, transaction.getAmount());

        // QUAN TRỌNG: Enum -> String conversion cho SQLite
        if (transaction.getType() != null) {
            values.put(TransactionEntry.COLUMN_TYPE, transaction.getType().getValue());
        }

        values.put(TransactionEntry.COLUMN_TRANSACTION_DATE, transaction.getTransactionDate());
        values.put(TransactionEntry.COLUMN_ICON_ID, transaction.getIconId());
        values.put(TransactionEntry.COLUMN_NOTE, transaction.getNote());
        values.put(TransactionEntry.COLUMN_CREATED_AT, transaction.getCreatedAt());
        values.put(TransactionEntry.COLUMN_UPDATED_AT, transaction.getUpdatedAt());

        long result = db.insert(TransactionEntry.TABLE_NAME, null, values);

        if (result == -1) {
            Log.e(TAG, "Failed to insert transaction");
            return null;
        }

        Log.d(TAG, "Inserted transaction with ID: " + transaction.getId());
        return transaction.getId();
    }

    // ==================== READ ====================

    /**
     * Lấy Transaction theo ID.
     * 
     * @param transactionId ID của transaction cần tìm
     * @return Transaction object, hoặc null nếu không tìm thấy
     */
    @Nullable
    public Transaction getById(@NonNull String transactionId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Transaction transaction = null;

        Cursor cursor = null;
        try {
            cursor = db.query(
                    TransactionEntry.TABLE_NAME,
                    null,
                    TransactionEntry.COLUMN_ID + " = ?",
                    new String[]{transactionId},
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                transaction = cursorToTransaction(cursor);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return transaction;
    }

    /**
     * Lấy danh sách Transaction kèm theo tên Category và Wallet.
     * Sử dụng JOIN query để lấy thông tin liên quan.
     * 
     * @param walletId ID của Wallet (null để lấy tất cả)
     * @param limit Số lượng kết quả tối đa (0 để không giới hạn)
     * @return Danh sách Transaction với categoryName và walletName đã được điền
     */
    public List<Transaction> getWithDetails(@Nullable String walletId, int limit) {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Raw SQL query với JOIN để lấy tên category và wallet
        // Sử dụng LEFT JOIN vì category_id có thể null (ON DELETE SET NULL)
        String query = "SELECT " +
                "t." + TransactionEntry.COLUMN_ID + ", " +
                "t." + TransactionEntry.COLUMN_WALLET_ID + ", " +
                "t." + TransactionEntry.COLUMN_CATEGORY_ID + ", " +
                "t." + TransactionEntry.COLUMN_AMOUNT + ", " +
                "t." + TransactionEntry.COLUMN_TYPE + ", " +
                "t." + TransactionEntry.COLUMN_TRANSACTION_DATE + ", " +
                "t." + TransactionEntry.COLUMN_ICON_ID + ", " +
                "t." + TransactionEntry.COLUMN_NOTE + ", " +
                "t." + TransactionEntry.COLUMN_CREATED_AT + ", " +
                "t." + TransactionEntry.COLUMN_UPDATED_AT + ", " +
                "c." + CategoryEntry.COLUMN_NAME + " AS category_name, " +
                "w." + WalletEntry.COLUMN_NAME + " AS wallet_name " +
                "FROM " + TransactionEntry.TABLE_NAME + " t " +
                "LEFT JOIN " + CategoryEntry.TABLE_NAME + " c " +
                "ON t." + TransactionEntry.COLUMN_CATEGORY_ID + " = c." + CategoryEntry.COLUMN_ID + " " +
                "INNER JOIN " + WalletEntry.TABLE_NAME + " w " +
                "ON t." + TransactionEntry.COLUMN_WALLET_ID + " = w." + WalletEntry.COLUMN_ID;

        // Thêm điều kiện lọc theo wallet nếu có
        String[] selectionArgs = null;
        if (walletId != null) {
            query += " WHERE t." + TransactionEntry.COLUMN_WALLET_ID + " = ?";
            selectionArgs = new String[]{walletId};
        }

        // Sắp xếp theo ngày giao dịch mới nhất
        query += " ORDER BY t." + TransactionEntry.COLUMN_TRANSACTION_DATE + " DESC";

        // Giới hạn số lượng kết quả
        if (limit > 0) {
            query += " LIMIT " + limit;
        }

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, selectionArgs);

            // Lặp qua Cursor và parse từng row
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Transaction transaction = cursorToTransactionWithDetails(cursor);
                    transactions.add(transaction);
                } while (cursor.moveToNext());
            }
        } finally {
            // QUAN TRỌNG: Luôn đóng Cursor trong finally block
            if (cursor != null) {
                cursor.close();
            }
        }

        Log.d(TAG, "Fetched " + transactions.size() + " transactions");
        return transactions;
    }

    /**
     * Lấy transactions trong khoảng thời gian.
     * 
     * @param walletId ID của wallet
     * @param startDate Ngày bắt đầu (timestamp)
     * @param endDate Ngày kết thúc (timestamp)
     * @return Danh sách Transaction trong khoảng thời gian
     */
    public List<Transaction> getByDateRange(@NonNull String walletId, long startDate, long endDate) {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = null;
        try {
            cursor = db.query(
                    TransactionEntry.TABLE_NAME,
                    null,
                    TransactionEntry.COLUMN_WALLET_ID + " = ? AND " +
                            TransactionEntry.COLUMN_TRANSACTION_DATE + " >= ? AND " +
                            TransactionEntry.COLUMN_TRANSACTION_DATE + " <= ?",
                    new String[]{walletId, String.valueOf(startDate), String.valueOf(endDate)},
                    null, null,
                    TransactionEntry.COLUMN_TRANSACTION_DATE + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    transactions.add(cursorToTransaction(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return transactions;
    }

    /**
     * Lấy transactions theo category.
     * 
     * @param categoryId ID của category
     * @return Danh sách Transaction theo category
     */
    public List<Transaction> getByCategoryId(@NonNull String categoryId) {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = null;
        try {
            cursor = db.query(
                    TransactionEntry.TABLE_NAME,
                    null,
                    TransactionEntry.COLUMN_CATEGORY_ID + " = ?",
                    new String[]{categoryId},
                    null, null,
                    TransactionEntry.COLUMN_TRANSACTION_DATE + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    transactions.add(cursorToTransaction(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return transactions;
    }

    // ==================== UPDATE ====================

    /**
     * Cập nhật Transaction.
     * Tự động cập nhật updated_at timestamp.
     * 
     * @param transaction Transaction object với thông tin mới
     * @return Số dòng bị ảnh hưởng
     */
    public int update(@NonNull Transaction transaction) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        transaction.setUpdatedAt(IdGenerator.getCurrentTimestamp());

        ContentValues values = new ContentValues();
        values.put(TransactionEntry.COLUMN_CATEGORY_ID, transaction.getCategoryId());
        values.put(TransactionEntry.COLUMN_AMOUNT, transaction.getAmount());
        if (transaction.getType() != null) {
            values.put(TransactionEntry.COLUMN_TYPE, transaction.getType().getValue());
        }
        values.put(TransactionEntry.COLUMN_TRANSACTION_DATE, transaction.getTransactionDate());
        values.put(TransactionEntry.COLUMN_ICON_ID, transaction.getIconId());
        values.put(TransactionEntry.COLUMN_NOTE, transaction.getNote());
        values.put(TransactionEntry.COLUMN_UPDATED_AT, transaction.getUpdatedAt());

        return db.update(
                TransactionEntry.TABLE_NAME,
                values,
                TransactionEntry.COLUMN_ID + " = ?",
                new String[]{transaction.getId()}
        );
    }

    // ==================== DELETE ====================

    /**
     * Xóa Transaction theo ID.
     * 
     * @param transactionId ID của transaction cần xóa
     * @return Số dòng bị xóa
     */
    public int delete(@NonNull String transactionId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
                TransactionEntry.TABLE_NAME,
                TransactionEntry.COLUMN_ID + " = ?",
                new String[]{transactionId}
        );
    }

    /**
     * Xóa tất cả transactions của một wallet.
     * 
     * @param walletId ID của wallet
     * @return Số dòng bị xóa
     */
    public int deleteByWalletId(@NonNull String walletId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
                TransactionEntry.TABLE_NAME,
                TransactionEntry.COLUMN_WALLET_ID + " = ?",
                new String[]{walletId}
        );
    }

    // ==================== AGGREGATE ====================

    /**
     * Lấy tổng số tiền theo loại giao dịch trong khoảng thời gian.
     * 
     * @param walletId ID của wallet
     * @param type Loại giao dịch (INCOME hoặc EXPENSE)
     * @param startDate Ngày bắt đầu (timestamp)
     * @param endDate Ngày kết thúc (timestamp)
     * @return Tổng số tiền
     */
    public long getTotalAmountByType(@NonNull String walletId, @NonNull TransactionType type,
                                     long startDate, long endDate) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        long total = 0;

        String query = "SELECT SUM(" + TransactionEntry.COLUMN_AMOUNT + ") AS total " +
                "FROM " + TransactionEntry.TABLE_NAME + " " +
                "WHERE " + TransactionEntry.COLUMN_WALLET_ID + " = ? " +
                "AND " + TransactionEntry.COLUMN_TYPE + " = ? " +
                "AND " + TransactionEntry.COLUMN_TRANSACTION_DATE + " >= ? " +
                "AND " + TransactionEntry.COLUMN_TRANSACTION_DATE + " <= ?";

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{
                    walletId,
                    type.getValue(),
                    String.valueOf(startDate),
                    String.valueOf(endDate)
            });

            if (cursor != null && cursor.moveToFirst()) {
                total = cursor.getLong(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return total;
    }

    /**
     * Đếm số lượng transactions của một wallet.
     * 
     * @param walletId ID của wallet
     * @return Số lượng transactions
     */
    public int countByWalletId(@NonNull String walletId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int count = 0;

        String query = "SELECT COUNT(*) FROM " + TransactionEntry.TABLE_NAME +
                " WHERE " + TransactionEntry.COLUMN_WALLET_ID + " = ?";

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{walletId});
            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return count;
    }

    // ==================== CURSOR MAPPING ====================

    /**
     * Parse Cursor thành Transaction object (không có JOIN data).
     */
    private Transaction cursorToTransaction(Cursor cursor) {
        // String -> Enum conversion
        String typeString = CursorUtils.getString(cursor, TransactionEntry.COLUMN_TYPE);
        TransactionType type = TransactionType.fromValue(typeString);

        return new Transaction.Builder()
                .setId(CursorUtils.getString(cursor, TransactionEntry.COLUMN_ID))
                .setWalletId(CursorUtils.getString(cursor, TransactionEntry.COLUMN_WALLET_ID))
                .setCategoryId(CursorUtils.getString(cursor, TransactionEntry.COLUMN_CATEGORY_ID))
                .setAmount(CursorUtils.getLong(cursor, TransactionEntry.COLUMN_AMOUNT))
                .setType(type)
                .setTransactionDate(CursorUtils.getLong(cursor, TransactionEntry.COLUMN_TRANSACTION_DATE))
                .setIconId(CursorUtils.getString(cursor, TransactionEntry.COLUMN_ICON_ID))
                .setNote(CursorUtils.getString(cursor, TransactionEntry.COLUMN_NOTE))
                .setCreatedAt(CursorUtils.getLong(cursor, TransactionEntry.COLUMN_CREATED_AT))
                .setUpdatedAt(CursorUtils.getLong(cursor, TransactionEntry.COLUMN_UPDATED_AT))
                .build();
    }

    /**
     * Parse Cursor thành Transaction object với thông tin từ JOIN.
     * Xử lý Enum và các kiểu dữ liệu đặc biệt từ Cursor.
     */
    private Transaction cursorToTransactionWithDetails(Cursor cursor) {
        // QUAN TRỌNG: String -> Enum conversion
        String typeString = CursorUtils.getString(cursor, TransactionEntry.COLUMN_TYPE);
        TransactionType type = TransactionType.fromValue(typeString);

        return new Transaction.Builder()
                .setId(CursorUtils.getString(cursor, TransactionEntry.COLUMN_ID))
                .setWalletId(CursorUtils.getString(cursor, TransactionEntry.COLUMN_WALLET_ID))
                .setCategoryId(CursorUtils.getString(cursor, TransactionEntry.COLUMN_CATEGORY_ID))
                .setAmount(CursorUtils.getLong(cursor, TransactionEntry.COLUMN_AMOUNT))
                .setType(type)
                .setTransactionDate(CursorUtils.getLong(cursor, TransactionEntry.COLUMN_TRANSACTION_DATE))
                .setIconId(CursorUtils.getString(cursor, TransactionEntry.COLUMN_ICON_ID))
                .setNote(CursorUtils.getString(cursor, TransactionEntry.COLUMN_NOTE))
                .setCreatedAt(CursorUtils.getLong(cursor, TransactionEntry.COLUMN_CREATED_AT))
                .setUpdatedAt(CursorUtils.getLong(cursor, TransactionEntry.COLUMN_UPDATED_AT))
                // Thông tin từ JOIN
                .setCategoryName(CursorUtils.getString(cursor, "category_name"))
                .setWalletName(CursorUtils.getString(cursor, "wallet_name"))
                .build();
    }
}
