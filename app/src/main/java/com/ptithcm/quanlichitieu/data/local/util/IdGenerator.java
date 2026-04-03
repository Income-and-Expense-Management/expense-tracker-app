package com.ptithcm.quanlichitieu.data.local.util;

import java.util.UUID;

/**
 * IdGenerator - Lớp tiện ích để tạo ID và timestamp.
 * 
 * Tính năng:
 * - Tạo UUID theo chuẩn RFC 4122
 * - Lấy timestamp hiện tại theo milliseconds
 * - Có thể mở rộng để hỗ trợ ID từ server khi sync
 */
public final class IdGenerator {

    // Private constructor để ngăn việc khởi tạo
    private IdGenerator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Tạo UUID mới cho các entity.
     * Sử dụng UUID version 4 (random).
     * 
     * @return UUID dạng String (ví dụ: "550e8400-e29b-41d4-a716-446655440000")
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Lấy timestamp hiện tại theo milliseconds.
     * Sử dụng cho các trường created_at, updated_at, transaction_date, etc.
     * 
     * @return Timestamp dạng long (milliseconds since epoch)
     */
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * Kiểm tra xem một String có phải là UUID hợp lệ không.
     * 
     * @param uuid String cần kiểm tra
     * @return true nếu là UUID hợp lệ, false nếu không
     */
    public static boolean isValidUUID(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return false;
        }
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
