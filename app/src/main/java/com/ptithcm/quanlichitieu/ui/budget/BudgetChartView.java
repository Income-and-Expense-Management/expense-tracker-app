package com.ptithcm.quanlichitieu.ui.budget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BudgetChartView extends View {

    private Paint budgetLinePaint;
    private Paint spentLinePaint;
    private Paint gridPaint;
    private Paint textPaint;
    private Paint todayLinePaint;

    private long limitAmount = 0;
    private long spentAmount = 0;
    private long startDate = 0;
    private long endDate = 0;

    private static final int PADDING_LEFT = 120;
    private static final int PADDING_RIGHT = 20;
    private static final int PADDING_TOP = 30;
    private static final int PADDING_BOTTOM = 10;

    public BudgetChartView(Context context) {
        super(context);
        init();
    }

    public BudgetChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        budgetLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        budgetLinePaint.setColor(Color.parseColor("#FF5252"));
        budgetLinePaint.setStrokeWidth(4f);
        budgetLinePaint.setStyle(Paint.Style.STROKE);

        spentLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        spentLinePaint.setColor(Color.parseColor("#4CAF50"));
        spentLinePaint.setStrokeWidth(4f);
        spentLinePaint.setStyle(Paint.Style.STROKE);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#333333"));
        gridPaint.setStrokeWidth(1f);
        gridPaint.setStyle(Paint.Style.STROKE);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#AAAAAA"));
        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.RIGHT);

        todayLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        todayLinePaint.setColor(Color.parseColor("#888888"));
        todayLinePaint.setStrokeWidth(2f);
        todayLinePaint.setStyle(Paint.Style.STROKE);
        todayLinePaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));
    }

    public void setBudgetData(long limit, long spent, long startDate, long endDate) {
        this.limitAmount = limit;
        this.spentAmount = spent;
        this.startDate = startDate;
        this.endDate = endDate;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        float chartLeft = PADDING_LEFT;
        float chartRight = width - PADDING_RIGHT;
        float chartTop = PADDING_TOP;
        float chartBottom = height - PADDING_BOTTOM;
        float chartWidth = chartRight - chartLeft;
        float chartHeight = chartBottom - chartTop;

        if (limitAmount <= 0) return;

        // Y-axis labels & grid lines (5 levels)
        int levels = 5;
        for (int i = 0; i <= levels; i++) {
            float y = chartBottom - (chartHeight * i / levels);
            // grid line
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint);
            // label
            long labelVal = (long) (limitAmount * i / levels);
            String label = formatShort(labelVal);
            canvas.drawText(label, chartLeft - 8, y + 10, textPaint);
        }

        // Budget limit line (flat red line across chart)
        float budgetY = chartTop; // limit is at the top
        canvas.drawLine(chartLeft, budgetY, chartRight, budgetY, budgetLinePaint);

        // Today marker
        long now = System.currentTimeMillis();
        if (startDate > 0 && endDate > startDate && now >= startDate && now <= endDate) {
            float totalMs = endDate - startDate;
            float elapsedMs = now - startDate;
            float todayX = chartLeft + (elapsedMs / totalMs) * chartWidth;
            canvas.drawLine(todayX, chartTop, todayX, chartBottom, todayLinePaint);
        }

        // Spent line (green, starts at 0 and shows current level)
        if (spentAmount > 0) {
            float spentRatio = Math.min((float) spentAmount / limitAmount, 1f);
            float spentY = chartBottom - (chartHeight * spentRatio);

            Path spentPath = new Path();
            if (startDate > 0 && endDate > startDate && now > startDate) {
                float totalMs = endDate - startDate;
                float elapsedMs = Math.min(now - startDate, totalMs);
                float currentX = chartLeft + (elapsedMs / totalMs) * chartWidth;

                spentPath.moveTo(chartLeft, chartBottom);
                spentPath.lineTo(currentX, spentY);
                canvas.drawPath(spentPath, spentLinePaint);
            }
        } else {
            // Draw a flat line at 0 (near bottom)
            canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, spentLinePaint);
        }
    }

    private String formatShort(long value) {
        if (value >= 1_000_000_000L) {
            return String.format(Locale.US, "%.1fB", value / 1_000_000_000.0);
        } else if (value >= 1_000_000L) {
            return String.format(Locale.US, "%.0fM", value / 1_000_000.0);
        } else if (value >= 1000L) {
            return String.format(Locale.US, "%.0fK", value / 1000.0);
        } else {
            return String.valueOf(value);
        }
    }
}