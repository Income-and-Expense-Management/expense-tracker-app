package com.ptithcm.quanlichitieu.ui.wallet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.ptithcm.quanlichitieu.R;

/**
 * AddWalletFragment: Form for creating a new wallet.
 * Đã được cập nhật để ẩn thanh điều hướng của Activity khi hiển thị.
 */
public class AddWalletFragment extends Fragment {

    private WalletViewModel viewModel;
    private EditText etName;
    private EditText etBalance;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_wallet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Ẩn thanh điều hướng và FAB để màn hình tạo ví chiếm toàn diện tích
        toggleBottomNavigation(false);

        viewModel = new ViewModelProvider(this).get(WalletViewModel.class);

        etName = view.findViewById(R.id.etName);
        etBalance = view.findViewById(R.id.etBalance);
        Button btnSave = view.findViewById(R.id.btnSave);
        ImageView btnBack = view.findViewById(R.id.btnBack);

        // Xử lý sự kiện nhấn nút quay lại (<)
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager().popBackStack();
            });
        }

        btnSave.setOnClickListener(v -> saveWallet());

        observeViewModel();
    }

    @Override
    public void onDestroyView() {
        // 2. Hiện lại thanh điều hướng khi thoát khỏi màn hình này
        toggleBottomNavigation(true);
        super.onDestroyView();
    }

    /**
     * Điều chỉnh hiển thị của thanh điều hướng và lề dưới của container.
     * @param isVisible true để hiện, false để ẩn
     */
    private void toggleBottomNavigation(boolean isVisible) {
        if (getActivity() == null) return;

        // Tìm các View trong layout của MainActivity
        View bottomAppBar = getActivity().findViewById(R.id.bottomAppBar);
        View fab = getActivity().findViewById(R.id.fabAdd);
        View container = getActivity().findViewById(R.id.fragmentContainer);

        // Ẩn/Hiện thanh điều hướng và nút FAB
        int visibility = isVisible ? View.VISIBLE : View.GONE;
        if (bottomAppBar != null) bottomAppBar.setVisibility(visibility);
        if (fab != null) fab.setVisibility(visibility);

        // Loại bỏ lề dưới (80dp) của container để Fragment tràn xuống đáy màn hình
        if (container != null && container.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) container.getLayoutParams();
            // 80dp là khoảng cách mặc định của thanh điều hướng trong activity_main.xml
            int marginInPx = isVisible ? (int) (80 * getResources().getDisplayMetrics().density) : 0;
            params.bottomMargin = marginInPx;
            container.setLayoutParams(params);
        }
    }

    private void observeViewModel() {
        viewModel.getSaveResult().observe(getViewLifecycleOwner(), result -> {
            Toast.makeText(requireContext(), result.getMessage(), Toast.LENGTH_SHORT).show();

            if (result.isSuccess()) {
                // Quay lại màn hình trước đó sau khi lưu thành công
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void saveWallet() {
        String name = etName.getText().toString().trim();
        String balanceStr = etBalance.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập tên ví", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.saveWallet(name, balanceStr);
    }
}
