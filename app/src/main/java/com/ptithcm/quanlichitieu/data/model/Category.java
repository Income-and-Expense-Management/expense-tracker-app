package com.ptithcm.quanlichitieu.data.model;

public class Category {
    private String id;
    private String userId;
    private String name;
    private TransactionType type;
    private String iconName;
    private boolean isActive = true;
    private long createdAt;
    private long updatedAt;
    private Long deletedAt;

    public Category() {}

    public Category(String id, String userId, String name, TransactionType type, String iconName, boolean isActive, long createdAt, long updatedAt, Long deletedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.iconName = iconName;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static class Builder {
        private String id;
        private String userId;
        private String name;
        private TransactionType type;
        private String iconName;
        private boolean isActive = true;
        private long createdAt;
        private long updatedAt;
        private Long deletedAt;

        public Builder setId(String id) { this.id = id; return this; }
        public Builder setUserId(String userId) { this.userId = userId; return this; }
        public Builder setName(String name) { this.name = name; return this; }
        public Builder setType(TransactionType type) { this.type = type; return this; }
        public Builder setIconName(String iconName) { this.iconName = iconName; return this; }
        public Builder setIsActive(boolean isActive) { this.isActive = isActive; return this; }
        public Builder setCreatedAt(long createdAt) { this.createdAt = createdAt; return this; }
        public Builder setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder setDeletedAt(Long deletedAt) { this.deletedAt = deletedAt; return this; }

        public Category build() {
            return new Category(id, userId, name, type, iconName, isActive, createdAt, updatedAt, deletedAt);
        }
    }

    public boolean isSystemCategory() {
        return userId == null;
    }

    // Getters and Setters...
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public Long getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Long deletedAt) { this.deletedAt = deletedAt; }
}
