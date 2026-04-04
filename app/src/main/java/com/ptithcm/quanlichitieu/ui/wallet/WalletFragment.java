package com.ptithcm.quanlichitieu.ui.wallet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.model.Wallet;

public class WalletFragment extends Fragment {

    private WalletViewModel viewModel;
    private WalletAdapter adapter;
    private RecyclerView rvWallets;
    private LinearLayout layoutEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wallet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(WalletViewModel.class);

        initViews(view);
        setupRecyclerView();
        observeViewModel();

        viewModel.loadAllWallets();
    }

    private void initViews(View view) {
        rvWallets = view.findViewById(R.id.rvWallets);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        MaterialButton btnAddFirstWallet = view.findViewById(R.id.btnAddFirstWallet);

        if (btnAddFirstWallet != null) {
            btnAddFirstWallet.setOnClickListener(v -> openAddWallet());
        }
    }

    private void setupRecyclerView() {
        adapter = new WalletAdapter();
        adapter.setOnWalletClickListener(this::showSelectionDialog);

        rvWallets.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvWallets.setAdapter(adapter);
    }

    /**
     * Hiển thị dialog xác nhận khi chọn ví bằng MaterialAlertDialogBuilder.
     */
    private void showSelectionDialog(Wallet wallet) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xác nhận chọn ví")
                .setMessage("Bạn có muốn chọn ví \"" + wallet.getName() + "\" để quản lý chi tiêu không?")
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    viewModel.selectWallet(wallet);
                    navigateToHome();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void navigateToHome() {
        BottomNavigationView nav = requireActivity().findViewById(R.id.bottomNav);
        if (nav != null) {
            nav.setSelectedItemId(R.id.nav_home);
        }
    }

    private void observeViewModel() {
        viewModel.getWallets().observe(getViewLifecycleOwner(), wallets -> {
            if (wallets == null || wallets.isEmpty()) {
                rvWallets.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
            } else {
                rvWallets.setVisibility(View.VISIBLE);
                layoutEmpty.setVisibility(View.GONE);
                adapter.setWallets(wallets);
            }
        });
    }

    private void openAddWallet() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new AddWalletFragment())
                .addToBackStack(null)
                .commit();
    }
}
