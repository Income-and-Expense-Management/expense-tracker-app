package com.ptithcm.quanlichitieu.ui.transaction;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.model.Category;
import com.ptithcm.quanlichitieu.ui.transaction.adapter.CategoryPickerAdapter;
import com.ptithcm.quanlichitieu.ui.transaction.adapter.WalletPickerAdapter;
import com.ptithcm.quanlichitieu.data.model.TransactionType;
import com.ptithcm.quanlichitieu.data.model.Wallet;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddTransactionFragment extends Fragment {

    private AddTransactionViewModel viewModel;

    private MaterialButtonToggleGroup toggleTransactionType;
    private LinearLayout layoutWalletSelector;
    private LinearLayout layoutCategorySelector;
    private LinearLayout layoutDateSelector;
    private TextView tvWalletName;
    private EditText etAmount;
    private EditText etNote;
    private TextView tvCategoryName;
    private ImageView imgCategoryIcon;
    private TextView tvDate;
    private ImageView btnClose;
    private ImageView btnPrevDate;
    private ImageView btnNextDate;
    private MaterialButton btnSave;

    private final Calendar selectedDate = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        toggleBottomNavigation(false);

        viewModel = new ViewModelProvider(this).get(AddTransactionViewModel.class);

        initViews(view);
        setupListeners();
        observeViewModel();

        viewModel.loadActiveWallet();
        updateDateDisplay();
    }

    @Override
    public void onDestroyView() {
        toggleBottomNavigation(true);
        super.onDestroyView();
    }

    private void toggleBottomNavigation(boolean isVisible) {
        if (getActivity() == null) return;

        View bottomAppBar = getActivity().findViewById(R.id.bottomAppBar);
        View fab = getActivity().findViewById(R.id.fabAdd);
        View container = getActivity().findViewById(R.id.fragmentContainer);

        int visibility = isVisible ? View.VISIBLE : View.GONE;
        if (bottomAppBar != null) bottomAppBar.setVisibility(visibility);
        if (fab != null) fab.setVisibility(visibility);

        if (container != null && container.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) container.getLayoutParams();
            params.bottomMargin = isVisible ? (int) (80 * getResources().getDisplayMetrics().density) : 0;
            container.setLayoutParams(params);
        }
    }

    private void initViews(View view) {
        toggleTransactionType = view.findViewById(R.id.toggleTransactionType);
        layoutWalletSelector = view.findViewById(R.id.layoutWalletSelector);
        layoutCategorySelector = view.findViewById(R.id.layoutCategorySelector);
        layoutDateSelector = view.findViewById(R.id.layoutDateSelector);
        tvWalletName = view.findViewById(R.id.tvWalletName);
        etAmount = view.findViewById(R.id.etAmount);
        etNote = view.findViewById(R.id.etNote);
        tvCategoryName = view.findViewById(R.id.tvCategoryName);
        imgCategoryIcon = view.findViewById(R.id.imgCategoryIcon);
        tvDate = view.findViewById(R.id.tvDate);
        btnClose = view.findViewById(R.id.btnClose);
        btnPrevDate = view.findViewById(R.id.btnPrevDate);
        btnNextDate = view.findViewById(R.id.btnNextDate);
        btnSave = view.findViewById(R.id.btnSave);
    }

    private void setupListeners() {
        btnClose.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        toggleTransactionType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnExpense) {
                viewModel.setTransactionType(TransactionType.EXPENSE);
            } else if (checkedId == R.id.btnIncome) {
                viewModel.setTransactionType(TransactionType.INCOME);
            }
        });

        layoutWalletSelector.setOnClickListener(v -> showWalletPicker());
        layoutCategorySelector.setOnClickListener(v -> showCategoryPicker());

        btnPrevDate.setOnClickListener(v -> {
            selectedDate.add(Calendar.DAY_OF_MONTH, -1);
            viewModel.setTransactionDate(selectedDate.getTimeInMillis());
            updateDateDisplay();
        });

        btnNextDate.setOnClickListener(v -> {
            selectedDate.add(Calendar.DAY_OF_MONTH, 1);
            viewModel.setTransactionDate(selectedDate.getTimeInMillis());
            updateDateDisplay();
        });

        layoutDateSelector.setOnClickListener(v -> showDatePicker());

        btnSave.setOnClickListener(v -> {
            String amount = etAmount.getText().toString().trim();
            String note = etNote.getText().toString().trim();
            viewModel.saveTransaction(amount, note);
        });
    }

    private void observeViewModel() {
        viewModel.getSelectedWallet().observe(getViewLifecycleOwner(), wallet -> {
            if (wallet != null) {
                tvWalletName.setText(wallet.getName());
                tvWalletName.setTextColor(Color.WHITE);
            }
        });

        viewModel.getSelectedCategory().observe(getViewLifecycleOwner(), category -> {
            if (category != null) {
                tvCategoryName.setText(category.getName());
                tvCategoryName.setTextColor(Color.WHITE);
                
                // Load icon thực tế từ drawable resources
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
            } else {
                tvCategoryName.setText(R.string.add_transaction_select_category);
                tvCategoryName.setTextColor(Color.parseColor("#AAAAAA"));
                // Clear icon - set to default placeholder
                imgCategoryIcon.setImageResource(R.drawable.ic_food);
            }
        });

        viewModel.getSaveResult().observe(getViewLifecycleOwner(), result -> {
            Toast.makeText(requireContext(), result.getMessage(), Toast.LENGTH_SHORT).show();
            if (result.isSuccess()) {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void updateDateDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
        String formatted = sdf.format(new Date(selectedDate.getTimeInMillis()));
        formatted = formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
        tvDate.setText(formatted);
    }

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                R.style.DatePickerDialogTheme,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    viewModel.setTransactionDate(selectedDate.getTimeInMillis());
                    updateDateDisplay();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void showWalletPicker() {
        List<Wallet> wallets = viewModel.getAllWallets();
        if (wallets == null || wallets.isEmpty()) {
            Toast.makeText(requireContext(), "Chưa có ví nào", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_select_wallet, null);
        dialog.setContentView(sheetView);

        RecyclerView rvWallets = sheetView.findViewById(R.id.rvWallets);
        LinearLayout layoutEmpty = sheetView.findViewById(R.id.layoutEmpty);

        Wallet currentWallet = viewModel.getSelectedWallet().getValue();
        String currentWalletId = currentWallet != null ? currentWallet.getId() : null;

        WalletPickerAdapter adapter = new WalletPickerAdapter(wallets, currentWalletId, wallet -> {
            viewModel.setSelectedWallet(wallet);
            dialog.dismiss();
        });

        rvWallets.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvWallets.setAdapter(adapter);

        if (wallets.isEmpty()) {
            rvWallets.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        }

        dialog.show();
    }

    private void showCategoryPicker() {
        List<Category> categories = viewModel.getCategoriesByType();
        if (categories == null || categories.isEmpty()) {
            Toast.makeText(requireContext(), "Không có nhóm nào", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_select_category, null);
        dialog.setContentView(sheetView);

        RecyclerView rvCategories = sheetView.findViewById(R.id.rvCategories);
        EditText etSearch = sheetView.findViewById(R.id.etSearch);
        LinearLayout layoutEmpty = sheetView.findViewById(R.id.layoutEmpty);
        // Resolve optional title/empty views by id name at runtime to avoid lint mismatch
        int titleId = getResources().getIdentifier("tvTitle", "id", requireContext().getPackageName());
        int emptyId = getResources().getIdentifier("tvEmpty", "id", requireContext().getPackageName());
        TextView tvTitle = titleId != 0 ? sheetView.findViewById(titleId) : null;
        TextView tvEmptyMsg = emptyId != 0 ? sheetView.findViewById(emptyId) : null;

        // Set title & empty message according to current transaction type
        TransactionType current = viewModel.getTransactionType().getValue();
        if (current == null) current = TransactionType.EXPENSE;
        if (tvTitle != null) {
            if (current == TransactionType.INCOME) {
                tvTitle.setText("Chọn nhóm thu nhập");
            } else {
                tvTitle.setText("Chọn nhóm chi tiêu");
            }
        }
        if (tvEmptyMsg != null) {
            if (current == TransactionType.INCOME) {
                tvEmptyMsg.setText("Không có nhóm thu nhập");
            } else {
                tvEmptyMsg.setText("Không có nhóm chi tiêu");
            }
        }

        CategoryPickerAdapter adapter = new CategoryPickerAdapter(categories, category -> {
            viewModel.setSelectedCategory(category);
            dialog.dismiss();
        });

        rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCategories.setAdapter(adapter);

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString(), categories);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        if (categories.isEmpty()) {
            rvCategories.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        }

        dialog.show();
    }

    // Các adapter đã được tách thành file độc lập trong package com.ptithcm.quanlichitieu.ui.transaction.adapter
}
