package com.ptithcm.quanlichitieu.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ptithcm.quanlichitieu.data.local.BudgetDatabaseHelper;
import com.ptithcm.quanlichitieu.data.local.contract.DatabaseContract.CategoryEntry;
import com.ptithcm.quanlichitieu.data.local.util.CursorUtils;
import com.ptithcm.quanlichitieu.data.local.util.IdGenerator;
import com.ptithcm.quanlichitieu.data.model.Category;
import com.ptithcm.quanlichitieu.data.model.TransactionType;

import java.util.ArrayList;
import java.util.List;

/**
 * CategoryDao - Data Access Object cho bảng categories.
 * 
 * Chịu trách nhiệm duy nhất: CRUD operations cho Category entity.
 * Tuân thủ Single Responsibility Principle (SRP).
 * 
 * Xử lý đặc biệt:
 * - TransactionType enum được lưu dạng String trong SQLite
 * - Hỗ trợ cả danh mục hệ thống (user_id = null) và danh mục người dùng
 */
public class CategoryDao {

    private static final String TAG = "CategoryDao";

    private final BudgetDatabaseHelper dbHelper;

    public CategoryDao(@NonNull BudgetDatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // ==================== CREATE ====================

    /**
     * Thêm Category mới vào database.
     * Chỉ dùng cho danh mục của user, không phải hệ thống.
     * 
     * @param category Category object cần thêm
     * @return ID của category mới, hoặc null nếu thất bại
     */
    @Nullable
    public String insert(@NonNull Category category) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        if (category.getId() == null) {
            category.setId(IdGenerator.generateUUID());
        }

        ContentValues values = new ContentValues();
        values.put(CategoryEntry.COLUMN_ID, category.getId());
        values.put(CategoryEntry.COLUMN_USER_ID, category.getUserId());
        values.put(CategoryEntry.COLUMN_NAME, category.getName());
        // Enum -> String conversion
        if (category.getType() != null) {
            values.put(CategoryEntry.COLUMN_TYPE, category.getType().getValue());
        }
        values.put(CategoryEntry.COLUMN_ICON_NAME, category.getIconName());

        long result = db.insert(CategoryEntry.TABLE_NAME, null, values);

        if (result == -1) {
            Log.e(TAG, "Failed to insert category");
            return null;
        }

        Log.d(TAG, "Inserted category with ID: " + category.getId());
        return category.getId();
    }

    // ==================== READ ====================

