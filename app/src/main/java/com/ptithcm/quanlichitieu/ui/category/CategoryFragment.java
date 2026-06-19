package com.ptithcm.quanlichitieu.ui.category;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.model.Category;
import com.ptithcm.quanlichitieu.data.model.TransactionType;
import com.ptithcm.quanlichitieu.ui.login.AuthViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CategoryFragment extends Fragment {

    private AuthViewModel authViewModel;
    private CategoryViewModel categoryViewModel;
    private CategoryAdapter adapter;
    private List<Category> allCategories = new ArrayList<>();
    private TransactionType currentType = TransactionType.EXPENSE;
    private String selectedIcon = "ic_food";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        toggleBottomNavigation(false);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
        setupViews(view);
        observeViewModel();
        categoryViewModel.loadCategoriesForManagement(authViewModel.getUserId());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (authViewModel != null && categoryViewModel != null) {
            categoryViewModel.refreshFromServer(authViewModel.getUserId());
        }
    }

    private void observeViewModel() {
        categoryViewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            allCategories.clear();
            if (categories != null) allCategories.addAll(categories);
            filterCategories();
        });

        categoryViewModel.getUsageInfo().observe(getViewLifecycleOwner(), usage -> {
            if (usage != null) showSmartDeleteDialog(usage);
        });

        categoryViewModel.getAddResult().observe(getViewLifecycleOwner(), s -> handleResult(s, "Thêm"));
        categoryViewModel.getUpdateResult().observe(getViewLifecycleOwner(), s -> handleResult(s, "Cập nhật"));
        categoryViewModel.getDeleteResult().observe(getViewLifecycleOwner(), s -> handleResult(s, "Xoá"));
        categoryViewModel.getSortOrder().observe(getViewLifecycleOwner(), o -> filterCategories());
    }

    private void handleResult(Boolean success, String action) {
        if (success == null) return;
        Toast.makeText(getContext(), success ? action + " thành công" : action + " thất bại", Toast.LENGTH_SHORT).show();
        categoryViewModel.resetResults();
    }

    private void setupViews(View view) {
        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        view.findViewById(R.id.btnFilter).setOnClickListener(v -> categoryViewModel.toggleSortOrder());

        TabLayout tabs = view.findViewById(R.id.tabLayout);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentType = (tab.getPosition() == 1) ? TransactionType.INCOME : TransactionType.EXPENSE;
                filterCategories();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        RecyclerView rv = view.findViewById(R.id.rvCategories);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CategoryAdapter(new CategoryAdapter.OnCategoryClickListener() {
            @Override public void onCategoryClick(Category c) { showEditCategoryDialog(c); }
            @Override public void onCategoryLongClick(Category c) { showDeleteConfirmDialog(c); }
            @Override public void onCategorySwitchToggled(Category c, boolean isChecked) {
                if (c.isActive() != isChecked) {
                    c.setActive(isChecked);
                    categoryViewModel.updateCategory(authViewModel.getUserId(), c);
                }
            }
        });
        rv.setAdapter(adapter);
        view.findViewById(R.id.btnAddNewCategory).setOnClickListener(v -> showAddCategoryDialog());
    }

    private void filterCategories() {
        if (adapter == null) return;
        List<Category> filtered = new ArrayList<>();
        for (Category c : allCategories) if (c.getType() == currentType) filtered.add(c);
        CategoryViewModel.SortOrder order = categoryViewModel.getSortOrder().getValue();
        Collections.sort(filtered, (a, b) -> {
            int res = a.getName().compareToIgnoreCase(b.getName());
            return (order == CategoryViewModel.SortOrder.Z_TO_A) ? -res : res;
        });
        adapter.updateData(filtered);
    }

    private void showSmartDeleteDialog(CategoryViewModel.UsageInfo usage) {
        String catName = "này";
        for (Category c : allCategories) if (c.getId().equals(usage.categoryId)) { catName = c.getName(); break; }
        new AlertDialog.Builder(requireContext())
                .setTitle("Danh mục đang có giao dịch")
                .setMessage("Danh mục '" + catName + "' có " + usage.transactionCount + " giao dịch. Bạn muốn làm gì?")
                .setPositiveButton("Chuyển mục khác", (d, w) -> showReassignDialog(usage.categoryId))
                .setNeutralButton("Xoá sạch luôn", (d, w) -> categoryViewModel.deleteCategoryAndTransactions(authViewModel.getUserId(), usage.categoryId))
                .setNegativeButton("Hủy", (d, w) -> categoryViewModel.resetResults())
                .setCancelable(false).show();
    }

    private void showReassignDialog(String oldId) {
        List<String> names = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (Category c : allCategories) {
            if (!c.getId().equals(oldId) && c.getType() == currentType) { names.add(c.getName()); ids.add(c.getId()); }
        }
        if (names.isEmpty()) { Toast.makeText(getContext(), "Không có mục nào để chuyển", Toast.LENGTH_SHORT).show(); return; }
        new AlertDialog.Builder(requireContext()).setTitle("Chọn danh mục chuyển đến")
                .setItems(names.toArray(new String[0]), (d, w) -> categoryViewModel.reassignAndDelete(authViewModel.getUserId(), oldId, ids.get(w)))
                .show();
    }

    private void showDeleteConfirmDialog(Category category) {
        if (category.isSystemCategory()) { Toast.makeText(getContext(), "Không thể xoá danh mục hệ thống", Toast.LENGTH_SHORT).show(); return; }
        new AlertDialog.Builder(requireContext()).setTitle("Xoá danh mục").setMessage("Bạn muốn xoá '" + category.getName() + "'?")
                .setPositiveButton("Xoá", (d, w) -> categoryViewModel.checkCategoryUsage(authViewModel.getUserId(), category.getId()))
                .setNegativeButton("Hủy", null).show();
    }

    // Các hàm phụ trợ Icon và UI khác (Giữ nguyên hoặc cập nhật như dưới)
    private void showAddCategoryDialog() {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null);
        AlertDialog d = new AlertDialog.Builder(requireContext()).setView(v).create();
        EditText et = v.findViewById(R.id.etCategoryName);
        RadioGroup rg = v.findViewById(R.id.rgCategoryType);
        ImageView iv = v.findViewById(R.id.ivSelectedIcon);
        selectedIcon = "ic_food"; updateIconImageView(iv, selectedIcon);
        iv.setOnClickListener(view -> showIconPickerDialog(iv));
        v.findViewById(R.id.btnSave).setOnClickListener(view -> {
            String n = et.getText().toString().trim();
            if (n.isEmpty()) return;
            categoryViewModel.addCategoryWithIcon(authViewModel.getUserId(), n, (rg.getCheckedRadioButtonId() == R.id.rbIncome ? TransactionType.INCOME : TransactionType.EXPENSE), selectedIcon);
            d.dismiss();
        });
        v.findViewById(R.id.btnCancel).setOnClickListener(view -> d.dismiss());
        d.show();
    }

    private void showEditCategoryDialog(Category c) {
        if (c.isSystemCategory()) return;
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_category, null);
        AlertDialog d = new AlertDialog.Builder(requireContext()).setView(v).create();
        EditText et = v.findViewById(R.id.etCategoryName);
        RadioGroup rg = v.findViewById(R.id.rgCategoryType);
        ImageView iv = v.findViewById(R.id.ivSelectedIcon);
        et.setText(c.getName());
        if (c.getType() == TransactionType.INCOME) ((RadioButton)v.findViewById(R.id.rbIncome)).setChecked(true);
        selectedIcon = c.getIconName() != null ? c.getIconName() : "ic_food";
        updateIconImageView(iv, selectedIcon);
        iv.setOnClickListener(view -> showIconPickerDialog(iv));
        v.findViewById(R.id.btnSave).setOnClickListener(view -> {
            c.setName(et.getText().toString().trim());
            c.setType(rg.getCheckedRadioButtonId() == R.id.rbIncome ? TransactionType.INCOME : TransactionType.EXPENSE);
            c.setIconName(selectedIcon);
            categoryViewModel.updateCategory(authViewModel.getUserId(), c);
            d.dismiss();
        });
        v.findViewById(R.id.btnCancel).setOnClickListener(view -> d.dismiss());
        d.show();
    }

    private void updateIconImageView(ImageView iv, String iconName) {
        int resId = getResources().getIdentifier(iconName, "drawable", requireContext().getPackageName());
        if (resId != 0) iv.setImageResource(resId);
    }

    private void showIconPickerDialog(ImageView iv) {
        TypedArray icons = getResources().obtainTypedArray(R.array.icon_pack_custom);
        List<String> list = new ArrayList<>();
        for (int i = 0; i < icons.length(); i++) {
            int resId = icons.getResourceId(i, 0);
            if (resId != 0) list.add(getResources().getResourceEntryName(resId));
        }
        icons.recycle();
        View dv = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_icon_picker, null);
        RecyclerView rv = dv.findViewById(R.id.rvIcons);
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        AlertDialog d = new AlertDialog.Builder(requireContext()).setView(dv).create();
        rv.setAdapter(new IconPickerAdapter(list, name -> { selectedIcon = name; updateIconImageView(iv, selectedIcon); d.dismiss(); }));
        d.show();
    }

    private void toggleBottomNavigation(boolean isVisible) {
        if (getActivity() == null) return;
        View nav = getActivity().findViewById(R.id.bottomAppBar);
        View fab = getActivity().findViewById(R.id.fabAdd);
        if (nav != null) nav.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        if (fab != null) fab.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    @Override public void onDestroyView() { toggleBottomNavigation(true); super.onDestroyView(); }
}