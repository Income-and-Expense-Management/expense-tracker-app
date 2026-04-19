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
        float pad = strokeWidth / 2f + 140f;
        float size = Math.min(w, h);
        float left = (w - size) / 2f + pad;
        float top = (h - size) / 2f + pad;
        float right = left + size - 2 * pad;
        float bottom = top + size - 2 * pad;
        rectF.set(left, top, right, bottom);
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

        float startAngle = -90f;
        for (PieSlice slice : slices) {
            float sweepAngle = (slice.value / total) * 360f;
            paint.setColor(slice.color);
            canvas.drawArc(rectF, startAngle, sweepAngle, false, paint);

            float percentage = (slice.value / total) * 100f;
            // Only draw text and icon if slice is large enough
            if (percentage >= 2f) {
                float midAngle = startAngle + sweepAngle / 2f;
                double rad = Math.toRadians(midAngle);

                // Tính toán các điểm để vẽ đường nối
                float startX = (float) (cx + radius * Math.cos(rad));
                float startY = (float) (cy + radius * Math.sin(rad));

                float extendRadius = radius + strokeWidth / 2f + 20f;
                float extendX = (float) (cx + extendRadius * Math.cos(rad));
                float extendY = (float) (cy + extendRadius * Math.sin(rad));

                boolean isRight = Math.cos(rad) >= 0;
                float horizontalLen = 40f;
                float endX = isRight ? extendX + horizontalLen : extendX - horizontalLen;
                float endY = extendY;

                // Vẽ đường nối
                canvas.drawLine(startX, startY, extendX, extendY, linePaint);
                canvas.drawLine(extendX, extendY, endX, endY, linePaint);

                textPaint.setTextAlign(isRight ? Paint.Align.LEFT : Paint.Align.RIGHT);
                String pctStr = String.format(Locale.getDefault(), "%.1f%%", percentage);

                float textOffset = 10f;
                float iconSize = 48f;

                // Vẽ icon và phần trăm
                if (slice.iconResId != 0) {
                    Drawable drawable = ContextCompat.getDrawable(getContext(), slice.iconResId);
                    if (drawable != null) {
                        float ix = isRight ? endX + textOffset + iconSize / 2f : endX - textOffset - iconSize / 2f;
                        float iy = endY;

                        float halfSize = iconSize / 2f;
                        drawable.setBounds((int) (ix - halfSize), (int) (iy - halfSize), (int) (ix + halfSize), (int) (iy + halfSize));
                        drawable.draw(canvas);

                        float textX = isRight ? endX + textOffset + iconSize + 10f : endX - textOffset - iconSize - 10f;
                        canvas.drawText(pctStr, textX, endY + textPaint.getTextSize() / 3f, textPaint);
                    }
                } else {
                    float textX = isRight ? endX + textOffset : endX - textOffset;
                    canvas.drawText(pctStr, textX, endY + textPaint.getTextSize() / 3f, textPaint);
                }
            }

            startAngle += sweepAngle;
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
