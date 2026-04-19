package com.ptithcm.quanlichitieu.ui.wallet;

import android.app.Application;

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
import com.ptithcm.quanlichitieu.data.repository.WalletRepository;
import com.ptithcm.quanlichitieu.data.repository.WalletRepositoryImpl;

import java.util.List;

/**
 * WalletViewModel — Quản lý state và business logic của màn hình Wallet.
 *
 * Kiến trúc: Local-First sync thông qua WalletRepository.
 * - Thao tác write (tạo/sửa/xóa) lưu local TRƯỚC → thông báo UI NGAY.
 * - Sau đó push lên server bất đồng bộ (fire-and-forget).
 * - syncStatus LiveData thông báo kết quả push về server cho UI.
 *
 * Scoped tới Activity (ViewModelProvider(requireActivity())) để chia sẻ
 * trạng thái ví đang chọn giữa HomeFragment, TransactionFragment, BudgetFragment.
 */
public class WalletViewModel extends AndroidViewModel {

    // ==================== TRẠNG THÁI SYNC VỚI SERVER ====================

    /**
     * Trạng thái đồng bộ với server.
     * - SYNCING: đang gọi API
     * - SYNC_SUCCESS: server xác nhận thành công
     * - SYNC_FAILED: server lỗi hoặc offline (dữ liệu đã lưu local)
     */
    public enum SyncStatus { SYNCING, SYNC_SUCCESS, SYNC_FAILED }

    // ==================== LIVE DATA ====================

    private final MutableLiveData<List<Wallet>> wallets = new MutableLiveData<>();
    private final MutableLiveData<Wallet> selectedWallet = new MutableLiveData<>();
    private final MutableLiveData<Wallet> singleWallet = new MutableLiveData<>();
    private final MutableLiveData<SaveResult> saveResult = new MutableLiveData<>();
    /** Kết quả push lên server — Fragment observe để hiện Snackbar nếu lỗi. */
    private final MutableLiveData<SyncStatus> syncStatus = new MutableLiveData<>();

    // ==================== DEPENDENCIES ====================

    private final WalletRepository walletRepository;
    private final TransactionDao transactionDao;
    private final BudgetDao budgetDao;
    private final WalletDao walletDao; // dùng riêng cho loadWalletById

    @Nullable
    private String currentUserId;

    // ==================== CONSTRUCTOR ====================

    public WalletViewModel(@NonNull Application application) {
        super(application);
        DatabaseManager dbManager = DatabaseManager.getInstance(application);
        this.walletDao = dbManager.getWalletDao();
        this.transactionDao = dbManager.getTransactionDao();
        this.budgetDao = dbManager.getBudgetDao();

        TokenStorage tokenStorage = EncryptedTokenStorage.getInstance(application);
        this.currentUserId = tokenStorage.getUserId();

        // Truyền userId vào repository để UPSERT từ server lọc đúng user
        this.walletRepository = new WalletRepositoryImpl(
                application, walletDao, tokenStorage, this.currentUserId);
    }

    // ==================== USER ID MANAGEMENT ====================

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

    // ==================== LIVE DATA GETTERS ====================

    public LiveData<List<Wallet>> getWallets() { return wallets; }
    public LiveData<Wallet> getSelectedWallet() { return selectedWallet; }
    public LiveData<Wallet> getSingleWallet() { return singleWallet; }
    public LiveData<SaveResult> getSaveResult() { return saveResult; }

    /**
     * Kết quả push lên server.
     * UI (WalletFragment) observe LiveData này để hiển thị Snackbar khi lỗi sync.
     */
    public LiveData<SyncStatus> getSyncStatus() { return syncStatus; }

    public void clearSaveResult() { saveResult.setValue(null); }
    public void clearSyncStatus() { syncStatus.setValue(null); }

    // ==================== LOAD OPERATIONS ====================

    public void loadWalletById(String walletId) {
        if (walletId == null) return;
        new Thread(() -> {
            Wallet wallet = walletDao.getWalletById(walletId);
            singleWallet.postValue(wallet);
        }).start();
    }

