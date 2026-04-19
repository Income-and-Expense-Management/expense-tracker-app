package com.ptithcm.quanlichitieu.ui.transaction.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.model.Transaction;
import com.ptithcm.quanlichitieu.data.model.TransactionGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * TransactionAdapter - Adapter cho RecyclerView hiển thị giao dịch theo nhóm ngày.
 * 
 * Cấu trúc hiển thị:
 * - Header: Thứ, ngày và tổng tiền trong ngày
 * - Items: Danh sách giao dịch trong ngày đó
 * - Section liền mạch với header bo góc trên, item cuối bo góc dưới
 */
public class TransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private final List<Object> items = new ArrayList<>();
    private final List<Integer> lastItemPositions = new ArrayList<>(); // Lưu vị trí item cuối cùng của mỗi group
    private final List<Integer> firstItemPositions = new ArrayList<>(); // Lưu vị trí item đầu tiên của mỗi group

    public void setGroups(List<TransactionGroup> groups) {
        items.clear();
        lastItemPositions.clear();
        firstItemPositions.clear();
        
        for (TransactionGroup group : groups) {
            items.add(group);
            List<Transaction> transactions = group.getTransactions();
            
            if (!transactions.isEmpty()) {
                // Lưu vị trí của item đầu tiên (ngay sau header)
                firstItemPositions.add(items.size());
                
                // Thêm tất cả transactions
                items.addAll(transactions);
                
                // Lưu vị trí của item cuối cùng trong group này
                lastItemPositions.add(items.size() - 1);
            }
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
            boolean isLastItem = lastItemPositions.contains(position);
            boolean isFirstItem = firstItemPositions.contains(position);
            ((TransactionViewHolder) holder).bind((Transaction) items.get(position), isLastItem, isFirstItem);
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
            String total = String.format(Locale.getDefault(), "%,d đ", group.getDayTotal());
            tvDayTotal.setText(total);
        }
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final View divider;
        private final View itemContainer;
        private final ImageView imgIcon;
        private final TextView tvCategory;
        private final TextView tvNote;
        private final TextView tvAmount;
        private final TextView tvTransactionType;
        private final ImageView imgTrend;

        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            divider = itemView.findViewById(R.id.divider);
            itemContainer = itemView.findViewById(R.id.itemContainer);
            imgIcon = itemView.findViewById(R.id.imgIcon);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvNote = itemView.findViewById(R.id.tvNote);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvTransactionType = itemView.findViewById(R.id.tvTransactionType);
            imgTrend = itemView.findViewById(R.id.imgTrend);
        }

        void bind(Transaction transaction, boolean isLastItem, boolean isFirstItem) {
            Context context = itemView.getContext();
            
            // Ẩn divider cho item đầu tiên ngay sau header
//            if (isFirstItem) {
//                divider.setVisibility(View.GONE);
//            } else {
//                divider.setVisibility(View.VISIBLE);
//            }
            
            // Set background dựa trên vị trí (item cuối cùng có bo góc dưới)
            Drawable background;
            if (isLastItem) {
                background = ContextCompat.getDrawable(context, R.drawable.bg_transaction_item_last);
            } else {
                background = ContextCompat.getDrawable(context, R.drawable.bg_transaction_item);
            }
            itemContainer.setBackground(background);
            
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
            
            // Hiển thị category name
            tvCategory.setText(transaction.getCategoryName());
            
            // Hiển thị note (nếu có) với truncate
            String note = transaction.getNote();
            if (!TextUtils.isEmpty(note)) {
                tvNote.setVisibility(View.VISIBLE);
                tvNote.setText(note);
            } else {
                tvNote.setVisibility(View.GONE);
            }
            
            // Hiển thị loại giao dịch thay vì wallet name
            if (transaction.isExpense()) {
                tvTransactionType.setText("Khoản chi");
            } else if (transaction.isIncome()) {
                tvTransactionType.setText("Khoản thu");
            } else {
                tvTransactionType.setText("Khoản vay");
            }

            // Format số tiền và màu sắc
            String amountStr;
            if (transaction.isExpense()) {
                amountStr = String.format(Locale.getDefault(), "- %,d đ", transaction.getAmount());
                tvAmount.setTextColor(context.getResources().getColor(R.color.home_expense_red, null));
                imgTrend.setImageResource(R.drawable.ic_trend_down);
            } else {
                amountStr = String.format(Locale.getDefault(), "+ %,d đ", transaction.getAmount());
                tvAmount.setTextColor(context.getResources().getColor(R.color.home_accent_green, null));
                imgTrend.setImageResource(R.drawable.ic_trend_up);
            }
            tvAmount.setText(amountStr);
        }
    }
}
