package com.ptithcm.quanlichitieu.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.ptithcm.quanlichitieu.data.local.BudgetDatabaseHelper;
import com.ptithcm.quanlichitieu.data.local.DatabaseManager;
import com.ptithcm.quanlichitieu.data.local.dao.CategoryDao;
import com.ptithcm.quanlichitieu.data.local.dao.TransactionDao;
import com.ptithcm.quanlichitieu.data.local.dao.BudgetDao;
import com.ptithcm.quanlichitieu.data.local.token.EncryptedTokenStorage;
import com.ptithcm.quanlichitieu.data.model.Category;
import com.ptithcm.quanlichitieu.data.model.Transaction;
import com.ptithcm.quanlichitieu.data.remote.CategoryApiService;
import com.ptithcm.quanlichitieu.data.remote.TransactionApiService;
import com.ptithcm.quanlichitieu.data.local.token.TokenStorage;

import java.util.List;

/**
 * Repository xử lý data layer cho Category.
 */
public class CategoryRepository {
    private static final String TAG = "CategoryRepository";

    private final CategoryDao categoryDao;
    private final TransactionDao transactionDao;
    private final BudgetDao budgetDao;
    private final CategoryApiService categoryApiService;
    private final TransactionApiService transactionApiService;
    private final DatabaseManager databaseManager;
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public CategoryRepository(Context context) {
        this.context = context;
        this.databaseManager = DatabaseManager.getInstance(context);
        this.categoryDao = databaseManager.getCategoryDao();
        this.transactionDao = databaseManager.getTransactionDao();
        this.budgetDao = databaseManager.getBudgetDao();
        TokenStorage tokenStorage = EncryptedTokenStorage.getInstance(context);
        this.categoryApiService = new CategoryApiService(context, tokenStorage);
        this.transactionApiService = new TransactionApiService(context, tokenStorage);
    }

    public void syncCategories(String userId, Runnable onSuccess) {
        categoryApiService.fetchCategoriesFromSyncPull(userId, new Response.Listener<List<Category>>() {
            @Override
            public void onResponse(List<Category> response) {
                if(response != null) {
                    new Thread(() -> {
                        for(Category category : response) {
                            Category existing = categoryDao.getById(category.getId());
                            if (category.getDeletedAt() != null) {
                                if (existing == null) {
                                    categoryDao.insertFromServer(category);
                                } else if (category.getUpdatedAt() > existing.getUpdatedAt()) {
                                    categoryDao.softDeleteFromServer(category);
                                }
                            } else {
                                if (existing == null) {
                                    categoryDao.insertFromServer(category);
                                } else if (category.getUpdatedAt() > existing.getUpdatedAt()) {
                                    categoryDao.updateFromServer(category);
                                }
                            }
                        }
                        if(onSuccess != null) onSuccess.run();
                    }).start();
                } else {
                    if (onSuccess != null) onSuccess.run();
                }
            }
        }, error -> {
            if (onSuccess != null) onSuccess.run();
        });
    }

    public List<Category> getAllCategoriesForManagement(String userId) {
        return categoryDao.getAllForManagement(userId);
    }

    public boolean addCategory(Category category) {
        String id = categoryDao.insert(category);
        if (id != null) {
            pushCreate(category);
            return true;
        }
        return false;
    }

    public boolean updateCategory(Category category) {
        boolean success = categoryDao.update(category) > 0;
        if (success) pushUpdate(category);
        return success;
    }

    public int getTransactionCount(String categoryId) {
        return getTransactionCount(null, categoryId);
    }

    public int getTransactionCount(String userId, String categoryId) {
        if (categoryId == null) return 0;
        return transactionDao.countByCategoryId(userId, categoryId);
    }

    public boolean deleteCategory(String categoryId) {
        if (categoryId == null) return false;
        boolean success = databaseManager.executeInTransaction(() -> {
            budgetDao.deleteByCategoryId(categoryId);
            categoryDao.delete(categoryId);
        });
        if (success) pushDelete(categoryId);
        return success;
    }

    public boolean deleteCategoryWithTransactions(String categoryId) {
        return deleteCategoryWithTransactions(null, categoryId);
    }

    public boolean deleteCategoryWithTransactions(String userId, String categoryId) {
        if (categoryId == null) return false;

        // 1. Lấy danh sách giao dịch để xóa trên server sau này
        List<Transaction> transactionsToDelete = transactionDao.getByCategoryIdSimple(userId, categoryId);

        // 2. Thực hiện xóa ở Local trong một Transaction
        boolean success = databaseManager.executeInTransaction(() -> {
            transactionDao.deleteByCategoryId(userId, categoryId);
            budgetDao.deleteByCategoryId(categoryId);
            categoryDao.delete(categoryId);
        });

        if (success) {
            // 3. Đẩy lệnh xóa danh mục lên server
            pushDelete(categoryId);
            
            // 4. Đẩy lệnh xóa từng giao dịch lên server
            if (!transactionsToDelete.isEmpty()) {
                new Thread(() -> {
                    for (Transaction t : transactionsToDelete) {
                        transactionApiService.deleteTransaction(t.getId(), null, null);
                    }
                }).start();
            }
        }

        return success;
    }

    private void pushCreate(Category category) {
        mainHandler.post(() -> categoryApiService.createCategory(category, null, null));
    }

    private void pushUpdate(Category category) {
        mainHandler.post(() -> categoryApiService.updateCategory(category, null, null));
    }

    private void pushDelete(String categoryId) {
        mainHandler.post(() -> categoryApiService.deleteCategory(categoryId, null, null));
    }
}
