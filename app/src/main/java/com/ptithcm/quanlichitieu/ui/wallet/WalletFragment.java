package com.ptithcm.quanlichitieu.ui.wallet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.model.Wallet;
import com.ptithcm.quanlichitieu.ui.login.AuthViewModel;

public class WalletFragment extends Fragment {

    private WalletViewModel viewModel;
    private WalletAdapter adapter;
    private RecyclerView rvWallets;
    private LinearLayout layoutEmpty;
    /** Root view dùng để hiển thị Snackbar sync status */
    private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_wallet, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(WalletViewModel.class);
        AuthViewModel authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        
        viewModel.setCurrentUserId(authViewModel.getUserId());

        initViews(view);
        setupRecyclerView();
        observeViewModel();

        viewModel.loadAllWallets();
    }

    private void initViews(View view) {
        rvWallets = view.findViewById(R.id.rvWallets);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        
        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            });
        }

        MaterialButton btnAddFirstWallet = view.findViewById(R.id.btnAddFirstWallet);
        if (btnAddFirstWallet != null) {
            btnAddFirstWallet.setOnClickListener(v -> openAddWallet());
        }

        FloatingActionButton fabAddWallet = view.findViewById(R.id.fabAddWallet);
        if (fabAddWallet != null) {
            fabAddWallet.setOnClickListener(v -> openAddWallet());
        }
    }

    private void setupRecyclerView() {
        adapter = new WalletAdapter();
        adapter.setOnWalletClickListener(this::showSelectionDialog);
        
        adapter.setOnWalletMenuListener(new WalletAdapter.OnWalletMenuListener() {
            @Override
            public void onEdit(Wallet wallet) {
                openEditWallet(wallet);
            }

            @Override
            public void onDelete(Wallet wallet) {
                showDeleteConfirmDialog(wallet);
            }
        });

        rvWallets.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvWallets.setAdapter(adapter);
    }

    private void showSelectionDialog(Wallet wallet) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xác nhận chọn ví")
                .setMessage("Bạn có muốn chọn ví \"" + wallet.getName() + "\" để quản lý chi tiêu không?")
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    viewModel.selectWallet(wallet);
                    navigateToHome();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDeleteConfirmDialog(Wallet wallet) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa ví")
                .setMessage("Bạn có chắc chắn muốn xóa ví \"" + wallet.getName() + "\" này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    viewModel.deleteWallet(wallet);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void navigateToHome() {
        BottomNavigationView nav = requireActivity().findViewById(R.id.bottomNav);
        if (nav != null) {
            nav.setSelectedItemId(R.id.nav_home);
        }
    }

    private void observeViewModel() {
        // Danh sách ví
        viewModel.getWallets().observe(getViewLifecycleOwner(), wallets -> {
            if (wallets == null || wallets.isEmpty()) {
                rvWallets.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
            } else {
                rvWallets.setVisibility(View.VISIBLE);
                layoutEmpty.setVisibility(View.GONE);
                adapter.setWallets(wallets);
            }
        });

        // Kết quả lưu/xóa (Toast đã hiện trong Fragment con, ở đây chỉ cần xử lý
        // trường hợp xóa từ WalletFragment — adapter menu onDelete)
        viewModel.getSaveResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && !result.isSuccess()) {
                showSnackbar(result.getMessage(), false);
                viewModel.clearSaveResult();
            }
        });

        // Trạng thái sync với server
        viewModel.getSyncStatus().observe(getViewLifecycleOwner(), status -> {
            if (status == null) return;
            if (status == WalletViewModel.SyncStatus.SYNC_FAILED) {
                showSnackbar("Không thể đồng bộ với server. Dữ liệu đã lưu nội bộ.", false);
            }
            // Reset để không hiện lại khi rotate / navigate
            viewModel.clearSyncStatus();
        });
    }

    /**
     * Hiển thị Snackbar nhẹ — không block UI.
     * @param message  Nội dung thông báo
     * @param isSuccess true = thành công (màu xanh), false = lỗi (màu đỏ nhạt)
     */
    private void showSnackbar(String message, boolean isSuccess) {
        if (rootView == null || !isAdded()) return;
        Snackbar snackbar = Snackbar.make(rootView, message,
                isSuccess ? Snackbar.LENGTH_SHORT : Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    private void openAddWallet() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new AddWalletFragment())
                .addToBackStack(null)
                .commit();
    }

    private void openEditWallet(Wallet wallet) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, EditWalletFragment.newInstance(wallet.getId()))
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Pull dữ liệu mới nhất từ server trước, sau đó reload list local.
        // Giải quyết: ví tạo từ web không hiện trên app vì app chỉ đọc SQLite local.
        // refreshFromServer() = GET /api/v1/wallets/ → UPSERT local → postValue(list)
        // Nếu offline → không block, list local vẫn hiển thị bình thường.
        viewModel.refreshFromServer();
    }
}
