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
import com.ptithcm.quanlichitieu.data.model.Wallet;
import com.ptithcm.quanlichitieu.ui.home.adapter.TopExpenseAdapter;

import java.util.Locale;

public class HomeFragment extends Fragment {

    public static final String ARG_USERNAME = "arg_username";

    private HomeViewModel viewModel;
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

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        initViews(view);
        setupTopExpensesList();
        setupPeriodToggle();
        setupWalletActions();
        observeViewModel();

        String username = getArguments() != null
                ? getArguments().getString(ARG_USERNAME, "Duy") : "Duy";
        viewModel.setUsername(username);
        viewModel.loadWallet();
        viewModel.loadTopExpenses();
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
                viewModel.setPeriodFilter(checkedId == R.id.btnMonth);
            }
        });
    }

    private void setupWalletActions() {
        View.OnClickListener addWalletListener = v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer,
                            new com.ptithcm.quanlichitieu.ui.wallet.AddWalletFragment())
                    .addToBackStack(null)
                    .commit();
        };

        if (tvSeeAllWallets != null) tvSeeAllWallets.setOnClickListener(addWalletListener);
        if (cardWallet != null) cardWallet.setOnClickListener(addWalletListener);
    }

    private void observeViewModel() {
        viewModel.getUsername().observe(getViewLifecycleOwner(), username -> {
            if (tvHomeTitle != null) {
                tvHomeTitle.setText("Hello " + username + "!");
            }
        });

        viewModel.getWallet().observe(getViewLifecycleOwner(), wallet -> {
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

        viewModel.getTopExpenses().observe(getViewLifecycleOwner(), expenses ->
                topExpenseAdapter.setExpenses(expenses));
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.loadWallet();
    }
}
