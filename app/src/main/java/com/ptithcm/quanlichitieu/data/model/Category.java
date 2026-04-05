package com.ptithcm.quanlichitieu.data.model;

/**
 * Model đại diện cho bảng categories.
 * Hỗ trợ cả danh mục hệ thống (user_id = null) và danh mục người dùng tự tạo.
 */
public class Category {
    private String id;           // UUID dạng String
    private String userId;       // null nếu là danh mục mặc định của hệ thống
    private String name;
    private TransactionType type;
    private String iconName;
    private boolean isActive = true;

    public Category() {
    }

    public Category(String id, String userId, String name, TransactionType type, String iconName) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.iconName = iconName;
    }

    public Category(String id, String userId, String name, TransactionType type, String iconName, boolean isActive) {
        this(id, userId, name, type, iconName);
        this.isActive = isActive;
    }

    /**
     * Kiểm tra xem danh mục có phải là danh mục hệ thống không.
     */
    public boolean isSystemCategory() {
        return userId == null;
    }

    // Builder pattern
    public static class Builder {
        private String id;
        private String userId;
        private String name;
        private TransactionType type;
        private String iconName;
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

        public Builder setType(TransactionType type) {
            this.type = type;
            return this;
        }

        public Builder setIconName(String iconName) {
            this.iconName = iconName;
            return this;
        }

        public Builder setIsActive(boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public Category build() {
            return new Category(id, userId, name, type, iconName, isActive);
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

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
