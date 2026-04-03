package com.ptithcm.quanlichitieu.data.local.util;

import android.database.Cursor;

import androidx.annotation.Nullable;

/**
 * CursorUtils - Lớp tiện ích chứa các hàm đọc dữ liệu an toàn từ Cursor.
 * 
 * Xử lý các trường hợp:
 * - Column không tồn tại (index = -1)
 * - Giá trị NULL trong database
 * - Tránh NullPointerException và IndexOutOfBoundsException
 */
public final class CursorUtils {

    // Private constructor để ngăn việc khởi tạo
    private CursorUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Đọc String từ Cursor một cách an toàn.
     * 
     * @param cursor Cursor chứa dữ liệu
     * @param columnName Tên cột cần đọc
     * @return Giá trị String, hoặc null nếu không tồn tại hoặc là NULL
     */
    @Nullable
    public static String getString(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndex(columnName);
        if (index == -1 || cursor.isNull(index)) {
            return null;
        }
        return cursor.getString(index);
    }

    /**
     * Đọc long từ Cursor một cách an toàn.
     * 
     * @param cursor Cursor chứa dữ liệu
     * @param columnName Tên cột cần đọc
     * @return Giá trị long, hoặc 0 nếu không tồn tại hoặc là NULL
     */
    public static long getLong(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndex(columnName);
        if (index == -1 || cursor.isNull(index)) {
            return 0;
        }
        return cursor.getLong(index);
    }

    /**
     * Đọc int từ Cursor một cách an toàn.
     * 
     * @param cursor Cursor chứa dữ liệu
     * @param columnName Tên cột cần đọc
     * @return Giá trị int, hoặc 0 nếu không tồn tại hoặc là NULL
     */
    public static int getInt(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndex(columnName);
        if (index == -1 || cursor.isNull(index)) {
            return 0;
        }
        return cursor.getInt(index);
    }

    /**
     * Đọc double từ Cursor một cách an toàn.
     * 
     * @param cursor Cursor chứa dữ liệu
     * @param columnName Tên cột cần đọc
     * @return Giá trị double, hoặc 0.0 nếu không tồn tại hoặc là NULL
     */
    public static double getDouble(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndex(columnName);
        if (index == -1 || cursor.isNull(index)) {
            return 0.0;
        }
        return cursor.getDouble(index);
    }

    /**
     * Đọc boolean từ Cursor (lưu dạng int 0/1 trong SQLite).
     * 
     * @param cursor Cursor chứa dữ liệu
     * @param columnName Tên cột cần đọc
     * @return true nếu giá trị = 1, false nếu khác
     */
    public static boolean getBoolean(Cursor cursor, String columnName) {
        return getInt(cursor, columnName) == 1;
    }

    /**
     * Đọc byte[] (BLOB) từ Cursor một cách an toàn.
     * 
     * @param cursor Cursor chứa dữ liệu
     * @param columnName Tên cột cần đọc
     * @return Giá trị byte[], hoặc null nếu không tồn tại hoặc là NULL
     */
    @Nullable
    public static byte[] getBlob(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndex(columnName);
        if (index == -1 || cursor.isNull(index)) {
            return null;
        }
        return cursor.getBlob(index);
    }
}
