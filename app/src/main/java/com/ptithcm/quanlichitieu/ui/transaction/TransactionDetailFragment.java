package com.ptithcm.quanlichitieu.ui.transaction;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.local.BudgetDatabaseHelper;
import com.ptithcm.quanlichitieu.data.local.dao.TransactionDao;
import com.ptithcm.quanlichitieu.data.model.Transaction;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TransactionDetailFragment extends Fragment {

    private static final String ARG_TRANSACTION_ID = "transaction_id";

    private String transactionId;
    private TransactionDao transactionDao;

    private ImageView btnClose, btnEdit, btnDelete, imgCategoryIcon;
    private TextView tvCategoryName, tvAmount, tvDate, tvWalletName, tvNote;

    public static TransactionDetailFragment newInstance(String transactionId) {
        TransactionDetailFragment fragment = new TransactionDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TRANSACTION_ID, transactionId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            transactionId = getArguments().getString(ARG_TRANSACTION_ID);
        }
        BudgetDatabaseHelper dbHelper = BudgetDatabaseHelper.getInstance(requireContext());
        transactionDao = new TransactionDao(dbHelper);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupListeners();
        loadTransactionDetails();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private void initViews(View view) {
        btnClose = view.findViewById(R.id.btnClose);
        btnEdit = view.findViewById(R.id.btnEdit);
        btnDelete = view.findViewById(R.id.btnDelete);
        imgCategoryIcon = view.findViewById(R.id.imgCategoryIcon);
        tvCategoryName = view.findViewById(R.id.tvCategoryName);
        tvAmount = view.findViewById(R.id.tvAmount);
        tvDate = view.findViewById(R.id.tvDate);
        tvWalletName = view.findViewById(R.id.tvWalletName);
        tvNote = view.findViewById(R.id.tvNote);
    }

    private void setupListeners() {
        btnClose.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        btnEdit.setOnClickListener(v -> {
            if (transactionId != null && getActivity() instanceof com.ptithcm.quanlichitieu.ui.main.MainActivity) {
                ((com.ptithcm.quanlichitieu.ui.main.MainActivity) getActivity()).openEditTransaction(transactionId);
            }
        });

        btnDelete.setOnClickListener(v -> {
            if (transactionId != null) {
                int rows = transactionDao.delete(transactionId);
                if (rows > 0) {
                    Toast.makeText(requireContext(), "Đã xoá giao dịch", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                } else {
                    Toast.makeText(requireContext(), "Lỗi khi xoá", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadTransactionDetails() {
        if (transactionId == null) return;
        Transaction transaction = transactionDao.getById(transactionId);
        if (transaction == null) {
            Toast.makeText(requireContext(), "Không tìm thấy giao dịch", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        tvCategoryName.setText(transaction.getCategoryName() != null ? transaction.getCategoryName() : "Không xác định");

        String amountStr;
        if (transaction.isExpense()) {
            amountStr = String.format(Locale.getDefault(), "- %,d đ", transaction.getAmount());
            tvAmount.setTextColor(getResources().getColor(R.color.home_expense_red, null));
        } else if (transaction.isIncome()) {
            amountStr = String.format(Locale.getDefault(), "+ %,d đ", transaction.getAmount());
            tvAmount.setTextColor(getResources().getColor(R.color.home_accent_green, null));
        } else {
            amountStr = String.format(Locale.getDefault(), "%,d đ", transaction.getAmount());
            tvAmount.setTextColor(getResources().getColor(R.color.white, null));
        }
        tvAmount.setText(amountStr);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(new Date(transaction.getTransactionDate())));

        tvWalletName.setText(transaction.getWalletName() != null ? transaction.getWalletName() : "Ví không xác định");

        tvNote.setVisibility(View.VISIBLE);
        if (!TextUtils.isEmpty(transaction.getNote())) {
            tvNote.setText(transaction.getNote());
        } else {
            tvNote.setText("Không có mô tả");
        }

        String iconId = transaction.getIconId();
        if (iconId != null && !iconId.isEmpty()) {
            int resId = getResources().getIdentifier(iconId, "drawable", requireContext().getPackageName());
            if (resId != 0) {
                imgCategoryIcon.setImageResource(resId);
            } else {
                imgCategoryIcon.setImageResource(R.drawable.ic_wallet);
            }
        } else {
            imgCategoryIcon.setImageResource(R.drawable.ic_wallet);
        }
    }
}
