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

public class CategoryViewModel extends AndroidViewModel {

    private final CategoryRepository repository;
    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private final MutableLiveData<Boolean> addResult = new MutableLiveData<>();

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

    public void resetAddResult() {
        addResult.setValue(null);
    }

    public void loadCategories(String userId) {
        // Run on background thread if needed, currently DAO might be sync
        new Thread(() -> {
            List<Category> list = repository.getUserCategories(userId);
            categories.postValue(list);
        }).start();
    }

    public void addCategory(String userId, String name, TransactionType type) {
        if (name == null || name.trim().isEmpty()) {
            addResult.postValue(false);
            return;
        }

        Category category = new Category.Builder()
                .setUserId(userId)
                .setName(name)
                .setType(type)
                .setIconName("ic_item") // default icon cho custom category
                .build();

        new Thread(() -> {
            boolean success = repository.addCategory(category);
            addResult.postValue(success);
            if (success) {
                loadCategories(userId); // reload
            }
        }).start();
    }
}
