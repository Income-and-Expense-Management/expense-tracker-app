package com.ptithcm.quanlichitieu.ui.budget;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.ui.common.ArcProgressBar;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;

/**
 * BudgetFragment
 *
 * Displays budget overview with circular progress chart and budget list.
 * Integrated with BudgetViewModel for data management.
 */
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
    private ImageView ivMenu;

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

        // Load initial data
        viewModel.loadWallets();
        viewModel.loadCategories();
    }

    private void initViews(View view) {
        tvWalletName = view.findViewById(R.id.tvWalletName);
        tvRemainingAmount = view.findViewById(R.id.tvRemainingAmount);
        tvTotalBudget = view.findViewById(R.id.tvTotalBudget);
        tvTotalSpent = view.findViewById(R.id.tvTotalSpent);
        tvPeriodDays = view.findViewById(R.id.tvPeriodDays);
        progressCircle = view.findViewById(R.id.progressCircle);
        btnCreateBudget = view.findViewById(R.id.btnCreateBudget);
        rvBudgetList = view.findViewById(R.id.rvBudgetList);
        llWalletSelector = view.findViewById(R.id.llWalletSelector);
        ivMenu = view.findViewById(R.id.ivMenu);
    }

    private void setupRecyclerView() {
        budgetAdapter = new BudgetAdapter(new ArrayList<>());
        
        budgetAdapter.setOnItemClickListener(item -> {
            showEditBudgetDialog(item);
        });

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

        ivMenu.setOnClickListener(v -> showMainMenu(v));
    }

    private void observeViewModel() {
        // Observe budget items
        viewModel.getBudgetItems().observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                budgetAdapter.updateData(items);
            }
        });

        // Observe selected wallet
        viewModel.getSelectedWallet().observe(getViewLifecycleOwner(), wallet -> {
            if (wallet != null) {
                tvWalletName.setText(wallet.getName());
            }
        });

        // Observe budget summary
        viewModel.getBudgetSummary().observe(getViewLifecycleOwner(), summary -> {
            if (summary != null) {
                updateSummaryUI(summary);
            }
        });

        // Observe operation results
        viewModel.getOperationResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                Toast.makeText(requireContext(), result.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSummaryUI(BudgetViewModel.BudgetSummary summary) {
        // Update remaining amount
        long remaining = summary.getRemaining();
        String remainingText = formatMoney(Math.abs(remaining));
        if (remaining >= 0) {
            tvRemainingAmount.setText("+ " + remainingText);
            tvRemainingAmount.setTextColor(getResources().getColor(android.R.color.holo_green_light));
        } else {
            tvRemainingAmount.setText("- " + remainingText);
            tvRemainingAmount.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        }

        // Update total budget
        tvTotalBudget.setText(summary.getFormattedTotalBudget());

        // Update total spent
        tvTotalSpent.setText(summary.getFormattedTotalSpent());

        // Update days remaining
        int days = summary.getDaysRemaining();
        tvPeriodDays.setText(days + " ngày");

        // Update progress circle
        progressCircle.setProgress(summary.getProgress());
    }

    private void showAddBudgetDialog() {
        AddBudgetDialogFragment dialog = AddBudgetDialogFragment.newInstance();
        dialog.setOnBudgetSavedListener(() -> {
            viewModel.refresh();
        });
        dialog.show(getChildFragmentManager(), AddBudgetDialogFragment.TAG);
    }

    private void showEditBudgetDialog(BudgetItem item) {
        EditBudgetDialogFragment dialog = EditBudgetDialogFragment.newInstance(item);
        dialog.setOnBudgetEditedListener(() -> {
            viewModel.refresh();
        });
        dialog.show(getChildFragmentManager(), EditBudgetDialogFragment.TAG);
    }

    private void showDeleteConfirmation(BudgetItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa ngân sách")
                .setMessage("Bạn có chắc muốn xóa ngân sách \"" + item.getCategoryName() + "\"?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    viewModel.deleteBudget(item.getId());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showWalletSelector() {
        SelectWalletBottomSheet bottomSheet = SelectWalletBottomSheet.newInstance();
        bottomSheet.setOnWalletSelectedListener(wallet -> {
            viewModel.selectWallet(wallet);
        });
        bottomSheet.show(getChildFragmentManager(), SelectWalletBottomSheet.TAG);
    }

    private void showMainMenu(View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenu().add(0, 1, 0, "Làm mới");
        popup.getMenu().add(0, 2, 1, "Xem tất cả ngân sách");
        
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                viewModel.refresh();
                return true;
            } else if (item.getItemId() == 2) {
                Toast.makeText(requireContext(), "Xem tất cả ngân sách", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        
        popup.show();
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
