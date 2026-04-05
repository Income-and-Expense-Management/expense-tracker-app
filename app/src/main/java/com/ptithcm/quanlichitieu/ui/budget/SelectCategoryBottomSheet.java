package com.ptithcm.quanlichitieu.ui.budget;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import com.ptithcm.quanlichitieu.data.model.Category;
import com.ptithcm.quanlichitieu.data.repository.BudgetRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * SelectCategoryBottomSheet - BottomSheet để chọn category cho budget.
 */
public class SelectCategoryBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "SelectCategoryBottomSheet";

    private RecyclerView rvCategories;
    private EditText etSearch;
    private LinearLayout layoutEmpty;

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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCategories(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadCategories() {
        BudgetRepository repository = BudgetRepository.getInstance(requireContext());
        allCategories = repository.getExpenseCategories(null);
        
        if (allCategories.isEmpty()) {
            rvCategories.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            rvCategories.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            adapter.setCategories(allCategories);
        }
    }

    private void filterCategories(String query) {
        if (query.isEmpty()) {
            adapter.setCategories(allCategories);
            return;
        }

        List<Category> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for (Category category : allCategories) {
            if (category.getName().toLowerCase().contains(lowerQuery)) {
                filtered.add(category);
            }
        }
        adapter.setCategories(filtered);
    }

    /**
     * Adapter for category selection list.
     */
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
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category_select, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(categories.get(position), clickListener);
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private final View viewCategoryIcon;
            private final TextView tvCategoryName;
            private final ImageView ivSelected;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                viewCategoryIcon = itemView.findViewById(R.id.viewCategoryIcon);
                tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
                ivSelected = itemView.findViewById(R.id.ivSelected);
            }

            void bind(Category category, OnCategoryClickListener listener) {
                tvCategoryName.setText(category.getName());

                // Set icon color based on category
                try {
                    String color = getCategoryColor(category.getIconName());
                    GradientDrawable drawable = new GradientDrawable();
                    drawable.setShape(GradientDrawable.OVAL);
                    drawable.setColor(Color.parseColor(color));
                    viewCategoryIcon.setBackground(drawable);
                } catch (Exception e) {
                    // Use default
                }

                ivSelected.setVisibility(View.GONE);

                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onClick(category);
                    }
                });
            }

            private static String getCategoryColor(String iconName) {
                if (iconName == null) return "#4CAF50";

                switch (iconName.toLowerCase()) {
                    case "food":
                    case "ic_food":
                        return "#E91E63";
                    case "shopping":
                    case "ic_shopping":
                        return "#9C27B0";
                    case "transport":
                    case "ic_transport":
                        return "#2196F3";
                    case "entertainment":
                        return "#FF9800";
                    case "health":
                        return "#00BCD4";
                    case "education":
                        return "#3F51B5";
                    case "bills":
                        return "#F44336";
                    default:
                        return "#4CAF50";
                }
            }
        }
    }
}
