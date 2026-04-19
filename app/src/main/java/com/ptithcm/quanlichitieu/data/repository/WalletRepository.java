package com.ptithcm.quanlichitieu.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ptithcm.quanlichitieu.data.model.Wallet;

import java.util.List;

/**
 * WalletRepository — Interface trừu tượng hóa thao tác dữ liệu Wallet.
 *
 * Tách rõ hai lớp:
 * - Local operations: đọc/ghi xuống SQLite ngay lập tức, đồng bộ.
 * - Remote operations: push thay đổi lên server, bất đồng bộ (fire-and-forget).
 *
 * Tuân thủ Dependency Inversion Principle — ViewModel phụ thuộc vào Interface,
 * không phụ thuộc vào implementation cụ thể.
 */
public interface WalletRepository {

    // ==================== LOCAL OPERATIONS (đồng bộ, chạy trên background thread) ====================

    /**
     * Lưu ví mới vào SQLite local.
     * @return walletId nếu thành công, null nếu lỗi
     */
    @Nullable
    String insertLocal(@NonNull Wallet wallet);

    /**
     * Cập nhật ví trong SQLite local.
     * @return số rows affected (1 = thành công, 0 = không tìm thấy)
     */
    int updateLocal(@NonNull Wallet wallet);

    /**
     * Soft-delete ví trong SQLite local (set deleted_at = now).
     * @return số rows affected
     */
    int deleteLocal(@NonNull String walletId);

    /**
     * Lấy danh sách ví theo userId (chỉ lấy ví chưa bị xóa).
     */
    @NonNull
    List<Wallet> getByUserId(@Nullable String userId);

    // ==================== REMOTE OPERATIONS (bất đồng bộ, Volley) ====================

    /**
     * Callback chung cho các thao tác sync với server.
     * Được gọi trên main thread (Volley đảm bảo).
     */
    interface SyncCallback {
        void onSuccess();
        void onError(@NonNull String message);
    }

    /**
     * Đẩy lệnh tạo ví mới lên server.
     * POST /api/v1/wallets/
     * Fire-and-forget: lỗi server không block UI.
     */
    void pushCreate(@NonNull Wallet wallet, @Nullable SyncCallback callback);

    /**
     * Đẩy lệnh cập nhật ví lên server.
     * PATCH /api/v1/wallets/:walletId
     */
    void pushUpdate(@NonNull Wallet wallet, @Nullable SyncCallback callback);

    /**
     * Đẩy lệnh xóa ví lên server (soft delete phía server).
     * DELETE /api/v1/wallets/:walletId — server trả 204 No Content
     */
    void pushDelete(@NonNull String walletId, @Nullable SyncCallback callback);

    /**
     * Kéo danh sách ví mới nhất từ server về, UPSERT vào local DB.
     * GET /api/v1/wallets/
     *
     * Giải quyết vấn đề: ví tạo từ web/thiết bị khác không xuất hiện trên app
     * vì app chỉ đọc SQLite local. Gọi method này để đồng bộ.
     *
     * @param onDone Runnable chạy SAU KHI UPSERT xong (trên background thread).
     *               ViewModel nên gọi loadAllWallets() bên trong onDone.
     */
    void fetchFromServer(@Nullable Runnable onDone);
}
