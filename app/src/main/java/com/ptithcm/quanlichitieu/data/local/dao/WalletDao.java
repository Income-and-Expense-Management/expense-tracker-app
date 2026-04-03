package com.ptithcm.quanlichitieu.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ptithcm.quanlichitieu.data.local.BudgetDatabaseHelper;
import com.ptithcm.quanlichitieu.data.local.contract.DatabaseContract.WalletEntry;
import com.ptithcm.quanlichitieu.data.local.util.CursorUtils;
import com.ptithcm.quanlichitieu.data.local.util.IdGenerator;
import com.ptithcm.quanlichitieu.data.model.Wallet;

import java.util.ArrayList;
import java.util.List;

/**
 * WalletDao - Data Access Object cho bảng wallets.
 * 
 * Chịu trách nhiệm duy nhất: CRUD operations cho Wallet entity.
 * Tuân thủ Single Responsibility Principle (SRP).
 * 
 * Xử lý đặc biệt:
 * - Boolean is_active được lưu dạng int (0/1) trong SQLite
 * - Hỗ trợ soft delete (đặt is_active = 0 thay vì xóa thật)
 */
public class WalletDao {

    private static final String TAG = "WalletDao";

    private final BudgetDatabaseHelper dbHelper;

    public WalletDao(@NonNull BudgetDatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // ==================== CREATE ====================

    /**
     * Thêm Wallet mới vào database.
     * Tự động tạo UUID và timestamps nếu chưa có.
     * 
     * @param wallet Wallet object cần thêm
     * @return ID của wallet mới, hoặc null nếu thất bại
     */
    @Nullable
    public String insert(@NonNull Wallet wallet) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Tạo UUID và timestamps nếu chưa có
        if (wallet.getId() == null) {
            wallet.setId(IdGenerator.generateUUID());
        }
        long now = IdGenerator.getCurrentTimestamp();
        if (wallet.getCreatedAt() == 0) {
            wallet.setCreatedAt(now);
        }
        wallet.setUpdatedAt(now);

        ContentValues values = new ContentValues();
        values.put(WalletEntry.COLUMN_ID, wallet.getId());
        values.put(WalletEntry.COLUMN_USER_ID, wallet.getUserId());
        values.put(WalletEntry.COLUMN_NAME, wallet.getName());
        values.put(WalletEntry.COLUMN_INITIAL_BALANCE, wallet.getInitialBalance());
        values.put(WalletEntry.COLUMN_CURRENCY, wallet.getCurrency());
        values.put(WalletEntry.COLUMN_ICON_ID, wallet.getIconId());
        values.put(WalletEntry.COLUMN_CREATED_AT, wallet.getCreatedAt());
        values.put(WalletEntry.COLUMN_UPDATED_AT, wallet.getUpdatedAt());
        // Boolean -> int conversion
        values.put(WalletEntry.COLUMN_IS_ACTIVE, wallet.getIsActiveAsInt());

        long result = db.insert(WalletEntry.TABLE_NAME, null, values);

        if (result == -1) {
            Log.e(TAG, "Failed to insert wallet");
            return null;
        }

        Log.d(TAG, "Inserted wallet with ID: " + wallet.getId());
        return wallet.getId();
    }

    // ==================== READ ====================

