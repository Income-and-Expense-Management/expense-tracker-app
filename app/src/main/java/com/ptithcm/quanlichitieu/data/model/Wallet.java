package com.ptithcm.quanlichitieu.data.model;

/**
 * Model đại diện cho bảng wallets.
 * Boolean is_active được lưu dưới dạng int (0/1) trong SQLite.
 * Timestamps được lưu dưới dạng long (milliseconds since epoch).
 */
public class Wallet {
    private String id;           // UUID dạng String
    private String userId;
    private String name;
    private long initialBalance; // Số dư ban đầu (VND, dạng INTEGER)
    private String currency;
    private String iconId;       // Tên tài nguyên icon trong drawable
    private long createdAt;      // Timestamp dạng long
    private long updatedAt;      // Timestamp dạng long
    private boolean isActive;    // Sẽ được convert sang 0/1 khi lưu SQLite

    public Wallet() {
        this.currency = "VND";
        this.isActive = true;
    }

    public Wallet(String id, String userId, String name, long initialBalance, 
                  String currency, String iconId, long createdAt, long updatedAt, boolean isActive) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.initialBalance = initialBalance;
        this.currency = currency != null ? currency : "VND";
        this.iconId = iconId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isActive = isActive;
    }

    // Builder pattern cho việc tạo object linh hoạt
    public static class Builder {
        private String id;
        private String userId;
        private String name;
        private long initialBalance;
        private String currency = "VND";
        private String iconId;
        private long createdAt;
        private long updatedAt;
        private boolean isActive = true;

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setInitialBalance(long initialBalance) {
            this.initialBalance = initialBalance;
            return this;
        }

        public Builder setCurrency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder setIconId(String iconId) {
            this.iconId = iconId;
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

        public Builder setIsActive(boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public Wallet build() {
            return new Wallet(id, userId, name, initialBalance, currency, 
                             iconId, createdAt, updatedAt, isActive);
        }
    }

    // Getters và Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(long initialBalance) {
        this.initialBalance = initialBalance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getIconId() {
        return iconId;
    }

    public void setIconId(String iconId) {
        this.iconId = iconId;
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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    /**
     * Chuyển đổi boolean isActive thành int để lưu vào SQLite.
     */
    public int getIsActiveAsInt() {
        return isActive ? 1 : 0;
    }

    /**
     * Thiết lập isActive từ giá trị int đọc từ SQLite.
     */
    public void setActiveFromInt(int value) {
        this.isActive = value == 1;
    }
}
