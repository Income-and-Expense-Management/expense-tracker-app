package com.ptithcm.quanlichitieu.ui.wallet;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.quanlichitieu.data.local.DatabaseManager;
import com.ptithcm.quanlichitieu.data.local.dao.BudgetDao;
import com.ptithcm.quanlichitieu.data.local.dao.TransactionDao;
import com.ptithcm.quanlichitieu.data.local.dao.WalletDao;
import com.ptithcm.quanlichitieu.data.local.token.EncryptedTokenStorage;
import com.ptithcm.quanlichitieu.data.local.token.TokenStorage;
import com.ptithcm.quanlichitieu.data.model.Wallet;
import com.ptithcm.quanlichitieu.data.repository.SyncRepository;
import com.ptithcm.quanlichitieu.data.repository.WalletRepository;
import com.ptithcm.quanlichitieu.data.repository.WalletRepositoryImpl;

import java.util.List;

public class WalletViewModel extends AndroidViewModel {

    public enum SyncStatus { SYNCING, SYNC_SUCCESS, SYNC_FAILED }

    private final MutableLiveData<List<Wallet>> wallets = new MutableLiveData<>();
    private final MutableLiveData<Wallet> selectedWallet = new MutableLiveData<>();
    private final MutableLiveData<Wallet> singleWallet = new MutableLiveData<>();
    private final MutableLiveData<SaveResult> saveResult = new MutableLiveData<>();
    private final MutableLiveData<SyncStatus> syncStatus = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>();

    private final WalletRepository walletRepository;
    private final TransactionDao transactionDao;
    private final BudgetDao budgetDao;
    private final WalletDao walletDao;

    @Nullable
    private String currentUserId;

    public WalletViewModel(@NonNull Application application) {
        super(application);
        DatabaseManager dbManager = DatabaseManager.getInstance(application);
        this.walletDao = dbManager.getWalletDao();
        this.transactionDao = dbManager.getTransactionDao();
        this.budgetDao = dbManager.getBudgetDao();

        TokenStorage tokenStorage = EncryptedTokenStorage.getInstance(application);
        this.currentUserId = tokenStorage.getUserId();

        this.walletRepository = new WalletRepositoryImpl(
                application, walletDao, tokenStorage, this.currentUserId);
    }

    public void setCurrentUserId(@Nullable String userId) {
        this.currentUserId = (userId == null || userId.trim().isEmpty()) ? null : userId;
    }

    private String getUserIdForKey() {
        return (currentUserId != null && !currentUserId.trim().isEmpty()) ? currentUserId : "default";
    }

    @Nullable
    private String userIdOrNull() {
        return (currentUserId == null || currentUserId.trim().isEmpty()) ? null : currentUserId;
    }

    public LiveData<List<Wallet>> getWallets() { return wallets; }
    public LiveData<Wallet> getSelectedWallet() { return selectedWallet; }
    public LiveData<Wallet> getSingleWallet() { return singleWallet; }
    public LiveData<SaveResult> getSaveResult() { return saveResult; }
    public LiveData<SyncStatus> getSyncStatus() { return syncStatus; }
    public LiveData<Boolean> getIsRefreshing() { return isRefreshing; }

    public void clearSaveResult() { saveResult.setValue(null); }
    public void clearSyncStatus() { syncStatus.setValue(null); }

    public void loadWalletById(String walletId) {
        if (walletId == null) return;
        new Thread(() -> {
            Wallet wallet = walletDao.getWalletById(walletId);
            singleWallet.postValue(wallet);
        }).start();
    }

    public void refreshFromServer() {
        isRefreshing.postValue(true);
        SyncRepository.getInstance(getApplication()).syncAll(currentUserId, () -> {
            // Sau khi pull xong, re-validate ví hiện tại ngay lập tức
            loadActiveWallet();
            isRefreshing.postValue(false);
        });
    }

    public void loadAllWallets() {
        new Thread(() -> {
            List<Wallet> list = getWalletsForCurrentUserOrLegacyFallback();
            wallets.postValue(list);
        }).start();
    }

