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
import com.ptithcm.quanlichitieu.data.model.TransactionType;
import com.ptithcm.quanlichitieu.ui.login.AuthViewModel;

/**
 * CategoryFragment: Quản lý danh mục (Categories).
 * Được thiết kế để hiện danh sách các category và cho phép thêm, sửa, xoá.
 */
public class CategoryFragment extends Fragment {

    private AuthViewModel authViewModel;
    private CategoryViewModel categoryViewModel;
    private CategoryAdapter adapter;

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
            adapter.updateData(categories);
        });

        categoryViewModel.getAddResult().observe(getViewLifecycleOwner(), success -> {
            if (getContext() != null) {
                if (success) {
                    Toast.makeText(getContext(), "Thêm danh mục thành công", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Thêm danh mục thất bại", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupViews(View view) {
        // Nút back
        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        }

        // Khởi tạo danh sách Category
        RecyclerView rvCategories = view.findViewById(R.id.rvCategories);
        if (rvCategories != null) {
            rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new CategoryAdapter();
            rvCategories.setAdapter(adapter);
        }

        // Nút thêm danh mục mới
        View fabAddCategory = view.findViewById(R.id.fabAddCategory);
        if (fabAddCategory != null) {
            fabAddCategory.setOnClickListener(v -> showAddCategoryDialog());
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

            TransactionType type = rgType.getCheckedRadioButtonId() == R.id.rbIncome
                    ? TransactionType.INCOME : TransactionType.EXPENSE;

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
