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

    public void setSortOrder(SortOrder order) {
        if (order == null) return;
        sortOrder.setValue(order);
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
        repository.syncCategories(userId, () -> postCategoriesForManagement(userId, generation));
    }

    public void loadCategoriesForManagement(String userId) {
        final int generation = loadGeneration.incrementAndGet();
        new Thread(() -> {
            List<Category> list = repository.getAllCategoriesForManagement(userId);
            postCategoriesIfLatest(list, generation);
        }).start();
        refreshFromServer(userId);
    }

    public void loadCategories(String userId) {
        final int generation = loadGeneration.incrementAndGet();
        new Thread(() -> {
            List<Category> list = repository.getUserCategories(userId);
            postCategoriesIfLatest(list, generation);
        }).start();
        refreshFromServer(userId);
    }

    public void addCategory(String userId, String name, TransactionType type) {
        addCategoryWithIcon(userId, name, type, "ic_item");
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
                // Dùng loadCategoriesForManagement để reload đồng nhất với CategoryFragment:
                // bao gồm cả system categories + user categories
                loadCategoriesForManagement(userId);
            }
        }).start();
    }

    public void updateCategory(String userId, Category category) {
        new Thread(() -> {
            boolean success = repository.updateCategory(category);
            updateResult.postValue(success);
            if (success) {
                // Dùng loadCategoriesForManagement để reload đồng nhất với CategoryFragment
                loadCategoriesForManagement(userId);
            }
        }).start();
    }

    public void deleteCategory(String userId, String categoryId) {
        new Thread(() -> {
            boolean success = repository.deleteCategory(categoryId);
            deleteResult.postValue(success);
            if (success) {
                // Dùng loadCategoriesForManagement để reload đồng nhất với CategoryFragment
                loadCategoriesForManagement(userId);
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
