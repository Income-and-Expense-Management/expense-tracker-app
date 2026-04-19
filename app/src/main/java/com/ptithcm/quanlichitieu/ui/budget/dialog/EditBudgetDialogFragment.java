package com.ptithcm.quanlichitieu.ui.budget.dialog;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.model.Category;
import com.ptithcm.quanlichitieu.data.model.Wallet;
import com.ptithcm.quanlichitieu.ui.budget.BudgetViewModel;
import com.ptithcm.quanlichitieu.ui.budget.bottomsheet.SelectPeriodBottomSheet;
import com.ptithcm.quanlichitieu.ui.budget.bottomsheet.SelectWalletBottomSheet;
import com.ptithcm.quanlichitieu.ui.budget.model.BudgetItem;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * EditBudgetDialogFragment - Dialog sửa ngân sách.
 * 
 * Hiển thị form để người dùng sửa thông tin ngân sách hiện có:
 * - Hiển thị nhóm chi tiêu (không cho phép sửa)
 * - Sửa số tiền
 * - Sửa kỳ hạn
 * - Sửa ví
 */
public class EditBudgetDialogFragment extends DialogFragment {

    public static final String TAG = "EditBudgetDialog";

    private static final String ARG_BUDGET_ID = "budget_id";
    private static final String ARG_CATEGORY_ID = "category_id";
    private static final String ARG_CATEGORY_NAME = "category_name";
    private static final String ARG_CATEGORY_ICON = "category_icon";
    private static final String ARG_AMOUNT = "amount";
    private static final String ARG_START_DATE = "start_date";
    private static final String ARG_END_DATE = "end_date";
    private static final String ARG_WALLET_ID = "wallet_id";

    private BudgetViewModel viewModel;

    // Views
    private ImageView ivClose;
    private TextView tvTitle;
    private LinearLayout llSelectCategory;
    private ImageView imgCategoryIcon;
    private TextView tvCategoryName;
    private EditText etAmount;
    private LinearLayout llSelectPeriod;
    private TextView tvPeriod;
    private LinearLayout llSelectWallet;
    private TextView tvWalletName;
    private Button btnSave;

    // Budget data
    private String budgetId;
    private String categoryId;
    private String categoryName;
    private String categoryIcon;
    private long amount;
    private long startDate;
    private long endDate;
    private String walletId;
    private String walletName;
    private Wallet selectedWallet;

    // Listeners
    private OnBudgetEditedListener onBudgetEditedListener;

    public interface OnBudgetEditedListener {
        void onBudgetEdited();
    }

    public static EditBudgetDialogFragment newInstance(BudgetItem budgetItem) {
        EditBudgetDialogFragment fragment = new EditBudgetDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BUDGET_ID, budgetItem.getId());
        args.putString(ARG_CATEGORY_ID, budgetItem.getCategoryId());
        args.putString(ARG_CATEGORY_NAME, budgetItem.getCategoryName());
        args.putString(ARG_CATEGORY_ICON, budgetItem.getCategoryIcon());
        args.putLong(ARG_AMOUNT, budgetItem.getLimit());
        args.putLong(ARG_START_DATE, budgetItem.getStartDate());
        args.putLong(ARG_END_DATE, budgetItem.getEndDate());
        args.putString(ARG_WALLET_ID, budgetItem.getWalletId());
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnBudgetEditedListener(OnBudgetEditedListener listener) {
        this.onBudgetEditedListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.FullScreenDialogTheme);

        if (getArguments() != null) {
            budgetId = getArguments().getString(ARG_BUDGET_ID);
            categoryId = getArguments().getString(ARG_CATEGORY_ID);
            categoryName = getArguments().getString(ARG_CATEGORY_NAME);
            categoryIcon = getArguments().getString(ARG_CATEGORY_ICON);
            amount = getArguments().getLong(ARG_AMOUNT);
            startDate = getArguments().getLong(ARG_START_DATE);
            endDate = getArguments().getLong(ARG_END_DATE);
            walletId = getArguments().getString(ARG_WALLET_ID);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_edit_budget, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(BudgetViewModel.class);
        
        // Clear old operation result to prevent showing stale toast
        viewModel.clearOperationResult();

        initViews(view);
        loadBudgetData();
        setupListeners();
        observeViewModel();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
    }

    private void initViews(View view) {
        ivClose = view.findViewById(R.id.ivClose);
        tvTitle = view.findViewById(R.id.tvTitle);
        llSelectCategory = view.findViewById(R.id.llSelectCategory);
        imgCategoryIcon = view.findViewById(R.id.imgCategoryIcon);
        tvCategoryName = view.findViewById(R.id.tvCategoryName);
        etAmount = view.findViewById(R.id.etAmount);
        llSelectPeriod = view.findViewById(R.id.llSelectPeriod);
        tvPeriod = view.findViewById(R.id.tvPeriod);
        llSelectWallet = view.findViewById(R.id.llSelectWallet);
        tvWalletName = view.findViewById(R.id.tvWalletName);
        btnSave = view.findViewById(R.id.btnSave);
    }