    public void loadActiveWallet() {
        new Thread(() -> {
            List<Wallet> walletList = getWalletsForCurrentUserOrLegacyFallback();
            
            // Cập nhật danh sách ví trước
            wallets.postValue(walletList);

            if (walletList == null || walletList.isEmpty()) {
                selectedWallet.postValue(null);
                return;
            }

            android.content.SharedPreferences prefs =
                    getApplication().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
            String savedWalletId = prefs.getString("active_wallet_id_" + getUserIdForKey(), null);

            Wallet active = null;
            if (savedWalletId != null) {
                for (Wallet w : walletList) {
                    if (w.getId().equals(savedWalletId)) {
                        active = w;
                        break;
                    }
                }
            }

            if (active == null) {
                // Ví đang chọn bị xóa hoặc chưa chọn -> lấy ví đầu tiên
                Wallet first = walletList.get(0);
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> selectWallet(first));
            } else {
                // FIX: Ngay cả khi ví tồn tại, ta vẫn postValue lại ví mới nhất từ DB 
                // để cập nhật các thông tin thay đổi (như tên ví) lên giao diện Home.
                selectedWallet.postValue(active);
            }
        }).start();
    }

    public void selectWallet(Wallet wallet) {
        if (wallet == null) return;
        android.content.SharedPreferences prefs =
                getApplication().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
        prefs.edit().putString("active_wallet_id_" + getUserIdForKey(), wallet.getId()).apply();
        selectedWallet.setValue(wallet);
        loadAllWallets();
    }

    private List<Wallet> getWalletsForCurrentUserOrLegacyFallback() {
        String userId = userIdOrNull();
        List<Wallet> list = walletRepository.getByUserId(userId);
        if ((list == null || list.isEmpty()) && userId != null) {
            list = walletRepository.getByUserId(null);
        }
        return list;
    }

    public void saveWallet(String name, String balanceStr, String iconId) {
        if (name == null || name.trim().isEmpty()) {
            saveResult.setValue(new SaveResult(false, "Vui lòng nhập tên ví"));
            return;
        }

        new Thread(() -> {
            try {
                long balance = parseBalance(balanceStr);
                String userId = userIdOrNull();

                Wallet wallet = new Wallet.Builder()
                        .setUserId(userId)
                        .setName(name.trim())
                        .setInitialBalance(balance)
                        .setCurrency("VND")
                        .setIconId(iconId)
                        .build();

                String walletId = walletRepository.insertLocal(wallet);

                if (walletId != null) {
                    saveResult.postValue(new SaveResult(true, "Thêm ví thành công"));
                    loadAllWallets();

                    List<Wallet> currentList = wallets.getValue();
                    if (currentList == null || currentList.isEmpty()) {
                        android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
                        h.post(() -> selectWallet(wallet));
                    }

                    syncStatus.postValue(SyncStatus.SYNCING);
                    android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
                    h.post(() -> walletRepository.pushCreate(wallet, new WalletRepository.SyncCallback() {
                        @Override
                        public void onSuccess() {
                            syncStatus.setValue(SyncStatus.SYNC_SUCCESS);
                        }

                        @Override
                        public void onError(@NonNull String message) {
                            syncStatus.setValue(SyncStatus.SYNC_FAILED);
                        }
                    }));
                } else {
                    saveResult.postValue(new SaveResult(false, "Lỗi khi lưu ví"));
                }
            } catch (Exception e) {
                saveResult.postValue(new SaveResult(false, "Dữ liệu không hợp lệ"));
            }
        }).start();
    }

    public void updateWallet(Wallet wallet, String name, String balanceStr) {
        if (wallet == null) return;
        if (name == null || name.trim().isEmpty()) {
            saveResult.setValue(new SaveResult(false, "Vui lòng nhập tên ví"));
            return;
        }

        new Thread(() -> {
            try {
                long balance = parseBalance(balanceStr);
                wallet.setName(name.trim());
                wallet.setInitialBalance(balance);

                int rows = walletRepository.updateLocal(wallet);

                if (rows > 0) {
                    saveResult.postValue(new SaveResult(true, "Cập nhật ví thành công"));
                    loadAllWallets();

                    Wallet currentActive = selectedWallet.getValue();
                    if (currentActive != null && currentActive.getId().equals(wallet.getId())) {
                        selectedWallet.postValue(wallet);
                    }

                    syncStatus.postValue(SyncStatus.SYNCING);
                    android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
                    h.post(() -> walletRepository.pushUpdate(wallet, new WalletRepository.SyncCallback() {
                        @Override
                        public void onSuccess() {
                            syncStatus.setValue(SyncStatus.SYNC_SUCCESS);
                        }

                        @Override
                        public void onError(@NonNull String message) {
                            syncStatus.setValue(SyncStatus.SYNC_FAILED);
                        }
                    }));
                } else {
                    saveResult.postValue(new SaveResult(false, "Lỗi khi cập nhật ví"));
                }
            } catch (Exception e) {
                saveResult.postValue(new SaveResult(false, "Dữ liệu lượng tiền không hợp lệ"));
            }
        }).start();
    }

    public void deleteWallet(Wallet wallet) {
        if (wallet == null) return;
        final String walletId = wallet.getId();

        new Thread(() -> {
            try {
                transactionDao.deleteByWalletId(walletId);
                budgetDao.deleteByWalletId(walletId);
                walletRepository.deleteLocal(walletId);

                List<Wallet> updatedList = getWalletsForCurrentUserOrLegacyFallback();
                wallets.postValue(updatedList);

                saveResult.postValue(new SaveResult(true, "Xóa ví thành công"));

                Wallet currentActive = selectedWallet.getValue();
                if (currentActive != null && currentActive.getId().equals(walletId)) {
                    android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
                    h.post(() -> {
                        if (updatedList != null && !updatedList.isEmpty()) {
                            selectWallet(updatedList.get(0));
                        } else {
                            android.content.SharedPreferences prefs =
                                    getApplication().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
                            prefs.edit().remove("active_wallet_id_" + getUserIdForKey()).apply();
                            selectedWallet.setValue(null);
                        }
                    });
                }

                syncStatus.postValue(SyncStatus.SYNCING);
                android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
                h.post(() -> walletRepository.pushDelete(walletId, new WalletRepository.SyncCallback() {
                    @Override
                    public void onSuccess() {
                        syncStatus.setValue(SyncStatus.SYNC_SUCCESS);
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        syncStatus.setValue(SyncStatus.SYNC_FAILED);
                    }
                }));
            } catch (Exception e) {
                saveResult.postValue(new SaveResult(false, "Lỗi khi xóa ví"));
            }
        }).start();
    }

    private long parseBalance(@Nullable String balanceStr) {
        if (balanceStr == null || balanceStr.trim().isEmpty()) return 0L;
        String clean = balanceStr.replaceAll("[^0-9]", "");
        return clean.isEmpty() ? 0L : Long.parseLong(clean);
    }

    public static class SaveResult {
        private final boolean success;
        private final String message;

        public SaveResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}
