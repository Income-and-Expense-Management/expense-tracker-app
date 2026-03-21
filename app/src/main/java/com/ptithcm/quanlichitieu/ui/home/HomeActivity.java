package com.ptithcm.quanlichitieu.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.db.BudgetDatabaseHelper;
import com.ptithcm.quanlichitieu.data.models.Wallet;
import com.ptithcm.quanlichitieu.ui.wallet.AddWalletActivity;

import java.util.Locale;

/**
 * HomeActivity: The main screen of the application after a successful login.
 * Displays a dashboard with balance, wallet, and report summary.
 */
public class HomeActivity extends AppCompatActivity {

    public static final String EXTRA_USERNAME = "extra_username";

    private BudgetDatabaseHelper dbHelper;
    private TextView tvBalanceValue;
    private TextView tvWalletDetailName;
    private TextView tvWalletDetailValue;
    private View cardWallet;
    private TextView tvSeeAllWallets;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        dbHelper = new BudgetDatabaseHelper(this);

        tvBalanceValue = findViewById(R.id.tvBalanceValue);
        tvWalletDetailName = findViewById(R.id.tvWalletDetailName);
        tvWalletDetailValue = findViewById(R.id.tvWalletDetailValue);
        cardWallet = findViewById(R.id.cardWallet);
        tvSeeAllWallets = findViewById(R.id.tvSeeAllWallets);

        // Retrieve the username passed from the Login screen
        String username = getIntent().getStringExtra(EXTRA_USERNAME);
        if (username == null || username.isEmpty()) {
            username = "Duy"; // Default from design
        }

        // Setup UI
        setupHeader(username);
        setupBottomNav();
        setupWalletActions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWalletData();
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

        // Disable the center placeholder so it cannot be selected

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_transaction) {
                Toast.makeText(this, "Feature coming soon!", Toast.LENGTH_SHORT).show();
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