    private void loadBudgetData() {
        // Set title
        tvTitle.setText("Sửa ngân sách");

        // Load category (không cho sửa category)
        tvCategoryName.setText(categoryName);
        tvCategoryName.setTextColor(Color.WHITE);
        updateCategoryIcon();

        // Load amount
        etAmount.setText(formatNumber(amount));

        // Load period
        updatePeriodText();

        // Load wallet - get wallet name from ViewModel
        Wallet currentWallet = viewModel.getSelectedWallet().getValue();
        if (currentWallet != null && currentWallet.getId().equals(walletId)) {
            walletName = currentWallet.getName();
            tvWalletName.setText(walletName);
        } else {
            // Fallback to wallet ID if can't get name
            tvWalletName.setText("Ví");
        }

        // Update save button state
        updateSaveButtonState();
    }

    private void setupListeners() {
        ivClose.setOnClickListener(v -> dismiss());

        // Category không cho sửa - hiển thị thông báo
        llSelectCategory.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Không thể thay đổi nhóm chi tiêu", Toast.LENGTH_SHORT).show();
        });

        llSelectPeriod.setOnClickListener(v -> showPeriodSelector());

        llSelectWallet.setOnClickListener(v -> showWalletSelector());

        // Format số tiền khi nhập
        etAmount.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    etAmount.removeTextChangedListener(this);

                    String cleanString = s.toString().replaceAll("[^\\d]", "");
                    if (!cleanString.isEmpty()) {
                        try {
                            long parsed = Long.parseLong(cleanString);
                            String formatted = formatNumber(parsed);
                            current = formatted;
                            etAmount.setText(formatted);
                            etAmount.setSelection(formatted.length());
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                    }

                    etAmount.addTextChangedListener(this);
                    updateSaveButtonState();
                }
            }
        });

        btnSave.setOnClickListener(v -> saveBudget());
    }

    private void observeViewModel() {
        viewModel.getOperationResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.getAction() == BudgetViewModel.Action.UPDATE && !result.hasBeenHandled()) {
                Toast.makeText(requireContext(), result.getMessage(), Toast.LENGTH_SHORT).show();
                if (result.isSuccess()) {
                    if (onBudgetEditedListener != null) {
                        onBudgetEditedListener.onBudgetEdited();
                    }
                    dismiss();
                }
            }
        });
    }

    private void showPeriodSelector() {
        SelectPeriodBottomSheet bottomSheet = SelectPeriodBottomSheet.newInstance(startDate, endDate);
        bottomSheet.setOnPeriodSelectedListener((start, end) -> {
            startDate = start;
            endDate = end;
            updatePeriodText();
        });
        bottomSheet.show(getChildFragmentManager(), SelectPeriodBottomSheet.TAG);
    }

    private void showWalletSelector() {
        SelectWalletBottomSheet bottomSheet = SelectWalletBottomSheet.newInstance();
        bottomSheet.setOnWalletSelectedListener(wallet -> {
            selectedWallet = wallet;
            walletId = wallet.getId();
            walletName = wallet.getName();
            tvWalletName.setText(walletName);
        });
        bottomSheet.show(getChildFragmentManager(), SelectWalletBottomSheet.TAG);
    }

    /**
     * Cập nhật hiển thị icon category.
     * Load icon thực tế từ drawable resources dựa vào iconName.
     * Đồng bộ với CategoryAdapter và SelectCategoryBottomSheet.
     */
    private void updateCategoryIcon() {
        if (categoryIcon != null && !categoryIcon.isEmpty()) {
            int resId = requireContext().getResources().getIdentifier(
                    categoryIcon, "drawable", requireContext().getPackageName());
            if (resId != 0) {
                imgCategoryIcon.setImageResource(resId);
            } else {
                // Fallback icon
                imgCategoryIcon.setImageResource(R.drawable.ic_food);
            }
        } else {
            // Default fallback icon
            imgCategoryIcon.setImageResource(R.drawable.ic_food);
        }
    }

    private void updatePeriodText() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
        String startStr = sdf.format(new Date(startDate));
        String endStr = sdf.format(new Date(endDate));

        SimpleDateFormat monthFormat = new SimpleDateFormat("M", Locale.getDefault());
        String month = monthFormat.format(new Date(startDate));

        tvPeriod.setText(String.format("Tháng %s (%s - %s)", month, startStr, endStr));
    }

    private void updateSaveButtonState() {
        boolean canSave = getAmount() > 0;

        btnSave.setEnabled(canSave);
        btnSave.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                canSave ? Color.parseColor("#4CAF50") : Color.parseColor("#333333")
        ));
    }

    private long getAmount() {
        String text = etAmount.getText().toString().replaceAll("[^\\d]", "");
        if (text.isEmpty()) return 0;
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void saveBudget() {
        long newAmount = getAmount();
        if (newAmount <= 0) {
            Toast.makeText(requireContext(), "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.updateBudget(
                budgetId,
                categoryId,
                newAmount,
                startDate,
                endDate,
                walletId
        );
    }

    private String formatNumber(long number) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        return formatter.format(number);
    }
}
