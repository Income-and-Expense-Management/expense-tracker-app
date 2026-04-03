package com.ptithcm.quanlichitieu.ui.account;

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
 * AccountFragment: Placeholder for user account and settings screen.
 * 
 * Future implementation will include:
 * - User profile information
 * - App settings and preferences
 * - Notification settings
 * - Security settings
 * - About app information
 * - Logout functionality
 * 
 * SOLID Principles:
 * - Single Responsibility: Only handles account/settings UI
 * - Open/Closed: Can be extended with new settings without modification
 */
public class AccountFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        TextView tvPlaceholder = view.findViewById(R.id.tvPlaceholder);
        if (tvPlaceholder != null) {
            tvPlaceholder.setText("Account - Feature coming soon\n\nHere you will be able to:\n• View and edit profile\n• Manage app settings\n• Configure notifications\n• Change password\n• View app information\n• Logout");
        }
    }
}
