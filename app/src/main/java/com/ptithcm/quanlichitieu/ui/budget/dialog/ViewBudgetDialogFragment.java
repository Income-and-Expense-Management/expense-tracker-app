package com.ptithcm.quanlichitieu.ui.budget.dialog;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.ui.budget.model.BudgetItem;
import com.ptithcm.quanlichitieu.ui.budget.view.BudgetChartView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ViewBudgetDialogFragment - Hiển thị chi tiết ngân sách.
 * Bao gồm: thông tin category, số tiền, biểu đồ và thống kê chi tiêu hàng ngày.
 */
public class ViewBudgetDialogFragment extends DialogFragment {

    public static final String TAG = "ViewBudgetDialog";

    private static final String ARG_ID = "id";
    private static final String ARG_CATEGORY_ID = "category_id";
    private static final String ARG_CATEGORY_NAME = "category_name";
    private static final String ARG_CATEGORY_ICON = "category_icon";
    private static final String ARG_LIMIT = "limit";
    private static final String ARG_SPENT = "spent";
    private static final String ARG_START_DATE = "start_date";
    private static final String ARG_END_DATE = "end_date";
    private static final String ARG_WALLET_ID = "wallet_id";
    private static final String ARG_COLOR = "color";

    private BudgetItem budgetItem;
    private OnBudgetActionListener actionListener;

    public interface OnBudgetActionListener {
        void onEditClicked(BudgetItem item);
        void onDeleteClicked(BudgetItem item);
    }

    public static ViewBudgetDialogFragment newInstance(BudgetItem item) {
        ViewBudgetDialogFragment fragment = new ViewBudgetDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, item.getId());
        args.putString(ARG_CATEGORY_ID, item.getCategoryId());
        args.putString(ARG_CATEGORY_NAME, item.getCategoryName());
        args.putString(ARG_CATEGORY_ICON, item.getCategoryIcon());
        args.putLong(ARG_LIMIT, item.getLimit());
        args.putLong(ARG_SPENT, item.getSpent());
        args.putLong(ARG_START_DATE, item.getStartDate());
        args.putLong(ARG_END_DATE, item.getEndDate());
        args.putString(ARG_WALLET_ID, item.getWalletId());
        args.putString(ARG_COLOR, item.getColor());
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnBudgetActionListener(OnBudgetActionListener listener) {
        this.actionListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.FullScreenDialogTheme);

