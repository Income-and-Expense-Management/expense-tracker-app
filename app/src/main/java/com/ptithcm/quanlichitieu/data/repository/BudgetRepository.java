package com.ptithcm.quanlichitieu.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ptithcm.quanlichitieu.data.local.DatabaseManager;
import com.ptithcm.quanlichitieu.data.local.dao.BudgetDao;
import com.ptithcm.quanlichitieu.data.local.dao.CategoryDao;
import com.ptithcm.quanlichitieu.data.local.dao.TransactionDao;
import com.ptithcm.quanlichitieu.data.local.dao.WalletDao;
import com.ptithcm.quanlichitieu.data.model.Budget;
import com.ptithcm.quanlichitieu.data.model.Category;
import com.ptithcm.quanlichitieu.data.model.TransactionType;
import com.ptithcm.quanlichitieu.data.model.Wallet;
import com.ptithcm.quanlichitieu.ui.budget.model.BudgetItem;

import java.util.ArrayList;
import java.util.List;

/**
 * BudgetRepository - Repository pattern cho Budget feature.
 * 
 * Vai trò:
 * - Abstraction layer giữa ViewModel và Data Sources (DAO)
 * - Kết hợp dữ liệu từ nhiều nguồn (Budget, Category, Transaction)
 * - Chuẩn bị sẵn cho việc tích hợp remote data source sau này
 * 
 * Tuân thủ Single Responsibility: Chỉ xử lý data access logic cho Budget
 */
public class BudgetRepository {

    private static volatile BudgetRepository instance;

    private final BudgetDao budgetDao;
    private final CategoryDao categoryDao;
    private final TransactionDao transactionDao;
    private final WalletDao walletDao;
    private final DatabaseManager databaseManager;

    private BudgetRepository(@NonNull Context context) {
        this.databaseManager = DatabaseManager.getInstance(context);
        this.budgetDao = databaseManager.getBudgetDao();
        this.categoryDao = databaseManager.getCategoryDao();
        this.transactionDao = databaseManager.getTransactionDao();
        this.walletDao = databaseManager.getWalletDao();
    }

