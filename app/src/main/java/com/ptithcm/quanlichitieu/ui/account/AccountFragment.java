package com.ptithcm.quanlichitieu.ui.account;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.ui.login.AuthViewModel;
import com.ptithcm.quanlichitieu.ui.login.LoginActivity;

/**
 * AccountFragment: User account screen with profile info, menu items, and logout.
 *
 * The AuthViewModel is scoped to the host Activity (MainActivity) so that
 * session-expired events are shared across all fragments.
 *
 * SOLID Principles:
 * - Single Responsibility: Only handles account/settings UI
 * - Open/Closed: Can be extended with new settings without modification
 */
public class AccountFragment extends Fragment {

    private static final String TAG = "AccountFragment";
    private AuthViewModel authViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        setupUserInfo(view);
        setupMenuItems(view);
        setupLogoutButton(view);
        observeLogoutState();
        observeSessionExpired();
    }

    private void setupUserInfo(View view) {
        TextView tvFullName = view.findViewById(R.id.tvFullName);
        TextView tvEmail = view.findViewById(R.id.tvEmail);

        String fullName = authViewModel.getUserFullName();
        String email = authViewModel.getUserEmail();
        Log.d(TAG, "setupUserInfo: fullName=" + fullName + ", email=" + email);

        tvFullName.setText(fullName != null ? fullName : "");
        tvEmail.setText(email != null ? email : "");
    }

    private void setupMenuItems(View view) {
        view.findViewById(R.id.menuMyWallet).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Vi cua toi - Coming soon", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.menuGroup).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Nhom - Coming soon", Toast.LENGTH_SHORT).show());
    }

    private void setupLogoutButton(View view) {
        Button btnLogout = view.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            Log.d(TAG, "Logout button tapped");
            authViewModel.logout();
        });
    }

    private void observeLogoutState() {
        authViewModel.getLogoutState().observe(getViewLifecycleOwner(), authState -> {
            Log.d(TAG, "observeLogoutState: status=" + authState.getStatus());
            switch (authState.getStatus()) {
                case LOADING:
                    Toast.makeText(requireContext(), "Logging out...", Toast.LENGTH_SHORT).show();
                    break;
                case SUCCESS:
                    navigateToLogin();
                    break;
                case ERROR:
                    Toast.makeText(requireContext(), authState.getData(), Toast.LENGTH_SHORT).show();
                    break;
                case IDLE:
                    break;
            }
        });
    }

    private void observeSessionExpired() {
        authViewModel.getSessionExpired().observe(getViewLifecycleOwner(), expired -> {
            Log.d(TAG, "observeSessionExpired: expired=" + expired);
            if (Boolean.TRUE.equals(expired)) {
                Toast.makeText(requireContext(), "Session expired. Please log in again.",
                        Toast.LENGTH_LONG).show();
                navigateToLogin();
            }
        });
    }

    private void navigateToLogin() {
        Log.d(TAG, "navigateToLogin: Clearing back stack and redirecting");
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
