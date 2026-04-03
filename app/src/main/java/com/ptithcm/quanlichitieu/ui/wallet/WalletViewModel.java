package com.ptithcm.quanlichitieu.ui.wallet;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.quanlichitieu.data.local.DatabaseManager;
import com.ptithcm.quanlichitieu.data.local.dao.WalletDao;
import com.ptithcm.quanlichitieu.data.model.Wallet;

import java.util.List;

/**
 * WalletViewModel: Manages wallet business logic and data operations.
 * 
 * SOLID Principles Applied:
 * - Single Responsibility: Only handles wallet-related business logic
 * - Dependency Inversion: Depends on WalletDao abstraction
 * 
 * This ViewModel encapsulates all database operations for wallets,
 * keeping the UI layer (Fragments) free of business logic.
 */
public class WalletViewModel extends AndroidViewModel {

    private final WalletDao walletDao;
    
    private final MutableLiveData<List<Wallet>> wallets = new MutableLiveData<>();
    private final MutableLiveData<Wallet> selectedWallet = new MutableLiveData<>();
    private final MutableLiveData<SaveResult> saveResult = new MutableLiveData<>();

    // TODO: Lấy userId từ session/auth service
    private String currentUserId = null;

    public WalletViewModel(@NonNull Application application) {
        super(application);
        this.walletDao = DatabaseManager.getInstance(application).getWalletDao();
    }

    public LiveData<List<Wallet>> getWallets() {
        return wallets;
    }

    public LiveData<Wallet> getSelectedWallet() {
        return selectedWallet;
    }

    public LiveData<SaveResult> getSaveResult() {
        return saveResult;
    }

    /**
     * Loads the first wallet from database.
     * Used for displaying primary wallet information.
     */
    public void loadFirstWallet() {
        List<Wallet> walletList = walletDao.getByUserId(currentUserId);
        if (!walletList.isEmpty()) {
            selectedWallet.setValue(walletList.get(0));
        } else {
            selectedWallet.setValue(null);
        }
    }

    /**
     * Loads all wallets from database.
     */
    public void loadAllWallets() {
        List<Wallet> walletList = walletDao.getByUserId(currentUserId);
        wallets.setValue(walletList);
    }

    /**
     * Validates and saves a new wallet to database.
     * 
     * @param name Wallet name
     * @param balanceStr Balance as string (will be parsed)
     */
    public void saveWallet(String name, String balanceStr) {
        // Validation
        if (name == null || name.trim().isEmpty()) {
            saveResult.setValue(new SaveResult(false, "Vui lòng nhập tên ví"));
            return;
        }

        if (balanceStr == null || balanceStr.trim().isEmpty()) {
            saveResult.setValue(new SaveResult(false, "Vui lòng nhập số dư"));
            return;
        }

        try {
            long balance = Long.parseLong(balanceStr.trim().replace(",", "").replace(".", ""));
            
            if (balance < 0) {
                saveResult.setValue(new SaveResult(false, "Số dư không thể âm"));
                return;
            }

            // Sử dụng Builder pattern để tạo Wallet
            Wallet wallet = new Wallet.Builder()
                    .setUserId(currentUserId)
                    .setName(name.trim())
                    .setInitialBalance(balance)
                    .setCurrency("VND")
                    .setIsActive(true)
                    .build();
            
            String walletId = walletDao.insert(wallet);
            
            if (walletId != null) {
                saveResult.setValue(new SaveResult(true, "Thêm ví thành công: " + name));
                
                // Reload wallet data
                loadFirstWallet();
                loadAllWallets();
            } else {
                saveResult.setValue(new SaveResult(false, "Lỗi khi lưu ví"));
            }
            
        } catch (NumberFormatException e) {
            saveResult.setValue(new SaveResult(false, "Số dư không hợp lệ"));
        }
    }

    /**
     * Result class for save operations.
     * Encapsulates success/failure state and message.
     */
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
