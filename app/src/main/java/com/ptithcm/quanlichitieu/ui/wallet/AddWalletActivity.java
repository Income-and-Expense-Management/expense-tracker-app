package com.ptithcm.quanlichitieu.ui.wallet;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.local.BudgetDatabaseHelper;
import com.ptithcm.quanlichitieu.data.model.Wallet;

public class AddWalletActivity extends AppCompatActivity {

    private EditText etName;
    private EditText etBalance;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_wallet);

        etName = findViewById(R.id.etName);
        etBalance = findViewById(R.id.etBalance);
        btnSave = findViewById(R.id.btnSave);

        btnSave.setOnClickListener(v -> saveWallet());
    }

    private void saveWallet() {
        String name = etName.getText().toString();
        String balanceStr = etBalance.getText().toString();

        if (name.isEmpty() || balanceStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double balance = Double.parseDouble(balanceStr);
            Wallet wallet = new Wallet(name, balance);

            BudgetDatabaseHelper dbHelper = new BudgetDatabaseHelper(this);
            dbHelper.addWallet(wallet);

            Toast.makeText(this, "Thêm ví thành công: " + name, Toast.LENGTH_SHORT).show();
            finish();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số dư không hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }
}
