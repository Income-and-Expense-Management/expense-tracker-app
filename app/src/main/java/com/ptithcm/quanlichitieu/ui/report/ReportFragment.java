package com.ptithcm.quanlichitieu.ui.report;

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
import com.ptithcm.quanlichitieu.data.model.Transaction;
import com.ptithcm.quanlichitieu.data.model.TransactionGroup;
import com.ptithcm.quanlichitieu.data.model.TransactionType;
import com.ptithcm.quanlichitieu.ui.budget.bottomsheet.SelectWalletBottomSheet;
import com.ptithcm.quanlichitieu.ui.transaction.TransactionViewModel;
import com.ptithcm.quanlichitieu.ui.wallet.WalletViewModel;
import com.ptithcm.quanlichitieu.ui.main.MainActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportFragment extends Fragment {

    private TransactionViewModel transactionViewModel;
    private WalletViewModel walletViewModel;

    private TextView tabPrevMonth, tabCurrentMonth, tabNextMonth;
    private TextView tvTotalExpense, tvTotalIncome;
    private TextView tvWalletName;
    private android.widget.ImageView ivWalletIcon;
    private RecyclerView rvReportItems;
    private ReportAdapter adapter;
    private PieChartView pieChart;
    private View btnExpenseCard, btnIncomeCard;

    private TransactionType currentReportType = TransactionType.EXPENSE;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Hide bottom nav if desired
        toggleBottomNavigation(false);

        // Share the same viewmodel from Activity or create a new one.
        // It's better to use Activity scope to keep selected month/wallet synced
        transactionViewModel = new ViewModelProvider(requireActivity()).get(TransactionViewModel.class);
        walletViewModel = new ViewModelProvider(requireActivity()).get(WalletViewModel.class);

        initViews(view);
        setupMonthTabs();
        setupRecyclerView();
        observeViewModels();
    }

    private void toggleBottomNavigation(boolean isVisible) {
        if (getActivity() == null) return;

        View bottomAppBar = getActivity().findViewById(R.id.bottomAppBar);
        View fab = getActivity().findViewById(R.id.fabAdd);

        int visibility = isVisible ? View.VISIBLE : View.GONE;
        if (bottomAppBar != null) bottomAppBar.setVisibility(visibility);
        if (fab != null) fab.setVisibility(visibility);
    }

    @Override
    public void onDestroyView() {
        toggleBottomNavigation(true);
        super.onDestroyView();
    }

    private void initViews(View view) {
        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        tvWalletName = view.findViewById(R.id.tvWalletName);
        ivWalletIcon = view.findViewById(R.id.ivWalletIcon);
        View walletChip = view.findViewById(R.id.walletChip);
        if (walletChip != null) {
            walletChip.setOnClickListener(v -> {
                SelectWalletBottomSheet bottomSheet = SelectWalletBottomSheet.newInstance();
                bottomSheet.setOnWalletSelectedListener(wallet -> {
                    if (walletViewModel != null) {
                        walletViewModel.selectWallet(wallet);
                    }
                });
                bottomSheet.show(getChildFragmentManager(), SelectWalletBottomSheet.TAG);
            });
        }

        tabPrevMonth = view.findViewById(R.id.tabPrevMonth);
        tabCurrentMonth = view.findViewById(R.id.tabCurrentMonth);
        tabNextMonth = view.findViewById(R.id.tabNextMonth);

        tvTotalExpense = view.findViewById(R.id.tvTotalExpense);
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome);

        btnExpenseCard = view.findViewById(R.id.tvTotalExpense).getParent() instanceof View ? (View) view.findViewById(R.id.tvTotalExpense).getParent() : null;
        btnIncomeCard = view.findViewById(R.id.tvTotalIncome).getParent() instanceof View ? (View) view.findViewById(R.id.tvTotalIncome).getParent() : null;

        if (btnExpenseCard != null) {
            btnExpenseCard.setOnClickListener(v -> {
                currentReportType = TransactionType.EXPENSE;
                updateChartAndList();
            });
        }

        if (btnIncomeCard != null) {
            btnIncomeCard.setOnClickListener(v -> {
                currentReportType = TransactionType.INCOME;
                updateChartAndList();
            });
        }

        pieChart = view.findViewById(R.id.pieChart);
        rvReportItems = view.findViewById(R.id.rvReportItems);
    }

    private void setupMonthTabs() {
        tabPrevMonth.setOnClickListener(v -> transactionViewModel.setMonthOffset(-1));
        tabCurrentMonth.setOnClickListener(v -> transactionViewModel.setMonthOffset(0));
        tabNextMonth.setOnClickListener(v -> transactionViewModel.setMonthOffset(1));
    }

    private void updateTabStyles(int offset) {
        if (!isAdded()) return;
        int activeColor = requireContext().getResources().getColor(R.color.white, null);
        int inactiveColor = requireContext().getResources().getColor(R.color.home_text_secondary, null);

        tabPrevMonth.setTextColor(offset == -1 ? activeColor : inactiveColor);
        tabCurrentMonth.setTextColor(offset == 0 ? activeColor : inactiveColor);
        tabNextMonth.setTextColor(offset == 1 ? activeColor : inactiveColor);
    }

    private void setupRecyclerView() {
        adapter = new ReportAdapter();
        rvReportItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvReportItems.setAdapter(adapter);
    }

    private void observeViewModels() {
        walletViewModel.getSelectedWallet().observe(getViewLifecycleOwner(), wallet -> {
            if (wallet != null && tvWalletName != null) {
                tvWalletName.setText(wallet.getName());
                if (ivWalletIcon != null) {
                    String iconId = wallet.getIconId();
                    if (iconId != null && !iconId.isEmpty()) {
                        int resId = requireContext().getResources().getIdentifier(iconId, "drawable", requireContext().getPackageName());
                        ivWalletIcon.setImageResource(resId != 0 ? resId : R.drawable.ic_wallet);
                    } else {
                        ivWalletIcon.setImageResource(R.drawable.ic_wallet);
                    }
                }
            } else {
                if (tvWalletName != null) tvWalletName.setText("Chưa có ví");
                if (ivWalletIcon != null) ivWalletIcon.setImageResource(R.drawable.ic_wallet);
            }
            transactionViewModel.loadData(wallet);
        });

        transactionViewModel.getMonthOffset().observe(getViewLifecycleOwner(), this::updateTabStyles);

        transactionViewModel.getTotalExpense().observe(getViewLifecycleOwner(), expense ->
            tvTotalExpense.setText(String.format(Locale.getDefault(), "-%,d đ", expense))
        );

        transactionViewModel.getTotalIncome().observe(getViewLifecycleOwner(), income ->
            tvTotalIncome.setText(String.format(Locale.getDefault(), "+%,d đ", income))
        );

        transactionViewModel.getTransactions().observe(getViewLifecycleOwner(), groups -> {
            updateChartAndList();
        });
    }

    private void updateCardStyles() {
        if (!isAdded() || btnExpenseCard == null || btnIncomeCard == null) return;

        if (currentReportType == TransactionType.EXPENSE) {
            btnExpenseCard.setBackgroundResource(R.drawable.bg_summary_card_expense_active);
            btnIncomeCard.setBackgroundResource(R.drawable.bg_summary_card);
        } else {
            btnExpenseCard.setBackgroundResource(R.drawable.bg_summary_card);
            btnIncomeCard.setBackgroundResource(R.drawable.bg_summary_card_income_active);
        }
    }

    private void updateChartAndList() {
        updateCardStyles();
        List<TransactionGroup> groups = transactionViewModel.getTransactions().getValue();
        if (groups == null) return;

        Map<String, ReportItem> map = new HashMap<>();
        for (TransactionGroup group : groups) {
            for (Transaction t : group.getTransactions()) {
                if (t.getType() == currentReportType) {
                    String catName = t.getCategoryName() != null ? t.getCategoryName() : "Khác";
                    ReportItem item = map.get(catName);
                    if (item == null) {
                        item = new ReportItem(catName, t.getIconId(), t.getType());
                        map.put(catName, item);
                    }
                    item.amount += t.getAmount();
                }
            }
        }

        List<ReportItem> list = new ArrayList<>(map.values());
        list.sort((a, b) -> Long.compare(b.amount, a.amount)); // desc

        // Update list
        if (adapter != null) {
            adapter.setItems(list);
        }

        // Update PieChart
        if (pieChart != null) {
            List<PieChartView.PieSlice> slices = new ArrayList<>();
            // Base colors suitable for dark theme
            int[] colors = {
                    android.graphics.Color.parseColor("#EF5350"),
                    android.graphics.Color.parseColor("#42A5F5"),
                    android.graphics.Color.parseColor("#66BB6A"),
                    android.graphics.Color.parseColor("#FFA726"),
                    android.graphics.Color.parseColor("#AB47BC"),
                    android.graphics.Color.parseColor("#26C6DA"),
                    android.graphics.Color.parseColor("#FFCA28"),
                    android.graphics.Color.parseColor("#8D6E63")
            };
            int i = 0;
            for (ReportItem item : list) {
                int resId = 0;
                if (item.iconName != null && !item.iconName.isEmpty()) {
                    resId = requireContext().getResources().getIdentifier(
                            item.iconName, "drawable", requireContext().getPackageName());
                }
                if (resId == 0) {
                    resId = R.drawable.ic_wallet; // Default icon
                }
                slices.add(new PieChartView.PieSlice(item.amount, colors[i % colors.length], resId));
                i++;
            }
            pieChart.setSlices(slices);
        }
    }

    public static class ReportItem {
        public String categoryName;
        public String iconName;
        public TransactionType type;
        public long amount;

        public ReportItem(String categoryName, String iconName, TransactionType type) {
            this.categoryName = categoryName;
            this.iconName = iconName;
            this.type = type;
            this.amount = 0;
        }
    }

    public static class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

        private List<ReportItem> items = new ArrayList<>();

        @android.annotation.SuppressLint("NotifyDataSetChanged")
        public void setItems(List<ReportItem> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_report_category, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ReportItem item = items.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            android.widget.ImageView imgCategoryIcon;
            TextView tvCategoryName;
            TextView tvAmount;

            ViewHolder(View itemView) {
                super(itemView);
                imgCategoryIcon = itemView.findViewById(R.id.imgCategoryIcon);
                tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
                tvAmount = itemView.findViewById(R.id.tvAmount);
            }

            @android.annotation.SuppressLint("DiscouragedApi")
            void bind(ReportItem item) {
                tvCategoryName.setText(item.categoryName);

                String formattedAmount;
                if (item.type == TransactionType.EXPENSE) {
                    formattedAmount = String.format(Locale.getDefault(), "-%,d đ", item.amount);
                    tvAmount.setTextColor(itemView.getContext().getResources().getColor(R.color.home_expense_red, null));
                } else if (item.type == TransactionType.INCOME) {
                    formattedAmount = String.format(Locale.getDefault(), "+%,d đ", item.amount);
                    tvAmount.setTextColor(itemView.getContext().getResources().getColor(R.color.home_accent_green, null));
                } else {
                    formattedAmount = String.format(Locale.getDefault(), "%,d đ", item.amount);
                    tvAmount.setTextColor(itemView.getContext().getResources().getColor(R.color.white, null));
                }
                tvAmount.setText(formattedAmount);

                if (item.iconName != null && !item.iconName.isEmpty()) {
                    int resId = itemView.getContext().getResources().getIdentifier(
                            item.iconName, "drawable", itemView.getContext().getPackageName());
                    if (resId != 0) {
                        imgCategoryIcon.setImageResource(resId);
                    } else {
                        imgCategoryIcon.setImageResource(R.drawable.ic_wallet);
                    }
                } else {
                    imgCategoryIcon.setImageResource(R.drawable.ic_wallet);
                }
            }
        }
    }
}
