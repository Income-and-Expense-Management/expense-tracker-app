package com.ptithcm.quanlichitieu.data.model;

public class Transaction {
    private String id;
    private String walletId;
    private String categoryId;
    private long amount;
    private long transactionDate;
    private String note;
    private long createdAt;
    private long updatedAt;
    private Long deletedAt;

    private Category category;
    private String walletName;

    public Transaction() {
    }

    public Transaction(String id, String walletId, String categoryId, long amount,
                       long transactionDate, String note, long createdAt, long updatedAt, Long deletedAt,
                       Category category, String walletName) {
        this.id = id;
        this.walletId = walletId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.note = note;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.category = category;
        this.walletName = walletName;
    }

    public static class Builder {
        private String id;
        private String walletId;
        private String categoryId;
        private long amount;
        private long transactionDate;
        private String note;
        private long createdAt;
        private long updatedAt;
        private Long deletedAt;
        private Category category;
        private String walletName;

        public Builder setId(String id) { this.id = id; return this; }
        public Builder setWalletId(String walletId) { this.walletId = walletId; return this; }
        public Builder setCategoryId(String categoryId) { this.categoryId = categoryId; return this; }
        public Builder setAmount(long amount) { this.amount = amount; return this; }
        public Builder setTransactionDate(long transactionDate) { this.transactionDate = transactionDate; return this; }
        public Builder setNote(String note) { this.note = note; return this; }
        public Builder setCreatedAt(long createdAt) { this.createdAt = createdAt; return this; }
        public Builder setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder setDeletedAt(Long deletedAt) { this.deletedAt = deletedAt; return this; }
        
        public Builder setCategory(Category category) { this.category = category; return this; }
        public Builder setCategoryName(String categoryName) { 
            if (this.category == null) this.category = new Category();
            this.category.setName(categoryName);
            return this; 
        }
        public Builder setCategoryType(TransactionType type) { 
            if (this.category == null) this.category = new Category();
            this.category.setType(type);
            return this; 
        }
        public Builder setIconId(String iconId) { 
            if (this.category == null) this.category = new Category();
            this.category.setIconName(iconId);
            return this; 
        }
        public Builder setWalletName(String walletName) { this.walletName = walletName; return this; }

        public Transaction build() {
            return new Transaction(id, walletId, categoryId, amount, transactionDate, note, createdAt, updatedAt, deletedAt, category, walletName);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
    public long getTransactionDate() { return transactionDate; }
    public void setTransactionDate(long transactionDate) { this.transactionDate = transactionDate; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public Long getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Long deletedAt) { this.deletedAt = deletedAt; }
    
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getWalletName() { return walletName; }
    public void setWalletName(String walletName) { this.walletName = walletName; }

    public TransactionType getType() { return category != null ? category.getType() : null; }
    public String getIconId() { return category != null ? category.getIconName() : null; }
    public String getCategoryName() { return category != null ? category.getName() : null; }
    
    public boolean isExpense() {
        return category != null && category.getType() == TransactionType.EXPENSE;
    }
    
    public boolean isIncome() {
        return category != null && category.getType() == TransactionType.INCOME;
    }
}
