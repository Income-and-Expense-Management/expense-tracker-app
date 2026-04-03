package com.ptithcm.quanlichitieu.ui.main;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.ui.account.AccountFragment;
import com.ptithcm.quanlichitieu.ui.home.HomeFragment;
import com.ptithcm.quanlichitieu.ui.transaction.TransactionFragment;
import com.ptithcm.quanlichitieu.ui.budget.BudgetFragment;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_USERNAME = "extra_username";

    private static final String TAG_HOME = "frag_home";
    private static final String TAG_TRANSACTION = "frag_transaction";
    private static final String TAG_ACCOUNT = "frag_account";
    private static final String TAG_BUDGET = "frag_budget";

    private Fragment activeFragment;
    private HomeFragment homeFragment;
    private TransactionFragment transactionFragment;
    private BudgetFragment budgetFragment;
    private AccountFragment accountFragment;

    private BottomNavigationView bottomNav;

    /**
     * Open Wallet list as a separate screen (not a bottom tab).
     * This keeps bottom navigation clean while still allowing Home -> "See all wallets".
     */
    public void openWalletList() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new com.ptithcm.quanlichitieu.ui.wallet.WalletFragment())
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
    }

    private void initFragments(String username) {
        homeFragment = HomeFragment.newInstance(username);
        transactionFragment = new TransactionFragment();
        budgetFragment = new BudgetFragment();
        accountFragment = new com.ptithcm.quanlichitieu.ui.account.AccountFragment();

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
        homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag(TAG_HOME);
        transactionFragment = (TransactionFragment) getSupportFragmentManager().findFragmentByTag(TAG_TRANSACTION);
        budgetFragment = (BudgetFragment) getSupportFragmentManager().findFragmentByTag(TAG_BUDGET);
        accountFragment = (AccountFragment) getSupportFragmentManager().findFragmentByTag(TAG_ACCOUNT);

        if (homeFragment != null && homeFragment.isVisible()) activeFragment = homeFragment;
        else if (transactionFragment != null && transactionFragment.isVisible()) activeFragment = transactionFragment;
        else if (budgetFragment != null && budgetFragment.isVisible()) activeFragment = budgetFragment;
        else if (accountFragment != null && accountFragment.isVisible()) activeFragment = accountFragment;
        else activeFragment = homeFragment;
    }

    private void setupBottomNav() {
        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                switchFragment(homeFragment);
                return true;
            } else if (id == R.id.nav_transaction) {
                switchFragment(transactionFragment);
                return true;
            } else if (id == R.id.nav_budget) {
                switchFragment(budgetFragment);
                return true;
            } else if (id == R.id.nav_account) {
                switchFragment(accountFragment);
                return true;
            }
            return false;
        });
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fabAdd);
        if (fab != null) {
            fab.setOnClickListener(v ->
                    Toast.makeText(this, "Add New Transaction", Toast.LENGTH_SHORT).show());
        }
    }

    private void switchFragment(Fragment target) {
        if (target == null || target == activeFragment) return;

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.hide(activeFragment);
        ft.show(target);
        ft.commit();

        activeFragment = target;
    }



    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                    return;
                }

                if (activeFragment != homeFragment) {
                    switchFragment(homeFragment);
                    bottomNav.setSelectedItemId(R.id.nav_home);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }
}
