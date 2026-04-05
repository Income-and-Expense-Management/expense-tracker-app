package com.ptithcm.quanlichitieu.ui.category;

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

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
        void onCategoryLongClick(Category category);
        void onCategorySwitchToggled(Category category, boolean isChecked);
    }

    private final List<Category> dataList = new ArrayList<>();
    private final OnCategoryClickListener listener;

    public CategoryAdapter(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<Category> newData) {
        dataList.clear();
        if (newData != null) {
            dataList.addAll(newData);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = dataList.get(position);
        holder.bind(category, listener);
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName;
        private final ImageView ivIcon;
        private final androidx.appcompat.widget.SwitchCompat switchCategory;

        public ViewHolder(@NonNull View itemView, OnCategoryClickListener listener) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            switchCategory = itemView.findViewById(R.id.switchCategory);
        }

        public void bind(Category category, OnCategoryClickListener listener) {
            tvName.setText(category.getName());

            // Remove listener temporarily to avoid firing events during bind
            switchCategory.setOnCheckedChangeListener(null);
            switchCategory.setChecked(category.isActive());

            // Highlight switch: xanh khi on, xám khi off
            if (category.isActive()) {
                switchCategory.getThumbDrawable().setTint(itemView.getContext().getResources().getColor(R.color.switch_thumb_on, null));
                switchCategory.getTrackDrawable().setTint(itemView.getContext().getResources().getColor(R.color.switch_track_on, null));
            } else {
                switchCategory.getThumbDrawable().setTint(itemView.getContext().getResources().getColor(R.color.switch_thumb_off, null));
                switchCategory.getTrackDrawable().setTint(itemView.getContext().getResources().getColor(R.color.switch_track_off, null));
            }

            if (category.getIconName() != null && !category.getIconName().isEmpty()) {
                int resId = itemView.getContext().getResources().getIdentifier(
                        category.getIconName(), "drawable", itemView.getContext().getPackageName());
                if (resId != 0) {
                    ivIcon.setImageResource(resId);
                } else {
                    ivIcon.setImageResource(R.drawable.ic_food); // default fallback
                }
            } else {
                ivIcon.setImageResource(R.drawable.ic_food); // default fallback
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryClick(category);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryLongClick(category);
                }
                return true;
            });

            switchCategory.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onCategorySwitchToggled(category, isChecked);
                }
            });
        }
    }
}
