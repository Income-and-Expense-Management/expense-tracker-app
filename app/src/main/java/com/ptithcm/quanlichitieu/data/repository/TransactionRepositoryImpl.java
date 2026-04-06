package com.ptithcm.quanlichitieu.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ptithcm.quanlichitieu.data.local.DatabaseManager;
import com.ptithcm.quanlichitieu.data.local.dao.TransactionDao;
import com.ptithcm.quanlichitieu.data.model.Transaction;
import com.ptithcm.quanlichitieu.data.model.TransactionGroup;
import com.ptithcm.quanlichitieu.data.model.TransactionType;
import com.ptithcm.quanlichitieu.utils.DateUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TransactionRepositoryImpl - Implementation thực của TransactionRepository.
 * 
 * Chịu trách nhiệm:
 * - Lấy dữ liệu giao dịch từ SQLite database thông qua TransactionDao
 * - Nhóm giao dịch theo ngày với format tiếng Việt
 * - Tính toán các tổng số (thu nhập, chi tiêu)
 * - Filter dữ liệu theo ví và khoảng thời gian
 * 
 * Tuân thủ SOLID principles:
 * - Single Responsibility: Chỉ xử lý logic nghiệp vụ cho transaction
 * - Open/Closed: Có thể mở rộng thêm logic mà không sửa code hiện tại
 * - Dependency Inversion: Phụ thuộc vào TransactionDao interface
 * 
 * Extensibility cho tương lai:
 * - Dễ dàng thêm caching layer
 * - Có thể tích hợp với remote API khi cần đồng bộ
 * - Có thể thêm offline-first sync logic
 */
public class TransactionRepositoryImpl implements TransactionRepository {

    private static final String TAG = "TransactionRepositoryImpl";

    private final TransactionDao transactionDao;

    /**
     * Constructor nhận Context để khởi tạo DatabaseManager.
     * 
     * @param context Application context
     */
    public TransactionRepositoryImpl(@NonNull Context context) {
        DatabaseManager dbManager = DatabaseManager.getInstance(context);
        this.transactionDao = dbManager.getTransactionDao();
    }

    /**
     * Constructor cho testing - có thể inject TransactionDao mock.
     * 
     * @param transactionDao TransactionDao instance (có thể là mock)
     */
    public TransactionRepositoryImpl(@NonNull TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
    }

    // ==================== PUBLIC METHODS ====================

    @Override
    public List<TransactionGroup> getTransactionsByMonth(int monthOffset) {
        // Legacy method - delegate to new method without wallet filter
        return getTransactionsByWalletAndMonth(null, monthOffset);
    }

    @Override
    public List<TransactionGroup> getTransactionsByWalletAndMonth(@Nullable String walletId, int monthOffset) {
        // Tính toán khoảng thời gian của tháng
        long startDate = DateUtils.getMonthStartTimestamp(monthOffset);
        long endDate = DateUtils.getMonthEndTimestamp(monthOffset);

        Log.d(TAG, "Fetching transactions for wallet: " + walletId + 
                   ", monthOffset: " + monthOffset + 
                   ", startDate: " + startDate + 
                   ", endDate: " + endDate);

        // Lấy danh sách giao dịch từ database
        List<Transaction> transactions;
        if (walletId != null) {
            // Lấy theo wallet và date range với details (category name, wallet name)
            transactions = transactionDao.getByDateRangeWithDetails(walletId, startDate, endDate);
        } else {
            // Nếu không có wallet, lấy tất cả (dùng getWithDetails với limit 0)
            transactions = transactionDao.getWithDetails(null, 0);
            // Filter theo date range manually
            transactions = filterByDateRange(transactions, startDate, endDate);
        }

        Log.d(TAG, "Fetched " + transactions.size() + " transactions from database");

        // Nhóm giao dịch theo ngày
        return groupTransactionsByDate(transactions);
    }

    @Override
    public double getTotalExpense() {
        // Legacy method - trả về 0 hoặc implement logic lấy tất cả
        return 0.0;
    }

    @Override
    public double getTotalExpense(@Nullable String walletId, int monthOffset) {
        if (walletId == null) {
            return 0.0;
        }

        long startDate = DateUtils.getMonthStartTimestamp(monthOffset);
        long endDate = DateUtils.getMonthEndTimestamp(monthOffset);

        long total = transactionDao.getTotalAmountByType(
                walletId, 
                TransactionType.EXPENSE, 
                startDate, 
                endDate
        );

        Log.d(TAG, "Total expense for wallet " + walletId + ": " + total);
        return (double) total;
    }

