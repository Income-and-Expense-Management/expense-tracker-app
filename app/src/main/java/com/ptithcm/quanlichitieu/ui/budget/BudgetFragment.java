package com.ptithcm.quanlichitieu.ui.budget;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.event.BudgetUpdateEvent;
import com.ptithcm.quanlichitieu.event.EventBus;
import com.ptithcm.quanlichitieu.ui.budget.bottomsheet.SelectWalletBottomSheet;
import com.ptithcm.quanlichitieu.ui.budget.dialog.AddBudgetDialogFragment;
import com.ptithcm.quanlichitieu.ui.budget.dialog.EditBudgetDialogFragment;
import com.ptithcm.quanlichitieu.ui.budget.dialog.ViewBudgetDialogFragment;
import com.ptithcm.quanlichitieu.ui.budget.model.BudgetItem;
import com.ptithcm.quanlichitieu.ui.common.ArcProgressBar;
import com.ptithcm.quanlichitieu.ui.budget.adapter.BudgetAdapter;


import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;

public class BudgetFragment extends Fragment {

    private TextView tvWalletName;
    private TextView tvRemainingAmount;
    private TextView tvTotalBudget;
    private TextView tvTotalSpent;
    private TextView tvPeriodDays;
    private ArcProgressBar progressCircle;
    private Button btnCreateBudget;
    private RecyclerView rvBudgetList;
    private LinearLayout llWalletSelector;
    private ImageView ivWalletIcon;
    private SwipeRefreshLayout swipeRefreshLayout;

