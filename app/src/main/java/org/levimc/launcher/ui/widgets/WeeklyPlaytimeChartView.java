package org.levimc.launcher.ui.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class WeeklyPlaytimeChartView extends View {

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private long[] valuesMs = new long[7];
    private String[] labels = new String[7];
    private int highlightedIndex = -1;

    public WeeklyPlaytimeChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        barPaint.setStyle(Paint.Style.FILL);
        trackPaint.setStyle(Paint.Style.FILL);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(11f * getResources().getDisplayMetrics().scaledDensity);
    }

    public void setBarColor(int color) {
        barPaint.setColor(color);
        invalidate();
    }

    public void setTrackColor(int color) {
        trackPaint.setColor(color);
        invalidate();
    }

    public void setLabelColor(int color) {
        labelPaint.setColor(color);
        invalidate();
    }

    public void setData(long[] valuesMs, String[] labels, int highlightedIndex) {
        this.valuesMs = valuesMs != null ? valuesMs : new long[7];
        this.labels = labels != null ? labels : new String[7];
        this.highlightedIndex = highlightedIndex;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int count = valuesMs.length;
        if (count == 0) return;

        float labelHeight = labelPaint.getTextSize() + dp(8);
        float chartTop = dp(4);
        float chartBottom = getHeight() - labelHeight;
        float chartHeight = Math.max(chartBottom - chartTop, dp(4));

        long max = 1L;
        for (long v : valuesMs) max = Math.max(max, v);

        float slot = (float) getWidth() / count;
        float barWidth = Math.min(slot * 0.42f, dp(18));
        float trackWidth = barWidth;
        float minBarHeight = dp(3);

        for (int i = 0; i < count; i++) {
            float centerX = slot * i + slot / 2f;

            rect.set(centerX - trackWidth / 2f, chartTop, centerX + trackWidth / 2f, chartBottom);
            float trackRadius = trackWidth / 2f;
            canvas.drawRoundRect(rect, trackRadius, trackRadius, trackPaint);

            float fraction = (float) valuesMs[i] / (float) max;
            float barHeight = Math.max(chartHeight * fraction, valuesMs[i] > 0 ? minBarHeight : 0f);
            if (barHeight > 0f) {
                rect.set(centerX - barWidth / 2f, chartBottom - barHeight, centerX + barWidth / 2f, chartBottom);
                float barRadius = barWidth / 2f;
                barPaint.setAlpha(i == highlightedIndex ? 255 : 200);
                canvas.drawRoundRect(rect, barRadius, barRadius, barPaint);
            }

            if (labels[i] != null) {
                labelPaint.setAlpha(i == highlightedIndex ? 255 : 150);
                canvas.drawText(labels[i], centerX, getHeight() - dp(2), labelPaint);
            }
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