    public static BudgetRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (BudgetRepository.class) {
                if (instance == null) {
                    instance = new BudgetRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    /**
     * Lấy danh sách BudgetItem với thông tin đầy đủ (category name, spent amount).
     * 
     * @param walletId ID của ví đang được chọn
     * @return Danh sách BudgetItem đã tính toán đầy đủ
     */
    public List<BudgetItem> getBudgetItemsForWallet(@NonNull String walletId) {
        List<BudgetItem> items = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        // Lấy tất cả budgets đang active của wallet
        List<Budget> budgets = budgetDao.getActiveBudgets(walletId, currentTime);

        for (Budget budget : budgets) {
            BudgetItem item = createBudgetItem(budget);
            if (item != null) {
                items.add(item);
            }
        }

        return items;
    }

    /**
     * Lấy tất cả budgets của wallet (bao gồm cả hết hạn).
     */
    public List<BudgetItem> getAllBudgetItemsForWallet(@NonNull String walletId) {
        List<BudgetItem> items = new ArrayList<>();
        List<Budget> budgets = budgetDao.getByWalletId(walletId);

        for (Budget budget : budgets) {
            BudgetItem item = createBudgetItem(budget);
            if (item != null) {
                items.add(item);
            }
        }

        return items;
    }

    /**
     * Tạo BudgetItem từ Budget model.
     * Kết hợp thông tin từ Category và tính toán spent amount.
     */
    @Nullable
    private BudgetItem createBudgetItem(@NonNull Budget budget) {
        // Lấy thông tin category
        Category category = categoryDao.getById(budget.getCategoryId());
        if (category == null) {
            return null;
        }

        // Tính tổng số tiền đã chi trong khoảng thời gian budget
        long spentAmount = transactionDao.getTotalAmountByType(
                budget.getWalletId(),
                TransactionType.EXPENSE,
                budget.getStartDate(),
                budget.getEndDate()
        );

        // Tính số tiền đã chi cho category cụ thể
        long categorySpent = getSpentAmountByCategory(
                budget.getWalletId(),
                budget.getCategoryId(),
                budget.getStartDate(),
                budget.getEndDate()
        );

        return new BudgetItem.Builder()
                .setId(budget.getId())
                .setCategoryId(budget.getCategoryId())
                .setCategoryName(category.getName())
                .setCategoryIcon(category.getIconName())
                .setSpent(categorySpent)
                .setLimit(budget.getTargetAmount())
                .setStartDate(budget.getStartDate())
                .setEndDate(budget.getEndDate())
                .setWalletId(budget.getWalletId())
                .setColor(getCategoryColor(category.getIconName()))
                .build();
    }

    /**
     * Tính số tiền đã chi cho một category trong khoảng thời gian.
     */
    private long getSpentAmountByCategory(@NonNull String walletId, @NonNull String categoryId,
                                          long startDate, long endDate) {
        return transactionDao.getTotalAmountByCategory(walletId, categoryId, startDate, endDate);
    }

    /**
     * Lưu budget mới vào database.
     */
    @Nullable
    public String insertBudget(@NonNull Budget budget) {
        return budgetDao.insert(budget);
    }

    /**
     * Cập nhật budget.
     */
    public int updateBudget(@NonNull Budget budget) {
        return budgetDao.update(budget);
    }

    /**
     * Xóa budget.
     */
    public int deleteBudget(@NonNull String budgetId) {
        return budgetDao.delete(budgetId);
    }

    /**
     * Lấy danh sách categories theo loại (EXPENSE cho budget).
     */
    public List<Category> getExpenseCategories(@Nullable String userId) {
        return categoryDao.getByType(userId, TransactionType.EXPENSE);
    }

    /**
     * Lấy danh sách tất cả categories có thể sử dụng.
     */
    public List<Category> getAllAvailableCategories(@Nullable String userId) {
        return categoryDao.getAllAvailable(userId);
    }

    /**
     * Lấy danh sách ví của user.
     */
    public List<Wallet> getWalletsForUser(@Nullable String userId) {
        return walletDao.getByUserId(userId);
    }

    /**
     * Lấy wallet theo ID.
     */
    @Nullable
    public Wallet getWalletById(@NonNull String walletId) {
        List<Wallet> wallets = walletDao.getByUserId(null);
        for (Wallet wallet : wallets) {
            if (wallet.getId().equals(walletId)) {
                return wallet;
            }
        }
        return null;
    }

    /**
     * Tính tổng ngân sách của wallet.
     */
    public long getTotalBudgetForWallet(@NonNull String walletId) {
        long total = 0;
        long currentTime = System.currentTimeMillis();
        List<Budget> budgets = budgetDao.getActiveBudgets(walletId, currentTime);
        for (Budget budget : budgets) {
            total += budget.getTargetAmount();
        }
        return total;
    }

    /**
     * Tính tổng đã chi của wallet trong tháng hiện tại.
     */
    public long getTotalSpentForWallet(@NonNull String walletId, long startDate, long endDate) {
        return transactionDao.getTotalAmountByType(walletId, TransactionType.EXPENSE, startDate, endDate);
    }

    /**
     * Map icon name sang color.
     * TODO: Có thể chuyển sang database hoặc config file
     */
    private String getCategoryColor(String iconName) {
        if (iconName == null) return "#4CAF50";
        
        switch (iconName.toLowerCase()) {
            case "food":
            case "ic_food":
                return "#E91E63";
            case "shopping":
            case "ic_shopping":
                return "#9C27B0";
            case "transport":
            case "ic_transport":
                return "#2196F3";
            case "entertainment":
                return "#FF9800";
            case "health":
                return "#00BCD4";
            case "education":
                return "#3F51B5";
            case "bills":
                return "#F44336";
            default:
                return "#4CAF50";
        }
    }
}
