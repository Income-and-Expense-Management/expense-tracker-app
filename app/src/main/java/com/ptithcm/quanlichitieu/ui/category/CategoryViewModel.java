package com.ptithcm.quanlichitieu.ui.category;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.quanlichitieu.data.model.Category;
import com.ptithcm.quanlichitieu.data.model.Transaction;
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
    private final MutableLiveData<Integer> transactionCount = new MutableLiveData<>();

    // Sort order state exposed to UI
    public enum SortOrder {
        A_TO_Z,
        Z_TO_A
    }

    private final MutableLiveData<SortOrder> sortOrder = new MutableLiveData<>(SortOrder.A_TO_Z);
    private final AtomicInteger loadGeneration = new AtomicInteger(0);

    public CategoryViewModel(@NonNull Application application) {
        super(application);
        repository = new CategoryRepository(application);
    }

    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    public LiveData<Boolean> getAddResult() {
        return addResult;
    }

    public LiveData<Boolean> getUpdateResult() {
        return updateResult;
    }

    public LiveData<Boolean> getDeleteResult() {
        return deleteResult;
    }

    public LiveData<SortOrder> getSortOrder() { return sortOrder; }

    public LiveData<Integer> getTransactionCountResult() {
        return transactionCount;
    }

    public void toggleSortOrder() {
        SortOrder current = sortOrder.getValue();
        if (current == SortOrder.Z_TO_A) {
            sortOrder.setValue(SortOrder.A_TO_Z);
        } else {
            sortOrder.setValue(SortOrder.Z_TO_A);
        }
    }

    public void resetAddResult() {
        addResult.setValue(null);
    }

    public void resetUpdateResult() {
        updateResult.setValue(null);
    }

    public void resetDeleteResult() {
        deleteResult.setValue(null);
    }

    public void syncCategories(String userId, Runnable onSuccess) {
        repository.syncCategories(userId, onSuccess);
    }

    public void refreshFromServer(String userId) {
        final int generation = loadGeneration.incrementAndGet();
        refreshFromServerWithGeneration(userId, generation);
    }

    private void refreshFromServerWithGeneration(String userId, int generation) {
        repository.syncCategories(userId, () -> postCategoriesForManagement(userId, generation));
    }

    public void loadCategoriesForManagement(String userId) {
        final int generation = loadGeneration.incrementAndGet();
        new Thread(() -> {
            List<Category> list = repository.getAllCategoriesForManagement(userId);
            postCategoriesIfLatest(list, generation);
            refreshFromServerWithGeneration(userId, generation);
        }).start();
    }

    public void addCategoryWithIcon(String userId, String name, TransactionType type, String iconName) {
        if (name == null || name.trim().isEmpty()) {
            addResult.postValue(false);
            return;
        }

        Category category = new Category.Builder()
                .setUserId(userId)
                .setName(name)
                .setType(type)
                .setIconName(iconName)
                .build();

        new Thread(() -> {
            boolean success = repository.addCategory(category);
            addResult.postValue(success);
            if (success) {
                loadCategoriesForManagement(userId);
            }
        }).start();
    }

    public void updateCategory(String userId, Category category) {
        new Thread(() -> {
            boolean success = repository.updateCategory(category);
            updateResult.postValue(success);
            if (success) {
                loadCategoriesForManagement(userId);
            }
        }).start();
    }

    public void checkTransactionCount(String userId, String categoryId) {
        transactionCount.postValue(null); // Reset count before checking
        new Thread(() -> {
            int count = repository.getTransactionCount(userId, categoryId);
            transactionCount.postValue(count);
        }).start();
    }

    public void deleteCategory(String userId, String categoryId) {
        new Thread(() -> {
            boolean success = repository.deleteCategory(categoryId);
            deleteResult.postValue(success);
            if (success) {
                List<Category> updatedList = repository.getAllCategoriesForManagement(userId);
                categories.postValue(updatedList);
                repository.syncCategories(userId, null);
            }
        }).start();
    }

    public void deleteCategoryWithTransactions(String userId, String categoryId) {
        new Thread(() -> {
            boolean success = repository.deleteCategoryWithTransactions(userId, categoryId);
            deleteResult.postValue(success);
            if (success) {
                List<Category> updatedList = repository.getAllCategoriesForManagement(userId);
                categories.postValue(updatedList);
                repository.syncCategories(userId, null);
            }
        }).start();
    }

    private void postCategoriesForManagement(String userId, int generation) {
        new Thread(() -> {
            List<Category> list = repository.getAllCategoriesForManagement(userId);
            postCategoriesIfLatest(list, generation);
        }).start();
    }

    private void postCategoriesIfLatest(List<Category> list, int generation) {
        if (generation == loadGeneration.get()) {
            categories.postValue(list);
        }
    }
}
