package com.ptithcm.quanlichitieu.ui.budget.bottomsheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.local.token.EncryptedTokenStorage;
import com.ptithcm.quanlichitieu.data.local.token.TokenStorage;
import com.ptithcm.quanlichitieu.data.model.Wallet;
import com.ptithcm.quanlichitieu.data.repository.BudgetRepository;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SelectWalletBottomSheet - BottomSheet để chọn ví cho budget.
 */
public class SelectWalletBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "SelectWalletBottomSheet";

    private RecyclerView rvWallets;
    private LinearLayout layoutEmpty;

    private WalletSelectAdapter adapter;
    private OnWalletSelectedListener listener;
    private String selectedWalletId;

    public interface OnWalletSelectedListener {
        void onWalletSelected(Wallet wallet);
    }

    public static SelectWalletBottomSheet newInstance() {
        return new SelectWalletBottomSheet();
    }

    public void setOnWalletSelectedListener(OnWalletSelectedListener listener) {
        this.listener = listener;
    }

    public void setSelectedWalletId(String walletId) {
        this.selectedWalletId = walletId;
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
        return inflater.inflate(R.layout.bottom_sheet_select_wallet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        loadWallets();
    }

    private void initViews(View view) {
        rvWallets = view.findViewById(R.id.rvWallets);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
    }

    private void setupRecyclerView() {
        adapter = new WalletSelectAdapter(selectedWalletId, wallet -> {
            if (listener != null) {
                listener.onWalletSelected(wallet);
            }
            dismiss();
        });
        rvWallets.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvWallets.setAdapter(adapter);
    }

    private void loadWallets() {
        BudgetRepository repository = BudgetRepository.getInstance(requireContext());
        TokenStorage tokenStorage = EncryptedTokenStorage.getInstance(requireContext());
        String userId = tokenStorage.getUserId();
        List<Wallet> wallets = repository.getWalletsForUser(userId);

        if (wallets.isEmpty()) {
            rvWallets.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            rvWallets.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            adapter.setWallets(wallets);
        }
    }

    /**
     * Adapter for wallet selection list.
     */
    private static class WalletSelectAdapter extends RecyclerView.Adapter<WalletSelectAdapter.ViewHolder> {

        private List<Wallet> wallets = new ArrayList<>();
        private final OnWalletClickListener clickListener;
        private final String selectedWalletId;

        interface OnWalletClickListener {
            void onClick(Wallet wallet);
        }

        WalletSelectAdapter(String selectedWalletId, OnWalletClickListener listener) {
            this.selectedWalletId = selectedWalletId;
            this.clickListener = listener;
        }

        void setWallets(List<Wallet> wallets) {
            this.wallets = wallets;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_wallet_select, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Wallet wallet = wallets.get(position);
            boolean isSelected = wallet.getId() != null && wallet.getId().equals(selectedWalletId);
            holder.bind(wallet, isSelected, clickListener);
        }

        @Override
        public int getItemCount() {
            return wallets.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private final ImageView ivWalletIcon;
            private final TextView tvWalletName;
            private final TextView tvWalletBalance;
            private final ImageView ivSelected;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivWalletIcon = itemView.findViewById(R.id.ivWalletIcon);
                tvWalletName = itemView.findViewById(R.id.tvWalletName);
                tvWalletBalance = itemView.findViewById(R.id.tvWalletBalance);
                ivSelected = itemView.findViewById(R.id.ivSelected);
            }

            void bind(Wallet wallet, boolean isSelected, OnWalletClickListener listener) {
                tvWalletName.setText(wallet.getName());
                tvWalletBalance.setText(formatMoney(wallet.getInitialBalance()));
                ivSelected.setVisibility(isSelected ? View.VISIBLE : View.GONE);

                // Load icon thực tế từ wallet.iconId
                String iconId = wallet.getIconId();
                if (iconId != null && !iconId.isEmpty()) {
                    int resId = itemView.getContext().getResources().getIdentifier(
                            iconId, "drawable", itemView.getContext().getPackageName());
                    if (resId != 0) {
                        ivWalletIcon.setImageResource(resId);
                    } else {
                        // Fallback icon
                        ivWalletIcon.setImageResource(R.drawable.ic_wallet);
                    }
                } else {
                    // Default fallback icon
                    ivWalletIcon.setImageResource(R.drawable.ic_wallet);
                }

                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onClick(wallet);
                    }
                });
            }

            private String formatMoney(long amount) {
                DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
                symbols.setGroupingSeparator('.');
                DecimalFormat formatter = new DecimalFormat("#,###", symbols);
                return formatter.format(amount) + " đ";
            }
        }
    }
}
