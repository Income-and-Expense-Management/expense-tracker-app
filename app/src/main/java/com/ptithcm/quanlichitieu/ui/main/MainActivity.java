package com.ptithcm.quanlichitieu.ui.main;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.ui.account.AccountFragment;
import com.ptithcm.quanlichitieu.ui.home.HomeFragment;
import com.ptithcm.quanlichitieu.ui.transaction.AddTransactionFragment;
import com.ptithcm.quanlichitieu.ui.transaction.TransactionDetailFragment;
import com.ptithcm.quanlichitieu.ui.transaction.TransactionFragment;
import com.ptithcm.quanlichitieu.ui.budget.BudgetFragment;
import com.ptithcm.quanlichitieu.ui.wallet.WalletFragment;
import com.ptithcm.quanlichitieu.ui.report.ReportFragment;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_USERNAME = "extra_username";

    private static final String TAG_HOME = "frag_home";
    private static final String TAG_TRANSACTION = "frag_transaction";
    private static final String TAG_ACCOUNT = "frag_account";
    private static final String TAG_BUDGET = "TAG_BUDGET";
    private static final String TAG_WALLET_LIST = "frag_wallet_list";
    private static final String TAG_ADD_TRANSACTION = "frag_add_transaction";
    private static final String TAG_TRANSACTION_DETAIL = "TAG_TRANSACTION_DETAIL";
    private static final String TAG_REPORT = "frag_report";

    private Fragment activeFragment;
    private HomeFragment homeFragment;
    private TransactionFragment transactionFragment;
    private BudgetFragment budgetFragment;
    private AccountFragment accountFragment;

    private BottomNavigationView bottomNav;

    public void openWalletList() {
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentContainer, new WalletFragment(), TAG_WALLET_LIST)
                .addToBackStack(null)
                .commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String username = getIntent().getStringExtra(EXTRA_USERNAME);
        if (username == null || username.isEmpty()) {
            username = "Duy";
        }

        if (savedInstanceState == null) {
            initFragments(username);
        } else {
            restoreFragments();
        }

        setupBottomNav();
        setupFab();
        setupBackNavigation();
        setupBottomNavVisibility();
    }

    private void initFragments(String username) {
        homeFragment = HomeFragment.newInstance(username);
        transactionFragment = new TransactionFragment();
        budgetFragment = new BudgetFragment();
        accountFragment = new AccountFragment();

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentContainer, homeFragment, TAG_HOME)
                .add(R.id.fragmentContainer, transactionFragment, TAG_TRANSACTION)
                .add(R.id.fragmentContainer, budgetFragment, TAG_BUDGET)
                .add(R.id.fragmentContainer, accountFragment, TAG_ACCOUNT)
                .hide(transactionFragment)
                .hide(budgetFragment)
                .hide(accountFragment)
                .commit();

        activeFragment = homeFragment;
    }

    private void restoreFragments() {
        FragmentManager fm = getSupportFragmentManager();
        homeFragment = (HomeFragment) fm.findFragmentByTag(TAG_HOME);
        transactionFragment = (TransactionFragment) fm.findFragmentByTag(TAG_TRANSACTION);
        budgetFragment = (BudgetFragment) fm.findFragmentByTag(TAG_BUDGET);
        accountFragment = (AccountFragment) fm.findFragmentByTag(TAG_ACCOUNT);

        if (homeFragment != null && homeFragment.isVisible()) activeFragment = homeFragment;
        else if (transactionFragment != null && transactionFragment.isVisible()) activeFragment = transactionFragment;
        else if (budgetFragment != null && budgetFragment.isVisible()) activeFragment = budgetFragment;
        else if (accountFragment != null && accountFragment.isVisible()) activeFragment = accountFragment;
        else activeFragment = homeFragment;
    }

    private void setupBottomNav() {
        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            switchToBottomTab(item.getItemId());
            return true;
        });
    }

    private void setupBottomNavVisibility() {
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            boolean isRoot = getSupportFragmentManager().getBackStackEntryCount() == 0;
            
            android.view.View bottomAppBar = findViewById(R.id.bottomAppBar);
            FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
            android.view.View fragmentContainer = findViewById(R.id.fragmentContainer);

            if (isRoot) {
                if (bottomAppBar != null) bottomAppBar.setVisibility(android.view.View.VISIBLE);
                if (fabAdd != null) fabAdd.show();
                
                // Trả lại margin bottom cho màn hình chính (80dp)
                if (fragmentContainer != null) {
                    android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) fragmentContainer.getLayoutParams();
                    params.bottomMargin = (int) (80 * getResources().getDisplayMetrics().density);
                    fragmentContainer.setLayoutParams(params);
                }
            } else {
                if (bottomAppBar != null) bottomAppBar.setVisibility(android.view.View.GONE);
                if (fabAdd != null) fabAdd.hide();
                
                // Loại bỏ margin bottom để màn hình phụ chiếm toàn màn hình
                if (fragmentContainer != null) {
                    android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) fragmentContainer.getLayoutParams();
                    params.bottomMargin = 0;
                    fragmentContainer.setLayoutParams(params);
                }
            }
        });
    }

    private void switchToBottomTab(int id) {
        Fragment target;
        if (id == R.id.nav_home) target = homeFragment;
        else if (id == R.id.nav_transaction) target = transactionFragment;
        else if (id == R.id.nav_budget) target = budgetFragment;
        else if (id == R.id.nav_account) target = accountFragment;
        else return;

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        // 1. Tìm và xóa màn hình phụ ngay trong transaction này để tránh bị nháy trang cũ
        Fragment walletFrag = fm.findFragmentByTag(TAG_WALLET_LIST);
        if (walletFrag != null) {
            ft.remove(walletFrag);
            // Dọn dẹp BackStack mà không làm gián đoạn UI
            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        // 2. Chuyển sang tab được chọn
        if (target != activeFragment) {
            ft.hide(activeFragment);
            ft.show(target);
            activeFragment = target;
        }

        ft.commit();
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fabAdd);
        if (fab != null) {
            fab.setOnClickListener(v -> openAddTransaction());
        }
    }

    private void openAddTransaction() {
        // avoid adding multiple add-transaction fragments
        if (getSupportFragmentManager().findFragmentByTag(TAG_ADD_TRANSACTION) != null) return;

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        // hide current active fragment so AddTransactionFragment is displayed cleanly
        if (activeFragment != null && activeFragment.isAdded()) {
            ft.hide(activeFragment);
        }
        ft.add(R.id.fragmentContainer, new AddTransactionFragment(), TAG_ADD_TRANSACTION)
                .addToBackStack(null)
                .commit();
    }

    public void openEditTransaction(String transactionId) {
        if (getSupportFragmentManager().findFragmentByTag(TAG_ADD_TRANSACTION) != null) return;

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (activeFragment != null && activeFragment.isAdded()) {
            ft.hide(activeFragment);
        }
        ft.add(R.id.fragmentContainer, AddTransactionFragment.newInstance(transactionId), TAG_ADD_TRANSACTION)
                .addToBackStack(null)
                .commit();
    }

    public void openTransactionDetail(String transactionId) {
        if (getSupportFragmentManager().findFragmentByTag(TAG_TRANSACTION_DETAIL) != null) return;

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (activeFragment != null && activeFragment.isAdded()) {
            ft.hide(activeFragment);
        }
        ft.add(R.id.fragmentContainer, TransactionDetailFragment.newInstance(transactionId), TAG_TRANSACTION_DETAIL)
                .addToBackStack(null)
                .commit();
    }

    public void openReport() {
        if (getSupportFragmentManager().findFragmentByTag(TAG_REPORT) != null) return;

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (activeFragment != null && activeFragment.isAdded()) {
            ft.hide(activeFragment);
        }
        ft.add(R.id.fragmentContainer, new ReportFragment(), TAG_REPORT)
                .addToBackStack(null)
                .commit();
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fm = getSupportFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                    return;
                }

                if (activeFragment != homeFragment) {
                    switchToBottomTab(R.id.nav_home);
                    bottomNav.setSelectedItemId(R.id.nav_home);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }
}