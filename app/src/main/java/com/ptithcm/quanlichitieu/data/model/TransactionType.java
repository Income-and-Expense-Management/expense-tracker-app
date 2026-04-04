package com.ptithcm.quanlichitieu.data.model;

/**
 * Enum đại diện cho loại giao dịch: Thu nhập hoặc Chi tiêu.
 * Sử dụng String value để lưu trữ vào SQLite, đảm bảo tương thích với schema.
 */
public enum TransactionType {
    INCOME("INCOME"),
    EXPENSE("EXPENSE"),
    LOAN("LOAN");

    private final String value;

    TransactionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Parse String từ database thành Enum.
     * @param value Giá trị String từ SQLite
     * @return TransactionType tương ứng, null nếu không hợp lệ
     */
    public static TransactionType fromValue(String value) {
        if (value == null) return null;
        for (TransactionType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
