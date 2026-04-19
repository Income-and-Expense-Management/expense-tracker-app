package com.ptithcm.quanlichitieu.ui.wallet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.model.Wallet;
import com.ptithcm.quanlichitieu.ui.login.AuthViewModel;

public class EditWalletFragment extends Fragment {

    private static final String ARG_WALLET_ID = "arg_wallet_id";
    
    private WalletViewModel viewModel;
    private Wallet currentWallet;
    private String walletId;
    private String selectedIconId = "ic_wallet"; // default icon

    private EditText etName;
    private EditText etBalance;
    private TextView tvCurrency;
    private ImageView ivWalletIcon;

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
        AuthViewModel authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        viewModel.setCurrentUserId(authViewModel.getUserId());

        initViews(view);
        observeViewModel();
        // Load wallet trực tiếp theo ID, không phụ thuộc vào getWallets().getValue()
        viewModel.loadWalletById(walletId);

        getParentFragmentManager().setFragmentResultListener(
                IconSelectionFragment.REQUEST_KEY_ICON,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    String newIcon = result.getString(IconSelectionFragment.RESULT_ICON_ID);
                    if (newIcon != null) {
                        selectedIconId = newIcon;
                        updateWalletIcon(newIcon);
                    }
                });
    }

    private void updateWalletIcon(String iconName) {
        if (ivWalletIcon != null && getContext() != null) {
            int resId = getResources().getIdentifier(iconName, "drawable", getContext().getPackageName());
            if (resId != 0) {
                ivWalletIcon.setImageResource(resId);
            }
        }
    }

    private void observeViewModel() {
        // Observe wallet được load theo ID để fill form
        viewModel.getSingleWallet().observe(getViewLifecycleOwner(), wallet -> {
            if (wallet != null) {
                currentWallet = wallet;
                fillForm(wallet);
            }
        });

        viewModel.getSaveResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                Toast.makeText(requireContext(), result.getMessage(), Toast.LENGTH_SHORT).show();
                if (result.isSuccess()) {
                    requireActivity().getSupportFragmentManager().popBackStack();
                    viewModel.clearSaveResult();
                }
            }
        });
    }

    private void fillForm(Wallet wallet) {
        etName.setText(wallet.getName());
        etBalance.setText(String.valueOf(wallet.getInitialBalance()));
        tvCurrency.setText(wallet.getCurrency().equals("VND") ? "Việt Nam Đồng" : wallet.getCurrency());
        if (wallet.getIconId() != null && !wallet.getIconId().isEmpty()) {
            selectedIconId = wallet.getIconId();
            updateWalletIcon(selectedIconId);
        }
    }

    private void initViews(View view) {
        etName = view.findViewById(R.id.etName);
        etBalance = view.findViewById(R.id.etBalance);
        tvCurrency = view.findViewById(R.id.tvCurrency);
        ivWalletIcon = view.findViewById(R.id.ivWalletIcon);

        ivWalletIcon.setOnClickListener(v -> openIconSelection());

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
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void loadWalletData() {
        // Deprecated: đã chuyển sang loadWalletById() + observe getSingleWallet()
        // Giữ lại method rỗng để không ảnh hưởng cấu trúc
    }

    private void openIconSelection() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragmentContainer, new IconSelectionFragment())
                .addToBackStack(null)
                .commit();
    }

    private void saveChanges() {
        String name = etName.getText().toString().trim();
        String balanceStr = etBalance.getText().toString().trim();
        
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập tên ví", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentWallet != null) {
            currentWallet.setIconId(selectedIconId);
            viewModel.updateWallet(currentWallet, name, balanceStr);
        }
    }
}
