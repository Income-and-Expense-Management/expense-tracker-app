package com.ptithcm.quanlichitieu.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.ui.home.adapter.TopExpenseAdapter;
import com.ptithcm.quanlichitieu.ui.main.MainActivity;
import com.ptithcm.quanlichitieu.ui.wallet.WalletViewModel;

import java.util.Locale;

public class HomeFragment extends Fragment {

    public static final String ARG_USERNAME = "arg_username";

    private HomeViewModel homeViewModel;
    private WalletViewModel walletViewModel;
    private TopExpenseAdapter topExpenseAdapter;

    private TextView tvHomeTitle;
    private TextView tvBalanceValue;
    private TextView tvWalletDetailName;
    private TextView tvWalletDetailValue;
    private View cardWallet;
    private TextView tvSeeAllWallets;
    private RecyclerView rvTopExpenses;
    private MaterialButtonToggleGroup togglePeriod;

    public static HomeFragment newInstance(String username) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USERNAME, username);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        // Sử dụng Activity scope để dùng chung dữ liệu ví với WalletFragment
        walletViewModel = new ViewModelProvider(requireActivity()).get(WalletViewModel.class);

        initViews(view);
        setupTopExpensesList();
        setupPeriodToggle();
        setupWalletActions();
        observeViewModels();

        String username = getArguments() != null
                ? getArguments().getString(ARG_USERNAME, "Duy") : "Duy";
        homeViewModel.setUsername(username);
        
        // Tải ví đang hoạt động
        walletViewModel.loadActiveWallet();
        homeViewModel.loadTopExpenses();
    }

    private void initViews(View view) {
        tvHomeTitle = view.findViewById(R.id.tvHomeTitle);
        tvBalanceValue = view.findViewById(R.id.tvBalanceValue);
        tvWalletDetailName = view.findViewById(R.id.tvWalletDetailName);
        tvWalletDetailValue = view.findViewById(R.id.tvWalletDetailValue);
        cardWallet = view.findViewById(R.id.cardWallet);
        tvSeeAllWallets = view.findViewById(R.id.tvSeeAllWallets);
        rvTopExpenses = view.findViewById(R.id.rvTopExpenses);
        togglePeriod = view.findViewById(R.id.togglePeriod);
    }

    private void setupTopExpensesList() {
        topExpenseAdapter = new TopExpenseAdapter();
        topExpenseAdapter.setOnExpenseClickListener(expense ->
                Toast.makeText(requireContext(),
                        "Clicked: " + expense.getCategory(), Toast.LENGTH_SHORT).show());

        rvTopExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTopExpenses.setAdapter(topExpenseAdapter);
        rvTopExpenses.setNestedScrollingEnabled(false);
    }

    private void setupPeriodToggle() {
        togglePeriod.check(R.id.btnMonth);
        togglePeriod.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                homeViewModel.setPeriodFilter(checkedId == R.id.btnMonth);
            }
        });
    }

    private void setupWalletActions() {
        if (tvSeeAllWallets != null) {
            tvSeeAllWallets.setOnClickListener(v -> {
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).openWalletList();
                }
            });
        }

        if (cardWallet != null) {
            cardWallet.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer,
                                new com.ptithcm.quanlichitieu.ui.wallet.AddWalletFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }
    }

    private void observeViewModels() {
        homeViewModel.getUsername().observe(getViewLifecycleOwner(), username -> {
            if (tvHomeTitle != null) tvHomeTitle.setText("Hello " + username + "!");
        });

        // Quan sát ví đang được chọn từ WalletViewModel (Shared)
        walletViewModel.getSelectedWallet().observe(getViewLifecycleOwner(), wallet -> {
            if (wallet != null) {
                String balanceStr = String.format(Locale.getDefault(), "%,d đ", wallet.getInitialBalance());
                tvBalanceValue.setText(balanceStr);
                if (tvWalletDetailName != null) tvWalletDetailName.setText(wallet.getName());
                if (tvWalletDetailValue != null) tvWalletDetailValue.setText(balanceStr);
            } else {
                tvBalanceValue.setText("0 đ");
                if (tvWalletDetailName != null) tvWalletDetailName.setText("Chưa có ví");
                if (tvWalletDetailValue != null) tvWalletDetailValue.setText("Nhấn để tạo");
            }
        });

        homeViewModel.getTopExpenses().observe(getViewLifecycleOwner(), expenses ->
                topExpenseAdapter.setExpenses(expenses));
    }

    @Override
    public void onResume() {
        super.onResume();
        walletViewModel.loadActiveWallet();
    }
}
