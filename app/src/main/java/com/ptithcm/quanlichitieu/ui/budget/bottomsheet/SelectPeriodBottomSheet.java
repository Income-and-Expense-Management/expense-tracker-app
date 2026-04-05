package com.ptithcm.quanlichitieu.ui.budget.bottomsheet;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.ptithcm.quanlichitieu.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * SelectPeriodBottomSheet - BottomSheet để chọn kỳ hạn cho budget.
 */
public class SelectPeriodBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "SelectPeriodBottomSheet";
    private static final String ARG_START_DATE = "start_date";
    private static final String ARG_END_DATE = "end_date";

    private RadioGroup rgPeriodOptions;
    private RadioButton rbThisMonth;
    private RadioButton rbThisWeek;
    private RadioButton rbThisYear;
    private RadioButton rbCustom;
    private LinearLayout layoutCustomDate;
    private LinearLayout llStartDate;
    private LinearLayout llEndDate;
    private TextView tvStartDate;
    private TextView tvEndDate;
    private Button btnConfirm;

    private long startDate;
    private long endDate;
    private OnPeriodSelectedListener listener;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public interface OnPeriodSelectedListener {
        void onPeriodSelected(long startDate, long endDate);
    }

    public static SelectPeriodBottomSheet newInstance(long startDate, long endDate) {
        SelectPeriodBottomSheet fragment = new SelectPeriodBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_START_DATE, startDate);
        args.putLong(ARG_END_DATE, endDate);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnPeriodSelectedListener(OnPeriodSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme);

        if (getArguments() != null) {
            startDate = getArguments().getLong(ARG_START_DATE, System.currentTimeMillis());
            endDate = getArguments().getLong(ARG_END_DATE, System.currentTimeMillis());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_select_period, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupListeners();
        updateDateTexts();

        // Select "This Month" by default
        rbThisMonth.setChecked(true);
    }

    private void initViews(View view) {
        rgPeriodOptions = view.findViewById(R.id.rgPeriodOptions);
        rbThisMonth = view.findViewById(R.id.rbThisMonth);
        rbThisWeek = view.findViewById(R.id.rbThisWeek);
        rbThisYear = view.findViewById(R.id.rbThisYear);
        rbCustom = view.findViewById(R.id.rbCustom);
        layoutCustomDate = view.findViewById(R.id.layoutCustomDate);
        llStartDate = view.findViewById(R.id.llStartDate);
        llEndDate = view.findViewById(R.id.llEndDate);
        tvStartDate = view.findViewById(R.id.tvStartDate);
        tvEndDate = view.findViewById(R.id.tvEndDate);
        btnConfirm = view.findViewById(R.id.btnConfirm);
    }

    private void setupListeners() {
        rgPeriodOptions.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbThisMonth) {
                setThisMonth();
                layoutCustomDate.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbThisWeek) {
                setThisWeek();
                layoutCustomDate.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbThisYear) {
                setThisYear();
                layoutCustomDate.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbCustom) {
                layoutCustomDate.setVisibility(View.VISIBLE);
            }
            updateDateTexts();
        });

        llStartDate.setOnClickListener(v -> showDatePicker(true));
        llEndDate.setOnClickListener(v -> showDatePicker(false));

        btnConfirm.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPeriodSelected(startDate, endDate);
            }
            dismiss();
        });
    }

    private void setThisMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        startDate = cal.getTimeInMillis();

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        endDate = cal.getTimeInMillis();
    }

    private void setThisWeek() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        startDate = cal.getTimeInMillis();

        cal.add(Calendar.DAY_OF_WEEK, 6);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        endDate = cal.getTimeInMillis();
    }

    private void setThisYear() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_YEAR, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        startDate = cal.getTimeInMillis();

        cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        endDate = cal.getTimeInMillis();
    }

    private void updateDateTexts() {
        tvStartDate.setText(dateFormat.format(new Date(startDate)));
        tvEndDate.setText(dateFormat.format(new Date(endDate)));
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(isStartDate ? startDate : endDate);

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                R.style.DatePickerDialogTheme,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);
                    
                    if (isStartDate) {
                        selected.set(Calendar.HOUR_OF_DAY, 0);
                        selected.set(Calendar.MINUTE, 0);
                        selected.set(Calendar.SECOND, 0);
                        startDate = selected.getTimeInMillis();
                    } else {
                        selected.set(Calendar.HOUR_OF_DAY, 23);
                        selected.set(Calendar.MINUTE, 59);
                        selected.set(Calendar.SECOND, 59);
                        endDate = selected.getTimeInMillis();
                    }
                    updateDateTexts();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }
}
