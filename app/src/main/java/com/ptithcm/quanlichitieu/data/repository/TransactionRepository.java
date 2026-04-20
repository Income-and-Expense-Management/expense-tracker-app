package com.ptithcm.quanlichitieu.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ptithcm.quanlichitieu.data.model.TransactionGroup;

import java.util.List;

/**
 * TransactionRepository - Interface định nghĩa các thao tác với dữ liệu giao dịch.
 * 
 * Tuân thủ Dependency Inversion Principle:
 * - ViewModel phụ thuộc vào interface này, không phụ thuộc vào implementation cụ thể
 * - Cho phép dễ dàng swap giữa Mock, Local DB, hoặc Remote API implementation
 * 
 * Extensibility:
 * - Hiện tại: TransactionRepositoryImpl (local SQLite)
 * - Tương lai: RemoteTransactionRepository (API), HybridRepository (local + remote sync)
 */
public interface TransactionRepository {

    /**
     * Lấy danh sách giao dịch theo tháng, được nhóm theo ngày.
     * Phương thức legacy - không filter theo wallet.
     * 
     * @param monthOffset Offset tháng (-1 = tháng trước, 0 = tháng này, 1 = tháng sau)
     * @return Danh sách TransactionGroup đã nhóm theo ngày
     */
    List<TransactionGroup> getTransactionsByMonth(int monthOffset);

    /**
     * Lấy danh sách giao dịch theo ví và tháng, được nhóm theo ngày.
     * 
     * @param walletId ID của ví cần lọc (null để lấy tất cả)
     * @param monthOffset Offset tháng (-1 = tháng trước, 0 = tháng này, 1 = tháng sau)
     * @return Danh sách TransactionGroup đã nhóm theo ngày
     */
    List<TransactionGroup> getTransactionsByWalletAndMonth(@Nullable String walletId, int monthOffset);

    /**
     * Lấy tổng chi tiêu (legacy - không filter theo wallet).
     * 
     * @return Tổng chi tiêu
     */
    long getTotalExpense();

    /**
     * Lấy tổng chi tiêu theo ví và tháng.
     * 
     * @param walletId ID của ví (null để lấy tất cả)
     * @param monthOffset Offset tháng
     * @return Tổng chi tiêu
     */
    long getTotalExpense(@Nullable String walletId, int monthOffset);

    /**
     * Lấy tổng thu nhập (legacy - không filter theo wallet).
     * 
     * @return Tổng thu nhập
     */
    long getTotalIncome();

    /**
     * Lấy tổng thu nhập theo ví và tháng.
     * 
     * @param walletId ID của ví (null để lấy tất cả)
     * @param monthOffset Offset tháng
     * @return Tổng thu nhập
     */
    long getTotalIncome(@Nullable String walletId, int monthOffset);

    /**
     * Lấy tổng số dư (legacy - không filter theo wallet).
     * 
     * @return Tổng số dư
     */
    long getTotalBalance();

    /**
     * Lấy số dư hiện tại của ví (Số dư ban đầu + Tổng thu - Tổng chi)
     *
     * @param walletId ID của ví
     * @param initialBalance Số dư ban đầu
     * @return Số dư hiện tại
     */
    long getCurrentBalance(@NonNull String walletId, long initialBalance);

    /**
     * Tìm kiếm giao dịch theo từ khóa trong ghi chú hoặc tên danh mục.
     * Kết quả được nhóm theo ngày, sắp xếp mới nhất trước.
     *
     * @param keyword  Từ khóa tìm kiếm
     * @param walletId ID ví để lọc (null = tìm tất cả ví)
     * @return Danh sách TransactionGroup khớp với từ khóa
     */
    List<TransactionGroup> searchTransactions(@NonNull String keyword, @Nullable String walletId);
}
