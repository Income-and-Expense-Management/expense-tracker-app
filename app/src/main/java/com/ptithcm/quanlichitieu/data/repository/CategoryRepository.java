package com.ptithcm.quanlichitieu.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.ptithcm.quanlichitieu.data.local.BudgetDatabaseHelper;
import com.ptithcm.quanlichitieu.data.local.dao.CategoryDao;
import com.ptithcm.quanlichitieu.data.local.token.EncryptedTokenStorage;
import com.ptithcm.quanlichitieu.data.model.Category;
import com.ptithcm.quanlichitieu.data.remote.CategoryApiService;
import com.ptithcm.quanlichitieu.data.local.token.TokenStorage;

import java.util.List;

/**
 * Repository xử lý data layer cho Category.
 */
public class CategoryRepository {
    private static final String TAG = "CategoryRepository";

    private final CategoryDao categoryDao;
    private final CategoryApiService categoryApiService;
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public CategoryRepository(Context context) {
        this.context = context;
        BudgetDatabaseHelper dbHelper = BudgetDatabaseHelper.getInstance(context);
        categoryDao = new CategoryDao(dbHelper);
        TokenStorage tokenStorage = EncryptedTokenStorage.getInstance(context);
        categoryApiService = new CategoryApiService(context, tokenStorage);
    }

    public void syncCategories(String userId, Runnable onSuccess) {
        categoryApiService.fetchCategories(context, new Response.Listener<List<Category>>() {
            @Override
            public void onResponse(List<Category> response) {
                if(response != null) {
                    for(Category category : response) {
                        Category existing = categoryDao.getById(category.getId());
                        if(existing != null) {
                            categoryDao.update(category);
                        } else {
                            categoryDao.insert(category);
                        }
                    }
                    Log.d(TAG, "Categories synced successfully.");
                    if(onSuccess != null) {
                        onSuccess.run();
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Error syncing categories", error);
            }
        });
    }

    public List<Category> getAllCategoriesForManagement(String userId) {
        return categoryDao.getAllForManagement(userId);
    }

    public List<Category> getUserCategories(String userId) {
        return categoryDao.getAllAvailable(userId);
    }

    public boolean addCategory(Category category) {
        String id = categoryDao.insert(category);
        boolean success = id != null;
        if (success) {
            pushCreate(category);
        }
        return success;
    }

    public boolean updateCategory(Category category) {
        boolean success = categoryDao.update(category) > 0;
        if (success) {
            pushUpdate(category);
        }
        return success;
    }

    public boolean deleteCategory(String categoryId) {
        boolean success = categoryDao.delete(categoryId) > 0;
        if (success) {
            pushDelete(categoryId);
        }
        return success;
    }

    private void pushCreate(Category category) {
        mainHandler.post(() -> categoryApiService.createCategory(
                category,
                response -> Log.d(TAG, "pushCreate: Server confirmed category creation"),
                error -> Log.e(TAG, "pushCreate: Server error", error)
        ));
    }

    private void pushUpdate(Category category) {
        mainHandler.post(() -> categoryApiService.updateCategory(
                category,
                response -> Log.d(TAG, "pushUpdate: Server confirmed category update"),
                error -> Log.e(TAG, "pushUpdate: Server error", error)
        ));
    }

    private void pushDelete(String categoryId) {
        mainHandler.post(() -> categoryApiService.deleteCategory(
                categoryId,
                response -> Log.d(TAG, "pushDelete: Server confirmed category deletion"),
                error -> Log.e(TAG, "pushDelete: Server error", error)
        ));
    }
}
