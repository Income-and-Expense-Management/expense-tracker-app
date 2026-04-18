package com.ptithcm.quanlichitieu.data.model;

/**
 * Model Ä‘áº¡i diá»‡n cho báº£ng wallets.
 * Timestamps Ä‘Æ°á»£c lÆ°u dÆ°á»›i dáº¡ng long (milliseconds since epoch).
 */
public class Wallet {
    private String id;           // UUID
    private String userId;
    private String name;
    private long initialBalance;
    private String currency;
    private String iconId;
    private long createdAt;
    private long updatedAt;
    private Long deletedAt;      

    public Wallet() {
        this.currency = "VND";
    }

    public Wallet(String id, String userId, String name, long initialBalance, 
                  String currency, String iconId, long createdAt, long updatedAt, Long deletedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.initialBalance = initialBalance;
        this.currency = currency != null ? currency : "VND";
        this.iconId = iconId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    // Builder pattern cho viá»‡c táº¡o object linh hoáº¡t
    public static class Builder {
        private String id;
        private String userId;
        private String name;
        private long initialBalance;
        private String currency = "VND";
        private String iconId;
        private long createdAt;
        private long updatedAt;
        private Long deletedAt;

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

        public Builder setDeletedAt(Long deletedAt) {
            this.deletedAt = deletedAt;
            return this;
        }

        public Wallet build() {
            return new Wallet(id, userId, name, initialBalance, currency, 
                             iconId, createdAt, updatedAt, deletedAt);
        }
    }

    // Getters vĂ  Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getInitialBalance() { return initialBalance; }
    public void setInitialBalance(long initialBalance) { this.initialBalance = initialBalance; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getIconId() { return iconId; }
    public void setIconId(String iconId) { this.iconId = iconId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public Long getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Long deletedAt) { this.deletedAt = deletedAt; }
}