    /**
     * Kéo ví mới nhất từ server về (UPSERT local), sau đó reload list.
     *
     * Giải quyết vấn đề: ví tạo từ web/thiết bị khác không xuất hiện trên app.
     * Gọi từ WalletFragment.onResume() để luôn hiển thị dữ liệu mới nhất.
     *
     * Luồng:
     *   Main thread → Volley GET /api/v1/wallets/
     *     → BG thread: UPSERT SQLite
     *     → onDone (BG thread): postValue(walletList) lên UI
     */
    public void refreshFromServer() {
        // fetchFromServer phải chạy trên main thread (Volley requirement)
        // onDone callback chạy trên BG thread (trong WalletApiService)
        walletRepository.fetchFromServer(() -> {
            // Callback này chạy sau khi UPSERT xong — reload list từ local DB
            List<Wallet> list = getWalletsForCurrentUserOrLegacyFallback();
            wallets.postValue(list); // postValue vì đang ở BG thread
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
                active = walletList.get(0);
                // gọi selectWallet nhưng không thể gọi trực tiếp vì đang ở thread khác
                final Wallet finalActive = active;
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> selectWallet(finalActive));
            } else {
                selectedWallet.postValue(active);
                wallets.postValue(walletList);
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

    // ==================== WRITE OPERATIONS (LOCAL-FIRST) ====================

    /**
     * Tạo ví mới.
     *
     * Luồng:
     * 1. [BG Thread] Validate → insert local DB → postValue(SUCCESS)
     * 2. [Volley Async] POST /api/v1/wallets/ → syncStatus
     */
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

                // Bước 1: Lưu local
                String walletId = walletRepository.insertLocal(wallet);

                if (walletId != null) {
                    saveResult.postValue(new SaveResult(true, "Thêm ví thành công"));
                    loadAllWallets();

                    // Nếu đây là ví đầu tiên → tự chọn làm active
                    List<Wallet> currentList = wallets.getValue();
                    if (currentList == null || currentList.isEmpty()) {
                        android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
                        h.post(() -> selectWallet(wallet));
                    }

                    // Bước 2: Push lên server (bất đồng bộ, không block)
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

    /**
     * Cập nhật ví.
     *
     * Luồng:
     * 1. [BG Thread] Validate → update local DB → postValue(SUCCESS)
     * 2. [Volley Async] PATCH /api/v1/wallets/:id → syncStatus
     */
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

                // Bước 1: Lưu local
                int rows = walletRepository.updateLocal(wallet);

                if (rows > 0) {
                    saveResult.postValue(new SaveResult(true, "Cập nhật ví thành công"));
                    loadAllWallets();

                    // Cập nhật selectedWallet nếu đây là ví đang active
                    Wallet currentActive = selectedWallet.getValue();
                    if (currentActive != null && currentActive.getId().equals(wallet.getId())) {
                        selectedWallet.postValue(wallet);
                    }

                    // Bước 2: Push lên server
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

    /**
     * Xóa ví (soft delete).
     *
     * Luồng:
     * 1. [BG Thread] Soft-delete ví + cascade transactions/budgets → postValue(SUCCESS)
     * 2. [Volley Async] DELETE /api/v1/wallets/:id → syncStatus
     */
    public void deleteWallet(Wallet wallet) {
        if (wallet == null) return;
        final String walletId = wallet.getId();

        new Thread(() -> {
            try {
                // Bước 1: Cascade soft delete local
                transactionDao.deleteByWalletId(walletId);
                budgetDao.deleteByWalletId(walletId);
                walletRepository.deleteLocal(walletId);

                saveResult.postValue(new SaveResult(true, "Xóa ví thành công"));
                loadAllWallets();

                // Nếu ví bị xóa là ví đang active → chuyển sang ví khác
                Wallet currentActive = selectedWallet.getValue();
                if (currentActive != null && currentActive.getId().equals(walletId)) {
                    android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
                    h.post(() -> {
                        List<Wallet> currentList = wallets.getValue();
                        if (currentList != null && !currentList.isEmpty()) {
                            selectWallet(currentList.get(0));
                        } else {
                            selectedWallet.setValue(null);
                        }
                    });
                }

                // Bước 2: Push lên server
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

    // ==================== HELPERS ====================

    /**
     * Parse chuỗi số tiền, chấp nhận dấu phẩy và chấm.
     */
    private long parseBalance(@Nullable String balanceStr) {
        if (balanceStr == null || balanceStr.trim().isEmpty()) return 0L;
        String clean = balanceStr.replaceAll("[^0-9]", "");
        return clean.isEmpty() ? 0L : Long.parseLong(clean);
    }

    // ==================== SAVE RESULT ====================

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
