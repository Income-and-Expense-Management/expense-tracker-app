package com.ptithcm.quanlichitieu.ui.budget;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.quanlichitieu.data.local.token.EncryptedTokenStorage;
import com.ptithcm.quanlichitieu.data.local.token.TokenStorage;
import com.ptithcm.quanlichitieu.data.model.Budget;
import com.ptithcm.quanlichitieu.data.model.Category;
import com.ptithcm.quanlichitieu.data.model.Wallet;
import com.ptithcm.quanlichitieu.data.repository.BudgetRepository;
import com.ptithcm.quanlichitieu.ui.budget.model.BudgetItem;

import android.content.SharedPreferences;

import java.util.Calendar;
import java.util.List;

/**
 * BudgetViewModel - ViewModel cho BudgetFragment.
 * 
 * Vai trò:
 * - Quản lý state của UI
 * - Xử lý business logic
 * - Cung cấp data cho Fragment qua LiveData
 * 
 * Tuân thủ MVVM pattern: Fragment chỉ observe và hiển thị,
 * ViewModel xử lý logic và giữ state.
 */
public class BudgetViewModel extends AndroidViewModel {

    private final BudgetRepository repository;

    // LiveData cho UI
    private final MutableLiveData<List<BudgetItem>> budgetItems = new MutableLiveData<>();
    private final MutableLiveData<Wallet> selectedWallet = new MutableLiveData<>();
    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private final MutableLiveData<List<Wallet>> wallets = new MutableLiveData<>();
    private final MutableLiveData<BudgetSummary> budgetSummary = new MutableLiveData<>();
    private final MutableLiveData<OperationResult> operationResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    @Nullable
    private String currentUserId;

    public BudgetViewModel(@NonNull Application application) {
        super(application);
        this.repository = BudgetRepository.getInstance(application);
        TokenStorage tokenStorage = EncryptedTokenStorage.getInstance(application);
        this.currentUserId = tokenStorage.getUserId();
    }

    // ==================== GETTERS cho LiveData ====================

    public LiveData<List<BudgetItem>> getBudgetItems() {
        return budgetItems;
    }

    public LiveData<Wallet> getSelectedWallet() {
        return selectedWallet;
    }

    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    public LiveData<List<Wallet>> getWallets() {
        return wallets;
    }

    public LiveData<BudgetSummary> getBudgetSummary() {
        return budgetSummary;
    }

