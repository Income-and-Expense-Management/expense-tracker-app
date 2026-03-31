package com.ptithcm.quanlichitieu.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.local.BudgetDatabaseHelper;
import com.ptithcm.quanlichitieu.data.model.Expense;
import com.ptithcm.quanlichitieu.data.model.Wallet;
import com.ptithcm.quanlichitieu.data.repository.ExpenseRepository;
import com.ptithcm.quanlichitieu.data.repository.MockExpenseRepository;
import com.ptithcm.quanlichitieu.ui.home.adapter.TopExpenseAdapter;
import com.ptithcm.quanlichitieu.ui.transaction.TransactionActivity;
import com.ptithcm.quanlichitieu.ui.wallet.AddWalletActivity;

import java.util.List;
import java.util.Locale;

/**
 * HomeActivity: The main screen of the application after a successful login.
 * Displays a dashboard with balance, wallet, report summary, and top expenses.
 *
 * ARCHITECTURE NOTE:
 * This Activity follows the Repository Pattern for data access:
 * - It depends on the ExpenseRepository INTERFACE, not a concrete implementation
 * - Currently uses MockExpenseRepository for development
 * - To switch to real API, simply replace the repository instantiation
 *
 * HOW TO REPLACE MOCK DATA WITH REAL API:
 * ────────────────────────────────────────────────────────────────────────────
 * 1. Create ApiExpenseRepository that implements ExpenseRepository
 * 2. Replace the line:
 *      expenseRepository = new MockExpenseRepository();
 *    with:
 *      expenseRepository = new ApiExpenseRepository(apiService);
 *
 * For better decoupling, consider using Dependency Injection (Hilt/Dagger):
 *    @Inject ExpenseRepository expenseRepository;
 * ────────────────────────────────────────────────────────────────────────────
 */
public class HomeActivity extends AppCompatActivity {

    public static final String EXTRA_USERNAME = "extra_username";
    private static final int TOP_EXPENSES_LIMIT = 3; // Number of top expenses to display

    private BudgetDatabaseHelper dbHelper;
    private TextView tvBalanceValue;
    private TextView tvWalletDetailName;
    private TextView tvWalletDetailValue;
    private View cardWallet;
    private TextView tvSeeAllWallets;

    // Expense list components
    private RecyclerView rvTopExpenses;
    private TopExpenseAdapter topExpenseAdapter;
    private MaterialButtonToggleGroup togglePeriod;

    /** Tracks the currently selected period filter (Week or Month) */
    private boolean isMonthSelected = true;

    /**
     * Repository for expense data.
     * Depends on the INTERFACE, not the concrete implementation (Dependency Inversion Principle).
     * This allows swapping MockExpenseRepository with ApiExpenseRepository without changing UI code.
     */
    private ExpenseRepository expenseRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize data sources
        dbHelper = new BudgetDatabaseHelper(this);

        /*
         * REPOSITORY INITIALIZATION:
         * Currently using MockExpenseRepository for development/testing.
         *
         * TO REPLACE WITH REAL API:
         * ─────────────────────────────────────────────────────────────────
         * Option 1 - Direct replacement:
         *   expenseRepository = new ApiExpenseRepository(RetrofitClient.getInstance().create(ExpenseApiService.class));
         *
         * Option 2 - Using Dependency Injection (recommended for production):
         *   // In your Application class or DI module, provide the repository
         *   // Then inject it here: @Inject ExpenseRepository expenseRepository;
         * ─────────────────────────────────────────────────────────────────
         */
        expenseRepository = new MockExpenseRepository();

        // Initialize views
        initViews();

        // Retrieve the username passed from the Login screen
        String username = getIntent().getStringExtra(EXTRA_USERNAME);
        if (username == null || username.isEmpty()) {
            username = "Duy"; // Default from design
        }

