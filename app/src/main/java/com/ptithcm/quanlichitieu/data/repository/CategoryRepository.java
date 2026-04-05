package com.ptithcm.quanlichitieu.data.repository;

import android.content.Context;

import com.ptithcm.quanlichitieu.data.local.BudgetDatabaseHelper;
import com.ptithcm.quanlichitieu.data.local.dao.CategoryDao;
import com.ptithcm.quanlichitieu.data.model.Category;

import java.util.List;

/**
 * Repository xử lý data layer cho Category.
 */
public class CategoryRepository {
    private final CategoryDao categoryDao;

    public CategoryRepository(Context context) {
        BudgetDatabaseHelper dbHelper = BudgetDatabaseHelper.getInstance(context);
        categoryDao = new CategoryDao(dbHelper);
    }

    public List<Category> getUserCategories(String userId) {
        return categoryDao.getAllAvailable(userId);
    }

    public boolean addCategory(Category category) {
        return categoryDao.insert(category) != null;
    }

    public boolean updateCategory(Category category) {
        return categoryDao.update(category) > 0;
    }

    public boolean deleteCategory(String categoryId) {
        return categoryDao.delete(categoryId) > 0;
    }
}
