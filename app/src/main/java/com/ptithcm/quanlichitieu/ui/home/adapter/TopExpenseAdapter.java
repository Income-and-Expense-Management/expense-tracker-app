package com.ptithcm.quanlichitieu.ui.home.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.model.Expense;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * TopExpenseAdapter: RecyclerView adapter for displaying top expense items.
 *
 * ARCHITECTURE NOTE:
 * This adapter follows the Single Responsibility Principle (S in SOLID):
 * - It ONLY handles the display of expense data in a RecyclerView
 * - It does NOT fetch data or handle business logic
 * - Data is provided via setExpenses() method from the Activity/ViewModel
 *
 * The adapter is decoupled from the data source - it doesn't know (or care)
 * whether the data comes from a mock, API, or database. This makes it:
 * - Reusable across different screens
 * - Easy to test with mock data
 * - Independent of data layer changes
 */
public class TopExpenseAdapter extends RecyclerView.Adapter<TopExpenseAdapter.ExpenseViewHolder> {

    private List<Expense> expenses = new ArrayList<>();
    private OnExpenseClickListener clickListener;

    /**
     * Interface for handling expense item clicks.
     * Follows the Observer pattern for loose coupling between adapter and activity.
     */
    public interface OnExpenseClickListener {
        void onExpenseClick(Expense expense);
    }

    public void setOnExpenseClickListener(OnExpenseClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * Updates the expense list and refreshes the RecyclerView.
     * Call this method when new data is available from the repository.
     *
     * @param newExpenses List of expenses to display
     */
    public void setExpenses(List<Expense> newExpenses) {
        this.expenses = newExpenses != null ? newExpenses : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_top_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenses.get(position);
        holder.bind(expense, clickListener);
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    /**
     * ViewHolder for expense items.
     * Holds references to views and handles binding data to them.
     */
    static class ExpenseViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imgCategoryIcon;
        private final TextView tvCategoryName;
        private final TextView tvCategoryDescription;
        private final TextView tvExpenseAmount;

        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCategoryIcon = itemView.findViewById(R.id.imgCategoryIcon);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvCategoryDescription = itemView.findViewById(R.id.tvCategoryDescription);
            tvExpenseAmount = itemView.findViewById(R.id.tvExpenseAmount);
        }

        void bind(Expense expense, OnExpenseClickListener listener) {
            // Set category icon
            imgCategoryIcon.setImageResource(expense.getIconResId());

            // Set category name
            tvCategoryName.setText(expense.getCategory());

            // Set description
            tvCategoryDescription.setText(expense.getDescription());

            // Format and set amount (Vietnamese Dong format)
            String formattedAmount = String.format(Locale.getDefault(), "-%,.0f đ", expense.getAmount());
            tvExpenseAmount.setText(formattedAmount);

            // Set click listener
            if (listener != null) {
                itemView.setOnClickListener(v -> listener.onExpenseClick(expense));
            }
        }
    }
}
