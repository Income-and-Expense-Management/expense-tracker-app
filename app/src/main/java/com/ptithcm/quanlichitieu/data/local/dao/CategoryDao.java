package com.ptithcm.quanlichitieu.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ptithcm.quanlichitieu.data.local.BudgetDatabaseHelper;
import com.ptithcm.quanlichitieu.data.local.contract.DatabaseContract.BudgetEntry;
import com.ptithcm.quanlichitieu.data.local.contract.DatabaseContract.CategoryEntry;
import com.ptithcm.quanlichitieu.data.local.contract.DatabaseContract.TransactionEntry;
import com.ptithcm.quanlichitieu.data.local.util.CursorUtils;
import com.ptithcm.quanlichitieu.utils.IdGenerator;
import com.ptithcm.quanlichitieu.data.model.Category;
import com.ptithcm.quanlichitieu.data.model.TransactionType;

import java.util.ArrayList;
import java.util.List;

/**
 * CategoryDao - Data Access Object cho bảng categories.
 */
public class CategoryDao {

    private static final String TAG = "CategoryDao";
    private final BudgetDatabaseHelper dbHelper;

    public CategoryDao(@NonNull BudgetDatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public String insert(@NonNull Category category) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (category.getId() == null) {
            category.setId(IdGenerator.generateUUID());
        }
        long now = IdGenerator.getCurrentTimestamp();
        ContentValues values = new ContentValues();
        values.put(CategoryEntry.COLUMN_ID, category.getId());
        values.put(CategoryEntry.COLUMN_USER_ID, category.getUserId());
        values.put(CategoryEntry.COLUMN_NAME, category.getName());
        if (category.getType() != null) {
            values.put(CategoryEntry.COLUMN_TYPE, category.getType().getValue());
        }
        values.put(CategoryEntry.COLUMN_ICON_NAME, category.getIconName());
        values.put(CategoryEntry.COLUMN_IS_ACTIVE, category.isActive() ? 1 : 0);
        values.put(CategoryEntry.COLUMN_CREATED_AT, now);
        values.put(CategoryEntry.COLUMN_UPDATED_AT, now);
        if (category.getDeletedAt() != null) {
            values.put(CategoryEntry.COLUMN_DELETED_AT, category.getDeletedAt());
        } else {
            values.putNull(CategoryEntry.COLUMN_DELETED_AT);
        }
        try {
            long result = db.insertOrThrow(CategoryEntry.TABLE_NAME, null, values);
            return category.getId();
        } catch (Exception e) {
            Log.e(TAG, "Failed to insert category", e);
            return null;
        }
    }

    @Nullable
    public String insertFromServer(@NonNull Category category) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Kiểm tra trùng lặp theo Name + Type + UserId để hợp nhất dữ liệu Seed và Server
        String selection;
        String[] selectionArgs;
        if (category.getUserId() == null) {
            selection = CategoryEntry.COLUMN_NAME + " = ? AND " + CategoryEntry.COLUMN_TYPE + " = ? AND " + CategoryEntry.COLUMN_USER_ID + " IS NULL";
            selectionArgs = new String[]{category.getName(), category.getType().getValue()};
        } else {
            selection = CategoryEntry.COLUMN_NAME + " = ? AND " + CategoryEntry.COLUMN_TYPE + " = ? AND " + CategoryEntry.COLUMN_USER_ID + " = ?";
            selectionArgs = new String[]{category.getName(), category.getType().getValue(), category.getUserId()};
        }

        Cursor cursor = db.query(CategoryEntry.TABLE_NAME, new String[]{CategoryEntry.COLUMN_ID}, selection, selectionArgs, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String existingId = cursor.getString(0);
            cursor.close();

            // Nếu ID khác nhau (do Seed dùng ID cứng), cập nhật ID local sang ID server để đồng bộ
            if (!existingId.equals(category.getId())) {
                updateCategoryIdReferences(db, existingId, category.getId());
                
                ContentValues idUpdate = new ContentValues();
                idUpdate.put(CategoryEntry.COLUMN_ID, category.getId());
                db.update(CategoryEntry.TABLE_NAME, idUpdate, CategoryEntry.COLUMN_ID + " = ?", new String[]{existingId});
            }
            
            updateFromServer(category);
            return category.getId();
        }
        if (cursor != null) cursor.close();

        ContentValues values = new ContentValues();
        values.put(CategoryEntry.COLUMN_ID, category.getId());
        values.put(CategoryEntry.COLUMN_USER_ID, category.getUserId());
        values.put(CategoryEntry.COLUMN_NAME, category.getName());
        if (category.getType() != null) {
            values.put(CategoryEntry.COLUMN_TYPE, category.getType().getValue());
        }
        values.put(CategoryEntry.COLUMN_ICON_NAME, category.getIconName());
        values.put(CategoryEntry.COLUMN_IS_ACTIVE, category.isActive() ? 1 : 0);
        values.put(CategoryEntry.COLUMN_CREATED_AT, category.getCreatedAt());
        values.put(CategoryEntry.COLUMN_UPDATED_AT, category.getUpdatedAt());
        if (category.getDeletedAt() != null) {
            values.put(CategoryEntry.COLUMN_DELETED_AT, category.getDeletedAt());
        } else {
            values.putNull(CategoryEntry.COLUMN_DELETED_AT);
        }

