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
import com.ptithcm.quanlichitieu.data.repository.DbExpenseRepository;
import com.ptithcm.quanlichitieu.data.repository.ExpenseRepository;
import com.ptithcm.quanlichitieu.data.repository.TransactionRepository;
import com.ptithcm.quanlichitieu.data.repository.TransactionRepositoryImpl;

import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private static final int TOP_EXPENSES_LIMIT = 3;

    private final TransactionDao transactionDao;
    private final WalletDao walletDao;
    private final TokenStorage tokenStorage;
    private final TransactionRepository transactionRepository;

    private final MutableLiveData<String> username = new MutableLiveData<>();
    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<List<Expense>> topExpenses = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isMonthSelected = new MutableLiveData<>(true);
    private final MutableLiveData<Long> totalSpent = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> totalIncome = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> totalBalance = new MutableLiveData<>(0L);
    private final MutableLiveData<List<Float>> chartData = new MutableLiveData<>();
    private final MutableLiveData<List<Float>> incomeChartData = new MutableLiveData<>();

    private int currentReportRequestId = 0;
    private int currentTopExpensesRequestId = 0;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        this.transactionDao = DatabaseManager.getInstance(application).getTransactionDao();
        this.walletDao = DatabaseManager.getInstance(application).getWalletDao();
        this.transactionRepository = new TransactionRepositoryImpl(application);
        this.tokenStorage = EncryptedTokenStorage.getInstance(application);
    }

    public LiveData<String> getUsername() { return username; }
    public LiveData<Wallet> getWallet() { return wallet; }
    public LiveData<List<Expense>> getTopExpenses() { return topExpenses; }
    public LiveData<Boolean> getIsMonthSelected() { return isMonthSelected; }

    public LiveData<Long> getTotalSpent() { return totalSpent; }
    public LiveData<Long> getTotalIncome() { return totalIncome; }
    public LiveData<Long> getTotalBalance() { return totalBalance; }
    public LiveData<List<Float>> getChartData() { return chartData; }
    public LiveData<List<Float>> getIncomeChartData() { return incomeChartData; }

    public void setUsername(String name) {
        username.setValue(name);
    }

    public void setWallet(Wallet w) {
        wallet.setValue(w);
        loadReportData();
        loadTopExpenses();
    }

    public void loadWallet() {
        String userId = tokenStorage.getUserId();
        List<Wallet> wallets = walletDao.getByUserId(userId);
        if (!wallets.isEmpty()) {
            wallet.setValue(wallets.get(0));
        } else {
            wallet.setValue(null);
        }
        loadReportData();
        loadTopExpenses();
    }

    public void loadReportData() {
        if (wallet.getValue() != null) {
            final int requestId = ++currentReportRequestId;
            new Thread(() -> {
                long spent = transactionRepository.getTotalExpense(wallet.getValue().getId(), 0);
                long income = transactionRepository.getTotalIncome(wallet.getValue().getId(), 0);

                // load chart data
                List<com.ptithcm.quanlichitieu.data.model.TransactionGroup> groups =
                    transactionRepository.getTransactionsByWalletAndMonth(wallet.getValue().getId(), 0);

                // Chart data should be cumulative spent per day over the month
                float[] dailyExpenses = new float[31];
                float[] dailyIncomes = new float[31];
                for (com.ptithcm.quanlichitieu.data.model.TransactionGroup g : groups) {
                    String dateStr = g.getDate();
                    if (dateStr != null && dateStr.contains("/")) {
                        try {
                            int day = Integer.parseInt(dateStr.split("/")[0]);
                            if (day >= 1 && day <= 31) {
                                long dayTotalExp = 0;
                                long dayTotalInc = 0;
                                for (Transaction t : g.getTransactions()) {
                                    if (t.isExpense()) dayTotalExp += Math.abs(t.getAmount());
                                    else dayTotalInc += Math.abs(t.getAmount());
                                }
                                dailyExpenses[day - 1] += dayTotalExp;
                                dailyIncomes[day - 1] += dayTotalInc;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }

                List<Float> accumulated = new java.util.ArrayList<>();
                List<Float> accumulatedIncome = new java.util.ArrayList<>();
                float currentTotal = 0;
                float currentTotalInc = 0;
                for (int i = 0; i < 31; i++) {
                    currentTotal += dailyExpenses[i];
                    accumulated.add(currentTotal);
                    currentTotalInc += dailyIncomes[i];
                    accumulatedIncome.add(currentTotalInc);
                }

                if (accumulated.isEmpty()) {
                    // default empty chart
                    accumulated.add(0f);
                    accumulated.add(0f);
                    accumulatedIncome.add(0f);
                    accumulatedIncome.add(0f);
                }

                if (requestId != currentReportRequestId) return;

                chartData.postValue(accumulated);
                incomeChartData.postValue(accumulatedIncome);
                totalSpent.postValue(spent);
                totalIncome.postValue(income);
            }).start();
        } else {
            currentReportRequestId++; // Invalidate pending stats
            totalSpent.postValue(0L);
            totalIncome.postValue(0L);
            java.util.List<Float> empty = new java.util.ArrayList<>();
            empty.add(0f); empty.add(0f);
            chartData.postValue(empty);
            incomeChartData.postValue(empty);
        }
    }

    public void calculateCurrentBalance(Wallet currentWallet) {
        if (currentWallet != null) {
            new Thread(() -> {
                long balance = transactionRepository.getCurrentBalance(currentWallet.getId(), currentWallet.getInitialBalance());
                totalBalance.postValue(balance);
            }).start();
        } else {
            totalBalance.setValue(0L);
        }
    }

    public void loadTopExpenses() {
        Wallet activeWallet = wallet.getValue();
        if (activeWallet == null) {
            topExpenses.setValue(new java.util.ArrayList<>());
            return;
        }

        boolean isMonth = Boolean.TRUE.equals(isMonthSelected.getValue());
        final int requestId = ++currentTopExpensesRequestId;

        new Thread(() -> {
            List<Transaction> allTransactions = transactionDao.getWithDetails(activeWallet.getId(), 0); // 0 means no limit

            long startTime;
            long endTime;
            if (isMonth) {
                startTime = com.ptithcm.quanlichitieu.utils.DateUtils.getMonthStartTimestamp(0);
                endTime = com.ptithcm.quanlichitieu.utils.DateUtils.getMonthEndTimestamp(0);
            } else {
                startTime = com.ptithcm.quanlichitieu.utils.DateUtils.getWeekStartTimestamp(0);
                endTime = com.ptithcm.quanlichitieu.utils.DateUtils.getWeekEndTimestamp(0);
            }

            // Group by category, sort by sum descending
            java.util.Map<String, Long> categorySum = new java.util.HashMap<>();
            java.util.Map<String, Transaction> categoryTx = new java.util.HashMap<>();

            for (Transaction t : allTransactions) {
                if (t.isExpense() && t.getTransactionDate() >= startTime && t.getTransactionDate() <= endTime) {
                    String catName = t.getCategoryName() != null ? t.getCategoryName() : "Khác";
                    long amount = Math.abs(t.getAmount());
                    categorySum.put(catName, categorySum.getOrDefault(catName, 0L) + amount);
                    if (!categoryTx.containsKey(catName)) {
                        categoryTx.put(catName, t);
                    }
                }
            }

            List<java.util.Map.Entry<String, Long>> sortedList = new java.util.ArrayList<>(categorySum.entrySet());
            sortedList.sort((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()));

            List<Expense> result = new java.util.ArrayList<>();
            int count = 0;
            for (java.util.Map.Entry<String, Long> entry : sortedList) {
                String catName = entry.getKey();
                Transaction t = categoryTx.get(catName);

                String iconName = t.getIconId();
                int resId = com.ptithcm.quanlichitieu.R.drawable.ic_food;
                if (iconName != null && !iconName.isEmpty()) {
                    resId = getApplication().getResources().getIdentifier(iconName, "drawable", getApplication().getPackageName());
                    if (resId == 0) resId = com.ptithcm.quanlichitieu.R.drawable.ic_food;
                }

                result.add(new Expense(
                    catName.hashCode(),
                    catName,
                    "Chi tiêu",
                    entry.getValue(),
                    resId,
                    t.getTransactionDate()
                ));
                count++;
                if (count >= TOP_EXPENSES_LIMIT) break;
            }

            if (requestId != currentTopExpensesRequestId) return;

            topExpenses.postValue(result);
        }).start();
    }

    public void setPeriodFilter(boolean isMonth) {
        isMonthSelected.setValue(isMonth);
        loadTopExpenses();
    }
}
