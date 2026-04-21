package com.ptithcm.quanlichitieu.ui.report;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Locale;

public class PieChartView extends View {

    private Paint paint;
    private Paint textPaint;
    private Paint linePaint;
    private RectF rectF;
    private List<PieSlice> slices;
    private float strokeWidth = 60f;

    public PieChartView(Context context) {
        super(context);
        init();
    }

    public PieChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setStrokeCap(Paint.Cap.BUTT);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#AAAAAA"));
        textPaint.setTextSize(32f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(Color.parseColor("#AAAAAA"));
        linePaint.setStrokeWidth(2f);

        rectF = new RectF();
    }

    public void setSlices(List<PieSlice> slices) {
        this.slices = slices;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Tăng pad để có đủ không gian vẽ text, icon và đường nối bên ngoài vòng tròn
        float pad = strokeWidth / 2f + 250f; // increased padding
        float size = Math.min(w, h);
        float centerX = w / 2f;
        float centerY = h / 2f;
        float radius = (size / 2f) - pad;
        rectF.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (slices == null || slices.isEmpty()) {
            paint.setColor(Color.parseColor("#3A3A3A"));
            canvas.drawArc(rectF, 0, 360, false, paint);
            return;
        }

        float total = 0;
        for (PieSlice slice : slices) {
            total += slice.value;
        }

        if (total == 0) {
            paint.setColor(Color.parseColor("#3A3A3A"));
            canvas.drawArc(rectF, 0, 360, false, paint);
            return;
        }

        float cx = rectF.centerX();
        float cy = rectF.centerY();
        float radius = rectF.width() / 2f;

        float currentAngle = -90f;
        for (PieSlice slice : slices) {
            float sweepAngle = (slice.value / total) * 360f;
            paint.setColor(slice.color);
            canvas.drawArc(rectF, currentAngle, sweepAngle, false, paint);

            float percentage = (slice.value / total) * 100f;
            // Only draw text and icon if slice is large enough
            if (percentage >= 1f) {
                float midAngle = currentAngle + sweepAngle / 2f;
                double rad = Math.toRadians(midAngle);

                // Tính toán các điểm để vẽ đường nối
                float startX = (float) (cx + (radius + strokeWidth / 2f) * Math.cos(rad));
                float startY = (float) (cy + (radius + strokeWidth / 2f) * Math.sin(rad));

                float extendRadius = radius + strokeWidth / 2f + 30f;
                float extendX = (float) (cx + extendRadius * Math.cos(rad));
                float extendY = (float) (cy + extendRadius * Math.sin(rad));

                boolean isRight = extendX >= cx;
                float horizontalLen = 60f;
                float endX = isRight ? extendX + horizontalLen : extendX - horizontalLen;
                float endY = extendY;

                // Vẽ đường nối
                canvas.drawLine(startX, startY, extendX, extendY, linePaint);
                canvas.drawLine(extendX, extendY, endX, endY, linePaint);

                String pctStr = String.format(Locale.getDefault(), "%.1f%%", percentage);

                float textOffset = 15f;
                float iconSize = 48f;

                textPaint.setTextAlign(isRight ? Paint.Align.LEFT : Paint.Align.RIGHT);

                // Vẽ icon và phần trăm
                if (slice.iconResId != 0) {
                    Drawable drawable = ContextCompat.getDrawable(getContext(), slice.iconResId);
                    if (drawable != null) {
                        float ix = isRight ? endX + textOffset : endX - textOffset - iconSize;
                        float iy = endY;

                        drawable.setBounds((int) ix, (int) (iy - iconSize / 2f), (int) (ix + iconSize), (int) (iy + iconSize / 2f));
                        drawable.draw(canvas);

                        float textX = isRight ? ix + iconSize + 10f : ix - 10f;
                        canvas.drawText(pctStr, textX, endY - ((textPaint.descent() + textPaint.ascent()) / 2f), textPaint);
                    }
                } else {
                    float textX = isRight ? endX + textOffset : endX - textOffset;
                    canvas.drawText(pctStr, textX, endY - ((textPaint.descent() + textPaint.ascent()) / 2f), textPaint);
                }
            }

            currentAngle += sweepAngle;
        }
    }

    public static class PieSlice {
        public float value;
        public int color;
        public int iconResId;

        public PieSlice(float value, int color, int iconResId) {
            this.value = value;
            this.color = color;
            this.iconResId = iconResId;
        }
    }
}
