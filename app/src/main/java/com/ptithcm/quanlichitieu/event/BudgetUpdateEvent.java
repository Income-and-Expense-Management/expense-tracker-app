package com.ptithcm.quanlichitieu.event;

/**
 * Event được gửi khi có thay đổi về transaction ảnh hưởng đến budget.
 * 
 * Tuân thủ Single Responsibility Principle (SRP):
 * - Class này chỉ chịu trách nhiệm đóng gói thông tin về sự kiện budget update
 * 
 * Immutable để đảm bảo thread-safety và tránh side-effects.
 */
public class BudgetUpdateEvent {
    
    private final String walletId;
    private final String categoryId;
    private final EventType eventType;
    
    public enum EventType {
        TRANSACTION_ADDED,
        TRANSACTION_UPDATED,
        TRANSACTION_DELETED
    }
    
    private BudgetUpdateEvent(Builder builder) {
        this.walletId = builder.walletId;
        this.categoryId = builder.categoryId;
        this.eventType = builder.eventType;
    }
    
    public String getWalletId() {
        return walletId;
    }
    
    public String getCategoryId() {
        return categoryId;
    }
    
    public EventType getEventType() {
        return eventType;
    }
    
    /**
     * Builder pattern để tạo event một cách linh hoạt.
     * Tuân thủ Open/Closed Principle: dễ mở rộng thêm fields mà không ảnh hưởng code hiện tại.
     */
    public static class Builder {
        private String walletId;
        private String categoryId;
        private EventType eventType = EventType.TRANSACTION_ADDED;
        
        public Builder setWalletId(String walletId) {
            this.walletId = walletId;
            return this;
        }
        
        public Builder setCategoryId(String categoryId) {
            this.categoryId = categoryId;
            return this;
        }
        
        public Builder setEventType(EventType eventType) {
            this.eventType = eventType;
            return this;
        }
        
        public BudgetUpdateEvent build() {
            return new BudgetUpdateEvent(this);
        }
    }
}
