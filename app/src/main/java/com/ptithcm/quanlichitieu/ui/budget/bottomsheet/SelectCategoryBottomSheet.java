package com.ptithcm.quanlichitieu.ui.budget.bottomsheet;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.local.token.EncryptedTokenStorage;
import com.ptithcm.quanlichitieu.data.local.token.TokenStorage;
import com.ptithcm.quanlichitieu.data.model.Category;
import com.ptithcm.quanlichitieu.data.repository.BudgetRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SelectCategoryBottomSheet - BottomSheet để chọn danh mục cho ngân sách.
 * Đã được refactor theo chuẩn Clean Code: dễ đọc, ưu tiên tính module và bảo trì.
 */
public class SelectCategoryBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "SelectCategoryBottomSheet";

    // Views
    private RecyclerView rvCategories;
    private EditText etSearch;
    private LinearLayout layoutEmpty;

    // State & Adapters
    private CategorySelectAdapter adapter;
    private List<Category> allCategories = new ArrayList<>();
    private OnCategorySelectedListener listener;

    public interface OnCategorySelectedListener {
        void onCategorySelected(Category category);
    }

    public static SelectCategoryBottomSheet newInstance() {
        return new SelectCategoryBottomSheet();
    }

    public void setOnCategorySelectedListener(OnCategorySelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_select_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupSearch();
        loadCategories();
    }

    private void initViews(View view) {
        rvCategories = view.findViewById(R.id.rvCategories);
        etSearch = view.findViewById(R.id.etSearch);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
    }

    private void setupRecyclerView() {
        adapter = new CategorySelectAdapter(category -> {
            if (listener != null) {
                listener.onCategorySelected(category);
            }
            dismiss();
        });
        rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCategories.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCategories(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadCategories() {
        BudgetRepository repository = BudgetRepository.getInstance(requireContext());
        TokenStorage tokenStorage = EncryptedTokenStorage.getInstance(requireContext());
        String userId = tokenStorage.getUserId();
        
        allCategories = repository.getExpenseCategories(userId);

        updateUIBasedOnData(allCategories);
    }

    private void filterCategories(String query) {
        if (query == null || query.trim().isEmpty()) {
            adapter.setCategories(allCategories);
            return;
        }

        String lowerQuery = query.toLowerCase().trim();
        List<Category> filtered = allCategories.stream()
                .filter(category -> category.getName() != null && category.getName().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
                
        adapter.setCategories(filtered);
    }

    private void updateUIBasedOnData(List<Category> categories) {
        boolean isEmpty = categories == null || categories.isEmpty();
        
        rvCategories.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        
        if (!isEmpty) {
            adapter.setCategories(categories);
        }
    }

    // =========================================================================
    // INNER CLASSES: Adapter & ViewHolder
    // =========================================================================

    private static class CategorySelectAdapter extends RecyclerView.Adapter<CategorySelectAdapter.ViewHolder> {

        private List<Category> categories = new ArrayList<>();
        private final OnCategoryClickListener clickListener;

        interface OnCategoryClickListener {
            void onClick(Category category);
        }

        CategorySelectAdapter(OnCategoryClickListener listener) {
            this.clickListener = listener;
        }

        void setCategories(List<Category> categories) {
            this.categories = categories;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_select, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(categories.get(position), clickListener);
        }

        @Override
        public int getItemCount() {
            return categories != null ? categories.size() : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private final ImageView imgCategoryIcon;
            private final TextView tvCategoryName;
            private final ImageView ivSelected;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                imgCategoryIcon = itemView.findViewById(R.id.imgCategoryIcon);
                tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
                ivSelected = itemView.findViewById(R.id.ivSelected);
            }

            void bind(Category category, OnCategoryClickListener listener) {
                tvCategoryName.setText(category.getName());
                loadIconResource(category.getIconName());
                
                ivSelected.setVisibility(View.GONE);

                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onClick(category);
                    }
                });
            }

            private void loadIconResource(String iconName) {
                int defaultIcon = R.drawable.ic_food;

                if (iconName == null || iconName.trim().isEmpty()) {
                    imgCategoryIcon.setImageResource(defaultIcon);
                    return;
                }

                int resId = itemView.getContext().getResources().getIdentifier(
                        iconName, "drawable", itemView.getContext().getPackageName());

                imgCategoryIcon.setImageResource(resId != 0 ? resId : defaultIcon);
            }
        }
    }
}
