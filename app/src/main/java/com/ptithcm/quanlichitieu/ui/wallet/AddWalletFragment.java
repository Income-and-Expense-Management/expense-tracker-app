package com.ptithcm.quanlichitieu.ui.wallet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.ptithcm.quanlichitieu.R;

/**
 * AddWalletFragment: Form for creating a new wallet.
 * 
 * Can be used as:
 * - Regular Fragment (full screen)
 * - BottomSheetDialogFragment (modal dialog)
 * 
 * SOLID Principles Applied:
 * - Single Responsibility: Only handles wallet creation UI
 * - Dependency Inversion: Depends on WalletViewModel abstraction
 * - Separation of Concerns: UI logic separated from business logic
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

        viewModel = new ViewModelProvider(this).get(WalletViewModel.class);

        etName = view.findViewById(R.id.etName);
        etBalance = view.findViewById(R.id.etBalance);
        Button btnSave = view.findViewById(R.id.btnSave);

        btnSave.setOnClickListener(v -> saveWallet());
        
        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getSaveResult().observe(getViewLifecycleOwner(), result -> {
            Toast.makeText(requireContext(), result.getMessage(), Toast.LENGTH_SHORT).show();
            
            if (result.isSuccess()) {
                // Close the fragment after successful save
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void saveWallet() {
        String name = etName.getText().toString();
        String balanceStr = etBalance.getText().toString();
        
        // Delegate validation and saving to ViewModel
        viewModel.saveWallet(name, balanceStr);
    }
}
