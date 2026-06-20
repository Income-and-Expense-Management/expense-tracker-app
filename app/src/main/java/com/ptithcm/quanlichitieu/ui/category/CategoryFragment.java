package com.ptithcm.quanlichitieu.ui.category;

import android.app.AlertDialog;
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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.tabs.TabLayout;
import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.model.Category;
import com.ptithcm.quanlichitieu.data.model.TransactionType;
import com.ptithcm.quanlichitieu.ui.login.AuthViewModel;

import java.util.ArrayList;
import java.util.Collections;
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
    private String selectedIcon = "ic_food";
    private Category currentDeletingCategory;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
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

        String userId = authViewModel.getUserId();
        categoryViewModel.loadCategoriesForManagement(userId);
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
            if (categories != null) {
                allCategories.addAll(categories);
            }
            filterCategories();
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        categoryViewModel.getSortOrder().observe(getViewLifecycleOwner(), order -> {
            View root = getView();
            if (root == null) return;
            ImageView btnFilter = root.findViewById(R.id.btnFilter);
            if (btnFilter != null) {
                if (order == CategoryViewModel.SortOrder.A_TO_Z) {
                    btnFilter.setColorFilter(getResources().getColor(R.color.home_accent_green, null));
                } else {
                    btnFilter.setColorFilter(getResources().getColor(android.R.color.darker_gray, null));
                }
            }
            filterCategories();
        });

        categoryViewModel.getAddResult().observe(getViewLifecycleOwner(), success -> {
            if (success != null) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), success ? "Thêm danh mục thành công" : "Thêm danh mục thất bại", Toast.LENGTH_SHORT).show();
                }
                categoryViewModel.resetAddResult();
            }
        });

        categoryViewModel.getUpdateResult().observe(getViewLifecycleOwner(), success -> {
            if (success != null) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), success ? "Cập nhật danh mục thành công" : "Cập nhật danh mục thất bại", Toast.LENGTH_SHORT).show();
                }
                categoryViewModel.resetUpdateResult();
            }
        });

        categoryViewModel.getDeleteResult().observe(getViewLifecycleOwner(), success -> {
            if (success != null) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), success ? "Xoá danh mục thành công" : "Xoá danh mục thất bại", Toast.LENGTH_SHORT).show();
                }
                categoryViewModel.resetDeleteResult();
            }
        });

        categoryViewModel.getTransactionCountResult().observe(getViewLifecycleOwner(), count -> {
            if (currentDeletingCategory != null && count != null) {
                showFinalDeleteDialog(currentDeletingCategory, count);
            }
        });
    }

    private void setupViews(View view) {
        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        }

        ImageView btnFilter = view.findViewById(R.id.btnFilter);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> categoryViewModel.toggleSortOrder());
        }

        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        if (tabLayout != null) {
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    currentType = tab.getPosition() == 1 ? TransactionType.INCOME : TransactionType.EXPENSE;
                    filterCategories();
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}

                @Override
                public void onTabReselected(TabLayout.Tab tab) {}
            });
        }

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(R.color.home_expense_red, R.color.home_accent_green);
            swipeRefreshLayout.setOnRefreshListener(() -> {
                if (authViewModel != null && categoryViewModel != null) {
                    categoryViewModel.refreshFromServer(authViewModel.getUserId());
                }
            });
        }

        RecyclerView rvCategories = view.findViewById(R.id.rvCategories);
        if (rvCategories != null) {
            rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new CategoryAdapter(new CategoryAdapter.OnCategoryClickListener() {
                @Override
                public void onCategoryClick(Category category) {
                    showEditCategoryDialog(category);
                }

                @Override
                public void onCategoryLongClick(Category category) {
                    showDeleteConfirmDialog(category);
                }

                @Override
                public void onCategorySwitchToggled(Category category, boolean isChecked) {
                    if (category.isActive() != isChecked) {
                        category.setActive(isChecked);
                        categoryViewModel.updateCategory(authViewModel.getUserId(), category);
                    }
                }
            });
            rvCategories.setAdapter(adapter);
        }

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

            CategoryViewModel.SortOrder order = categoryViewModel.getSortOrder().getValue();
            if (order == CategoryViewModel.SortOrder.Z_TO_A) {
                Collections.sort(filteredList, (a, b) -> b.getName().compareToIgnoreCase(a.getName()));
            } else {
                Collections.sort(filteredList, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
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

            TransactionType type = rgType.getCheckedRadioButtonId() == R.id.rbIncome ? TransactionType.INCOME : TransactionType.EXPENSE;
            categoryViewModel.addCategoryWithIcon(authViewModel.getUserId(), name, type, selectedIcon);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showEditCategoryDialog(Category category) {
        if (category.isSystemCategory()) {
            Toast.makeText(getContext(), "Không thể sửa danh mục hệ thống", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_category, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        EditText etCategoryName = dialogView.findViewById(R.id.etCategoryName);
        RadioGroup rgCategoryType = dialogView.findViewById(R.id.rgCategoryType);
        RadioButton rbExpense = dialogView.findViewById(R.id.rbExpense);
        RadioButton rbIncome = dialogView.findViewById(R.id.rbIncome);
        ImageView ivSelectedIcon = dialogView.findViewById(R.id.ivSelectedIcon);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        etCategoryName.setText(category.getName());
        if (category.getType() == TransactionType.INCOME) {
            rbIncome.setChecked(true);
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

            TransactionType type = rgCategoryType.getCheckedRadioButtonId() == R.id.rbIncome ? TransactionType.INCOME : TransactionType.EXPENSE;
            category.setName(name);
            category.setType(type);
            category.setIconName(selectedIcon);

            categoryViewModel.updateCategory(authViewModel.getUserId(), category);
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

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        IconPickerAdapter rvAdapter = new IconPickerAdapter(iconList, iconName -> {
            selectedIcon = iconName;
            updateIconImageView(ivSelectedIcon, selectedIcon);
            dialog.dismiss();
        });

        recyclerView.setAdapter(rvAdapter);
        dialog.show();
    }

    private void showDeleteConfirmDialog(Category category) {
        if (category.isSystemCategory()) {
            Toast.makeText(getContext(), "Không thể xoá danh mục hệ thống", Toast.LENGTH_SHORT).show();
            return;
        }

        currentDeletingCategory = category;
        categoryViewModel.checkTransactionCount(authViewModel.getUserId(), category.getId());
    }

    private void showFinalDeleteDialog(Category category, int transactionCount) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Xoá danh mục");

        if (transactionCount > 0) {
            builder.setMessage("Danh mục '" + category.getName() + "' hiện có " + transactionCount + " giao dịch liên quan. Bạn có chắc chắn muốn xoá toàn bộ danh mục và các giao dịch liên quan không?");
            builder.setPositiveButton("Xoá toàn bộ", (dialog, which) -> {
                categoryViewModel.deleteCategoryWithTransactions(authViewModel.getUserId(), category.getId());
                currentDeletingCategory = null;
            });
        } else {
            builder.setMessage("Danh mục '" + category.getName() + "' hiện có 0 giao dịch liên quan. Bạn có chắc chắn muốn xoá danh mục này không?");
            builder.setPositiveButton("Xoá", (dialog, which) -> {
                categoryViewModel.deleteCategory(authViewModel.getUserId(), category.getId());
                currentDeletingCategory = null;
            });
        }

        builder.setNegativeButton("Hủy", (dialog, which) -> currentDeletingCategory = null);
        builder.setOnDismissListener(dialog -> currentDeletingCategory = null);
        builder.show();
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
            int marginInPx = isVisible ? (int) (80 * getResources().getDisplayMetrics().density) : 0;
            params.bottomMargin = marginInPx;
            container.setLayoutParams(params);
        }
    }
}
