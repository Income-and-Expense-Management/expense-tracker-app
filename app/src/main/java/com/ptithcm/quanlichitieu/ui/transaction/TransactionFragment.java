package com.ptithcm.quanlichitieu.ui.transaction;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.ui.transaction.adapter.TransactionAdapter;
import com.ptithcm.quanlichitieu.ui.wallet.WalletViewModel;

import java.util.Locale;

public class TransactionFragment extends Fragment {

    private TransactionViewModel viewModel;
    private WalletViewModel walletViewModel;
    private TransactionAdapter transactionAdapter;

    private TextView tvTotalBalance;
    private TextView tvTotalExpense;
    private TextView tvTotalIncome;
    private RecyclerView rvTransactions;

    private TextView tvWalletName;

    private TextView tabPrevMonth;
    private TextView tabCurrentMonth;
    private TextView tabNextMonth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        // Dùng chung WalletViewModel với HomeFragment
        walletViewModel = new ViewModelProvider(requireActivity()).get(WalletViewModel.class);

        initViews(view);
        setupTransactionList();
        setupMonthTabs();
        observeViewModels();

        // Tải dữ liệu ban đầu
        walletViewModel.loadActiveWallet();
    }

    private void initViews(View view) {
        tvTotalBalance = view.findViewById(R.id.tvTotalBalance);
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense);
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome);
        rvTransactions = view.findViewById(R.id.rvTransactions);

        tvWalletName = view.findViewById(R.id.tvWalletName);

        tabPrevMonth = view.findViewById(R.id.tabPrevMonth);
        tabCurrentMonth = view.findViewById(R.id.tabCurrentMonth);
        tabNextMonth = view.findViewById(R.id.tabNextMonth);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    private void setupTransactionList() {
        transactionAdapter = new TransactionAdapter();
        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTransactions.setAdapter(transactionAdapter);
        rvTransactions.setNestedScrollingEnabled(false);
    }

    private void setupMonthTabs() {
        tabPrevMonth.setOnClickListener(v -> viewModel.setMonthOffset(-1));
        tabCurrentMonth.setOnClickListener(v -> viewModel.setMonthOffset(0));
        tabNextMonth.setOnClickListener(v -> viewModel.setMonthOffset(1));
    }

    private void updateTabStyles(int offset) {
        if (!isAdded()) return;
        int activeColor = requireContext().getResources().getColor(R.color.white, null);
        int inactiveColor = requireContext().getResources().getColor(R.color.home_text_secondary, null);

        tabPrevMonth.setTextColor(offset == -1 ? activeColor : inactiveColor);
        tabCurrentMonth.setTextColor(offset == 0 ? activeColor : inactiveColor);
        tabNextMonth.setTextColor(offset == 1 ? activeColor : inactiveColor);
    }

    private void observeViewModels() {
        // Quan sát ví từ Shared WalletViewModel
        walletViewModel.getSelectedWallet().observe(getViewLifecycleOwner(), wallet -> {
            if (wallet != null) {
                if (tvWalletName != null) tvWalletName.setText(wallet.getName());
                // Khi ví thay đổi, yêu cầu TransactionViewModel tải lại dữ liệu với ví mới
                viewModel.loadData(wallet);
            } else {
                if (tvWalletName != null) tvWalletName.setText("Chưa có ví");
                viewModel.loadData(null);
            }
        });

        viewModel.getTotalBalance().observe(getViewLifecycleOwner(), balance ->
                tvTotalBalance.setText(String.format(Locale.getDefault(), "%,d đ", balance)));

        viewModel.getTotalExpense().observe(getViewLifecycleOwner(), expense ->
                tvTotalExpense.setText(String.format(Locale.getDefault(), "%,d đ", expense)));

        viewModel.getTotalIncome().observe(getViewLifecycleOwner(), income ->
                tvTotalIncome.setText(String.format(Locale.getDefault(), "%,d đ", income)));

        viewModel.getTransactions().observe(getViewLifecycleOwner(), groups ->
                transactionAdapter.setGroups(groups));

        viewModel.getMonthOffset().observe(getViewLifecycleOwner(), this::updateTabStyles);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Cập nhật lại ví khi quay lại màn hình
        walletViewModel.loadActiveWallet();

        // Tải lại danh sách giao dịch phòng trường hợp vừa thêm mới từ trang khác
        com.ptithcm.quanlichitieu.data.model.Wallet currentWallet = walletViewModel.getSelectedWallet().getValue();
        if (currentWallet != null) {
            viewModel.loadData(currentWallet);
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            // Khi tab được hiển thị lại (trong trường hợp sử dụng hide/show fragment)
            walletViewModel.loadActiveWallet();

            com.ptithcm.quanlichitieu.data.model.Wallet currentWallet = walletViewModel.getSelectedWallet().getValue();
            if (currentWallet != null) {
                viewModel.loadData(currentWallet);
            }
        }
    }
}
