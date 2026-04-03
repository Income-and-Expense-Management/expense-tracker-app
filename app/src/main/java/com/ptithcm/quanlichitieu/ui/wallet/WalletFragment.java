package com.ptithcm.quanlichitieu.ui.wallet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.ptithcm.quanlichitieu.R;

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
        adapter.setOnWalletClickListener(wallet -> {
            Toast.makeText(requireContext(), "Chọn ví: " + wallet.getName(), Toast.LENGTH_SHORT).show();
            viewModel.selectWallet(wallet);
        });

        rvWallets.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvWallets.setAdapter(adapter);
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