        // Setup UI components
        setupHeader(username);
        setupBottomNav();
        setupWalletActions();
        setupTopExpensesList();
    }

    /**
     * Initializes all view references.
     * Separated for cleaner code and easier testing.
     */
    private void initViews() {
        tvBalanceValue = findViewById(R.id.tvBalanceValue);
        tvWalletDetailName = findViewById(R.id.tvWalletDetailName);
        tvWalletDetailValue = findViewById(R.id.tvWalletDetailValue);
        cardWallet = findViewById(R.id.cardWallet);
        tvSeeAllWallets = findViewById(R.id.tvSeeAllWallets);
        rvTopExpenses = findViewById(R.id.rvTopExpenses);
        togglePeriod = findViewById(R.id.togglePeriod);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWalletData();
        loadTopExpenses();

        // Ensure "Home" tab is selected when returning to this screen
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            // Check if not already selected to avoid unnecessary re-layout or listener triggers
            if (bottomNav.getSelectedItemId() != R.id.nav_home) {
                // Set the selected item without triggering the listener action logic if possible
                // (Though our listener logic is safe: case nav_home returns true instantly)
                bottomNav.setSelectedItemId(R.id.nav_home);
            }
        }
    }

    /**
     * Sets up the RecyclerView for displaying top expenses.
     * Uses LinearLayoutManager for a vertical list layout.
     */
    private void setupTopExpensesList() {
        topExpenseAdapter = new TopExpenseAdapter();

        // Set click listener for expense items
        topExpenseAdapter.setOnExpenseClickListener(expense -> {
            // Handle expense item click - navigate to expense details or edit screen
            Toast.makeText(this,
                    "Clicked: " + expense.getCategory(),
                    Toast.LENGTH_SHORT).show();
        });

        rvTopExpenses.setLayoutManager(new LinearLayoutManager(this));
        rvTopExpenses.setAdapter(topExpenseAdapter);

        // Disable nested scrolling for smooth scrolling within NestedScrollView
        rvTopExpenses.setNestedScrollingEnabled(false);

        // Setup period toggle (Week/Month)
        setupPeriodToggle();
    }

    /**
     * Sets up the Week/Month segmented toggle for filtering expenses.
     * Default selection is "Month" (Tháng).
     */
    private void setupPeriodToggle() {
        // Set default selection to Month
        togglePeriod.check(R.id.btnMonth);

        togglePeriod.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnWeek) {
                    isMonthSelected = false;
                    // Reload expenses for weekly view
                    loadTopExpenses();
                } else if (checkedId == R.id.btnMonth) {
                    isMonthSelected = true;
                    // Reload expenses for monthly view
                    loadTopExpenses();
                }
            }
        });
    }

    /**
     * Loads top expenses from the repository and updates the UI.
     * Filters based on the currently selected period (Week or Month).
     *
     * NOTE: Currently runs on the main thread with mock data.
     * When using a real API, wrap this in an AsyncTask, Coroutine, or RxJava
     * to avoid blocking the UI thread.
     *
     * TO ADD PERIOD FILTERING WITH REAL API:
     * ─────────────────────────────────────────────────────────────────
     * 1. Add a method to ExpenseRepository: getTopExpenses(int limit, Period period)
     * 2. Pass the period enum (WEEK/MONTH) to filter by date range
     * 3. The API call would include: ?period=week or ?period=month
     * ─────────────────────────────────────────────────────────────────
     */
    private void loadTopExpenses() {
        // Currently using the same mock data for both periods
        // In a real implementation, the repository would filter by date range
        List<Expense> topExpenses = expenseRepository.getTopExpenses(TOP_EXPENSES_LIMIT);
        topExpenseAdapter.setExpenses(topExpenses);
    }

    private void setupWalletActions() {
        View.OnClickListener addWalletListener = v -> {
            Intent intent = new Intent(HomeActivity.this, AddWalletActivity.class);
            startActivity(intent);
        };

        if (tvSeeAllWallets != null) {
            tvSeeAllWallets.setOnClickListener(addWalletListener);
        }

        if (cardWallet != null) {
            cardWallet.setOnClickListener(addWalletListener);
        }
    }

    private void loadWalletData() {
        Wallet wallet = dbHelper.getFirstWallet();
        if (wallet != null) {
            String balanceStr = String.format(Locale.getDefault(), "%,.0f đ", wallet.getBalance());
            tvBalanceValue.setText(balanceStr);

            if (tvWalletDetailName != null) {
                tvWalletDetailName.setText(wallet.getName());
            }
            if (tvWalletDetailValue != null) {
                tvWalletDetailValue.setText(balanceStr);
            }
        } else {
            tvBalanceValue.setText("0 đ");
            if (tvWalletDetailName != null) {
                tvWalletDetailName.setText("Chưa có ví");
            }
            if (tvWalletDetailValue != null) {
                tvWalletDetailValue.setText("Nhấn để tạo");
            }
        }
    }

    private void setupHeader(String username) {
        TextView tvHomeTitle = findViewById(R.id.tvHomeTitle);
        if (tvHomeTitle != null) {
            tvHomeTitle.setText("Hello " + username + "!");
        }
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        FloatingActionButton fab = findViewById(R.id.fabAdd);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_transaction) {
                startActivity(new Intent(this, TransactionActivity.class));
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
            fab.setOnClickListener(v -> {
                Toast.makeText(this, "Add New Transaction", Toast.LENGTH_SHORT).show();
            });
        }
    }
}