    /**
     * Lấy Category theo ID.
     * 
     * @param categoryId ID của category cần tìm
     * @return Category object, hoặc null nếu không tìm thấy
     */
    @Nullable
    public Category getById(@NonNull String categoryId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Category category = null;

        Cursor cursor = null;
        try {
            cursor = db.query(
                    CategoryEntry.TABLE_NAME,
                    null,
                    CategoryEntry.COLUMN_ID + " = ?",
                    new String[]{categoryId},
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                category = cursorToCategory(cursor);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return category;
    }

    /**
     * Lấy tất cả Category theo loại (INCOME/EXPENSE).
     * Bao gồm cả danh mục hệ thống (user_id = null) và danh mục của user.
     * 
     * @param userId ID của user (có thể null)
     * @param type Loại giao dịch (INCOME hoặc EXPENSE)
     * @return Danh sách Category theo loại
     */
    public List<Category> getByType(@Nullable String userId, @NonNull TransactionType type) {
        List<Category> categories = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Lấy cả danh mục hệ thống và danh mục của user
        String selection = CategoryEntry.COLUMN_TYPE + " = ? AND (" +
                CategoryEntry.COLUMN_USER_ID + " IS NULL OR " +
                CategoryEntry.COLUMN_USER_ID + " = ?)";
        String[] selectionArgs = {type.getValue(), userId};

        Cursor cursor = null;
        try {
            cursor = db.query(
                    CategoryEntry.TABLE_NAME,
                    null,
                    selection,
                    selectionArgs,
                    null, null,
                    CategoryEntry.COLUMN_NAME + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    categories.add(cursorToCategory(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return categories;
    }

    /**
     * Lấy tất cả danh mục hệ thống (user_id = null).
     * 
     * @return Danh sách danh mục hệ thống
     */
    public List<Category> getSystemCategories() {
        List<Category> categories = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = null;
        try {
            cursor = db.query(
                    CategoryEntry.TABLE_NAME,
                    null,
                    CategoryEntry.COLUMN_USER_ID + " IS NULL",
                    null,
                    null, null,
                    CategoryEntry.COLUMN_TYPE + ", " + CategoryEntry.COLUMN_NAME + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    categories.add(cursorToCategory(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return categories;
    }

    /**
     * Lấy tất cả danh mục của user (không bao gồm hệ thống).
     * 
     * @param userId ID của user
     * @return Danh sách danh mục của user
     */
    public List<Category> getUserCategories(@NonNull String userId) {
        List<Category> categories = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = null;
        try {
            cursor = db.query(
                    CategoryEntry.TABLE_NAME,
                    null,
                    CategoryEntry.COLUMN_USER_ID + " = ?",
                    new String[]{userId},
                    null, null,
                    CategoryEntry.COLUMN_TYPE + ", " + CategoryEntry.COLUMN_NAME + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    categories.add(cursorToCategory(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return categories;
    }

    /**
     * Lấy tất cả Category (hệ thống + user).
     * 
     * @param userId ID của user
     * @return Danh sách tất cả Category có thể sử dụng
     */
    public List<Category> getAllAvailable(@Nullable String userId) {
        List<Category> categories = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String selection = CategoryEntry.COLUMN_USER_ID + " IS NULL OR " +
                CategoryEntry.COLUMN_USER_ID + " = ?";
        String[] selectionArgs = {userId};

        Cursor cursor = null;
        try {
            cursor = db.query(
                    CategoryEntry.TABLE_NAME,
                    null,
                    selection,
                    selectionArgs,
                    null, null,
                    CategoryEntry.COLUMN_TYPE + ", " + CategoryEntry.COLUMN_NAME + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    categories.add(cursorToCategory(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return categories;
    }

    // ==================== UPDATE ====================

    /**
     * Cập nhật thông tin Category.
     * Chỉ cho phép cập nhật danh mục của user, không phải hệ thống.
     * 
     * @param category Category object với thông tin mới
     * @return Số dòng bị ảnh hưởng
     */
    public int update(@NonNull Category category) {
        // Không cho phép cập nhật danh mục hệ thống
        if (category.isSystemCategory()) {
            Log.w(TAG, "Cannot update system category");
            return 0;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(CategoryEntry.COLUMN_NAME, category.getName());
        if (category.getType() != null) {
            values.put(CategoryEntry.COLUMN_TYPE, category.getType().getValue());
        }
        values.put(CategoryEntry.COLUMN_ICON_NAME, category.getIconName());

        return db.update(
                CategoryEntry.TABLE_NAME,
                values,
                CategoryEntry.COLUMN_ID + " = ? AND " + CategoryEntry.COLUMN_USER_ID + " IS NOT NULL",
                new String[]{category.getId()}
        );
    }

    // ==================== DELETE ====================

    /**
     * Xóa Category theo ID.
     * Chỉ cho phép xóa danh mục của user, không phải hệ thống.
     * Lưu ý: Transactions sử dụng category này sẽ có category_id = NULL (ON DELETE SET NULL).
     * 
     * @param categoryId ID của category cần xóa
     * @return Số dòng bị xóa
     */
    public int delete(@NonNull String categoryId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
                CategoryEntry.TABLE_NAME,
                CategoryEntry.COLUMN_ID + " = ? AND " + CategoryEntry.COLUMN_USER_ID + " IS NOT NULL",
                new String[]{categoryId}
        );
    }

    // ==================== CURSOR MAPPING ====================

    /**
     * Parse Cursor thành Category object.
     * Xử lý String -> Enum conversion cho type.
     */
    private Category cursorToCategory(Cursor cursor) {
        // String -> Enum conversion
        String typeString = CursorUtils.getString(cursor, CategoryEntry.COLUMN_TYPE);
        TransactionType type = TransactionType.fromValue(typeString);

        return new Category.Builder()
                .setId(CursorUtils.getString(cursor, CategoryEntry.COLUMN_ID))
                .setUserId(CursorUtils.getString(cursor, CategoryEntry.COLUMN_USER_ID))
                .setName(CursorUtils.getString(cursor, CategoryEntry.COLUMN_NAME))
                .setType(type)
                .setIconName(CursorUtils.getString(cursor, CategoryEntry.COLUMN_ICON_NAME))
                .build();
    }
}
