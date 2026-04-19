package com.ptithcm.quanlichitieu.ui.transaction;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.quanlichitieu.data.model.TransactionGroup;
import com.ptithcm.quanlichitieu.data.model.Wallet;
import com.ptithcm.quanlichitieu.data.repository.TransactionRepository;
import com.ptithcm.quanlichitieu.data.repository.TransactionRepositoryImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * TransactionViewModel - ViewModel cho màn hình giao dịch.
 * 
 * Chức năng:
 * - Quản lý state của màn hình transaction
 * - Tải dữ liệu từ repository
 * - Tính toán số dư dựa trên ví và giao dịch
 * - Hỗ trợ filter theo tháng (offset)
 * 
 * Tuân thủ MVVM pattern:
 * - Không chứa logic UI
 * - Expose dữ liệu qua LiveData
 * - Giao tiếp với Repository layer
 */
public class TransactionViewModel extends AndroidViewModel {

    private final TransactionRepository transactionRepository;

    private final MutableLiveData<List<TransactionGroup>> transactions = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Long> totalBalance = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> totalExpense = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> totalIncome = new MutableLiveData<>(0L);
    private final MutableLiveData<Integer> monthOffset = new MutableLiveData<>(0);

    // Lưu wallet hiện tại để sử dụng khi thay đổi monthOffset
    private Wallet currentWallet;

    public TransactionViewModel(@NonNull Application application) {
        super(application);
        // Sử dụng implementation thật với database
        this.transactionRepository = new TransactionRepositoryImpl(application.getApplicationContext());
    }

    // ==================== GETTERS FOR LIVEDATA ====================

    public LiveData<List<TransactionGroup>> getTransactions() { 
        return transactions; 
    }
    
    public LiveData<Long> getTotalBalance() { 
        return totalBalance; 
    }
    
    public LiveData<Long> getTotalExpense() { 
        return totalExpense; 
    }
    
    public LiveData<Long> getTotalIncome() { 
        return totalIncome; 
    }
    
    public LiveData<Integer> getMonthOffset() { 
        return monthOffset; 
    }

    // ==================== PUBLIC METHODS ====================

    /**
     * Thiết lập month offset và tải lại dữ liệu.
     * 
     * @param offset Offset tháng (-1 = tháng trước, 0 = tháng này, 1 = tháng sau)
     */
    public void setMonthOffset(int offset) {
        monthOffset.setValue(offset);
        loadData(currentWallet);
    }

    /**
     * Tải dữ liệu giao dịch dựa trên ví đang chọn.
     * 
     * Logic tính số dư:
     * - Nếu có ví: Số dư = initialBalance + tổng thu nhập - tổng chi tiêu (của tháng hiện tại)
     * - Nếu không có ví: Hiển thị 0
     * 
     * @param selectedWallet Ví đang được chọn (null nếu chưa có ví)
     */
    public void loadData(Wallet selectedWallet) {
        // Lưu wallet hiện tại để dùng khi thay đổi monthOffset
        this.currentWallet = selectedWallet;
        
        int offset = monthOffset.getValue() != null ? monthOffset.getValue() : 0;
        
        if (selectedWallet == null) {
            // Không có ví được chọn - hiển thị empty state
            transactions.setValue(new ArrayList<>());
            totalBalance.setValue(0L);
            totalExpense.setValue(0L);
            totalIncome.setValue(0L);
            return;
        }

        String walletId = selectedWallet.getId();
        
        // Tải danh sách giao dịch từ database, filter theo wallet
        List<TransactionGroup> transactionGroups = transactionRepository
                .getTransactionsByWalletAndMonth(walletId, offset);
        transactions.setValue(transactionGroups);
        
        // Tính tổng thu nhập và chi tiêu trong tháng
        long income = transactionRepository.getTotalIncome(walletId, offset);
        long expense = transactionRepository.getTotalExpense(walletId, offset);
        
        totalIncome.setValue(income);
        totalExpense.setValue(expense);
        
        // Tính số dư hiện tại của ví
        // Công thức: Số dư ban đầu + Tổng thu nhập - Tổng chi tiêu
        long balance = selectedWallet.getInitialBalance() + income - expense;
        totalBalance.setValue(balance);
    }
}
