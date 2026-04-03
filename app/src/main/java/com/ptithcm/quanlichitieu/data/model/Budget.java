package com.ptithcm.quanlichitieu.data.model;

/**
 * Model đại diện cho bảng budgets.
 * start_date và end_date được lưu dưới dạng long (milliseconds since epoch).
 */
public class Budget {
    private String id;           // UUID dạng String
    private String walletId;
    private String categoryId;
    private long targetAmount;   // Số tiền mục tiêu (VND)
    private long startDate;      // Timestamp dạng long
    private long endDate;        // Timestamp dạng long

    public Budget() {
    }

    public Budget(String id, String walletId, String categoryId, 
                  long targetAmount, long startDate, long endDate) {
        this.id = id;
        this.walletId = walletId;
        this.categoryId = categoryId;
        this.targetAmount = targetAmount;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Builder pattern
    public static class Builder {
        private String id;
        private String walletId;
        private String categoryId;
        private long targetAmount;
        private long startDate;
        private long endDate;

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

        public Builder setTargetAmount(long targetAmount) {
            this.targetAmount = targetAmount;
            return this;
        }

        public Builder setStartDate(long startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder setEndDate(long endDate) {
            this.endDate = endDate;
            return this;
        }

        public Budget build() {
            return new Budget(id, walletId, categoryId, targetAmount, startDate, endDate);
        }
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

    public long getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(long targetAmount) {
        this.targetAmount = targetAmount;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }
}
