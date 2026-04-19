package com.ptithcm.quanlichitieu.ui.transaction.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryPickerAdapter extends RecyclerView.Adapter<CategoryPickerAdapter.VH> {

    private List<Category> categories;
    private final OnCategoryClick listener;

    public interface OnCategoryClick {
        void onClick(Category category);
    }

    public CategoryPickerAdapter(List<Category> categories, OnCategoryClick listener) {
        this.categories = categories;
        this.listener = listener;
    }

    public void filter(String query, List<Category> allCategories) {
        if (query == null || query.isEmpty()) {
            this.categories = allCategories;
        } else {
            String lower = query.toLowerCase();
            List<Category> filtered = new ArrayList<>();
            for (Category c : allCategories) {
                if (c.getName().toLowerCase().contains(lower)) {
                    filtered.add(c);
                }
            }
            this.categories = filtered;
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_select, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Category category = categories.get(position);
        holder.tvName.setText(category.getName());

        String iconName = category.getIconName();
        if (iconName != null && !iconName.isEmpty()) {
            int resId = holder.itemView.getContext().getResources().getIdentifier(
                    iconName, "drawable", holder.itemView.getContext().getPackageName());
            if (resId != 0) {
                holder.imgIcon.setImageResource(resId);
            } else {
                holder.imgIcon.setImageResource(R.drawable.ic_food);
            }
        } else {
            holder.imgIcon.setImageResource(R.drawable.ic_food);
        }

        holder.ivSelected.setVisibility(View.GONE);
        holder.itemView.setOnClickListener(v -> listener.onClick(category));
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView tvName;
        ImageView ivSelected;

        VH(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.imgCategoryIcon);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            ivSelected = itemView.findViewById(R.id.ivSelected);
        }
    }
}
