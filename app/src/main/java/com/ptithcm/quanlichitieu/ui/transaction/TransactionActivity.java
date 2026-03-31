package com.ptithcm.quanlichitieu.ui.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.model.TransactionGroup;
import com.ptithcm.quanlichitieu.data.repository.MockTransactionRepository;
import com.ptithcm.quanlichitieu.data.repository.TransactionRepository;
import com.ptithcm.quanlichitieu.ui.home.HomeActivity;
import com.ptithcm.quanlichitieu.ui.transaction.adapter.TransactionAdapter;

import java.util.List;
import java.util.Locale;

public class TransactionActivity extends AppCompatActivity {

    private TransactionRepository transactionRepository;
    private TransactionAdapter transactionAdapter;

    private TextView tvTotalBalance;
    private TextView tvTotalExpense;
    private TextView tvTotalIncome;
    private RecyclerView rvTransactions;

    private TextView tabPrevMonth;
    private TextView tabCurrentMonth;
    private TextView tabNextMonth;

    private int currentMonthOffset = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        transactionRepository = new MockTransactionRepository();

        initViews();
        setupBottomNav();
        setupMonthTabs();
        setupTransactionList();
        loadData();
    }

    private void initViews() {
        tvTotalBalance = findViewById(R.id.tvTotalBalance);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        rvTransactions = findViewById(R.id.rvTransactions);

        tabPrevMonth = findViewById(R.id.tabPrevMonth);
        tabCurrentMonth = findViewById(R.id.tabCurrentMonth);
        tabNextMonth = findViewById(R.id.tabNextMonth);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupTransactionList() {
        transactionAdapter = new TransactionAdapter();
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(transactionAdapter);
        rvTransactions.setNestedScrollingEnabled(false);
    }

    private void setupMonthTabs() {
        tabPrevMonth.setOnClickListener(v -> {
            currentMonthOffset = -1;
            updateTabStyles();
            loadData();
        });

        tabCurrentMonth.setOnClickListener(v -> {
            currentMonthOffset = 0;
            updateTabStyles();
            loadData();
        });

        tabNextMonth.setOnClickListener(v -> {
            currentMonthOffset = 1;
            updateTabStyles();
            loadData();
        });
    }

    private void updateTabStyles() {
        int activeColor = getResources().getColor(R.color.white, null);
        int inactiveColor = getResources().getColor(R.color.home_text_secondary, null);

        tabPrevMonth.setTextColor(currentMonthOffset == -1 ? activeColor : inactiveColor);
        tabCurrentMonth.setTextColor(currentMonthOffset == 0 ? activeColor : inactiveColor);
        tabNextMonth.setTextColor(currentMonthOffset == 1 ? activeColor : inactiveColor);
    }

    private void loadData() {
        tvTotalBalance.setText(String.format(Locale.getDefault(), "%,.0f đ", transactionRepository.getTotalBalance()));
        tvTotalExpense.setText(String.format(Locale.getDefault(), "%,.0f đ", transactionRepository.getTotalExpense()));
        tvTotalIncome.setText(String.format(Locale.getDefault(), "%,.0f đ", transactionRepository.getTotalIncome()));

        List<TransactionGroup> groups = transactionRepository.getTransactionsByMonth(currentMonthOffset);
        transactionAdapter.setGroups(groups);
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        FloatingActionButton fab = findViewById(R.id.fabAdd);

        bottomNav.setSelectedItemId(R.id.nav_transaction);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent = new Intent(this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_transaction) {
                return true;
            } else if (id == R.id.nav_wallet) {
                Toast.makeText(this, "Feature coming soon!", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_account) {
                Toast.makeText(this, "Feature coming soon!", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        if (fab != null) {
            fab.setOnClickListener(v ->
                    Toast.makeText(this, "Add New Transaction", Toast.LENGTH_SHORT).show());
        }
    }
}
