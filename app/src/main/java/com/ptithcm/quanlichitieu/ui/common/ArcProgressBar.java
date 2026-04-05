package com.ptithcm.quanlichitieu.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.ptithcm.quanlichitieu.R;

public class ArcProgressBar extends View {

    private Paint backgroundPaint;
    private Paint progressPaint;
    private RectF rectF;

    // Default dimensions, will scale
    private float strokeWidth = 40f;
    private int max = 100;
    private int progress = 75; // Set preview data

    public ArcProgressBar(Context context) {
        super(context);
        init();
    }

    public ArcProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public ArcProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init() {
        init(null);
    }

    private void init(AttributeSet attrs) {
        // Bắt buộc để ROUND cap hoạt động đúng trên arc
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(0xFF3A3A3A);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(strokeWidth);
        backgroundPaint.setStrokeCap(Paint.Cap.ROUND);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(ContextCompat.getColor(getContext(), R.color.home_accent_green));
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        rectF = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float pad = strokeWidth / 2f + 5f;
        // Bounding box cho hình tròn
        rectF.set(pad, pad, w - pad, h - pad);
    }

    public void setMax(int max) {
        this.max = max;
        invalidate();
    }

    public void setProgress(int progress) {
        this.progress = progress;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Vẽ nền nửa vòng tròn (góc bắt đầu 180 độ, độ dài 180 độ)
        canvas.drawArc(rectF, 180f, 180f, false, backgroundPaint);

        // Vẽ tiến độ (tỉ lệ trên max=100 tương đương 180 độ)
        float sweepAngle = 180f * progress / max;
        canvas.drawArc(rectF, 180f, sweepAngle, false, progressPaint);
    }
}