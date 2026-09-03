package org.levimc.launcher.ui.widgets;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class ElasticIndicatorView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();
    private final float[] radii = new float[8];

    private float pillLeft = 0f;
    private float pillWidth = 0f;
    private float pillHeight = 0f;
    private float radiusLeft = 0f;
    private float radiusRight = 0f;
    private boolean initialized = false;
    private ValueAnimator animator;

    public ElasticIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setStyle(Paint.Style.FILL);
    }

    public void setIndicatorColor(int color) {
        paint.setColor(color);
        invalidate();
    }

    public void moveTo(float targetCenterX, float targetWidth, float targetHeight, boolean animate) {
        float targetLeft = targetCenterX - targetWidth / 2f;

        if (!initialized || !animate) {
            pillLeft = targetLeft;
            pillWidth = targetWidth;
            pillHeight = targetHeight;
            radiusLeft = targetHeight / 2f;
            radiusRight = targetHeight / 2f;
            initialized = true;
            invalidate();
            return;
        }

        if (animator != null) {
            animator.cancel();
        }

        final float startLeft = pillLeft;
        final float distance = targetLeft - startLeft;
        final boolean movingRight = distance >= 0;
        final float maxStretch = Math.min(Math.abs(distance) * 0.32f, targetWidth * 0.75f);
        final float finalHeight = targetHeight;
        final float baseWidth = targetWidth;

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(520);
        animator.setInterpolator(new DecelerateInterpolator(1.6f));
        animator.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            float bump = (float) Math.sin(f * Math.PI) * maxStretch;

            pillHeight = finalHeight;
            pillWidth = baseWidth + bump;

            float travel = startLeft + distance * f;
            pillLeft = movingRight ? travel : travel - bump;

            float roundness = finalHeight / 2f;
            float squeezeAmount = roundness * (bump / (maxStretch + 0.001f)) * 0.55f;
            float squeezedRadius = Math.max(roundness - squeezeAmount, roundness * 0.35f);

            if (movingRight) {
                radiusLeft = roundness;
                radiusRight = squeezedRadius;
            } else {
                radiusLeft = squeezedRadius;
                radiusRight = roundness;
            }
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (pillWidth <= 0f || pillHeight <= 0f) return;

        float top = (getHeight() - pillHeight) / 2f;
        rect.set(pillLeft, top, pillLeft + pillWidth, top + pillHeight);

        radii[0] = radii[1] = radiusLeft;
        radii[2] = radii[3] = radiusRight;
        radii[4] = radii[5] = radiusRight;
        radii[6] = radii[7] = radiusLeft;

        path.reset();
        path.addRoundRect(rect, radii, Path.Direction.CW);
        canvas.drawPath(path, paint);
    }
}
