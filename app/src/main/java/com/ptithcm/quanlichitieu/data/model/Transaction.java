package com.ptithcm.quanlichitieu.data.model;

/**
 * Model đại diện cho bảng transactions.
 * Tất cả các timestamps được lưu dưới dạng long (milliseconds since epoch).
 * TransactionType enum sẽ được convert sang String khi lưu vào SQLite.
 */
public class Transaction {
    private String id;               // UUID dạng String
    private String walletId;
    private String categoryId;
    private long amount;             // Số tiền (VND, dạng INTEGER)
    private TransactionType type;    // INCOME hoặc EXPENSE
    private long transactionDate;    // Ngày giao dịch (timestamp)
    private String iconId;           // Tên tài nguyên icon
    private String note;
    private long createdAt;          // Timestamp dạng long
    private long updatedAt;          // Timestamp dạng long

    // Các trường bổ sung cho JOIN query (không lưu trong DB)
    private String categoryName;     // Tên category từ JOIN
    private String walletName;       // Tên wallet từ JOIN

    public Transaction() {
    }

    public Transaction(String id, String walletId, String categoryId, long amount,
                       TransactionType type, long transactionDate, String iconId,
                       String note, long createdAt, long updatedAt) {
        this.id = id;
        this.walletId = walletId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.type = type;
        this.transactionDate = transactionDate;
        this.iconId = iconId;
        this.note = note;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Builder pattern
    public static class Builder {
        private String id;
        private String walletId;
        private String categoryId;
        private long amount;
        private TransactionType type;
        private long transactionDate;
        private String iconId;
        private String note;
        private long createdAt;
        private long updatedAt;
        private String categoryName;
        private String walletName;

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setWalletId(String walletId) {
            this.walletId = walletId;
            return this;
        }

        public Builder setCategoryId(String categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public Builder setAmount(long amount) {
            this.amount = amount;
            return this;
        }

        public Builder setType(TransactionType type) {
            this.type = type;
            return this;
        }

        public Builder setTransactionDate(long transactionDate) {
            this.transactionDate = transactionDate;
            return this;
        }

        public Builder setIconId(String iconId) {
            this.iconId = iconId;
            return this;
        }

        public Builder setNote(String note) {
            this.note = note;
            return this;
        }

        public Builder setCreatedAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder setUpdatedAt(long updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder setCategoryName(String categoryName) {
            this.categoryName = categoryName;
            return this;
        }

        public Builder setWalletName(String walletName) {
            this.walletName = walletName;
            return this;
        }

        public Transaction build() {
            Transaction t = new Transaction(id, walletId, categoryId, amount, type,
                    transactionDate, iconId, note, createdAt, updatedAt);
            t.setCategoryName(categoryName);
            t.setWalletName(walletName);
            return t;
        }
    }

    // Utility methods
    public boolean isExpense() {
        return type == TransactionType.EXPENSE;
    }

    public boolean isIncome() {
        return type == TransactionType.INCOME;
    }

    // Getters và Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public long getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(long transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getIconId() {
        return iconId;
    }

    public void setIconId(String iconId) {
        this.iconId = iconId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getWalletName() {
        return walletName;
    }

    public void setWalletName(String walletName) {
        this.walletName = walletName;
    }
}