    public LiveData<OperationResult> getOperationResult() {
        return operationResult;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    // ==================== SETTERS ====================

    public void setCurrentUserId(@Nullable String userId) {
        this.currentUserId = userId;
    }

    /**
     * Clear operation result - gọi sau khi đã xử lý xong result.
     * Tránh bug LiveData emit lại giá trị cũ khi observer mới đăng ký.
     */
    public void clearOperationResult() {
        operationResult.setValue(null);
    }

    // ==================== DATA LOADING ====================

    /**
     * Load danh sách ví của user.
     */
    /**
     * Load danh sách ví của user.
     */
    public void loadWallets() {
        new Thread(() -> {
            List<Wallet> walletList = repository.getWalletsForUser(currentUserId);
            wallets.postValue(walletList);

            if (walletList == null || walletList.isEmpty()) {
                selectedWallet.postValue(null);
                budgetItems.postValue(new java.util.ArrayList<>());
                budgetSummary.postValue(new BudgetSummary(0, 0, 0, 0, 0));
                return;
            }

            SharedPreferences prefs = getApplication().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
            // Chuẩn hóa key nhất quán với WalletViewModel và AddTransactionViewModel:
            // "active_wallet_id_" + userId (hoặc "default" khi chưa đăng nhập)
            String userKey = (currentUserId != null && !currentUserId.trim().isEmpty()) ? currentUserId : "default";
            String savedWalletId = prefs.getString("active_wallet_id_" + userKey, null);

            Wallet activeWallet = null;
            if (savedWalletId != null) {
                for (Wallet w : walletList) {
                    if (w.getId().equals(savedWalletId)) {
                        activeWallet = w;
                        break;
                    }
                }
            }

            if (activeWallet == null) {
                activeWallet = walletList.get(0);
                prefs.edit().putString("active_wallet_id_" + userKey, activeWallet.getId()).apply();
            }

            selectedWallet.postValue(activeWallet);
            loadBudgetsForWallet(activeWallet.getId());
        }).start();
    }


    /**
     * Chọn ví và load dữ liệu budget.
     */
    public void selectWallet(@NonNull Wallet wallet) {
        SharedPreferences prefs = getApplication().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
        String userKey = (currentUserId != null && !currentUserId.trim().isEmpty()) ? currentUserId : "default";
        prefs.edit().putString("active_wallet_id_" + userKey, wallet.getId()).apply();

        selectedWallet.setValue(wallet);
        loadBudgetsForWallet(wallet.getId());
    }

    /**
     * Load danh sách budget và tính toán summary.
     */
    public void loadBudgetsForWallet(@NonNull String walletId) {
        isLoading.postValue(true);

        new Thread(() -> {
            List<BudgetItem> items = repository.getBudgetItemsForWallet(walletId);
            budgetItems.postValue(items);
            calculateBudgetSummary(walletId, items);

            // Fetch từ server
            repository.fetchFromServer(() -> {
                new Thread(() -> {
                    List<BudgetItem> syncedItems = repository.getBudgetItemsForWallet(walletId);
                    budgetItems.postValue(syncedItems);
                    calculateBudgetSummary(walletId, syncedItems);
                    isLoading.postValue(false);
                }).start();
            });
        }).start();
    }

    /**
     * Refresh data - gọi khi cần reload từ database.
     */
    public void refresh() {
        loadWallets();
    }

    /**
     * Load danh sách categories cho việc tạo budget mới.
     */
    public void loadCategories() {
        List<Category> categoryList = repository.getExpenseCategories(currentUserId);
        categories.setValue(categoryList);
    }

    // ==================== BUDGET OPERATIONS ====================

    /**
     * Tạo budget mới.
     */
    public void createBudget(@NonNull String categoryId, long targetAmount,
                             long startDate, long endDate, @NonNull String walletId) {
        Budget budget = new Budget.Builder()
                .setCategoryId(categoryId)
                .setTargetAmount(targetAmount)
                .setStartDate(startDate)
                .setEndDate(endDate)
                .setWalletId(walletId)
                .build();

        String id = repository.insertBudget(budget);
        
        if (id != null) {
            budget.setId(id);
            repository.pushCreate(budget, null);
            operationResult.setValue(new OperationResult(Action.CREATE, true, "Tạo ngân sách thành công"));
            refresh();
        } else {
            operationResult.setValue(new OperationResult(Action.CREATE, false, "Không thể tạo ngân sách"));
        }
    }

    /**
     * Cập nhật budget.
     */
    public void updateBudget(@NonNull String budgetId, @NonNull String categoryId,
                             long targetAmount, long startDate, long endDate,
                             @NonNull String walletId) {
        Budget budget = new Budget.Builder()
                .setId(budgetId)
                .setCategoryId(categoryId)
                .setTargetAmount(targetAmount)
                .setStartDate(startDate)
                .setEndDate(endDate)
                .setWalletId(walletId)
                .build();

        int result = repository.updateBudget(budget);
        
        if (result > 0) {
            repository.pushUpdate(budget, null);
            operationResult.setValue(new OperationResult(Action.UPDATE, true, "Cập nhật ngân sách thành công"));
            refresh();
        } else {
            operationResult.setValue(new OperationResult(Action.UPDATE, false, "Không thể cập nhật ngân sách"));
        }
    }

    /**
     * Xóa budget.
     */
    public void deleteBudget(@NonNull String budgetId) {
        int result = repository.deleteBudget(budgetId);
        
        if (result > 0) {
            repository.pushDelete(budgetId, null);
            operationResult.setValue(new OperationResult(Action.DELETE, true, "Xóa ngân sách thành công"));
            refresh();
        } else {
            operationResult.setValue(new OperationResult(Action.DELETE, false, "Không thể xóa ngân sách"));
        }
    }

    // ==================== PRIVATE METHODS ====================

    /**
     * Tính toán budget summary (tổng ngân sách, đã chi, số ngày còn lại).
     */
    private void calculateBudgetSummary(@NonNull String walletId, List<BudgetItem> items) {
        long totalBudget = 0;
        long totalSpent = 0;
        long earliestStart = Long.MAX_VALUE;
        long latestEnd = 0;

        if (items != null) {
            for (BudgetItem item : items) {
                totalBudget += item.getLimit();
                totalSpent += item.getSpent();
                
                if (item.getStartDate() < earliestStart) {
                    earliestStart = item.getStartDate();
                }
                if (item.getEndDate() > latestEnd) {
                    latestEnd = item.getEndDate();
                }
            }
        }

        // Tính số ngày còn lại
        long now = System.currentTimeMillis();
        int daysRemaining = 0;
        if (latestEnd > now) {
            daysRemaining = (int) ((latestEnd - now) / (24 * 60 * 60 * 1000));
        }

        long remaining = totalBudget - totalSpent;
        int progress = totalBudget > 0 ? (int) ((totalSpent * 100) / totalBudget) : 0;

        BudgetSummary summary = new BudgetSummary(
                totalBudget,
                totalSpent,
                remaining,
                daysRemaining,
                progress
        );
        budgetSummary.postValue(summary);
    }



    // ==================== INNER CLASSES ====================

    public enum Action {
        CREATE, UPDATE, DELETE
    }

    /**
     * Kết quả của các operation (create, update, delete).
     */
    public static class OperationResult {
        private final Action action;
        private final boolean success;
        private final String message;
        private boolean handled = false;

        public OperationResult(Action action, boolean success, String message) {
            this.action = action;
            this.success = success;
            this.message = message;
        }

        public Action getAction() {
            return action;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        /**
         * Kiểm tra xem event đã được xử lý chưa (dạng SingleLiveEvent).
         * Ngăn nhiều view cùng gọi hoặc show lại Toast khi xoay màn hình.
         * @return false nếu chưa xử lý và cho phép chạy, true nếu đã xử lý rồi.
         */
        public boolean hasBeenHandled() {
            if (handled) {
                return true;
            } else {
                handled = true;
                return false;
            }
        }
    }

    /**
     * Summary data cho hiển thị tổng quan ngân sách.
     */
    public static class BudgetSummary {
        private final long totalBudget;
        private final long totalSpent;
        private final long remaining;
        private final int daysRemaining;
        private final int progress;

        public BudgetSummary(long totalBudget, long totalSpent, long remaining,
                           int daysRemaining, int progress) {
            this.totalBudget = totalBudget;
            this.totalSpent = totalSpent;
            this.remaining = remaining;
            this.daysRemaining = daysRemaining;
            this.progress = progress;
        }



        public long getRemaining() {
            return remaining;
        }

        public int getDaysRemaining() {
            return daysRemaining;
        }

        public int getProgress() {
            return progress;
        }

        public String getFormattedTotalBudget() {
            return formatMoney(totalBudget);
        }

        public String getFormattedTotalSpent() {
            return formatMoney(totalSpent);
        }



        private String formatMoney(long amount) {
            if (amount >= 1000000) {
                return String.format("%.1f M", amount / 1000000.0);
            } else if (amount >= 1000) {
                return String.format("%.0f K", amount / 1000.0);
            } else {
                return amount + " đ";
            }
        }
    }
}