    @Override
    public double getTotalIncome() {
        // Legacy method - trả về 0 hoặc implement logic lấy tất cả
        return 0.0;
    }

    @Override
    public double getTotalIncome(@Nullable String walletId, int monthOffset) {
        if (walletId == null) {
            return 0.0;
        }

        long startDate = DateUtils.getMonthStartTimestamp(monthOffset);
        long endDate = DateUtils.getMonthEndTimestamp(monthOffset);

        long total = transactionDao.getTotalAmountByType(
                walletId, 
                TransactionType.INCOME, 
                startDate, 
                endDate
        );

        Log.d(TAG, "Total income for wallet " + walletId + ": " + total);
        return (double) total;
    }

    @Override
    public double getTotalBalance() {
        // Legacy method - trả về 0
        // Balance được tính từ wallet.initialBalance + income - expense
        return 0.0;
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Nhóm các giao dịch theo ngày.
     * 
     * Thuật toán:
     * 1. Dùng LinkedHashMap để giữ thứ tự insertion (các ngày gần nhất trước)
     * 2. Key là dateKey (yyyyMMdd), value là list các transaction trong ngày
     * 3. Với mỗi nhóm, tính tổng tiền trong ngày
     * 4. Format thứ và ngày bằng tiếng Việt
     * 
     * @param transactions Danh sách transaction đã sort theo date DESC
     * @return Danh sách TransactionGroup đã nhóm
     */
    private List<TransactionGroup> groupTransactionsByDate(List<Transaction> transactions) {
        // Sử dụng LinkedHashMap để giữ thứ tự
        Map<String, List<Transaction>> groupedMap = new LinkedHashMap<>();

        // Nhóm transactions theo dateKey
        for (Transaction transaction : transactions) {
            String dateKey = DateUtils.getDateKey(transaction.getTransactionDate());
            
            if (!groupedMap.containsKey(dateKey)) {
                groupedMap.put(dateKey, new ArrayList<>());
            }
            
            groupedMap.get(dateKey).add(transaction);
        }

        // Convert map thành list TransactionGroup
        List<TransactionGroup> groups = new ArrayList<>();
        
        for (Map.Entry<String, List<Transaction>> entry : groupedMap.entrySet()) {
            List<Transaction> dayTransactions = entry.getValue();
            
            // Lấy transaction đầu tiên để lấy timestamp (tất cả cùng ngày)
            if (dayTransactions.isEmpty()) {
                continue;
            }
            
            long timestamp = dayTransactions.get(0).getTransactionDate();
            
            // Format thứ và ngày
            String dayOfWeek = DateUtils.formatDayOfWeek(timestamp);
            String date = DateUtils.formatDate(timestamp);
            
            // Tính tổng tiền trong ngày (income - expense)
            double dayTotal = calculateDayTotal(dayTransactions);
            
            // Tạo TransactionGroup
            TransactionGroup group = new TransactionGroup(dayOfWeek, date, dayTotal, dayTransactions);
            groups.add(group);
        }

        Log.d(TAG, "Grouped into " + groups.size() + " transaction groups");
        return groups;
    }

    /**
     * Tính tổng tiền trong ngày.
     * Công thức: Tổng INCOME - Tổng EXPENSE
     * 
     * @param transactions Danh sách transaction trong ngày
     * @return Tổng tiền (dương = thu nhiều hơn chi, âm = chi nhiều hơn thu)
     */
    private double calculateDayTotal(List<Transaction> transactions) {
        double total = 0.0;
        
        for (Transaction transaction : transactions) {
            if (transaction.getType() == TransactionType.INCOME) {
                total += transaction.getAmount();
            } else if (transaction.getType() == TransactionType.EXPENSE) {
                total -= transaction.getAmount();
            }
            // LOAN type không tính vào day total (hoặc có thể customize tùy yêu cầu)
        }
        
        return total;
    }

    /**
     * Filter danh sách transaction theo khoảng thời gian.
     * Dùng khi không có sẵn method filter trong DAO.
     * 
     * @param transactions Danh sách transaction gốc
     * @param startDate Timestamp bắt đầu
     * @param endDate Timestamp kết thúc
     * @return Danh sách đã filter
     */
    private List<Transaction> filterByDateRange(List<Transaction> transactions, long startDate, long endDate) {
        List<Transaction> filtered = new ArrayList<>();
        
        for (Transaction transaction : transactions) {
            long txDate = transaction.getTransactionDate();
            if (txDate >= startDate && txDate <= endDate) {
                filtered.add(transaction);
            }
        }
        
        return filtered;
    }
}
