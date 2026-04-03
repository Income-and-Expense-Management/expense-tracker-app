package com.ptithcm.quanlichitieu.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ptithcm.quanlichitieu.data.local.BudgetDatabaseHelper;
import com.ptithcm.quanlichitieu.data.local.contract.DatabaseContract.UserEntry;
import com.ptithcm.quanlichitieu.data.local.util.CursorUtils;
import com.ptithcm.quanlichitieu.data.local.util.IdGenerator;
import com.ptithcm.quanlichitieu.data.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * UserDao - Data Access Object cho bảng users.
 * 
 * Chịu trách nhiệm duy nhất: CRUD operations cho User entity.
 * Tuân thủ Single Responsibility Principle (SRP).
 */
public class UserDao {

    private static final String TAG = "UserDao";

    private final BudgetDatabaseHelper dbHelper;

    public UserDao(@NonNull BudgetDatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // ==================== CREATE ====================

    /**
     * Thêm User mới vào database.
     * 
     * @param user User object cần thêm
     * @return ID của user mới, hoặc null nếu thất bại
     */
    @Nullable
    public String insert(@NonNull User user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Tạo UUID nếu chưa có
        if (user.getId() == null) {
            user.setId(IdGenerator.generateUUID());
        }
        if (user.getCreatedAt() == 0) {
            user.setCreatedAt(IdGenerator.getCurrentTimestamp());
        }

        ContentValues values = new ContentValues();
        values.put(UserEntry.COLUMN_ID, user.getId());
        values.put(UserEntry.COLUMN_FULL_NAME, user.getFullName());
        values.put(UserEntry.COLUMN_EMAIL, user.getEmail());
        values.put(UserEntry.COLUMN_AVATAR_URL, user.getAvatarUrl());
        values.put(UserEntry.COLUMN_AUTH_PROVIDER, user.getAuthProvider());
        values.put(UserEntry.COLUMN_CREATED_AT, user.getCreatedAt());

        long result = db.insert(UserEntry.TABLE_NAME, null, values);

        if (result == -1) {
            Log.e(TAG, "Failed to insert user");
            return null;
        }

        Log.d(TAG, "Inserted user with ID: " + user.getId());
        return user.getId();
    }

    // ==================== READ ====================

    /**
     * Lấy User theo ID.
     * 
     * @param userId ID của user cần tìm
     * @return User object, hoặc null nếu không tìm thấy
     */
    @Nullable
    public User getById(@NonNull String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        User user = null;

        Cursor cursor = null;
        try {
            cursor = db.query(
                    UserEntry.TABLE_NAME,
                    null,
                    UserEntry.COLUMN_ID + " = ?",
                    new String[]{userId},
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                user = cursorToUser(cursor);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return user;
    }

    /**
     * Lấy User theo email.
     * 
     * @param email Email của user cần tìm
     * @return User object, hoặc null nếu không tìm thấy
     */
    @Nullable
    public User getByEmail(@NonNull String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        User user = null;

        Cursor cursor = null;
        try {
            cursor = db.query(
                    UserEntry.TABLE_NAME,
                    null,
                    UserEntry.COLUMN_EMAIL + " = ?",
                    new String[]{email},
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                user = cursorToUser(cursor);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return user;
    }

    /**
     * Lấy tất cả User.
     * 
     * @return Danh sách tất cả User
     */
    public List<User> getAll() {
        List<User> users = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = null;
        try {
            cursor = db.query(
                    UserEntry.TABLE_NAME,
                    null, null, null, null, null,
                    UserEntry.COLUMN_CREATED_AT + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    users.add(cursorToUser(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return users;
    }

    // ==================== UPDATE ====================

    /**
     * Cập nhật thông tin User.
     * 
     * @param user User object với thông tin mới
     * @return Số dòng bị ảnh hưởng
     */
    public int update(@NonNull User user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(UserEntry.COLUMN_FULL_NAME, user.getFullName());
        values.put(UserEntry.COLUMN_EMAIL, user.getEmail());
        values.put(UserEntry.COLUMN_AVATAR_URL, user.getAvatarUrl());
        values.put(UserEntry.COLUMN_AUTH_PROVIDER, user.getAuthProvider());

        return db.update(
                UserEntry.TABLE_NAME,
                values,
                UserEntry.COLUMN_ID + " = ?",
                new String[]{user.getId()}
        );
    }

    // ==================== DELETE ====================

    /**
     * Xóa User theo ID.
     * Lưu ý: Sẽ cascade delete các bảng liên quan (wallets, categories).
     * 
     * @param userId ID của user cần xóa
     * @return Số dòng bị xóa
     */
    public int delete(@NonNull String userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
                UserEntry.TABLE_NAME,
                UserEntry.COLUMN_ID + " = ?",
                new String[]{userId}
        );
    }

    // ==================== CURSOR MAPPING ====================

    /**
     * Parse Cursor thành User object.
     * Sử dụng Builder pattern để đảm bảo immutability.
     */
    private User cursorToUser(Cursor cursor) {
        return new User.Builder()
                .setId(CursorUtils.getString(cursor, UserEntry.COLUMN_ID))
                .setFullName(CursorUtils.getString(cursor, UserEntry.COLUMN_FULL_NAME))
                .setEmail(CursorUtils.getString(cursor, UserEntry.COLUMN_EMAIL))
                .setAvatarUrl(CursorUtils.getString(cursor, UserEntry.COLUMN_AVATAR_URL))
                .setAuthProvider(CursorUtils.getString(cursor, UserEntry.COLUMN_AUTH_PROVIDER))
                .setCreatedAt(CursorUtils.getLong(cursor, UserEntry.COLUMN_CREATED_AT))
                .build();
    }
}
