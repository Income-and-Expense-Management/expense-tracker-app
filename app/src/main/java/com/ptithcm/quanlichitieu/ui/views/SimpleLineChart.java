package com.ptithcm.quanlichitieu.ui.views;

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

    private Paint linePaint;
    private Paint gridPaint;
    private Path linePath;

    public SimpleLineChart(Context context) {
        super(context);
        init();
    }

    public SimpleLineChart(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint = new Paint();
        linePaint.setColor(ContextCompat.getColor(getContext(), R.color.home_chart_line));
        linePaint.setStrokeWidth(8f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);

        gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor("#444444"));
        gridPaint.setStrokeWidth(2f);

        linePath = new Path();
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

        // Draw the S-curve line (Mock data visualization)
        // Starting bottom left
        linePath.reset();
        linePath.moveTo(0, height);

        // Cubic Bezier curve to simulate the growth trend in the image
        // Point 1: Low start
        linePath.cubicTo(
                width * 0.2f, height * 0.9f,  // Control point 1
                width * 0.4f, height * 0.8f,  // Control point 2
                width * 0.5f, height * 0.6f   // Mid point
        );

        // Point 2: Rapid growth
        linePath.cubicTo(
                width * 0.6f, height * 0.4f,
                width * 0.8f, height * 0.3f,
                width, height * 0.2f // End point (high)
        );

        canvas.drawPath(linePath, linePaint);

        // Draw a dot at the end
        linePaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(width, height * 0.2f, 12f, linePaint);
        linePaint.setStyle(Paint.Style.STROKE); // Reset
    }
}
