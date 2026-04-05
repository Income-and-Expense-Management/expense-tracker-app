package com.ptithcm.quanlichitieu.ui.wallet;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.model.Wallet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WalletAdapter extends RecyclerView.Adapter<WalletAdapter.WalletViewHolder> {

    private List<Wallet> wallets = new ArrayList<>();
    private OnWalletClickListener clickListener;
    private OnWalletMenuListener menuListener;

    public interface OnWalletClickListener {
        void onWalletClick(Wallet wallet);
    }

    public interface OnWalletMenuListener {
        void onEdit(Wallet wallet);
        void onDelete(Wallet wallet);
    }

    public void setOnWalletClickListener(OnWalletClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnWalletMenuListener(OnWalletMenuListener listener) {
        this.menuListener = listener;
    }

    public void setWallets(List<Wallet> wallets) {
        this.wallets = wallets;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WalletViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallet, parent, false);
        return new WalletViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WalletViewHolder holder, int position) {
        holder.bind(wallets.get(position), clickListener, menuListener);
    }

    @Override
    public int getItemCount() {
        return wallets.size();
    }

    public static class WalletViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvBalance;
        private final ImageView ivMore;
        private final ImageView ivIcon;

        public WalletViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvWalletName);
            tvBalance = itemView.findViewById(R.id.tvWalletBalance);
            ivMore = itemView.findViewById(R.id.ivMore);
            ivIcon = itemView.findViewById(R.id.ivWalletIcon);
        }

        public void bind(Wallet wallet, OnWalletClickListener clickListener, OnWalletMenuListener menuListener) {
            tvName.setText(wallet.getName());
            tvBalance.setText(String.format(Locale.getDefault(), "%,d đ", wallet.getInitialBalance()));
            
            if (wallet.getIconId() != null && !wallet.getIconId().isEmpty()) {
                int resId = itemView.getContext().getResources().getIdentifier(
                        wallet.getIconId(), "drawable", itemView.getContext().getPackageName());
                if (resId != 0) {
                    ivIcon.setImageResource(resId);
                } else {
                    ivIcon.setImageResource(R.drawable.ic_wallet);
                }
            } else {
                ivIcon.setImageResource(R.drawable.ic_wallet);
            }

            // Click vào thẻ để chọn ví
            itemView.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onWalletClick(wallet);
            });

            // Click vào dấu 3 chấm để hiện Menu
            ivMore.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                popup.getMenu().add(0, 1, 0, "Sửa");
                popup.getMenu().add(0, 2, 1, "Xóa");
                
                // Sửa từ setOnMenuItemListener thành setOnMenuItemClickListener
                popup.setOnMenuItemClickListener(item -> {
                    if (menuListener == null) return false;
                    
                    int id = item.getItemId();
                    if (id == 1) {
                        menuListener.onEdit(wallet);
                        return true;
                    } else if (id == 2) {
                        menuListener.onDelete(wallet);
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }
    }
}