        long result = db.insert(CategoryEntry.TABLE_NAME, null, values);
        return result != -1 ? category.getId() : null;
    }

    private void updateCategoryIdReferences(SQLiteDatabase db, String oldId, String newId) {
        ContentValues values = new ContentValues();
        values.put(TransactionEntry.COLUMN_CATEGORY_ID, newId);
        db.update(TransactionEntry.TABLE_NAME, values, TransactionEntry.COLUMN_CATEGORY_ID + " = ?", new String[]{oldId});

        ContentValues budgetValues = new ContentValues();
        budgetValues.put(BudgetEntry.COLUMN_CATEGORY_ID, newId);
        db.update(BudgetEntry.TABLE_NAME, budgetValues, BudgetEntry.COLUMN_CATEGORY_ID + " = ?", new String[]{oldId});
    }

    @Nullable
    public Category getById(@NonNull String categoryId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Category category = null;
        Cursor cursor = db.query(CategoryEntry.TABLE_NAME, null, CategoryEntry.COLUMN_ID + " = ?", new String[]{categoryId}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            category = cursorToCategory(cursor);
        }
        if (cursor != null) cursor.close();
        return category;
    }

    public List<Category> getByType(@Nullable String userId, @NonNull TransactionType type) {
        List<Category> categories = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection;
        String[] selectionArgs;
        if (userId == null) {
            selection = CategoryEntry.COLUMN_TYPE + " = ? AND " + CategoryEntry.COLUMN_USER_ID + " IS NULL AND " + CategoryEntry.COLUMN_DELETED_AT + " IS NULL";
            selectionArgs = new String[]{ type.getValue() };
        } else {
            selection = CategoryEntry.COLUMN_TYPE + " = ? AND (" + CategoryEntry.COLUMN_USER_ID + " IS NULL OR " + CategoryEntry.COLUMN_USER_ID + " = ?) AND " + CategoryEntry.COLUMN_DELETED_AT + " IS NULL";
            selectionArgs = new String[]{ type.getValue(), userId };
        }
        Cursor cursor = db.query(CategoryEntry.TABLE_NAME, null, selection, selectionArgs, null, null, CategoryEntry.COLUMN_NAME + " ASC");
        if (cursor != null && cursor.moveToFirst()) {
            do { categories.add(cursorToCategory(cursor)); } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();
        return categories;
    }

    public List<Category> getAllAvailable(@Nullable String userId) {
        List<Category> categories = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection;
        String[] selectionArgs;
        if (userId == null) {
            selection = CategoryEntry.COLUMN_USER_ID + " IS NULL AND " + CategoryEntry.COLUMN_IS_ACTIVE + " = 1 AND " + CategoryEntry.COLUMN_DELETED_AT + " IS NULL";
            selectionArgs = null;
        } else {
            selection = "(" + CategoryEntry.COLUMN_USER_ID + " IS NULL OR " + CategoryEntry.COLUMN_USER_ID + " = ?) AND " + CategoryEntry.COLUMN_IS_ACTIVE + " = 1 AND " + CategoryEntry.COLUMN_DELETED_AT + " IS NULL";
            selectionArgs = new String[]{ userId };
        }
        Cursor cursor = db.query(CategoryEntry.TABLE_NAME, null, selection, selectionArgs, null, null, CategoryEntry.COLUMN_TYPE + ", " + CategoryEntry.COLUMN_NAME + " ASC");
        if (cursor != null && cursor.moveToFirst()) {
            do { categories.add(cursorToCategory(cursor)); } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();
        return categories;
    }

    public List<Category> getAllForManagement(@Nullable String userId) {
        List<Category> categories = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection;
        String[] selectionArgs;
        if (userId == null) {
            selection = CategoryEntry.COLUMN_USER_ID + " IS NULL AND " + CategoryEntry.COLUMN_DELETED_AT + " IS NULL";
            selectionArgs = null;
        } else {
            selection = "(" + CategoryEntry.COLUMN_USER_ID + " IS NULL OR " + CategoryEntry.COLUMN_USER_ID + " = ?) AND " + CategoryEntry.COLUMN_DELETED_AT + " IS NULL";
            selectionArgs = new String[]{ userId };
        }
        Cursor cursor = db.query(CategoryEntry.TABLE_NAME, null, selection, selectionArgs, null, null, CategoryEntry.COLUMN_TYPE + ", " + CategoryEntry.COLUMN_NAME + " ASC");
        if (cursor != null && cursor.moveToFirst()) {
            do { categories.add(cursorToCategory(cursor)); } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();
        return categories;
    }

    public int update(@NonNull Category category) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long now = IdGenerator.getCurrentTimestamp();
        ContentValues values = new ContentValues();
        if (category.isSystemCategory()) {
            values.put(CategoryEntry.COLUMN_IS_ACTIVE, category.isActive() ? 1 : 0);
            values.put(CategoryEntry.COLUMN_UPDATED_AT, now);
        } else {
            values.put(CategoryEntry.COLUMN_NAME, category.getName());
            if (category.getType() != null) values.put(CategoryEntry.COLUMN_TYPE, category.getType().getValue());
            values.put(CategoryEntry.COLUMN_ICON_NAME, category.getIconName());
            values.put(CategoryEntry.COLUMN_IS_ACTIVE, category.isActive() ? 1 : 0);
            values.put(CategoryEntry.COLUMN_UPDATED_AT, now);
            if (category.getDeletedAt() != null) values.put(CategoryEntry.COLUMN_DELETED_AT, category.getDeletedAt());
            else values.putNull(CategoryEntry.COLUMN_DELETED_AT);
        }
        return db.update(CategoryEntry.TABLE_NAME, values, CategoryEntry.COLUMN_ID + " = ?", new String[]{category.getId()});
    }

    public int updateFromServer(@NonNull Category category) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(CategoryEntry.COLUMN_NAME, category.getName());
        if (category.getType() != null) values.put(CategoryEntry.COLUMN_TYPE, category.getType().getValue());
        values.put(CategoryEntry.COLUMN_ICON_NAME, category.getIconName());
        values.put(CategoryEntry.COLUMN_IS_ACTIVE, category.isActive() ? 1 : 0);
        values.put(CategoryEntry.COLUMN_UPDATED_AT, category.getUpdatedAt() > 0 ? category.getUpdatedAt() : IdGenerator.getCurrentTimestamp());
        if (category.getDeletedAt() != null) values.put(CategoryEntry.COLUMN_DELETED_AT, category.getDeletedAt());
        else values.putNull(CategoryEntry.COLUMN_DELETED_AT);
        return db.update(CategoryEntry.TABLE_NAME, values, CategoryEntry.COLUMN_ID + " = ?", new String[]{category.getId()});
    }

    public int softDeleteFromServer(@NonNull Category category) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(CategoryEntry.COLUMN_IS_ACTIVE, 0);
        values.put(CategoryEntry.COLUMN_UPDATED_AT, category.getUpdatedAt() > 0 ? category.getUpdatedAt() : IdGenerator.getCurrentTimestamp());
        values.put(CategoryEntry.COLUMN_DELETED_AT, category.getDeletedAt() != null ? category.getDeletedAt() : IdGenerator.getCurrentTimestamp());
        return db.update(CategoryEntry.TABLE_NAME, values, CategoryEntry.COLUMN_ID + " = ?", new String[]{category.getId()});
    }

    public int delete(@NonNull String categoryId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(CategoryEntry.COLUMN_DELETED_AT, IdGenerator.getCurrentTimestamp());
        values.put(CategoryEntry.COLUMN_UPDATED_AT, IdGenerator.getCurrentTimestamp());
        return db.update(CategoryEntry.TABLE_NAME, values, CategoryEntry.COLUMN_ID + " = ? AND " + CategoryEntry.COLUMN_USER_ID + " IS NOT NULL", new String[]{categoryId});
    }

    private Category cursorToCategory(Cursor cursor) {
        String typeString = CursorUtils.getString(cursor, CategoryEntry.COLUMN_TYPE);
        TransactionType type = TransactionType.fromValue(typeString);
        int isActiveInt = cursor.getInt(cursor.getColumnIndex(CategoryEntry.COLUMN_IS_ACTIVE));
        return new Category.Builder()
                .setId(CursorUtils.getString(cursor, CategoryEntry.COLUMN_ID))
                .setUserId(CursorUtils.getString(cursor, CategoryEntry.COLUMN_USER_ID))
                .setName(CursorUtils.getString(cursor, CategoryEntry.COLUMN_NAME))
                .setType(type)
                .setIconName(CursorUtils.getString(cursor, CategoryEntry.COLUMN_ICON_NAME))
                .setIsActive(isActiveInt == 1)
                .setCreatedAt(CursorUtils.getLong(cursor, CategoryEntry.COLUMN_CREATED_AT))
                .setUpdatedAt(CursorUtils.getLong(cursor, CategoryEntry.COLUMN_UPDATED_AT))
                .setDeletedAt(CursorUtils.getLong(cursor, CategoryEntry.COLUMN_DELETED_AT) == 0 ? null : CursorUtils.getLong(cursor, CategoryEntry.COLUMN_DELETED_AT))
                .build();
    }
}
