package com.ptithcm.quanlichitieu.ui.transaction;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ptithcm.quanlichitieu.data.model.TransactionGroup;
import com.ptithcm.quanlichitieu.data.model.Wallet;
import com.ptithcm.quanlichitieu.data.repository.MockTransactionRepository;
import com.ptithcm.quanlichitieu.data.repository.TransactionRepository;

import java.util.List;

public class TransactionViewModel extends ViewModel {

    private final TransactionRepository transactionRepository;

    private final MutableLiveData<List<TransactionGroup>> transactions = new MutableLiveData<>();
    private final MutableLiveData<Double> totalBalance = new MutableLiveData<>();
    private final MutableLiveData<Double> totalExpense = new MutableLiveData<>();
    private final MutableLiveData<Double> totalIncome = new MutableLiveData<>();
    private final MutableLiveData<Integer> monthOffset = new MutableLiveData<>(0);

    public TransactionViewModel() {
        this.transactionRepository = new MockTransactionRepository();
    }

    public LiveData<List<TransactionGroup>> getTransactions() { return transactions; }
    public LiveData<Double> getTotalBalance() { return totalBalance; }
    public LiveData<Double> getTotalExpense() { return totalExpense; }
    public LiveData<Double> getTotalIncome() { return totalIncome; }
    public LiveData<Integer> getMonthOffset() { return monthOffset; }

    public void setMonthOffset(int offset) {
        monthOffset.setValue(offset);
        loadData(null);
    }

    /**
     * Tải dữ liệu giao dịch dựa trên ví đang chọn.
     * Nếu có ví, sử dụng số dư của ví đó làm "Tổng số dư".
     */
    public void loadData(Wallet selectedWallet) {
        int offset = monthOffset.getValue() != null ? monthOffset.getValue() : 0;
        
        // Cập nhật danh sách giao dịch (hiện tại vẫn dùng mock)
        transactions.setValue(transactionRepository.getTransactionsByMonth(offset));
        
        // Cập nhật số dư: Nếu có ví thật, lấy từ ví. Nếu không, lấy từ mock.
        if (selectedWallet != null) {
            totalBalance.setValue((double) selectedWallet.getInitialBalance());
        } else {
            totalBalance.setValue(transactionRepository.getTotalBalance());
        }
        
        totalExpense.setValue(transactionRepository.getTotalExpense());
        totalIncome.setValue(transactionRepository.getTotalIncome());
    }
}
