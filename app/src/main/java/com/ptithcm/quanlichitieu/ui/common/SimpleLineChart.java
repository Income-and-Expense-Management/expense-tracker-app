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
    private Paint dateTextPaint;
    private Paint dotPaint;
    private Paint fillPaint;
    private Path expensePath;
    private Path incomePath;
    private Path fillPath;
    private Paint gradientPaint;
    private Paint boxPaint;
    private Paint boxStrokePaint;
    private Paint valPaint;
    private Paint selectedDayPaint;
    private Paint faintLinePaint;

    private java.util.List<Float> expenseData = new java.util.ArrayList<>();
    private java.util.List<Float> incomeData = new java.util.ArrayList<>();
    private boolean isExpenseMode = true;
    private int selectedIndex = -1;
    private float[] xPoints;
    private float[] yPoints;

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
        this.selectedIndex = -1; // Reset selection
        invalidate();
    }

    public void setExpenseMode(boolean isExpenseMode) {
        this.isExpenseMode = isExpenseMode;
        this.selectedIndex = -1; // Reset selection
        invalidate();
    }

    private void init() {
        expensePaint = new Paint();
        expensePaint.setColor(ContextCompat.getColor(getContext(), R.color.home_chart_line));
        expensePaint.setStrokeWidth(8f);
        expensePaint.setStyle(Paint.Style.STROKE);
        expensePaint.setAntiAlias(true);
        expensePaint.setStrokeJoin(Paint.Join.ROUND);
        expensePaint.setStrokeCap(Paint.Cap.ROUND);
        expensePaint.setPathEffect(new android.graphics.CornerPathEffect(20f));

        incomePaint = new Paint();
        int incomeColor = Color.parseColor("#4CAF50");
        try { incomeColor = ContextCompat.getColor(getContext(), R.color.home_accent_green); } catch (Exception e) {}
        incomePaint.setColor(incomeColor);
        incomePaint.setStrokeWidth(8f);
        incomePaint.setStyle(Paint.Style.STROKE);
        incomePaint.setAntiAlias(true);
        incomePaint.setStrokeJoin(Paint.Join.ROUND);
        incomePaint.setStrokeCap(Paint.Cap.ROUND);
        incomePaint.setPathEffect(new android.graphics.CornerPathEffect(20f));

        gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor("#444444"));
        gridPaint.setStrokeWidth(2f);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{10, 10}, 0));

        textPaint = new Paint();
        textPaint.setTextSize(30f);
        textPaint.setAntiAlias(true);

        dateTextPaint = new Paint();
        dateTextPaint.setColor(Color.parseColor("#888888"));
        dateTextPaint.setTextSize(32f);
        dateTextPaint.setAntiAlias(true);

        dotPaint = new Paint();
        dotPaint.setColor(Color.WHITE);
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setAntiAlias(true);

        fillPaint = new Paint();
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);

        gradientPaint = new Paint();
        gradientPaint.setStyle(Paint.Style.FILL);
        gradientPaint.setAntiAlias(true);

        boxPaint = new Paint();
        boxPaint.setColor(Color.parseColor("#333333"));
        boxPaint.setStyle(Paint.Style.FILL);
        boxPaint.setAntiAlias(true);

        boxStrokePaint = new Paint();
        boxStrokePaint.setColor(Color.parseColor("#444444"));
        boxStrokePaint.setStyle(Paint.Style.STROKE);
        boxStrokePaint.setStrokeWidth(2f);
        boxStrokePaint.setAntiAlias(true);

        valPaint = new Paint();
        valPaint.setTextSize(36f);
        valPaint.setFakeBoldText(true);
        valPaint.setAntiAlias(true);

        selectedDayPaint = new Paint();
        selectedDayPaint.setColor(Color.parseColor("#888888"));
        selectedDayPaint.setTextSize(26f);
        selectedDayPaint.setAntiAlias(true);

        faintLinePaint = new Paint();
        faintLinePaint.setColor(Color.parseColor("#444444"));
        faintLinePaint.setStrokeWidth(2f);
        faintLinePaint.setStyle(Paint.Style.STROKE);
        faintLinePaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{10, 10}, 0));

        expensePath = new Path();
        incomePath = new Path();
        fillPath = new Path();
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        if (xPoints == null || xPoints.length == 0) return false;

        switch (event.getAction()) {
            case android.view.MotionEvent.ACTION_DOWN:
            case android.view.MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float minDist = Float.MAX_VALUE;
                int closest = -1;

                java.util.Calendar cal = java.util.Calendar.getInstance();
                int currentDay = cal.get(java.util.Calendar.DAY_OF_MONTH);
                java.util.List<Float> activeData = isExpenseMode ? expenseData : incomeData;
                int targetIndex = Math.min(currentDay - 1, activeData.size() - 1);

                for (int i = 0; i <= targetIndex && i < xPoints.length; i++) {
                    float dist = Math.abs(x - xPoints[i]);
                    if (dist < minDist) {
                        minDist = dist;
                        closest = i;
                    }
                }

                if (closest != -1 && closest != selectedIndex) {
                    selectedIndex = closest;
                    invalidate();
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(@androidx.annotation.NonNull Canvas canvas) {
        super.onDraw(canvas);

        if ((expenseData == null || expenseData.isEmpty()) && (incomeData == null || incomeData.isEmpty())) {
            return;
        }

        int width = getWidth();
        int height = getHeight();
        float padY = height * 0.15f;
        float drawableHeight = height - (padY * 2);

        java.util.List<Float> activeData = isExpenseMode ? expenseData : incomeData;
        Paint activePaint = isExpenseMode ? expensePaint : incomePaint;

        // 1. Calculate properly rounded Y-axis max value
        float max = 0f;
        if (activeData != null) {
            for (Float val : activeData) {
                if (val != null && val > max) max = val;
            }
        }

        if (max <= 0f) {
            max = 4f;
        } else {
            float rawStep = max / 4f;
            float mag = (float) Math.pow(10, Math.floor(Math.log10(rawStep)));
            float magMult = rawStep / mag;
            float niceMult;
            if (magMult <= 1.0f) niceMult = 1.0f;
            else if (magMult <= 2.0f) niceMult = 2.0f;
            else if (magMult <= 2.5f) niceMult = 2.5f;
            else if (magMult <= 4.0f) niceMult = 4.0f;
            else if (magMult <= 5.0f) niceMult = 5.0f;
            else niceMult = 10.0f;

            float step = (float) Math.ceil(niceMult * mag);
            max = step * 4f;
        }

        // 2. Build paths
        Path path = isExpenseMode ? expensePath : incomePath;
        path.reset();
        fillPath.reset();

        float drawWidth = width - 120;
        int dataSize = activeData != null ? activeData.size() : 1;
        float stepX = drawWidth / Math.max(1, dataSize - 1);

        xPoints = new float[dataSize];
        yPoints = new float[dataSize];

        java.util.Calendar calToday = java.util.Calendar.getInstance();
        int currentDay = calToday.get(java.util.Calendar.DAY_OF_MONTH);
        int currentMonth = calToday.get(java.util.Calendar.MONTH) + 1;
        int targetIndex = Math.min(currentDay - 1, dataSize - 1);

        if (selectedIndex == -1 || selectedIndex > targetIndex) {
            selectedIndex = targetIndex;
        }

        // Draw points strictly up to the current day
        for (int i = 0; i <= targetIndex; i++) {
            float val = activeData.get(i);
            float normalized = val / max;
            float x = i * stepX;
            float y = height - padY - (normalized * drawableHeight);

            xPoints[i] = x;
            yPoints[i] = y;

            if (i == 0) {
                path.moveTo(x, y);
                fillPath.moveTo(x, y);
            } else {
                path.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
        }

        // Setup gradient
        int color = activePaint.getColor();
        int fadeColor = Color.argb(60, Color.red(color), Color.green(color), Color.blue(color));

        float lastX = xPoints[targetIndex];
        fillPath.lineTo(lastX, height - padY);
        fillPath.lineTo(0, height - padY);
        fillPath.close();

        gradientPaint.setShader(new android.graphics.LinearGradient(
                0, padY, 0, height - padY,
                fadeColor, Color.TRANSPARENT, android.graphics.Shader.TileMode.CLAMP));

        // 3. Draw chart elements (bottom-most layer first)
        canvas.drawPath(fillPath, gradientPaint);
        canvas.drawPath(path, activePaint);

        // 4. Draw grid lines and labels on TOP of the line graph so 0 isn't hidden
        for (int i = 0; i <= 4; i++) {
            float y = height - padY - (i * drawableHeight / 4);
            float val = (max / 4) * i;

            canvas.drawLine(0, y, drawWidth, y, gridPaint);

            String text;
            if (i == 0) {
                text = "0";
            } else if (val >= 1000000) {
                float valM = val / 1000000f;
                if (valM == (long) valM) text = String.format(java.util.Locale.US, "%.0f M", valM);
                else text = String.format(java.util.Locale.US, "%.1f M", valM);
            } else if (val >= 1000) {
                float valK = val / 1000f;
                if (valK == (long) valK) text = String.format(java.util.Locale.US, "%.0f K", valK);
                else text = String.format(java.util.Locale.US, "%.1f K", valK);
            } else {
                text = String.format(java.util.Locale.US, "%.0f", val);
            }
            canvas.drawText(text, width - 110, y + 10, dateTextPaint);
        }

        // 5. Draw X-axis Dates
        int maxDays = calToday.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
        String startText = String.format(java.util.Locale.US, "01/%02d", currentMonth);
        String endText = String.format(java.util.Locale.US, "%02d/%02d", maxDays, currentMonth);

        canvas.drawText(startText, 0, height - 10, dateTextPaint);
        float endTextWidth = dateTextPaint.measureText(endText);
        canvas.drawText(endText, width - endTextWidth - 120, height - 10, dateTextPaint);

        // 6. Draw selection node overlay
        if (selectedIndex >= 0 && selectedIndex <= targetIndex) {
            float currentX = xPoints[selectedIndex];
            float currentY = yPoints[selectedIndex];

            // Vertical interaction line
            canvas.drawLine(currentX, height - padY, currentX, currentY, faintLinePaint);

            // Node Dot
            fillPaint.setColor(color);
            canvas.drawCircle(currentX, currentY, 15f, fillPaint);
            dotPaint.setColor(Color.WHITE);
            canvas.drawCircle(currentX, currentY, 8f, dotPaint);

            // Value Tooltip Box
            valPaint.setColor(color);
            float boxWidth = 280f;
            float boxHeight = 110f;
            float left = currentX - boxWidth / 2;
            if (left < 0) left = 0;
            if (left + boxWidth > width) left = width - boxWidth;
            float top = currentY - boxHeight - 30;
            if (top < 0) top = currentY + 30;

            android.graphics.RectF rect = new android.graphics.RectF(left, top, left + boxWidth, top + boxHeight);
            canvas.drawRoundRect(rect, 15f, 15f, boxPaint);
            canvas.drawRoundRect(rect, 15f, 15f, boxStrokePaint);

            String selectedVal = String.format(java.util.Locale.US, "%,.0f", activeData.get(selectedIndex));
            float valWidth = valPaint.measureText(selectedVal);
            canvas.drawText(selectedVal, left + (boxWidth - valWidth) / 2, top + 45, valPaint);

            String selectedDateStr = String.format(java.util.Locale.US, "%02d/%02d/%d", selectedIndex + 1, currentMonth, calToday.get(java.util.Calendar.YEAR));
            float dateWidth = selectedDayPaint.measureText(selectedDateStr);
            canvas.drawText(selectedDateStr, left + (boxWidth - dateWidth) / 2, top + 85, selectedDayPaint);
        }
    }
}
