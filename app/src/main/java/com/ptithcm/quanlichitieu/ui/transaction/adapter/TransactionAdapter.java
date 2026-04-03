package com.ptithcm.quanlichitieu.ui.transaction.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.model.Transaction;
import com.ptithcm.quanlichitieu.data.model.TransactionGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private final List<Object> items = new ArrayList<>();

    public void setGroups(List<TransactionGroup> groups) {
        items.clear();
        for (TransactionGroup group : groups) {
            items.add(group);
            items.addAll(group.getTransactions());
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof TransactionGroup ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_transaction_date_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_transaction, parent, false);
            return new TransactionViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((TransactionGroup) items.get(position));
        } else if (holder instanceof TransactionViewHolder) {
            ((TransactionViewHolder) holder).bind((Transaction) items.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDayOfWeek;
        private final TextView tvDate;
        private final TextView tvDayTotal;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayOfWeek = itemView.findViewById(R.id.tvDayOfWeek);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDayTotal = itemView.findViewById(R.id.tvDayTotal);
        }

        void bind(TransactionGroup group) {
            tvDayOfWeek.setText(group.getDayOfWeek());
            tvDate.setText(group.getDate());
            String total = String.format(Locale.getDefault(), "%,.0f đ", group.getDayTotal());
            tvDayTotal.setText(total);
        }
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgIcon;
        private final TextView tvCategory;
        private final TextView tvAmount;
        private final TextView tvWalletSource;
        private final ImageView imgTrend;

        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.imgIcon);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvWalletSource = itemView.findViewById(R.id.tvWalletSource);
            imgTrend = itemView.findViewById(R.id.imgTrend);
        }

        void bind(Transaction transaction) {
            Context context = itemView.getContext();
            
            // Lấy icon resource từ iconId (tên drawable)
            String iconId = transaction.getIconId();
            if (iconId != null && !iconId.isEmpty()) {
                int resId = context.getResources().getIdentifier(
                        iconId, "drawable", context.getPackageName());
                if (resId != 0) {
                    imgIcon.setImageResource(resId);
                } else {
                    imgIcon.setImageResource(R.drawable.ic_wallet);
                }
            } else {
                imgIcon.setImageResource(R.drawable.ic_wallet);
            }
            
            // Sử dụng categoryName thay vì getCategory()
            tvCategory.setText(transaction.getCategoryName());
            tvWalletSource.setText(transaction.getWalletName());

            String amountStr;
            if (transaction.isExpense()) {
                amountStr = String.format(Locale.getDefault(), "- %,d", transaction.getAmount());
                tvAmount.setTextColor(itemView.getContext().getResources().getColor(R.color.home_expense_red, null));
                imgTrend.setImageResource(R.drawable.ic_trend_down);
            } else {
                amountStr = String.format(Locale.getDefault(), "+ %,d", transaction.getAmount());
                tvAmount.setTextColor(itemView.getContext().getResources().getColor(R.color.home_accent_green, null));
                imgTrend.setImageResource(R.drawable.ic_trend_up);
            }
            tvAmount.setText(amountStr);
        }
    }
}
