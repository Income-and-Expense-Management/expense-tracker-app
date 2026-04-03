package com.ptithcm.quanlichitieu.ui.transaction;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ptithcm.quanlichitieu.data.model.TransactionGroup;
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
        loadData();
    }

    public void loadData() {
        int offset = monthOffset.getValue() != null ? monthOffset.getValue() : 0;
        transactions.setValue(transactionRepository.getTransactionsByMonth(offset));
        totalBalance.setValue(transactionRepository.getTotalBalance());
        totalExpense.setValue(transactionRepository.getTotalExpense());
        totalIncome.setValue(transactionRepository.getTotalIncome());
    }
}
