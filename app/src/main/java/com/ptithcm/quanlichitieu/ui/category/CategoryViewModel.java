package com.ptithcm.quanlichitieu.ui.category;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.ptithcm.quanlichitieu.data.model.Category;
import com.ptithcm.quanlichitieu.data.model.TransactionType;
import com.ptithcm.quanlichitieu.data.repository.CategoryRepository;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CategoryViewModel extends AndroidViewModel {
    private final CategoryRepository repository;
    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private final MutableLiveData<Boolean> addResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> updateResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleteResult = new MutableLiveData<>();
    private final MutableLiveData<UsageInfo> usageInfo = new MutableLiveData<>();
    private final MutableLiveData<SortOrder> sortOrder = new MutableLiveData<>(SortOrder.A_TO_Z);
    private final AtomicInteger loadGeneration = new AtomicInteger(0);

    public static class UsageInfo {
        public final String categoryId;
        public final int transactionCount;
        public UsageInfo(String categoryId, int transactionCount) {
            this.categoryId = categoryId;
            this.transactionCount = transactionCount;
        }
    }

    public enum SortOrder { A_TO_Z, Z_TO_A }

    public CategoryViewModel(@NonNull Application application) {
        super(application);
        repository = new CategoryRepository(application);
    }

    public LiveData<List<Category>> getCategories() { return categories; }
    public LiveData<Boolean> getAddResult() { return addResult; }
    public LiveData<Boolean> getUpdateResult() { return updateResult; }
    public LiveData<Boolean> getDeleteResult() { return deleteResult; }
    public LiveData<UsageInfo> getUsageInfo() { return usageInfo; }
    public LiveData<SortOrder> getSortOrder() { return sortOrder; }

    public void toggleSortOrder() {
        SortOrder current = sortOrder.getValue();
        sortOrder.setValue(current == SortOrder.Z_TO_A ? SortOrder.A_TO_Z : SortOrder.Z_TO_A);
    }

    public void resetResults() {
        addResult.setValue(null); updateResult.setValue(null);
        deleteResult.setValue(null); usageInfo.setValue(null);
    }

    public void resetAddResult() { addResult.setValue(null); }
    public void resetUpdateResult() { updateResult.setValue(null); }
    public void resetDeleteResult() { deleteResult.setValue(null); }

    public void loadCategoriesForManagement(String userId) {
        final int gen = loadGeneration.incrementAndGet();
        new Thread(() -> {
            List<Category> list = repository.getAllCategoriesForManagement(userId);
            if (gen == loadGeneration.get()) categories.postValue(list);
            repository.syncCategories(userId, () -> {
                new Thread(() -> {
                    List<Category> updated = repository.getAllCategoriesForManagement(userId);
                    if (gen == loadGeneration.get()) categories.postValue(updated);
                }).start();
            });
        }).start();
    }

    public void refreshFromServer(String userId) {
        if (userId == null) return;
        repository.syncCategories(userId, () -> new Thread(() ->
                categories.postValue(repository.getAllCategoriesForManagement(userId))).start());
    }

    public void addCategoryWithIcon(String userId, String name, TransactionType type, String icon) {
        new Thread(() -> {
            Category c = new Category.Builder().setUserId(userId).setName(name).setType(type).setIconName(icon).build();
            boolean success = repository.addCategory(c);
            addResult.postValue(success);
            if (success) loadCategoriesForManagement(userId);
        }).start();
    }

    public void updateCategory(String userId, Category c) {
        new Thread(() -> {
            boolean success = repository.updateCategory(c);
            updateResult.postValue(success);
            if (success) loadCategoriesForManagement(userId);
        }).start();
    }

    public void checkCategoryUsage(String userId, String categoryId) {
        new Thread(() -> {
            int count = repository.getTransactionCount(categoryId);
            if (count > 0) usageInfo.postValue(new UsageInfo(categoryId, count));
            else performDelete(userId, categoryId, false);
        }).start();
    }

    public void reassignAndDelete(String userId, String oldId, String newId) {
        new Thread(() -> {
            repository.reassignTransactions(oldId, newId);
            performDelete(userId, oldId, false);
        }).start();
    }

    public void deleteCategoryAndTransactions(String userId, String categoryId) {
        new Thread(() -> performDelete(userId, categoryId, true)).start();
    }

    private void performDelete(String userId, String categoryId, boolean deleteTransactions) {
        boolean success = deleteTransactions ? repository.deleteCategoryAndTransactions(categoryId) : repository.deleteCategory(categoryId);
        deleteResult.postValue(success);
        if (success) loadCategoriesForManagement(userId);
    }
}