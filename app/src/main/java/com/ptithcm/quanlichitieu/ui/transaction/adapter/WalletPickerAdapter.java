package com.ptithcm.quanlichitieu.ui.transaction.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.model.Wallet;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class WalletPickerAdapter extends RecyclerView.Adapter<WalletPickerAdapter.VH> {

    private final List<Wallet> wallets;
    private final String selectedId;
    private final OnWalletClick listener;

    public interface OnWalletClick {
        void onClick(Wallet wallet);
    }

    public WalletPickerAdapter(List<Wallet> wallets, String selectedId, OnWalletClick listener) {
        this.wallets = wallets;
        this.selectedId = selectedId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wallet_select, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Wallet wallet = wallets.get(position);
        holder.tvName.setText(wallet.getName());
        holder.tvBalance.setText(formatMoney(wallet.getInitialBalance()));
        
        String iconId = wallet.getIconId();
        if (iconId != null && !iconId.isEmpty()) {
            int resId = holder.itemView.getContext().getResources().getIdentifier(
                    iconId, "drawable", holder.itemView.getContext().getPackageName());
            if (resId != 0) {
                holder.ivWalletIcon.setImageResource(resId);
            } else {
                holder.ivWalletIcon.setImageResource(R.drawable.ic_wallet);
            }
        } else {
            holder.ivWalletIcon.setImageResource(R.drawable.ic_wallet);
        }
        
        boolean isSelected = wallet.getId() != null && wallet.getId().equals(selectedId);
        holder.ivSelected.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(v -> listener.onClick(wallet));
    }

    @Override
    public int getItemCount() {
        return wallets.size();
    }

    private String formatMoney(long amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        return formatter.format(amount) + " đ";
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvBalance;
        ImageView ivWalletIcon, ivSelected;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvWalletName);
            tvBalance = itemView.findViewById(R.id.tvWalletBalance);
            ivWalletIcon = itemView.findViewById(R.id.ivWalletIcon);
            ivSelected = itemView.findViewById(R.id.ivSelected);
        }
    }
}
