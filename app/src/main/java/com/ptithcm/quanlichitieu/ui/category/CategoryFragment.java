package com.ptithcm.quanlichitieu.ui.category;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.model.Category;
import com.ptithcm.quanlichitieu.data.model.TransactionType;
import com.ptithcm.quanlichitieu.ui.login.AuthViewModel;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * CategoryFragment: Quản lý danh mục (Categories).
 * Được thiết kế để hiện danh sách các category và cho phép thêm, sửa, xoá.
 */
public class CategoryFragment extends Fragment {

    private AuthViewModel authViewModel;
    private CategoryViewModel categoryViewModel;
    private CategoryAdapter adapter;
    private List<Category> allCategories = new ArrayList<>();
    private TransactionType currentType = TransactionType.EXPENSE;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ẩn thanh bottom navigation và fab chung của MainActivity khi vào màn hình này
        toggleBottomNavigation(false);

        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);

        setupViews(view);
        observeViewModel();

        String userId = authViewModel.getUserId();
        categoryViewModel.loadCategories(userId);
    }

    private void observeViewModel() {
        categoryViewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            allCategories = categories;
            filterCategories();
        });

        categoryViewModel.getAddResult().observe(getViewLifecycleOwner(), success -> {
            if (success == null) return;
            if (getContext() != null) {
                if (success) {
                    Toast.makeText(getContext(), "Thêm danh mục thành công", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Thêm danh mục thất bại", Toast.LENGTH_SHORT).show();
                }
            }
            categoryViewModel.resetAddResult();
        });
    }

    private void setupViews(View view) {
        // Nút back
        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        }

        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        if (tabLayout != null) {
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    switch (tab.getPosition()) {
                        case 0:
                            currentType = TransactionType.EXPENSE;
                            break;
                        case 1:
                            currentType = TransactionType.INCOME;
                            break;
                        case 2:
                            currentType = TransactionType.LOAN;
                            break;
                    }
                    filterCategories();
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}

                @Override
                public void onTabReselected(TabLayout.Tab tab) {}
            });
        }

        // Khởi tạo danh sách Category
        RecyclerView rvCategories = view.findViewById(R.id.rvCategories);
        if (rvCategories != null) {
            rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new CategoryAdapter();
            rvCategories.setAdapter(adapter);
        }

        // Nút thêm danh mục mới
        View btnAddNewCategory = view.findViewById(R.id.btnAddNewCategory);
        if (btnAddNewCategory != null) {
            btnAddNewCategory.setOnClickListener(v -> showAddCategoryDialog());
        }
    }

    private void filterCategories() {
        if (adapter != null) {
            List<Category> filteredList = new ArrayList<>();
            for (Category cat : allCategories) {
                if (cat.getType() == currentType) {
                    filteredList.add(cat);
                }
            }
            adapter.updateData(filteredList);
        }
    }

    private void showAddCategoryDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        EditText etConfigName = dialogView.findViewById(R.id.etCategoryName);
        RadioGroup rgType = dialogView.findViewById(R.id.rgCategoryType);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etConfigName.getText().toString().trim();
            if (name.isEmpty()) {
                etConfigName.setError("Vui lòng nhập tên");
                return;
            }

            TransactionType type = TransactionType.EXPENSE;
            int checkedId = rgType.getCheckedRadioButtonId();
            if (checkedId == R.id.rbIncome) {
                type = TransactionType.INCOME;
            } else if (checkedId == R.id.rbLoan) {
                type = TransactionType.LOAN;
            }

            categoryViewModel.addCategory(authViewModel.getUserId(), name, type);
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        // Hiện lại thanh bottom navigation khi thoát
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
            int marginInPx = isVisible ? (int) (80 * getResources().getDisplayMetrics().density) : 0;
            params.bottomMargin = marginInPx;
            container.setLayoutParams(params);
        }
    }
}