    private BudgetAdapter budgetAdapter;
    private BudgetViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);
        initViews(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(BudgetViewModel.class);

        setupRecyclerView();
        setupListeners();
        observeViewModel();

        viewModel.loadWallets();
        viewModel.loadCategories();
    }

    private void initViews(View view) {
        tvWalletName = view.findViewById(R.id.tvWalletName);
        ivWalletIcon = view.findViewById(R.id.ivWalletIcon);
        tvRemainingAmount = view.findViewById(R.id.tvRemainingAmount);
        tvTotalBudget = view.findViewById(R.id.tvTotalBudget);
        tvTotalSpent = view.findViewById(R.id.tvTotalSpent);
        tvPeriodDays = view.findViewById(R.id.tvPeriodDays);
        progressCircle = view.findViewById(R.id.progressCircle);
        btnCreateBudget = view.findViewById(R.id.btnCreateBudget);
        rvBudgetList = view.findViewById(R.id.rvBudgetList);
        llWalletSelector = view.findViewById(R.id.llWalletSelector);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
    }

    private void setupRecyclerView() {
        budgetAdapter = new BudgetAdapter(new ArrayList<>());

        // Item click -> mở màn hình xem chi tiết ngân sách
        budgetAdapter.setOnItemClickListener(item -> showViewBudgetDialog(item));

        // Menu click -> edit hoặc delete trực tiếp
        budgetAdapter.setOnMenuClickListener(new BudgetAdapter.OnBudgetMenuClickListener() {
            @Override
            public void onEditClick(BudgetItem item) {
                showEditBudgetDialog(item);
            }

            @Override
            public void onDeleteClick(BudgetItem item) {
                showDeleteConfirmation(item);
            }
        });

        rvBudgetList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvBudgetList.setAdapter(budgetAdapter);
    }

    private void setupListeners() {
        btnCreateBudget.setOnClickListener(v -> showAddBudgetDialog());
        llWalletSelector.setOnClickListener(v -> showWalletSelector());

        swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.refresh();
        });
    }

    private void observeViewModel() {
        viewModel.getBudgetItems().observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                budgetAdapter.updateData(items);
            }
            swipeRefreshLayout.setRefreshing(false);
        });

        viewModel.getSelectedWallet().observe(getViewLifecycleOwner(), wallet -> {
            if (wallet != null) {
                tvWalletName.setText(wallet.getName());
                // Load and set wallet icon; fallback to default if not found
                String iconId = wallet.getIconId();
                if (iconId != null && !iconId.isEmpty()) {
                    int resId = requireContext().getResources().getIdentifier(iconId, "drawable", requireContext().getPackageName());
                    if (resId != 0) {
                        ivWalletIcon.setImageResource(resId);
                    } else {
                        ivWalletIcon.setImageResource(R.drawable.ic_wallet);
                    }
                } else {
                    ivWalletIcon.setImageResource(R.drawable.ic_wallet);
                }
            }
        });

        viewModel.getBudgetSummary().observe(getViewLifecycleOwner(), summary -> {
            if (summary != null) {
                updateSummaryUI(summary);
            }
        });
        
        viewModel.getOperationResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.getAction() == BudgetViewModel.Action.DELETE && !result.hasBeenHandled()) {
                Toast.makeText(requireContext(), result.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        // Observe budget update events từ EventBus
        // Tuân thủ Dependency Inversion Principle: phụ thuộc vào abstraction (LiveData/EventBus)
        // không phụ thuộc vào concrete implementation của AddTransactionFragment
        EventBus.getInstance().getBudgetUpdateEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null) {
                handleBudgetUpdateEvent(event);
                EventBus.getInstance().clearBudgetUpdateEvent();
            }
        });
    }

    private void updateSummaryUI(BudgetViewModel.BudgetSummary summary) {
        long remaining = summary.getRemaining();
        String remainingText = formatMoney(Math.abs(remaining));
        if (remaining >= 0) {
            tvRemainingAmount.setText("+ " + remainingText);
            tvRemainingAmount.setTextColor(getResources().getColor(android.R.color.holo_green_light));
        } else {
            tvRemainingAmount.setText("- " + remainingText);
            tvRemainingAmount.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        }

        tvTotalBudget.setText(summary.getFormattedTotalBudget());
        tvTotalSpent.setText(summary.getFormattedTotalSpent());

        int days = summary.getDaysRemaining();
        tvPeriodDays.setText(days + " ngày");

        progressCircle.setProgress(summary.getProgress());
    }
    
    /**
     * Xử lý budget update event từ EventBus.
     * 
     * Tuân thủ Single Responsibility Principle:
     * - Method này chỉ chịu trách nhiệm handle event và trigger refresh
     * - Logic refresh được delegate cho ViewModel
     * 
     * @param event Event chứa thông tin về budget update
     */
    private void handleBudgetUpdateEvent(BudgetUpdateEvent event) {
        // Chỉ refresh nếu event liên quan đến ví hiện tại
        if (viewModel.getSelectedWallet().getValue() != null &&
            event.getWalletId() != null &&
            event.getWalletId().equals(viewModel.getSelectedWallet().getValue().getId())) {
            viewModel.refresh();
        }
    }

    /** Mở màn hình XEM chi tiết ngân sách (bấm vào item) */
    private void showViewBudgetDialog(BudgetItem item) {
        ViewBudgetDialogFragment dialog = ViewBudgetDialogFragment.newInstance(item);
        dialog.setOnBudgetActionListener(new ViewBudgetDialogFragment.OnBudgetActionListener() {
            @Override
            public void onEditClicked(BudgetItem budgetItem) {
                showEditBudgetDialog(budgetItem);
            }

            @Override
            public void onDeleteClicked(BudgetItem budgetItem) {
                showDeleteConfirmation(budgetItem);
            }
        });
        dialog.show(getChildFragmentManager(), ViewBudgetDialogFragment.TAG);
    }

    /** Mở màn hình CHỈNH SỬA ngân sách */
    private void showAddBudgetDialog() {
        AddBudgetDialogFragment dialog = AddBudgetDialogFragment.newInstance();
        dialog.setOnBudgetSavedListener(() -> viewModel.refresh());
        dialog.show(getChildFragmentManager(), AddBudgetDialogFragment.TAG);
    }

    private void showEditBudgetDialog(BudgetItem item) {
        EditBudgetDialogFragment dialog = EditBudgetDialogFragment.newInstance(item);
        dialog.setOnBudgetEditedListener(() -> viewModel.refresh());
        dialog.show(getChildFragmentManager(), EditBudgetDialogFragment.TAG);
    }

    private void showDeleteConfirmation(BudgetItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa ngân sách")
                .setMessage("Bạn có chắc muốn xóa ngân sách \"" + item.getCategoryName() + "\"?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    // deleteBudget() đã tự gọi refresh() bên trong khi thành công
                    // Không cần gọi viewModel.refresh() thêm lần nữa
                    viewModel.deleteBudget(item.getId());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showWalletSelector() {
        SelectWalletBottomSheet bottomSheet = SelectWalletBottomSheet.newInstance();
        bottomSheet.setOnWalletSelectedListener(wallet -> viewModel.selectWallet(wallet));
        bottomSheet.show(getChildFragmentManager(), SelectWalletBottomSheet.TAG);
    }


    private String formatMoney(long amount) {
        if (amount >= 1000000) {
            return String.format(Locale.getDefault(), "%.1f M", amount / 1000000.0);
        } else if (amount >= 1000) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
            symbols.setGroupingSeparator('.');
            DecimalFormat formatter = new DecimalFormat("#,###", symbols);
            return formatter.format(amount) + " đ";
        } else {
            return amount + " đ";
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refresh();
    }
}