        if (getArguments() != null) {
            budgetItem = new BudgetItem.Builder()
                    .setId(getArguments().getString(ARG_ID))
                    .setCategoryId(getArguments().getString(ARG_CATEGORY_ID))
                    .setCategoryName(getArguments().getString(ARG_CATEGORY_NAME))
                    .setCategoryIcon(getArguments().getString(ARG_CATEGORY_ICON))
                    .setLimit(getArguments().getLong(ARG_LIMIT))
                    .setSpent(getArguments().getLong(ARG_SPENT))
                    .setStartDate(getArguments().getLong(ARG_START_DATE))
                    .setEndDate(getArguments().getLong(ARG_END_DATE))
                    .setWalletId(getArguments().getString(ARG_WALLET_ID))
                    .setColor(getArguments().getString(ARG_COLOR))
                    .build();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_view_budget, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (budgetItem != null) {
            bindViews(view);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
    }

    private void bindViews(View view) {
        // --- Header buttons ---
        view.findViewById(R.id.ivClose).setOnClickListener(v -> dismiss());

        view.findViewById(R.id.ivEdit).setOnClickListener(v -> {
            dismiss();
            if (actionListener != null) actionListener.onEditClicked(budgetItem);
        });

        view.findViewById(R.id.ivDelete).setOnClickListener(v -> {
            dismiss();
            if (actionListener != null) actionListener.onDeleteClicked(budgetItem);
        });

        // --- Category icon ---
        View viewCategoryIcon = view.findViewById(R.id.viewCategoryIcon);
        try {
            String colorStr = budgetItem.getColor() != null ? budgetItem.getColor() : "#4CAF50";
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(Color.parseColor(colorStr));
            viewCategoryIcon.setBackground(drawable);
        } catch (Exception ignored) {}

        // --- Category name ---
        ((TextView) view.findViewById(R.id.tvCategoryName))
                .setText(budgetItem.getCategoryName());

        // --- Main amount ---
        ((TextView) view.findViewById(R.id.tvAmount))
                .setText(formatMoneyFull(budgetItem.getLimit()));

        // --- Spent ---
        ((TextView) view.findViewById(R.id.tvSpent))
                .setText(formatMoneyFull(budgetItem.getSpent()));

        // --- Remaining ---
        long remaining = budgetItem.getLimit() - budgetItem.getSpent();
        ((TextView) view.findViewById(R.id.tvRemaining))
                .setText(formatMoneyFull(remaining));

        // --- Progress bar ---
        ProgressBar progressBar = view.findViewById(R.id.progressBar);
        progressBar.setProgress(budgetItem.getProgress());

        // --- Today marker position ---
        View todayMarker = view.findViewById(R.id.viewTodayMarker);
        if (budgetItem.getStartDate() > 0 && budgetItem.getEndDate() > budgetItem.getStartDate()) {
            long now = System.currentTimeMillis();
            long total = budgetItem.getEndDate() - budgetItem.getStartDate();
            long elapsed = now - budgetItem.getStartDate();
            float ratio = Math.max(0f, Math.min(1f, (float) elapsed / total));
            // Offset the marker by ratio of parent width (done via post for layout)
            progressBar.post(() -> {
                int barWidth = progressBar.getWidth();
                int markerX = (int) (barWidth * ratio);
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) todayMarker.getLayoutParams();
                lp.leftMargin = markerX - todayMarker.getWidth() / 2;
                todayMarker.setLayoutParams(lp);
            });
        }

        // --- Date range ---
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
        SimpleDateFormat sdfFull = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        String startStr = sdf.format(new Date(budgetItem.getStartDate()));
        String endStr = sdf.format(new Date(budgetItem.getEndDate()));
        ((TextView) view.findViewById(R.id.tvDateRange))
                .setText(startStr + " - " + endStr);

        int daysLeft = budgetItem.getDaysRemaining();
        ((TextView) view.findViewById(R.id.tvDaysLeft))
                .setText("Còn " + daysLeft + " ngày");

        // --- Chart date labels ---
        ((TextView) view.findViewById(R.id.tvStartDateLabel))
                .setText(sdfFull.format(new Date(budgetItem.getStartDate())));
        ((TextView) view.findViewById(R.id.tvEndDateLabel))
                .setText(sdfFull.format(new Date(budgetItem.getEndDate())));

        // --- Chart ---
        BudgetChartView chartView = view.findViewById(R.id.budgetChart);
        chartView.setBudgetData(
                budgetItem.getLimit(),
                budgetItem.getSpent(),
                budgetItem.getStartDate(),
                budgetItem.getEndDate()
        );

        // --- Daily recommendation ---
        long dailyRecommend = 0;
        if (daysLeft > 0) {
            dailyRecommend = remaining / daysLeft;
        }
        ((TextView) view.findViewById(R.id.tvDailyRecommend))
                .setText(formatMoneyFull(dailyRecommend));

        // --- Expected spending (projected at current daily rate) ---
        long now = System.currentTimeMillis();
        long totalDays = (budgetItem.getEndDate() - budgetItem.getStartDate()) / (24 * 60 * 60 * 1000L);
        long elapsedDays = Math.max(1, (now - budgetItem.getStartDate()) / (24 * 60 * 60 * 1000L));
        long expectedTotal = 0;
        if (budgetItem.getSpent() > 0 && totalDays > 0) {
            double dailyRate = (double) budgetItem.getSpent() / elapsedDays;
            expectedTotal = (long) (dailyRate * totalDays);
        }
        ((TextView) view.findViewById(R.id.tvExpected))
                .setText(formatMoneyFull(expectedTotal));

        // --- Actual daily spending ---
        long actualDaily = 0;
        if (budgetItem.getSpent() > 0) {
            actualDaily = budgetItem.getSpent() / elapsedDays;
        }
        ((TextView) view.findViewById(R.id.tvActualDaily))
                .setText(formatMoneyFull(actualDaily));
    }

    private String formatMoneyFull(long amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        return formatter.format(amount) + " đ";
    }
}