package com.ptithcm.quanlichitieu.ui.wallet;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.model.Wallet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WalletAdapter extends RecyclerView.Adapter<WalletAdapter.WalletViewHolder> {

    private List<Wallet> wallets = new ArrayList<>();
    private OnWalletClickListener listener;

    public interface OnWalletClickListener {
        void onWalletClick(Wallet wallet);
    }

    public void setOnWalletClickListener(OnWalletClickListener listener) {
        this.listener = listener;
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
        holder.bind(wallets.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return wallets.size();
    }

    static class WalletViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvBalance;

        public WalletViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvWalletName);
            tvBalance = itemView.findViewById(R.id.tvWalletBalance);
        }

        public void bind(Wallet wallet, OnWalletClickListener listener) {
            tvName.setText(wallet.getName());
            tvBalance.setText(String.format(Locale.getDefault(), "%,d đ", wallet.getInitialBalance()));
            
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onWalletClick(wallet);
            });
        }
    }
}
