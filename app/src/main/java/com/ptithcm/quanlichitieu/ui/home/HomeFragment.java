package com.ptithcm.quanlichitieu.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import com.ptithcm.quanlichitieu.event.BudgetUpdateEvent;
import com.ptithcm.quanlichitieu.event.EventBus;
import com.ptithcm.quanlichitieu.ui.home.adapter.TopExpenseAdapter;
import com.ptithcm.quanlichitieu.ui.main.MainActivity;
import com.ptithcm.quanlichitieu.ui.search.SearchTransactionFragment;
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
    private ImageView imgWalletIcon;
    private ImageView imgSearch;
    private View cardWallet;
    private TextView tvSeeAllWallets;
    private TextView tvTotalSpentValue;
    private TextView tvTotalIncomeValue;
    private RecyclerView rvTopExpenses;
    private MaterialButtonToggleGroup togglePeriod;
    private com.ptithcm.quanlichitieu.ui.common.SimpleLineChart lineChart;

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
        observeEvents();

        String username = getArguments() != null
                ? getArguments().getString(ARG_USERNAME, "Duy") : "Duy";
        homeViewModel.setUsername(username);
        
        // Tải ví đang hoạt động
        walletViewModel.loadActiveWallet();
        homeViewModel.loadTopExpenses();
    }

    private void observeEvents() {
        // Lắng nghe event khi có thay đổi transaction (thêm/sửa/xoá) để refresh dashboard ngay.
        // Không phụ thuộc vào onResume vì MainActivity dùng hide/show cho bottom tabs.
        EventBus.getInstance().getBudgetUpdateEvent().observe(getViewLifecycleOwner(), event -> {
            if (event == null) return;

            handleTransactionChanged(event);
            EventBus.getInstance().clearBudgetUpdateEvent();
        });
    }

    private void handleTransactionChanged(@NonNull BudgetUpdateEvent event) {
        // Chỉ refresh nếu event liên quan đến ví hiện tại (nếu có).
        com.ptithcm.quanlichitieu.data.model.Wallet currentWallet = walletViewModel.getSelectedWallet().getValue();
        if (currentWallet != null && event.getWalletId() != null && !event.getWalletId().equals(currentWallet.getId())) {
            return;
        }

        homeViewModel.refreshDashboard();
    }

    private void initViews(View view) {
        tvHomeTitle = view.findViewById(R.id.tvHomeTitle);
        tvBalanceValue = view.findViewById(R.id.tvBalanceValue);
        tvWalletDetailName = view.findViewById(R.id.tvWalletDetailName);
        tvWalletDetailValue = view.findViewById(R.id.tvWalletDetailValue);
        imgWalletIcon = view.findViewById(R.id.imgWalletIcon);
        imgSearch = view.findViewById(R.id.imgSearch);
        cardWallet = view.findViewById(R.id.cardWallet);
        tvSeeAllWallets = view.findViewById(R.id.tvSeeAllWallets);
        tvTotalSpentValue = view.findViewById(R.id.tvTotalSpentValue);
        tvTotalIncomeValue = view.findViewById(R.id.tvTotalIncomeValue);
        rvTopExpenses = view.findViewById(R.id.rvTopExpenses);
        togglePeriod = view.findViewById(R.id.togglePeriod);
        lineChart = view.findViewById(R.id.lineChart);

        // Mở màn hình tìm kiếm với walletId hiện tại
        imgSearch.setOnClickListener(v -> openSearch());

        View tabExpense = view.findViewById(R.id.tabExpense);
        View tabIncome = view.findViewById(R.id.tabIncome);
        View lineExpense = view.findViewById(R.id.lineExpense);
        View lineIncome = view.findViewById(R.id.lineIncome);

        if (tabExpense != null && tabIncome != null && lineExpense != null && lineIncome != null) {
            tabExpense.setOnClickListener(v -> {
                lineChart.setExpenseMode(true);
                lineExpense.setBackgroundColor(requireContext().getResources().getColor(R.color.home_expense_red, null));
                lineIncome.setBackgroundColor(android.graphics.Color.parseColor("#222222"));
                if (tvTotalSpentValue != null) tvTotalSpentValue.setTextColor(requireContext().getResources().getColor(R.color.home_expense_red, null));
                if (tvTotalIncomeValue != null) tvTotalIncomeValue.setTextColor(android.graphics.Color.parseColor("#555555"));
            });

            tabIncome.setOnClickListener(v -> {
                lineChart.setExpenseMode(false);
                lineIncome.setBackgroundColor(requireContext().getResources().getColor(R.color.home_accent_green, null));
                lineExpense.setBackgroundColor(android.graphics.Color.parseColor("#222222"));
                if (tvTotalIncomeValue != null) tvTotalIncomeValue.setTextColor(requireContext().getResources().getColor(R.color.home_accent_green, null));
                if (tvTotalSpentValue != null) tvTotalSpentValue.setTextColor(android.graphics.Color.parseColor("#555555"));
            });
        }
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

    private void openSearch() {
        Wallet currentWallet = walletViewModel.getSelectedWallet().getValue();
        String walletId = currentWallet != null ? currentWallet.getId() : null;
        SearchTransactionFragment searchFragment = SearchTransactionFragment.newInstance(walletId);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, searchFragment)
                .addToBackStack(null)
                .commit();
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
            homeViewModel.setWallet(wallet); // Add this line

            if (wallet != null) {
                homeViewModel.calculateCurrentBalance(wallet);
                if (tvWalletDetailName != null) tvWalletDetailName.setText(wallet.getName());

                if (imgWalletIcon != null) {
                    if (wallet.getIconId() != null && !wallet.getIconId().isEmpty()) {
                        int resId = requireContext().getResources().getIdentifier(
                                wallet.getIconId(), "drawable", requireContext().getPackageName());
                        if (resId != 0) {
                            imgWalletIcon.setImageResource(resId);
                        } else {
                            imgWalletIcon.setImageResource(R.drawable.ic_payment_method);
                        }
                    } else {
                        imgWalletIcon.setImageResource(R.drawable.ic_payment_method);
                    }
                }
            } else {
                homeViewModel.calculateCurrentBalance(null);
                if (tvWalletDetailName != null) tvWalletDetailName.setText("Chưa có ví");
                if (tvWalletDetailValue != null) tvWalletDetailValue.setText("Nhấn để tạo");
                if (imgWalletIcon != null) {
                    imgWalletIcon.setImageResource(R.drawable.ic_payment_method);
                }
            }
        });

        homeViewModel.getTotalBalance().observe(getViewLifecycleOwner(), balance -> {
            String balanceStr = String.format(Locale.getDefault(), "%,d đ", balance);
            if (tvBalanceValue != null) tvBalanceValue.setText(balanceStr);
            if (tvWalletDetailValue != null) tvWalletDetailValue.setText(balanceStr);
        });

        homeViewModel.getTotalSpent().observe(getViewLifecycleOwner(), spent -> {
            if (tvTotalSpentValue != null) {
                tvTotalSpentValue.setText(String.format(Locale.getDefault(), "%,d đ", spent));
            }
        });

        homeViewModel.getTotalIncome().observe(getViewLifecycleOwner(), income -> {
            if (tvTotalIncomeValue != null) {
                tvTotalIncomeValue.setText(String.format(Locale.getDefault(), "%,d đ", income));
            }
        });

        homeViewModel.getChartData().observe(getViewLifecycleOwner(), expenseData -> {
            if (lineChart != null) {
                java.util.List<Float> incomeData = homeViewModel.getIncomeChartData().getValue();
                lineChart.setData(expenseData, incomeData);
            }
        });

        homeViewModel.getIncomeChartData().observe(getViewLifecycleOwner(), incomeData -> {
            if (lineChart != null) {
                java.util.List<Float> expenseData = homeViewModel.getChartData().getValue();
                lineChart.setData(expenseData, incomeData);
            }
        });

        homeViewModel.getTopExpenses().observe(getViewLifecycleOwner(), expenses ->
                topExpenseAdapter.setExpenses(expenses));
    }

    @Override
    public void onResume() {
        super.onResume();
        walletViewModel.loadActiveWallet();
        homeViewModel.loadReportData();
        homeViewModel.calculateCurrentBalance(walletViewModel.getSelectedWallet().getValue());
    }
}
