package com.ptithcm.quanlichitieu.ui.wallet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.model.Wallet;

import java.util.Locale;

public class EditWalletFragment extends Fragment {

    private static final String ARG_WALLET_ID = "arg_wallet_id";
    
    private WalletViewModel viewModel;
    private Wallet currentWallet;
    private String walletId;

    private EditText etName;
    private EditText etBalance;
    private TextView tvCurrency;

    public static EditWalletFragment newInstance(String walletId) {
        EditWalletFragment fragment = new EditWalletFragment();
        Bundle args = new Bundle();
        args.putString(ARG_WALLET_ID, walletId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            walletId = getArguments().getString(ARG_WALLET_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_wallet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(requireActivity()).get(WalletViewModel.class);
        
        initViews(view);
        loadWalletData();
    }

    private void initViews(View view) {
        etName = view.findViewById(R.id.etName);
        etBalance = view.findViewById(R.id.etBalance);
        tvCurrency = view.findViewById(R.id.tvCurrency);
        
        view.findViewById(R.id.btnClose).setOnClickListener(v -> 
                requireActivity().getSupportFragmentManager().popBackStack());
        
        view.findViewById(R.id.tvSave).setOnClickListener(v -> saveChanges());
        
        view.findViewById(R.id.btnDelete).setOnClickListener(v -> showDeleteConfirmDialog());
    }

    private void showDeleteConfirmDialog() {
        if (currentWallet == null) return;
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa ví")
                .setMessage("Bạn có chắc chắn muốn xóa ví \"" + currentWallet.getName() + "\" này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    viewModel.deleteWallet(currentWallet);
                    Toast.makeText(requireContext(), "Đã xóa ví", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void loadWalletData() {
        // In a real app, you'd get this from the ViewModel/Repo by ID
        // For now, let's find it in the current list
        if (viewModel.getWallets().getValue() != null) {
            for (Wallet w : viewModel.getWallets().getValue()) {
                if (w.getId().equals(walletId)) {
                    currentWallet = w;
                    break;
                }
            }
        }

        if (currentWallet != null) {
            etName.setText(currentWallet.getName());
            etBalance.setText(String.valueOf(currentWallet.getInitialBalance()));
            tvCurrency.setText(currentWallet.getCurrency().equals("VND") ? "Việt Nam Đồng" : currentWallet.getCurrency());
        }
    }

    private void saveChanges() {
        String name = etName.getText().toString().trim();
        String balanceStr = etBalance.getText().toString().trim();
        
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập tên ví", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentWallet != null) {
            viewModel.updateWallet(currentWallet, name, balanceStr);
        }

        Toast.makeText(requireContext(), "Đã lưu thay đổi", Toast.LENGTH_SHORT).show();
        requireActivity().getSupportFragmentManager().popBackStack();
    }
}
