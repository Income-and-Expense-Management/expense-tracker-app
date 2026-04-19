package com.ptithcm.quanlichitieu.data.model;

public class Budget {
    private String id;           
    private String walletId;
    private String categoryId;
    private long targetAmount;   
    private long startDate;      
    private long endDate;        
    private long createdAt;
    private long updatedAt;
    private Long deletedAt;

    public Budget() {}

    public Budget(String id, String walletId, String categoryId, 
                  long targetAmount, long startDate, long endDate,
                  long createdAt, long updatedAt, Long deletedAt) {
        this.id = id;
        this.walletId = walletId;
        this.categoryId = categoryId;
        this.targetAmount = targetAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static class Builder {
        private String id;
        private String walletId;
        private String categoryId;
        private long targetAmount;
        private long startDate;
        private long endDate;
        private long createdAt;
        private long updatedAt;
        private Long deletedAt;

        public Builder setId(String id) { this.id = id; return this; }
        public Builder setWalletId(String walletId) { this.walletId = walletId; return this; }
        public Builder setCategoryId(String categoryId) { this.categoryId = categoryId; return this; }
        public Builder setTargetAmount(long targetAmount) { this.targetAmount = targetAmount; return this; }
        public Builder setStartDate(long startDate) { this.startDate = startDate; return this; }
        public Builder setEndDate(long endDate) { this.endDate = endDate; return this; }
        public Builder setCreatedAt(long createdAt) { this.createdAt = createdAt; return this; }
        public Builder setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder setDeletedAt(Long deletedAt) { this.deletedAt = deletedAt; return this; }

        public Budget build() {
            return new Budget(id, walletId, categoryId, targetAmount, startDate, endDate, createdAt, updatedAt, deletedAt);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public long getTargetAmount() { return targetAmount; }
    public void setTargetAmount(long targetAmount) { this.targetAmount = targetAmount; }
    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }
    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public Long getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Long deletedAt) { this.deletedAt = deletedAt; }
}
