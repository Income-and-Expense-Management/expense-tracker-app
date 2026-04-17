package com.ptithcm.quanlichitieu.ui.wallet;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.quanlichitieu.data.local.DatabaseManager;
import com.ptithcm.quanlichitieu.data.local.dao.WalletDao;
import com.ptithcm.quanlichitieu.data.model.Wallet;

import java.util.List;

public class WalletViewModel extends AndroidViewModel {

    private final WalletDao walletDao;

    private final MutableLiveData<List<Wallet>> wallets = new MutableLiveData<>();
    private final MutableLiveData<Wallet> selectedWallet = new MutableLiveData<>();
    private final MutableLiveData<SaveResult> saveResult = new MutableLiveData<>();

    /**
     * When auth isn't implemented yet, legacy data may have user_id = NULL.
     * We keep currentUserId nullable by default for backward compatibility.
     */
    @Nullable
    private String currentUserId;

    public WalletViewModel(@NonNull Application application) {
        super(application);
        this.walletDao = DatabaseManager.getInstance(application).getWalletDao();
        this.currentUserId = null;
    }

    /**
     * Allow Activities/Fragments to set the current userId (when auth is implemented).
     */
    public void setCurrentUserId(@Nullable String userId) {
        this.currentUserId = (userId == null || userId.trim().isEmpty()) ? null : userId;
    }

    @Nullable
    private String userIdOrNull() {
        return (currentUserId == null || currentUserId.trim().isEmpty()) ? null : currentUserId;
    }

    private List<Wallet> getWalletsForCurrentUserOrLegacyFallback() {
        String userId = userIdOrNull();
        List<Wallet> list = walletDao.getByUserId(userId);
        if ((list == null || list.isEmpty()) && userId != null) {
            // Fallback: app versions before auth stored wallets with user_id = NULL.
            list = walletDao.getByUserId(null);
        }
        return list;
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

    public void clearSaveResult() {
        saveResult.setValue(null);
    }

    /**
     * Tải ví đang được chọn (active) hoặc ví đầu tiên nếu chưa có ví nào active
     */
    public void loadActiveWallet() {
        List<Wallet> walletList = getWalletsForCurrentUserOrLegacyFallback();
        if (walletList == null || walletList.isEmpty()) {
            selectedWallet.setValue(null);
            return;
        }

        Wallet active = null;
        for (Wallet w : walletList) {
            if (w.isActive()) {
                active = w;
                break;
            }
        }

        if (active == null) {
            active = walletList.get(0);
            selectWallet(active);
        } else {
            selectedWallet.setValue(active);
        }
    }

    /**
     * Thực hiện chọn ví: Cập nhật DB và thông báo cho các Fragment đang quan sát
     */
    public void selectWallet(Wallet wallet) {
        if (wallet == null) return;

        String userId = userIdOrNull();
        // If wallet belongs to legacy NULL user, keep operating on NULL user group
        if (wallet.getUserId() == null) userId = null;

        walletDao.setActiveWallet(wallet.getId(), userId);
        selectedWallet.setValue(wallet);
        loadAllWallets();
    }

    public void loadAllWallets() {
        List<Wallet> walletList = getWalletsForCurrentUserOrLegacyFallback();
        wallets.setValue(walletList);
    }

    public void saveWallet(String name, String balanceStr, String iconId) {
        if (name == null || name.trim().isEmpty()) {
            saveResult.setValue(new SaveResult(false, "Vui lòng nhập tên ví"));
            return;
        }

        try {
            long balance = Long.parseLong(balanceStr.trim().replace(",", "").replace(".", ""));

            String userId = userIdOrNull();
            Wallet wallet = new Wallet.Builder()
                    .setUserId(userId)
                    .setName(name.trim())
                    .setInitialBalance(balance)
                    .setCurrency("VND")
                    .setIconId(iconId)
                    .setIsActive(wallets.getValue() == null || wallets.getValue().isEmpty())
                    .build();

            String walletId = walletDao.insert(wallet);

            if (walletId != null) {
                saveResult.setValue(new SaveResult(true, "Thêm ví thành công"));
                loadAllWallets();
                if (wallet.isActive()) selectedWallet.setValue(wallet);
            } else {
                saveResult.setValue(new SaveResult(false, "Lỗi khi lưu ví"));
            }
        } catch (Exception e) {
            saveResult.setValue(new SaveResult(false, "Dữ liệu không hợp lệ"));
        }
    }

    public void deleteWallet(Wallet wallet) {
        if (wallet == null) return;
        try {
            com.ptithcm.quanlichitieu.data.local.dao.TransactionDao transactionDao = DatabaseManager.getInstance(getApplication()).getTransactionDao();
            com.ptithcm.quanlichitieu.data.local.dao.BudgetDao budgetDao = DatabaseManager.getInstance(getApplication()).getBudgetDao();
            
            // Delete related transactions and budgets
            transactionDao.deleteByWalletId(wallet.getId());
            budgetDao.deleteByWalletId(wallet.getId());
            
            // Delete the wallet
            walletDao.delete(wallet.getId());
            
            saveResult.setValue(new SaveResult(true, "Xóa ví thành công"));
            
            // Reload all wallets
            loadAllWallets();
            
            // If the deleted wallet was the active one, pick another or set to null
            Wallet currentActive = selectedWallet.getValue();
            if (currentActive != null && currentActive.getId().equals(wallet.getId())) {
                List<Wallet> currentList = wallets.getValue();
                if (currentList != null && !currentList.isEmpty()) {
                    selectWallet(currentList.get(0));
                } else {
                    selectedWallet.setValue(null);
                }
            }
        } catch (Exception e) {
            saveResult.setValue(new SaveResult(false, "Lỗi khi xóa ví"));
        }
    }

    public void updateWallet(Wallet wallet, String name, String balanceStr) {
        if (wallet == null) return;
        if (name == null || name.trim().isEmpty()) {
            saveResult.setValue(new SaveResult(false, "Vui lòng nhập tên ví"));
            return;
        }

        try {
            long balance = 0;
            if (balanceStr != null && !balanceStr.trim().isEmpty()) {
                String cleanBal = balanceStr.replaceAll("[^0-9]", "");
                if (!cleanBal.isEmpty()) {
                    balance = Long.parseLong(cleanBal);
                }
            }

            wallet.setName(name.trim());
            wallet.setInitialBalance(balance);

            int rows = walletDao.update(wallet);
            if (rows > 0) {
                saveResult.setValue(new SaveResult(true, "Cập nhật ví thành công"));
                loadAllWallets();
                Wallet currentActive = selectedWallet.getValue();
                if (currentActive != null && currentActive.getId().equals(wallet.getId())) {
                    selectedWallet.setValue(wallet);
                }
            } else {
                saveResult.setValue(new SaveResult(false, "Lỗi khi cập nhật ví"));
            }
        } catch (Exception e) {
            saveResult.setValue(new SaveResult(false, "Dữ liệu lượng tiền không hợp lệ"));
        }
    }

    public static class SaveResult {
        private final boolean success;
        private final String message;
        public SaveResult(boolean success, String message) { this.success = success; this.message = message; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}
