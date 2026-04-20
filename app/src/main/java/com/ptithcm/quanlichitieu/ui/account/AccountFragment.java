package com.ptithcm.quanlichitieu.ui.account;

import android.content.Intent;
import android.os.Bundle;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.ui.login.AuthViewModel;
import com.ptithcm.quanlichitieu.ui.login.LoginActivity;
import com.ptithcm.quanlichitieu.ui.main.MainActivity;
import com.ptithcm.quanlichitieu.ui.category.CategoryFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * AccountFragment: User account screen with profile info, menu items, and logout.
 */
public class AccountFragment extends Fragment {

    private static final String TAG = "AccountFragment";
    private AuthViewModel authViewModel;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvSyncStatus;

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

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        tvSyncStatus = view.findViewById(R.id.tvSyncStatus);

        if (swipeRefreshLayout != null) {
            setupSwipeRefresh();
        }

        setupUserInfo(view);
        setupMenuItems(view);
        setupLogoutButton(view);
        observeLogoutState();
        observeSessionExpired();
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(R.color.home_expense_red, R.color.home_accent_green);
        updateSyncTime();
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Re-load data when swiped
            if (authViewModel != null) {
                // In a real app we would refresh user info from server here
            }

            // Simulation of network delay
            swipeRefreshLayout.postDelayed(() -> {
                swipeRefreshLayout.setRefreshing(false);
                updateSyncTime();

                // Re-setup user info
                if (getView() != null) {
                    setupUserInfo(getView());
                }
            }, 800);
        });
    }

    private void updateSyncTime() {
        if (tvSyncStatus != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm. dd/MM", Locale.getDefault());
            String time = sdf.format(new Date());
            tvSyncStatus.setText("Đồng bộ lần cuối: vừa xong (" + time + ")");
        }
    }

    private void setupUserInfo(View view) {
        TextView tvFullName = view.findViewById(R.id.tvFullName);
        TextView tvEmail = view.findViewById(R.id.tvEmail);

        String fullName = authViewModel.getUserFullName();
        String email = authViewModel.getUserEmail();

        tvFullName.setText(fullName != null ? fullName : "");
        tvEmail.setText(email != null ? email : "");
    }

    private void setupMenuItems(View view) {
        // Chức năng "Ví của tôi" trong Account có hành vi giống với "Xem tất cả" ở Home
        view.findViewById(R.id.menuMyWallet).setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openWalletList();
            }
        });

        view.findViewById(R.id.menuGroup).setOnClickListener(v ->
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new CategoryFragment())
                    .addToBackStack(null)
                    .commit()
        );
    }

    private void setupLogoutButton(View view) {
        Button btnLogout = view.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> authViewModel.logout());
    }

    private void observeLogoutState() {
        authViewModel.getLogoutState().observe(getViewLifecycleOwner(), authState -> {
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
            if (Boolean.TRUE.equals(expired)) {
                Toast.makeText(requireContext(), "Session expired. Please log in again.",
                        Toast.LENGTH_LONG).show();
                navigateToLogin();
            }
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
