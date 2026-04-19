package com.ptithcm.quanlichitieu.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.ptithcm.quanlichitieu.R;

public class SimpleLineChart extends View {

    private Paint expensePaint;
    private Paint incomePaint;
    private Paint gridPaint;
    private Paint textPaint;
    private Path expensePath;
    private Path incomePath;
    private java.util.List<Float> expenseData = new java.util.ArrayList<>();
    private java.util.List<Float> incomeData = new java.util.ArrayList<>();
    private boolean isExpenseMode = true;

    public SimpleLineChart(Context context) {
        super(context);
        init();
    }

    public SimpleLineChart(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setData(java.util.List<Float> expenseData, java.util.List<Float> incomeData) {
        this.expenseData = expenseData != null ? expenseData : new java.util.ArrayList<>();
        this.incomeData = incomeData != null ? incomeData : new java.util.ArrayList<>();
        invalidate();
    }

    public void setExpenseMode(boolean isExpenseMode) {
        this.isExpenseMode = isExpenseMode;
        invalidate();
    }

    private void init() {
        expensePaint = new Paint();
        expensePaint.setColor(ContextCompat.getColor(getContext(), R.color.home_chart_line)); // default red-ish
        expensePaint.setStrokeWidth(8f);
        expensePaint.setStyle(Paint.Style.STROKE);
        expensePaint.setAntiAlias(true);

        incomePaint = new Paint();
        // Assuming there's a home_income_green color, fallback to Color.GREEN if it doesn't exist
        int incomeColor = Color.parseColor("#4CAF50"); // Default green
        try {
            incomeColor = ContextCompat.getColor(getContext(), R.color.home_accent_green);
        } catch (Exception e) {}
        incomePaint.setColor(incomeColor);
        incomePaint.setStrokeWidth(8f);
        incomePaint.setStyle(Paint.Style.STROKE);
        incomePaint.setAntiAlias(true);

        gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor("#444444"));
        gridPaint.setStrokeWidth(2f);

        textPaint = new Paint();
        textPaint.setTextSize(24f);
        textPaint.setAntiAlias(true);

        expensePath = new Path();
        incomePath = new Path();
    }

    @Override
    protected void onDraw(@androidx.annotation.NonNull Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // Draw horizontal grid lines
        for (int i = 1; i <= 4; i++) {
            float y = height - (height / 5f) * i;
            canvas.drawLine(0, y, width, y, gridPaint);
        }

        if ((expenseData == null || expenseData.isEmpty()) && (incomeData == null || incomeData.isEmpty())) {
            return; // don't draw anything if there's no data
        }

        float max = 0f;
        if (expenseData != null) {
            for (Float val : expenseData) {
                if (val != null && val > max) max = val;
            }
        }
        if (incomeData != null) {
            for (Float val : incomeData) {
                if (val != null && val > max) max = val;
            }
        }

        if (max == 0f) {
            max = 1f; // Prevent division by zero
        }

        float padY = height * 0.1f;
        float drawableHeight = height - (padY * 2);

        // Draw ExpenseLine
        if (isExpenseMode && expenseData != null && expenseData.size() > 1) {
            expensePath.reset();
            float stepX = (float) width / (expenseData.size() - 1);
            float currentX = 0f, currentY = 0f, previousX = 0f, previousY = 0f;

            for (int i = 0; i < expenseData.size(); i++) {
                float val = expenseData.get(i);
                float normalized = val / max; // 0 to 1
                float x = i * stepX;
                float y = height - padY - (normalized * drawableHeight);

                if (i == 0) {
                    expensePath.moveTo(x, y);
                } else {
                    float controlX1 = previousX + (x - previousX) / 2f;
                    float controlX2 = previousX + (x - previousX) / 2f;
                    expensePath.cubicTo(controlX1, previousY, controlX2, y, x, y);

                    // Draw change value text if the value changed
                    if (val > expenseData.get(i-1)) {
                        float diff = val - expenseData.get(i-1);
                        if (diff > 0) {
                            textPaint.setColor(expensePaint.getColor());
                            String diffStr = String.format(java.util.Locale.getDefault(), "+%,.0f", diff);
                            canvas.drawText(diffStr, x - 20f, y - 20f, textPaint);
                        }
                    }
                }

                previousX = x; previousY = y;
                currentX = x; currentY = y;
            }

            canvas.drawPath(expensePath, expensePaint);
            expensePaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(currentX, currentY, 12f, expensePaint);
            expensePaint.setStyle(Paint.Style.STROKE);
        }

        // Draw IncomeLine
        if (!isExpenseMode && incomeData != null && incomeData.size() > 1) {
            incomePath.reset();
            float stepX = (float) width / (incomeData.size() - 1);
            float currentX = 0f, currentY = 0f, previousX = 0f, previousY = 0f;

            for (int i = 0; i < incomeData.size(); i++) {
                float val = incomeData.get(i);
                float normalized = val / max; // 0 to 1
                float x = i * stepX;
                float y = height - padY - (normalized * drawableHeight);

                if (i == 0) {
                    incomePath.moveTo(x, y);
                } else {
                    float controlX1 = previousX + (x - previousX) / 2f;
                    float controlX2 = previousX + (x - previousX) / 2f;
                    incomePath.cubicTo(controlX1, previousY, controlX2, y, x, y);

                    // Draw change value text if the value changed
                    if (val > incomeData.get(i-1)) {
                        float diff = val - incomeData.get(i-1);
                        if (diff > 0) {
                            textPaint.setColor(incomePaint.getColor());
                            String diffStr = String.format(java.util.Locale.getDefault(), "+%,.0f", diff);
                            canvas.drawText(diffStr, x - 20f, y - 20f, textPaint);
                        }
                    }
                }

                previousX = x; previousY = y;
                currentX = x; currentY = y;
            }

            canvas.drawPath(incomePath, incomePaint);
            incomePaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(currentX, currentY, 12f, incomePaint);
            incomePaint.setStyle(Paint.Style.STROKE);
        }
    }
}
