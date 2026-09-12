package org.levimc.launcher.core.personalization;

import android.animation.ValueAnimator;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import java.util.WeakHashMap;

public class ChromaTextEffect {

    private static final int[] COLORS = new int[] {
            0xFFFF5F6D,
            0xFFFFC371,
            0xFF47E0A7,
            0xFF4FA9F5,
            0xFFB96BFF,
            0xFFFF5F6D
    };

    private static final int DURATION_MS = 3200;
    private static final WeakHashMap<TextView, ValueAnimator> ACTIVE = new WeakHashMap<>();

    public static void apply(TextView view, boolean enabled) {
        if (view == null) {
            return;
        }
        stop(view);
        if (!enabled) {
            view.getPaint().setShader(null);
            view.invalidate();
            return;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(DURATION_MS);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            float phase = (float) animation.getAnimatedValue();
            applyShader(view, phase);
        });
        animator.start();
        ACTIVE.put(view, animator);
    }

    public static void stop(TextView view) {
        if (view == null) {
            return;
        }
        ValueAnimator existing = ACTIVE.remove(view);
        if (existing != null) {
            existing.cancel();
        }
    }

    private static void applyShader(TextView view, float phase) {
        CharSequence text = view.getText();
        if (text == null || text.length() == 0) {
            return;
        }
        float width = view.getPaint().measureText(text, 0, text.length());
        if (width <= 0f) {
            return;
        }
        float offset = phase * width * 2f;
        LinearGradient shader = new LinearGradient(
                -offset,
                0f,
                width * 2f - offset,
                0f,
                COLORS,
                null,
                Shader.TileMode.MIRROR
        );
        view.getPaint().setShader(shader);
        view.postInvalidateOnAnimation();
    }
}