    /**
     * Lấy Wallet theo ID.
     * 
     * @param walletId ID của wallet cần tìm
     * @return Wallet object, hoặc null nếu không tìm thấy
     */
    @Nullable
    public Wallet getById(@NonNull String walletId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Wallet wallet = null;

        Cursor cursor = null;
        try {
            cursor = db.query(
                    WalletEntry.TABLE_NAME,
                    null,
                    WalletEntry.COLUMN_ID + " = ?",
                    new String[]{walletId},
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                wallet = cursorToWallet(cursor);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return wallet;
    }

    /**
     * Lấy tất cả Wallet active của một User.
     * Chỉ trả về các wallet có is_active = 1.
     * 
     * @param userId ID của user
     * @return Danh sách Wallet active của user
     */
    public List<Wallet> getByUserId(@Nullable String userId) {
        List<Wallet> wallets = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = null;
        try {
            String selection;
            String[] selectionArgs;
            
            if (userId != null) {
                selection = WalletEntry.COLUMN_USER_ID + " = ? AND " + WalletEntry.COLUMN_IS_ACTIVE + " = 1";
                selectionArgs = new String[]{userId};
            } else {
                // Lấy wallet không có user (guest mode)
                selection = WalletEntry.COLUMN_USER_ID + " IS NULL AND " + WalletEntry.COLUMN_IS_ACTIVE + " = 1";
                selectionArgs = null;
            }

            cursor = db.query(
                    WalletEntry.TABLE_NAME,
                    null,
                    selection,
                    selectionArgs,
                    null, null,
                    WalletEntry.COLUMN_CREATED_AT + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    wallets.add(cursorToWallet(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return wallets;
    }

    /**
     * Lấy tất cả Wallet (bao gồm cả inactive).
     * 
     * @param userId ID của user
     * @return Danh sách tất cả Wallet của user
     */
    public List<Wallet> getAllByUserId(@NonNull String userId) {
        List<Wallet> wallets = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = null;
        try {
            cursor = db.query(
                    WalletEntry.TABLE_NAME,
                    null,
                    WalletEntry.COLUMN_USER_ID + " = ?",
                    new String[]{userId},
                    null, null,
                    WalletEntry.COLUMN_CREATED_AT + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    wallets.add(cursorToWallet(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return wallets;
    }

    // ==================== UPDATE ====================

    /**
     * Cập nhật thông tin Wallet.
     * Tự động cập nhật updated_at timestamp.
     * 
     * @param wallet Wallet object với thông tin mới
     * @return Số dòng bị ảnh hưởng
     */
    public int update(@NonNull Wallet wallet) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        wallet.setUpdatedAt(IdGenerator.getCurrentTimestamp());

        ContentValues values = new ContentValues();
        values.put(WalletEntry.COLUMN_NAME, wallet.getName());
        values.put(WalletEntry.COLUMN_INITIAL_BALANCE, wallet.getInitialBalance());
        values.put(WalletEntry.COLUMN_CURRENCY, wallet.getCurrency());
        values.put(WalletEntry.COLUMN_ICON_ID, wallet.getIconId());
        values.put(WalletEntry.COLUMN_UPDATED_AT, wallet.getUpdatedAt());
        values.put(WalletEntry.COLUMN_IS_ACTIVE, wallet.getIsActiveAsInt());

        return db.update(
                WalletEntry.TABLE_NAME,
                values,
                WalletEntry.COLUMN_ID + " = ?",
                new String[]{wallet.getId()}
        );
    }

    // ==================== DELETE ====================

    /**
     * Soft delete Wallet (đặt is_active = 0).
     * Không xóa thật để giữ lại dữ liệu transaction history.
     * 
     * @param walletId ID của wallet cần soft delete
     * @return Số dòng bị ảnh hưởng
     */
    public int softDelete(@NonNull String walletId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(WalletEntry.COLUMN_IS_ACTIVE, 0);
        values.put(WalletEntry.COLUMN_UPDATED_AT, IdGenerator.getCurrentTimestamp());

        return db.update(
                WalletEntry.TABLE_NAME,
                values,
                WalletEntry.COLUMN_ID + " = ?",
                new String[]{walletId}
        );
    }

    /**
     * Hard delete Wallet.
     * Sẽ cascade delete các transactions và budgets liên quan.
     * 
     * @param walletId ID của wallet cần xóa
     * @return Số dòng bị xóa
     */
    public int delete(@NonNull String walletId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
                WalletEntry.TABLE_NAME,
                WalletEntry.COLUMN_ID + " = ?",
                new String[]{walletId}
        );
    }

    /**
     * Khôi phục Wallet đã bị soft delete.
     * 
     * @param walletId ID của wallet cần khôi phục
     * @return Số dòng bị ảnh hưởng
     */
    public int restore(@NonNull String walletId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(WalletEntry.COLUMN_IS_ACTIVE, 1);
        values.put(WalletEntry.COLUMN_UPDATED_AT, IdGenerator.getCurrentTimestamp());

        return db.update(
                WalletEntry.TABLE_NAME,
                values,
                WalletEntry.COLUMN_ID + " = ?",
                new String[]{walletId}
        );
    }

    // ==================== CURSOR MAPPING ====================

    /**
     * Parse Cursor thành Wallet object.
     * Xử lý int -> Boolean conversion cho is_active.
     */
    private Wallet cursorToWallet(Cursor cursor) {
        return new Wallet.Builder()
                .setId(CursorUtils.getString(cursor, WalletEntry.COLUMN_ID))
                .setUserId(CursorUtils.getString(cursor, WalletEntry.COLUMN_USER_ID))
                .setName(CursorUtils.getString(cursor, WalletEntry.COLUMN_NAME))
                .setInitialBalance(CursorUtils.getLong(cursor, WalletEntry.COLUMN_INITIAL_BALANCE))
                .setCurrency(CursorUtils.getString(cursor, WalletEntry.COLUMN_CURRENCY))
                .setIconId(CursorUtils.getString(cursor, WalletEntry.COLUMN_ICON_ID))
                .setCreatedAt(CursorUtils.getLong(cursor, WalletEntry.COLUMN_CREATED_AT))
                .setUpdatedAt(CursorUtils.getLong(cursor, WalletEntry.COLUMN_UPDATED_AT))
                // int -> Boolean conversion
                .setIsActive(CursorUtils.getBoolean(cursor, WalletEntry.COLUMN_IS_ACTIVE))
                .build();
    }
}
