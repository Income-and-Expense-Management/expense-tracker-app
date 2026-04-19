package com.ptithcm.quanlichitieu.ui.transaction;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.quanlichitieu.data.local.DatabaseManager;
import com.ptithcm.quanlichitieu.data.local.dao.CategoryDao;
import com.ptithcm.quanlichitieu.data.local.dao.TransactionDao;
import com.ptithcm.quanlichitieu.data.local.dao.WalletDao;
import com.ptithcm.quanlichitieu.data.local.token.EncryptedTokenStorage;
import com.ptithcm.quanlichitieu.data.local.token.TokenStorage;
import com.ptithcm.quanlichitieu.data.model.Category;
import com.ptithcm.quanlichitieu.data.model.Transaction;
import com.ptithcm.quanlichitieu.data.model.TransactionType;
import com.ptithcm.quanlichitieu.data.model.Wallet;
import com.ptithcm.quanlichitieu.event.BudgetUpdateEvent;
import com.ptithcm.quanlichitieu.event.EventBus;

import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.content.SharedPreferences;

public class AddTransactionViewModel extends AndroidViewModel {

    private final TransactionDao transactionDao;
    private final WalletDao walletDao;
    private final CategoryDao categoryDao;
    private final TokenStorage tokenStorage;

    private final MutableLiveData<Wallet> selectedWallet = new MutableLiveData<>();
    private final MutableLiveData<Category> selectedCategory = new MutableLiveData<>();
    private final MutableLiveData<TransactionType> transactionType = new MutableLiveData<>(TransactionType.EXPENSE);
    private final MutableLiveData<Long> transactionDate = new MutableLiveData<>(System.currentTimeMillis());
    private final MutableLiveData<SaveResult> saveResult = new MutableLiveData<>();
    private final MutableLiveData<String> initialAmount = new MutableLiveData<>();
    private final MutableLiveData<String> initialNote = new MutableLiveData<>();

    public AddTransactionViewModel(@NonNull Application application) {
        super(application);
        DatabaseManager dbManager = DatabaseManager.getInstance(application);
        this.transactionDao = dbManager.getTransactionDao();
        this.walletDao = dbManager.getWalletDao();
        this.categoryDao = dbManager.getCategoryDao();
        this.tokenStorage = EncryptedTokenStorage.getInstance(application);
    }

    public LiveData<Wallet> getSelectedWallet() {
        return selectedWallet;
    }

    public LiveData<Category> getSelectedCategory() {
        return selectedCategory;
    }

    public LiveData<TransactionType> getTransactionType() {
        return transactionType;
    }

    public LiveData<Long> getTransactionDate() {
        return transactionDate;
    }

    public LiveData<SaveResult> getSaveResult() {
        return saveResult;
    }

    public LiveData<String> getInitialAmount() {
        return initialAmount;
    }

    public LiveData<String> getInitialNote() {
        return initialNote;
    }

    public void setSelectedWallet(Wallet wallet) {
        selectedWallet.setValue(wallet);
    }

    public void setSelectedCategory(Category category) {
        selectedCategory.setValue(category);
    }

    public void setTransactionType(TransactionType type) {
        TransactionType current = transactionType.getValue();
        if (current != type) {
            transactionType.setValue(type);
            selectedCategory.setValue(null);
        }
    }

    public void setTransactionDate(long date) {
        transactionDate.setValue(date);
    }

    public void loadActiveWallet() {
        String userId = tokenStorage.getUserId();
        List<Wallet> wallets = walletDao.getByUserId(userId);
        if (wallets != null && !wallets.isEmpty()) {
            SharedPreferences prefs = getApplication().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            String key = "active_wallet_id_" + (userId != null ? userId : "default");
            String savedWalletId = prefs.getString(key, null);

            if (savedWalletId != null) {
                for (Wallet w : wallets) {
                    if (w.getId().equals(savedWalletId)) {
                        selectedWallet.setValue(w);
                        return;
                    }
                }
            }
            selectedWallet.setValue(wallets.get(0));
        }
    }

    public List<Wallet> getAllWallets() {
        return walletDao.getByUserId(tokenStorage.getUserId());
    }

    public List<Category> getCategoriesByType() {
        TransactionType currentType = transactionType.getValue();
        if (currentType == null) currentType = TransactionType.EXPENSE;

        // Use getByType instead of getAllCategories
        return categoryDao.getByType(tokenStorage.getUserId(), currentType);
    }

