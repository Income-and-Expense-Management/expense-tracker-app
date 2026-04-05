package com.ptithcm.quanlichitieu.event;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * EventBus sử dụng LiveData để gửi events giữa các components.
 * 
 * Tuân thủ các nguyên tắc SOLID:
 * - Single Responsibility: Chỉ chịu trách nhiệm distribute events
 * - Dependency Inversion: Components phụ thuộc vào abstraction (LiveData) thay vì concrete implementation
 * - Singleton pattern: Đảm bảo chỉ có một instance trong toàn app
 * 
 * Thread-safe với Double-Check Locking.
 */
public class EventBus {
    
    private static volatile EventBus instance;
    
    private final MutableLiveData<BudgetUpdateEvent> budgetUpdateEvent = new MutableLiveData<>();
    
    private EventBus() {
        // Private constructor để enforce Singleton pattern
    }
    
    /**
     * Thread-safe Singleton implementation với Double-Check Locking.
     */
    public static EventBus getInstance() {
        if (instance == null) {
            synchronized (EventBus.class) {
                if (instance == null) {
                    instance = new EventBus();
                }
            }
        }
        return instance;
    }
    
    /**
     * Gửi event khi có budget update.
     * 
     * @param event BudgetUpdateEvent chứa thông tin về update
     */
    public void postBudgetUpdate(@NonNull BudgetUpdateEvent event) {
        budgetUpdateEvent.postValue(event);
    }
    
    /**
     * Subscribe để nhận budget update events.
     * 
     * @return LiveData để observe
     */
    public LiveData<BudgetUpdateEvent> getBudgetUpdateEvent() {
        return budgetUpdateEvent;
    }
    
    /**
     * Clear event sau khi đã xử lý.
     * Tránh bug LiveData emit lại giá trị cũ cho observer mới.
     */
    public void clearBudgetUpdateEvent() {
        budgetUpdateEvent.postValue(null);
    }
}
