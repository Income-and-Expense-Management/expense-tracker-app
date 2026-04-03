package com.ptithcm.quanlichitieu.ui.wallet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ptithcm.quanlichitieu.R;

/**
 * WalletFragment: Placeholder for wallet management screen.
 * 
 * Future implementation will include:
 * - List of all user wallets
 * - Add/Edit/Delete wallet operations
 * - Wallet balance overview
 * - Transfer between wallets
 * 
 * SOLID Principles:
 * - Single Responsibility: Only handles wallet list UI
 * - Open/Closed: Can be extended with wallet operations without modification
 */
public class WalletFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wallet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        TextView tvPlaceholder = view.findViewById(R.id.tvPlaceholder);
        if (tvPlaceholder != null) {
            tvPlaceholder.setText("Wallet - Feature coming soon\n\nHere you will be able to:\n• View all wallets\n• Add new wallets\n• Edit wallet details\n• Transfer between wallets");
        }
    }
}
