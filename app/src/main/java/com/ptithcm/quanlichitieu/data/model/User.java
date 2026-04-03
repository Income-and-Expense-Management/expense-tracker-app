package com.ptithcm.quanlichitieu.data.model;

/**
 * Model đại diện cho bảng users.
 * UUID được lưu dưới dạng String.
 * Timestamp được lưu dưới dạng long (milliseconds since epoch).
 */
public class User {
    private String id;           // UUID dạng String
    private String fullName;
    private String email;
    private String avatarUrl;
    private String authProvider;
    private long createdAt;      // Timestamp dạng long

    public User() {
    }

    public User(String id, String fullName, String email, String avatarUrl, 
                String authProvider, long createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.authProvider = authProvider;
        this.createdAt = createdAt;
    }

    // Builder pattern cho việc tạo object linh hoạt
    public static class Builder {
        private String id;
        private String fullName;
        private String email;
        private String avatarUrl;
        private String authProvider;
        private long createdAt;

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setFullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
            return this;
        }

        public Builder setAuthProvider(String authProvider) {
            this.authProvider = authProvider;
            return this;
        }

        public Builder setCreatedAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public User build() {
            return new User(id, fullName, email, avatarUrl, authProvider, createdAt);
        }
    }

    // Getters và Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
