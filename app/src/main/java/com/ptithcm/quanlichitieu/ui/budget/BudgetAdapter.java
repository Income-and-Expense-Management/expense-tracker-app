package com.ptithcm.quanlichitieu.ui.budget;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ptithcm.quanlichitieu.R;

import java.util.ArrayList;
import java.util.List;

/**
 * BudgetAdapter - Adapter cho hiển thị danh sách budget items.
 * 
 * Cung cấp callback cho các action: click item, edit, delete.
 */
public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    private List<BudgetItem> budgetItems;
    private OnBudgetItemClickListener itemClickListener;
    private OnBudgetMenuClickListener menuClickListener;

    public interface OnBudgetItemClickListener {
        void onItemClick(BudgetItem item);
    }

    public interface OnBudgetMenuClickListener {
        void onEditClick(BudgetItem item);
        void onDeleteClick(BudgetItem item);
    }

    public BudgetAdapter(List<BudgetItem> budgetItems) {
        this.budgetItems = budgetItems != null ? budgetItems : new ArrayList<>();
    }

    public void setOnItemClickListener(OnBudgetItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setOnMenuClickListener(OnBudgetMenuClickListener listener) {
        this.menuClickListener = listener;
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        BudgetItem item = budgetItems.get(position);
        holder.bind(item, itemClickListener, menuClickListener);
    }

    @Override
    public int getItemCount() {
        return budgetItems.size();
    }

    public void updateData(List<BudgetItem> newItems) {
        this.budgetItems = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class BudgetViewHolder extends RecyclerView.ViewHolder {
        private final View ivCategoryIcon;
        private final TextView tvBudgetCategoryName;
        private final TextView tvBudgetAmount;
        private final TextView tvBudgetTags;
        private final ProgressBar progressBudget;
        private final ImageView ivBudgetMenu;

        public BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvBudgetCategoryName = itemView.findViewById(R.id.tvBudgetCategoryName);
            tvBudgetAmount = itemView.findViewById(R.id.tvBudgetAmount);
            tvBudgetTags = itemView.findViewById(R.id.tvBudgetTags);
            progressBudget = itemView.findViewById(R.id.progressBudget);
            ivBudgetMenu = itemView.findViewById(R.id.ivBudgetMenu);
        }

        public void bind(BudgetItem item, OnBudgetItemClickListener itemClickListener,
                        OnBudgetMenuClickListener menuClickListener) {
            tvBudgetCategoryName.setText(item.getCategoryName());
            tvBudgetAmount.setText(item.getFormattedAmount());
            
            // Set tags/description
            int daysRemaining = item.getDaysRemaining();
            if (daysRemaining > 0) {
                tvBudgetTags.setText("Còn " + daysRemaining + " ngày");
            } else {
                tvBudgetTags.setText("Đã hết hạn");
            }

            // Set progress
            int progress = item.getProgress();
            progressBudget.setProgress(progress);

            // Update progress color based on budget status
            if (item.isOverBudget()) {
                // Red for over budget
                tvBudgetAmount.setTextColor(Color.parseColor("#FF4444"));
            } else if (progress > 80) {
                // Yellow/Orange for warning
                tvBudgetAmount.setTextColor(Color.parseColor("#FFA500"));
            } else {
                // Default color
                tvBudgetAmount.setTextColor(Color.WHITE);
            }

            // Set icon background color
            try {
                String color = item.getColor();
                if (color != null && !color.isEmpty()) {
                    GradientDrawable drawable = new GradientDrawable();
                    drawable.setShape(GradientDrawable.OVAL);
                    drawable.setColor(Color.parseColor(color));
                    ivCategoryIcon.setBackground(drawable);
                }
            } catch (Exception e) {
                // Use default color on error
            }

            // Item click
            itemView.setOnClickListener(v -> {
                if (itemClickListener != null) {
                    itemClickListener.onItemClick(item);
                }
            });

            // Menu click
            ivBudgetMenu.setOnClickListener(v -> {
                showPopupMenu(v, item, menuClickListener);
            });
        }

        private void showPopupMenu(View anchor, BudgetItem item, OnBudgetMenuClickListener listener) {
            PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
            popup.getMenuInflater().inflate(R.menu.menu_budget_item, popup.getMenu());
            
            popup.setOnMenuItemClickListener(menuItem -> {
                if (listener == null) return false;
                
                int itemId = menuItem.getItemId();
                if (itemId == R.id.action_edit) {
                    listener.onEditClick(item);
                    return true;
                } else if (itemId == R.id.action_delete) {
                    listener.onDeleteClick(item);
                    return true;
                }
                return false;
            });
            
            popup.show();
        }
    }
}