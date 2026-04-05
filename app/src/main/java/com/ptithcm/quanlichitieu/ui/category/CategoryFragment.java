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
import androidx.recyclerview.widget.GridLayoutManager;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.model.Category;
import com.ptithcm.quanlichitieu.data.model.TransactionType;
import com.ptithcm.quanlichitieu.ui.login.AuthViewModel;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.widget.GridLayout;
import android.content.res.TypedArray;

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
    private String selectedIcon = "ic_food";

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
            allCategories.clear();
            if (categories != null) {
                allCategories.addAll(categories);
            }
            filterCategories();
        });

        categoryViewModel.getAddResult().observe(getViewLifecycleOwner(), success -> {
            if (success != null) {
                if (getContext() != null) {
                    if (success) {
                        Toast.makeText(getContext(), "Thêm danh mục thành công", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Thêm danh mục thất bại", Toast.LENGTH_SHORT).show();
                    }
                }
                categoryViewModel.resetAddResult();
            }
        });

        categoryViewModel.getUpdateResult().observe(getViewLifecycleOwner(), success -> {
            if (success != null) {
                if (getContext() != null) {
                    if (success) {
                        Toast.makeText(getContext(), "Cập nhật danh mục thành công", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Cập nhật danh mục thất bại", Toast.LENGTH_SHORT).show();
                    }
                }
                categoryViewModel.resetUpdateResult();
            }
        });

        categoryViewModel.getDeleteResult().observe(getViewLifecycleOwner(), success -> {
            if (success != null) {
                if (getContext() != null) {
                    if (success) {
                        Toast.makeText(getContext(), "Xoá danh mục thành công", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Xoá danh mục thất bại", Toast.LENGTH_SHORT).show();
                    }
                }
                categoryViewModel.resetDeleteResult();
            }
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
            adapter = new CategoryAdapter(new CategoryAdapter.OnCategoryClickListener() {
                @Override
                public void onCategoryClick(com.ptithcm.quanlichitieu.data.model.Category category) {
                    showEditCategoryDialog(category);
                }

                @Override
                public void onCategoryLongClick(com.ptithcm.quanlichitieu.data.model.Category category) {
                    showDeleteConfirmDialog(category);
                }
            });
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
        ImageView ivSelectedIcon = dialogView.findViewById(R.id.ivSelectedIcon);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        selectedIcon = "ic_food";
        updateIconImageView(ivSelectedIcon, selectedIcon);

        ivSelectedIcon.setOnClickListener(v -> showIconPickerDialog(ivSelectedIcon));

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

            categoryViewModel.addCategoryWithIcon(authViewModel.getUserId(), name, type, selectedIcon);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showEditCategoryDialog(com.ptithcm.quanlichitieu.data.model.Category category) {
        if (category.isSystemCategory()) {
            Toast.makeText(getContext(), "Không thể sửa danh mục hệ thống", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_category, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        EditText etCategoryName = dialogView.findViewById(R.id.etCategoryName);
        RadioGroup rgCategoryType = dialogView.findViewById(R.id.rgCategoryType);
        RadioButton rbExpense = dialogView.findViewById(R.id.rbExpense);
        RadioButton rbIncome = dialogView.findViewById(R.id.rbIncome);
        RadioButton rbLoan = dialogView.findViewById(R.id.rbLoan);
        ImageView ivSelectedIcon = dialogView.findViewById(R.id.ivSelectedIcon);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        // Set current values
        etCategoryName.setText(category.getName());
        if (category.getType() == TransactionType.INCOME) {
            rbIncome.setChecked(true);
        } else if (category.getType() == TransactionType.LOAN) {
            rbLoan.setChecked(true);
        } else {
            rbExpense.setChecked(true);
        }

        selectedIcon = category.getIconName() != null ? category.getIconName() : "ic_food";
        updateIconImageView(ivSelectedIcon, selectedIcon);

        ivSelectedIcon.setOnClickListener(v -> showIconPickerDialog(ivSelectedIcon));

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etCategoryName.getText().toString().trim();
            if (name.isEmpty()) {
                etCategoryName.setError("Vui lòng nhập tên danh mục");
                return;
            }

            TransactionType type = TransactionType.EXPENSE;
            int checkedId = rgCategoryType.getCheckedRadioButtonId();
            if (checkedId == R.id.rbIncome) {
                type = TransactionType.INCOME;
            } else if (checkedId == R.id.rbLoan) {
                type = TransactionType.LOAN;
            }

            category.setName(name);
            category.setType(type);
            category.setIconName(selectedIcon);

            String userId = authViewModel.getUserId();
            categoryViewModel.updateCategory(userId, category);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateIconImageView(ImageView iv, String iconName) {
        int resId = getResources().getIdentifier(iconName, "drawable", requireContext().getPackageName());
        if (resId != 0) {
            iv.setImageResource(resId);
        }
    }

    private void showIconPickerDialog(ImageView ivSelectedIcon) {
        TypedArray icons = getResources().obtainTypedArray(R.array.icon_pack_custom);
        List<String> iconList = new ArrayList<>();
        for (int i = 0; i < icons.length(); i++) {
            int resId = icons.getResourceId(i, 0);
            if (resId != 0) {
                iconList.add(getResources().getResourceEntryName(resId));
            }
        }
        icons.recycle();

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_icon_picker, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.rvIcons);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 4));

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        AlertDialog dialog = builder.setView(dialogView).create();

        IconPickerAdapter rvAdapter = new IconPickerAdapter(iconList, iconName -> {
            selectedIcon = iconName;
            updateIconImageView(ivSelectedIcon, selectedIcon);
            dialog.dismiss();
        });

        recyclerView.setAdapter(rvAdapter);
        dialog.show();
    }

    private void showDeleteConfirmDialog(com.ptithcm.quanlichitieu.data.model.Category category) {
        if (category.isSystemCategory()) {
            Toast.makeText(getContext(), "Không thể xoá danh mục hệ thống", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Xoá danh mục")
                .setMessage("Bạn có chắc chắn muốn xoá danh mục '" + category.getName() + "' không?")
                .setPositiveButton("Xoá", (dialog, which) -> {
                    String userId = authViewModel.getUserId();
                    categoryViewModel.deleteCategory(userId, category.getId());
                })
                .setNegativeButton("Hủy", null)
                .show();
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
