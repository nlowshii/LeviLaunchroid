package org.levimc.launcher.ui.widgets;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

public class SwirlingLoaderView extends View {

    private static final float MIN_SWEEP_DEGREES = 8f;
    private static final float MAX_SWEEP_DEGREES = 300f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();

    private float rotationDegrees = 0f;
    private float sweepAngle = MIN_SWEEP_DEGREES;
    private long durationMs = 1500L;

    private ValueAnimator rotateAnimator;
    private ValueAnimator sweepAnimator;

    public SwirlingLoaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(dpToPx(4f));
    }

    public void setIndicatorColor(int color) {
        paint.setColor(color);
        invalidate();
    }

    public void setStrokeWidthDp(float dp) {
        paint.setStrokeWidth(dpToPx(dp));
        requestLayout();
        invalidate();
    }

    public void setCycleDurationMs(long duration) {
        this.durationMs = duration;
        if (rotateAnimator != null) {
            start();
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float inset = paint.getStrokeWidth() / 2f + 1f;
        arcRect.set(inset, inset, w - inset, h - inset);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getVisibility() == VISIBLE) {
            start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    public void start() {
        stop();

        rotateAnimator = ValueAnimator.ofFloat(0f, 360f);
        rotateAnimator.setDuration(Math.round(durationMs * 1.333333f));
        rotateAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotateAnimator.setInterpolator(new LinearInterpolator());
        rotateAnimator.addUpdateListener(a -> {
            rotationDegrees = (float) a.getAnimatedValue();
            invalidate();
        });
        rotateAnimator.start();

        sweepAnimator = ValueAnimator.ofFloat(MIN_SWEEP_DEGREES, MAX_SWEEP_DEGREES);
        sweepAnimator.setDuration(durationMs);
        sweepAnimator.setRepeatCount(ValueAnimator.INFINITE);
        sweepAnimator.setRepeatMode(ValueAnimator.REVERSE);
        sweepAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        sweepAnimator.addUpdateListener(a -> {
            sweepAngle = (float) a.getAnimatedValue();
            invalidate();
        });
        sweepAnimator.start();
    }

    public void stop() {
        if (rotateAnimator != null) {
            rotateAnimator.cancel();
            rotateAnimator = null;
        }
        if (sweepAnimator != null) {
            sweepAnimator.cancel();
            sweepAnimator = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float startAngle = rotationDegrees - 90f;
        canvas.drawArc(arcRect, startAngle, sweepAngle, false, paint);
    }
}
