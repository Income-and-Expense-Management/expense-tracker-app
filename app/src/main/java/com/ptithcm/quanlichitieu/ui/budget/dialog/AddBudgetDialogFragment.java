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
import android.widget.CheckBox;
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
import com.ptithcm.quanlichitieu.ui.budget.bottomsheet.SelectCategoryBottomSheet;
import com.ptithcm.quanlichitieu.ui.budget.bottomsheet.SelectPeriodBottomSheet;
import com.ptithcm.quanlichitieu.ui.budget.bottomsheet.SelectWalletBottomSheet;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * AddBudgetDialogFragment - Dialog thêm ngân sách mới.
 * 
 * Hiển thị form để người dùng nhập thông tin ngân sách:
 * - Chọn nhóm chi tiêu (category)
 * - Nhập số tiền
 * - Chọn kỳ hạn
 * - Chọn ví
 * - Tùy chọn lặp lại
 */
public class AddBudgetDialogFragment extends DialogFragment {

    public static final String TAG = "AddBudgetDialog";

    private BudgetViewModel viewModel;

    // Views
    private ImageView ivClose;
    private LinearLayout llSelectCategory;
    private ImageView imgCategoryIcon;
    private TextView tvCategoryName;
    private EditText etAmount;
    private LinearLayout llSelectPeriod;
    private TextView tvPeriod;
    private LinearLayout llSelectWallet;
    private TextView tvWalletName;
    private CheckBox cbRepeat;
    private Button btnSave;

    // Selected data
    private Category selectedCategory;
    private Wallet selectedWallet;
    private long startDate;
    private long endDate;
    private boolean isRepeat = false;

    // Listeners
    private OnBudgetSavedListener onBudgetSavedListener;

    public interface OnBudgetSavedListener {
        void onBudgetSaved();
    }

    public static AddBudgetDialogFragment newInstance() {
        return new AddBudgetDialogFragment();
    }

    public void setOnBudgetSavedListener(OnBudgetSavedListener listener) {
        this.onBudgetSavedListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.FullScreenDialogTheme);
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
        return inflater.inflate(R.layout.dialog_add_budget, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(BudgetViewModel.class);
        
        // Clear old operation result to prevent showing stale toast
        viewModel.clearOperationResult();

        initViews(view);
        initDefaultValues();
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
        llSelectCategory = view.findViewById(R.id.llSelectCategory);
        imgCategoryIcon = view.findViewById(R.id.imgCategoryIcon);
        tvCategoryName = view.findViewById(R.id.tvCategoryName);
        etAmount = view.findViewById(R.id.etAmount);
        llSelectPeriod = view.findViewById(R.id.llSelectPeriod);
        tvPeriod = view.findViewById(R.id.tvPeriod);
        llSelectWallet = view.findViewById(R.id.llSelectWallet);
        tvWalletName = view.findViewById(R.id.tvWalletName);
        cbRepeat = view.findViewById(R.id.cbRepeat);
        btnSave = view.findViewById(R.id.btnSave);
    }

    private void initDefaultValues() {
        // Set default period to current month
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        startDate = cal.getTimeInMillis();

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        endDate = cal.getTimeInMillis();

        updatePeriodText();

        // Set default wallet from ViewModel
        Wallet wallet = viewModel.getSelectedWallet().getValue();
        if (wallet != null) {
            selectedWallet = wallet;
            tvWalletName.setText(wallet.getName());
        }
    }

    private void setupListeners() {
        ivClose.setOnClickListener(v -> dismiss());

        llSelectCategory.setOnClickListener(v -> showCategorySelector());

        llSelectPeriod.setOnClickListener(v -> showPeriodSelector());

        llSelectWallet.setOnClickListener(v -> showWalletSelector());

        cbRepeat.setOnCheckedChangeListener((buttonView, isChecked) -> isRepeat = isChecked);

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
            if (result != null) {
                Toast.makeText(requireContext(), result.getMessage(), Toast.LENGTH_SHORT).show();
                if (result.isSuccess()) {
                    if (onBudgetSavedListener != null) {
                        onBudgetSavedListener.onBudgetSaved();
                    }
                    dismiss();
                    // ✅ Clear sau khi dismiss để tránh toast hiện lại
                    viewModel.clearOperationResult();
                }
            }
        });
    }

    private void showCategorySelector() {
        SelectCategoryBottomSheet bottomSheet = SelectCategoryBottomSheet.newInstance();
        bottomSheet.setOnCategorySelectedListener(category -> {
            selectedCategory = category;
            tvCategoryName.setText(category.getName());
            tvCategoryName.setTextColor(Color.WHITE);
            updateCategoryIcon(category);
            updateSaveButtonState();
        });
        bottomSheet.show(getChildFragmentManager(), SelectCategoryBottomSheet.TAG);
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
            tvWalletName.setText(wallet.getName());
        });
        bottomSheet.show(getChildFragmentManager(), SelectWalletBottomSheet.TAG);
    }

    /**
     * Cập nhật hiển thị icon category.
     * Load icon thực tế từ drawable resources dựa vào iconName.
     * Đồng bộ với CategoryAdapter và SelectCategoryBottomSheet.
     */
    private void updateCategoryIcon(Category category) {
        String iconName = category.getIconName();
        if (iconName != null && !iconName.isEmpty()) {
            int resId = requireContext().getResources().getIdentifier(
                    iconName, "drawable", requireContext().getPackageName());
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
        
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startDate);
        int month = cal.get(Calendar.MONTH) + 1;
        
        tvPeriod.setText(String.format("Tháng %d (%s - %s)", month, startStr, endStr));
    }

    private void updateSaveButtonState() {
        boolean canSave = selectedCategory != null 
                && selectedWallet != null 
                && getAmount() > 0;
        
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
        if (selectedCategory == null) {
            Toast.makeText(requireContext(), "Vui lòng chọn nhóm chi tiêu", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedWallet == null) {
            Toast.makeText(requireContext(), "Vui lòng chọn ví", Toast.LENGTH_SHORT).show();
            return;
        }

        long amount = getAmount();
        if (amount <= 0) {
            Toast.makeText(requireContext(), "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.createBudget(
                selectedCategory.getId(),
                amount,
                startDate,
                endDate,
                selectedWallet.getId()
        );
    }

    private String formatNumber(long number) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        return formatter.format(number);
    }
}