    public void loadTransaction(String transactionId) {
        Transaction transaction = transactionDao.getById(transactionId);
        if (transaction != null) {
            Wallet wallet = walletDao.getWalletById(transaction.getWalletId());
            if (wallet != null) setSelectedWallet(wallet);

            Category category = categoryDao.getById(transaction.getCategoryId());
            if (category != null) {
                // Thêm cái này trước để tránh xóa chọn nhóm
                setTransactionType(category.getType());
                setSelectedCategory(category);
            }

            setTransactionDate(transaction.getTransactionDate());
            initialAmount.setValue(String.valueOf(transaction.getAmount()));
            initialNote.setValue(transaction.getNote());
        }
    }

    public void updateTransaction(String transactionId, String amountStr, String note) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            saveResult.setValue(new SaveResult(false, "Vui lòng nhập số tiền"));
            return;
        }

        long amount;
        try {
            amount = Long.parseLong(amountStr.trim().replace(",", "").replace(".", ""));
        } catch (NumberFormatException e) {
            saveResult.setValue(new SaveResult(false, "Số tiền không hợp lệ"));
            return;
        }

        if (amount <= 0) {
            saveResult.setValue(new SaveResult(false, "Số tiền phải lớn hơn 0"));
            return;
        }

        Wallet wallet = selectedWallet.getValue();
        if (wallet == null) {
            saveResult.setValue(new SaveResult(false, "Vui lòng chọn ví"));
            return;
        }

        Category category = selectedCategory.getValue();
        if (category == null) {
            saveResult.setValue(new SaveResult(false, "Vui lòng chọn nhóm"));
            return;
        }

        Long date = transactionDate.getValue();
        if (date == null) date = System.currentTimeMillis();

        Transaction transaction = transactionDao.getById(transactionId);
        if (transaction != null) {
            transaction.setWalletId(wallet.getId());
            transaction.setCategoryId(category.getId());
            transaction.setAmount(amount);
            transaction.setTransactionDate(date);
            transaction.setNote(note != null && !note.trim().isEmpty() ? note.trim() : null);

            int rows = transactionDao.update(transaction);
            if (rows > 0) {
                BudgetUpdateEvent event = new BudgetUpdateEvent.Builder()
                        .setWalletId(wallet.getId())
                        .setCategoryId(category.getId())
                        .setEventType(BudgetUpdateEvent.EventType.TRANSACTION_ADDED) // Trigger reload
                        .build();
                EventBus.getInstance().postBudgetUpdate(event);

                saveResult.setValue(new SaveResult(true, "Cập nhật giao dịch thành công"));
            } else {
                saveResult.setValue(new SaveResult(false, "Lỗi khi cập nhật giao dịch"));
            }
        } else {
            saveResult.setValue(new SaveResult(false, "Không tìm thấy giao dịch"));
        }
    }

    public void saveTransaction(String amountStr, String note) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            saveResult.setValue(new SaveResult(false, "Vui lòng nhập số tiền"));
            return;
        }

        long amount;
        try {
            amount = Long.parseLong(amountStr.trim().replace(",", "").replace(".", ""));
        } catch (NumberFormatException e) {
            saveResult.setValue(new SaveResult(false, "Số tiền không hợp lệ"));
            return;
        }

        if (amount <= 0) {
            saveResult.setValue(new SaveResult(false, "Số tiền phải lớn hơn 0"));
            return;
        }

        Wallet wallet = selectedWallet.getValue();
        if (wallet == null) {
            saveResult.setValue(new SaveResult(false, "Vui lòng chọn ví"));
            return;
        }

        Category category = selectedCategory.getValue();
        if (category == null) {
            saveResult.setValue(new SaveResult(false, "Vui lòng chọn nhóm"));
            return;
        }

        TransactionType type = transactionType.getValue();
        if (type == null) type = TransactionType.EXPENSE;

        Long date = transactionDate.getValue();
        if (date == null) date = System.currentTimeMillis();

        Transaction transaction = new Transaction.Builder()
                .setWalletId(wallet.getId())
                .setCategoryId(category.getId())
                .setAmount(amount)
                .setTransactionDate(date)
                .setNote(note != null ? note.trim() : null)
                .build();

        String id = transactionDao.insert(transaction);

        if (id != null) {
            // Gửi event để notify BudgetFragment refresh data
            // Tuân thủ Open/Closed Principle: BudgetFragment không cần biết về AddTransactionViewModel
            BudgetUpdateEvent event = new BudgetUpdateEvent.Builder()
                    .setWalletId(wallet.getId())
                    .setCategoryId(category.getId())
                    .setEventType(BudgetUpdateEvent.EventType.TRANSACTION_ADDED)
                    .build();
            EventBus.getInstance().postBudgetUpdate(event);
            
            saveResult.setValue(new SaveResult(true, "Thêm giao dịch thành công"));
        } else {
            saveResult.setValue(new SaveResult(false, "Lỗi khi thêm giao dịch"));
        }
    }

    public static class SaveResult {
        private final boolean success;
        private final String message;

        public SaveResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
