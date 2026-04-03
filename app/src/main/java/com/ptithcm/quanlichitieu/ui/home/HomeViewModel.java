package com.ptithcm.quanlichitieu.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.quanlichitieu.data.local.DatabaseManager;
import com.ptithcm.quanlichitieu.data.local.dao.WalletDao;
import com.ptithcm.quanlichitieu.data.model.Expense;
import com.ptithcm.quanlichitieu.data.model.Wallet;
import com.ptithcm.quanlichitieu.data.repository.ExpenseRepository;
import com.ptithcm.quanlichitieu.data.repository.MockExpenseRepository;

import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private static final int TOP_EXPENSES_LIMIT = 3;

    private final ExpenseRepository expenseRepository;
    private final WalletDao walletDao;

    private final MutableLiveData<String> username = new MutableLiveData<>();
    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<List<Expense>> topExpenses = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isMonthSelected = new MutableLiveData<>(true);

    public HomeViewModel(@NonNull Application application) {
        super(application);
        this.expenseRepository = new MockExpenseRepository();
        this.walletDao = DatabaseManager.getInstance(application).getWalletDao();
    }

    public LiveData<String> getUsername() { return username; }
    public LiveData<Wallet> getWallet() { return wallet; }
    public LiveData<List<Expense>> getTopExpenses() { return topExpenses; }
    public LiveData<Boolean> getIsMonthSelected() { return isMonthSelected; }

    public void setUsername(String name) {
        username.setValue(name);
    }

    public void loadWallet() {
        // TODO: Lấy userId từ session/auth service
        // Hiện tại trả về null vì chưa có user
        List<Wallet> wallets = walletDao.getByUserId(null);
        if (!wallets.isEmpty()) {
            wallet.setValue(wallets.get(0));
        } else {
            wallet.setValue(null);
        }
    }

    public void loadTopExpenses() {
        topExpenses.setValue(expenseRepository.getTopExpenses(TOP_EXPENSES_LIMIT));
    }

    public void setPeriodFilter(boolean isMonth) {
        isMonthSelected.setValue(isMonth);
        loadTopExpenses();
    }
}
