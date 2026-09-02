package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.levimc.launcher.core.mods.inbuilt.nativemod.PojavControlsMod;

public class VirtualCursorMod {
    private static boolean active = false;
    private static float cursorX = 0;
    private static float cursorY = 0;
    private static int cursorPointer = -1;
    private static float lastTouchX;
    private static float lastTouchY;
    private static boolean cursorMoved;

    private static android.widget.ImageView cursorView;
    private static ViewGroup cursorRoot;

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean isActive, Activity activity) {
        active = isActive;
        if (active) {
            android.util.DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            if (cursorX == 0 && cursorY == 0) {
                cursorX = metrics.widthPixels / 2f;
                cursorY = metrics.heightPixels / 2f;
            }
            PojavControlsMod.setEnabled(true);
            PojavControlsMod.nativeSendPointer(cursorX, cursorY);
        } else {
            PojavControlsMod.setEnabled(false);
        }
        setCursorVisible(active, activity);
    }

    private static void setCursorVisible(boolean visible, Activity activity) {
        if (visible) {
            if (cursorView == null && activity != null) {
                android.view.View content = activity.findViewById(android.R.id.content);
                if (!(content instanceof ViewGroup)) return;
                cursorRoot = (ViewGroup) content;
                cursorView = new android.widget.ImageView(activity);
                cursorView.setImageResource(org.levimc.launcher.R.drawable.ic_virtual_cursor);
                cursorView.setClickable(false);
                cursorView.setFocusable(false);
                cursorView.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                cursorView.setElevation(Float.MAX_VALUE);

                int size = (int) (24 * activity.getResources().getDisplayMetrics().density);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
                params.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
                cursorRoot.addView(cursorView, params);
                cursorView.setX(cursorX);
                cursorView.setY(cursorY);
                cursorView.bringToFront();
                cursorRoot.requestLayout();
                cursorRoot.invalidate();
            } else if (cursorView != null) {
                cursorView.bringToFront();
            }
        } else {
            if (cursorView != null) {
                if (cursorView.getParent() instanceof ViewGroup) {
                    ((ViewGroup) cursorView.getParent()).removeView(cursorView);
                }
                cursorView = null;
                cursorRoot = null;
            }
            cursorPointer = -1;
        }
    }

    public static void processTouchEvent(MotionEvent event, Activity activity) {
        if (!active) return;

        android.util.DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        float screenWidth = metrics.widthPixels;
        float screenHeight = metrics.heightPixels;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                cursorPointer = event.getPointerId(event.getActionIndex());
                lastTouchX = event.getX(event.getActionIndex());
                lastTouchY = event.getY(event.getActionIndex());
                cursorMoved = false;
                if (cursorView != null) {
                    cursorView.bringToFront();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int pointerIndex = event.findPointerIndex(cursorPointer);
                if (pointerIndex >= 0) {
                    float touchX = event.getX(pointerIndex);
                    float touchY = event.getY(pointerIndex);
                    float dx = touchX - lastTouchX;
                    float dy = touchY - lastTouchY;

                    float sensitivity = org.levimc.launcher.core.mods.inbuilt.manager.InbuiltModManager.getInstance(activity).getCursorSensitivity() / 100f;

                    cursorX += dx * sensitivity;
                    cursorY += dy * sensitivity;
                    cursorX = Math.max(0, Math.min(cursorX, screenWidth));
                    cursorY = Math.max(0, Math.min(cursorY, screenHeight));

                    if (cursorView != null) {
                        cursorView.setX(cursorX);
                        cursorView.setY(cursorY);
                    }
                    PojavControlsMod.nativeSendPointer(cursorX, cursorY);
                    cursorMoved |= Math.abs(dx) > 1f || Math.abs(dy) > 1f;
                    lastTouchX = touchX;
                    lastTouchY = touchY;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (event.getPointerId(event.getActionIndex()) == cursorPointer) {
                    long duration = event.getEventTime() - event.getDownTime();
                    if (duration < 200 && !cursorMoved) {
                        sendClick();
                    }
                    cursorPointer = -1;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                cursorPointer = -1;
                break;
        }
    }

    private static void sendClick() {
        PojavControlsMod.nativeSendPointer(cursorX, cursorY);
        PojavControlsMod.nativeSendMouseButton(MotionEvent.BUTTON_PRIMARY, true);
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> PojavControlsMod.nativeSendMouseButton(MotionEvent.BUTTON_PRIMARY, false), 40);
    }
}
