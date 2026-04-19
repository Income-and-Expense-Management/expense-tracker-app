package com.ptithcm.quanlichitieu.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.quanlichitieu.data.local.DatabaseManager;
import com.ptithcm.quanlichitieu.data.local.dao.WalletDao;
import com.ptithcm.quanlichitieu.data.local.token.EncryptedTokenStorage;
import com.ptithcm.quanlichitieu.data.local.token.TokenStorage;
import com.ptithcm.quanlichitieu.data.model.Expense;
import com.ptithcm.quanlichitieu.data.model.Wallet;
import com.ptithcm.quanlichitieu.data.model.Transaction;
import com.ptithcm.quanlichitieu.data.local.dao.TransactionDao;

import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private static final int TOP_EXPENSES_LIMIT = 3;

    private final TransactionDao transactionDao;
    private final WalletDao walletDao;
    private final TokenStorage tokenStorage;

    private final MutableLiveData<String> username = new MutableLiveData<>();
    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<List<Expense>> topExpenses = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isMonthSelected = new MutableLiveData<>(true);

    public HomeViewModel(@NonNull Application application) {
        super(application);
        this.transactionDao = DatabaseManager.getInstance(application).getTransactionDao();
        this.walletDao = DatabaseManager.getInstance(application).getWalletDao();
        this.tokenStorage = EncryptedTokenStorage.getInstance(application);
    }

    public LiveData<String> getUsername() { return username; }
    public LiveData<Wallet> getWallet() { return wallet; }
    public LiveData<List<Expense>> getTopExpenses() { return topExpenses; }
    public LiveData<Boolean> getIsMonthSelected() { return isMonthSelected; }

    public void setUsername(String name) {
        username.setValue(name);
    }

    public void loadWallet() {
        String userId = tokenStorage.getUserId();
        List<Wallet> wallets = walletDao.getByUserId(userId);
        if (!wallets.isEmpty()) {
            wallet.setValue(wallets.get(0));
        } else {
            wallet.setValue(null);
        }
    }

    public void loadTopExpenses() {
        Wallet activeWallet = wallet.getValue();
        if (activeWallet == null) {
            topExpenses.setValue(new java.util.ArrayList<>());
            return;
        }

        new Thread(() -> {
            List<Transaction> transactions = transactionDao.getWithDetails(activeWallet.getId(), TOP_EXPENSES_LIMIT * 4);
            List<Expense> result = new java.util.ArrayList<>();
            int count = 0;
            for (Transaction t : transactions) {
                if (t.isExpense()) {
                    String iconName = t.getIconId();
                    int resId = com.ptithcm.quanlichitieu.R.drawable.ic_food;
                    if (iconName != null && !iconName.isEmpty()) {
                        resId = getApplication().getResources().getIdentifier(iconName, "drawable", getApplication().getPackageName());
                        if (resId == 0) resId = com.ptithcm.quanlichitieu.R.drawable.ic_food;
                    }

                    result.add(new Expense(
                        t.getId().hashCode(),
                        t.getCategoryName() != null ? t.getCategoryName() : "Khác",
                        t.getNote() != null && !t.getNote().isEmpty() ? t.getNote() : "Chi tiêu",
                        t.getAmount(),
                        resId,
                        t.getTransactionDate()
                    ));
                    count++;
                    if (count >= TOP_EXPENSES_LIMIT) break;
                }
            }
            topExpenses.postValue(result);
        }).start();
    }

    public void setPeriodFilter(boolean isMonth) {
        isMonthSelected.setValue(isMonth);
        loadTopExpenses();
    }
}
