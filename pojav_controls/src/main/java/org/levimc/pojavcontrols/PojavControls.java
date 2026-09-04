package org.levimc.pojavcontrols;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Region;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import java.lang.ref.WeakReference;

public final class PojavControls {
    public static final String ACTION_PROFILE_CHANGED = "org.levimc.pojavcontrols.PROFILE_CHANGED";

    private static WeakReference<Activity> attachedActivity = new WeakReference<>(null);
    private static PojavControlOverlay overlay;
    private static WindowManager.LayoutParams overlayParams;
    private static Object touchRegionListenerProxy;
    private static WeakReference<Activity> editorActivity = new WeakReference<>(null);
    private static PojavControlsEditorView editor;
    private static boolean hiddenFromRecording = false;

    private PojavControls() {}

    public static synchronized void setEnabled(Activity activity, PojavControlsHost host, boolean enabled) {
        if (enabled) attach(activity, host);
        else detach();
    }

    public static synchronized boolean isEnabled() {
        return overlay != null && overlay.isAttachedToWindow();
    }

    public static void launchEditor(Activity activity) {
        activity.runOnUiThread(() -> showEditor(activity));
    }

    public static synchronized boolean closeEditor() {
        if (editor == null) return false;
        editor.close();
        return true;
    }

    public static synchronized boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        return editor != null && editor.handleActivityResult(requestCode, resultCode, data);
    }

    public static synchronized boolean ownsTouchInput() {
        View target = editor != null && editor.isAttachedToWindow() ? editor : overlay;
        return target != null && target.isAttachedToWindow() && target.getVisibility() == View.VISIBLE;
    }

    public static synchronized void reload() {
        if (overlay != null) overlay.reloadProfile();
    }

    public static synchronized void setHiddenFromRecording(boolean hidden) {
        hiddenFromRecording = hidden;
        if (overlay == null || overlayParams == null) return;
        Activity activity = attachedActivity.get();
        if (activity == null) return;
        if (hidden) {
            overlayParams.flags |= WindowManager.LayoutParams.FLAG_SECURE;
        } else {
            overlayParams.flags &= ~WindowManager.LayoutParams.FLAG_SECURE;
        }
        try {
            activity.getWindowManager().updateViewLayout(overlay, overlayParams);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private static void attach(Activity activity, PojavControlsHost host) {
        Activity current = attachedActivity.get();
        if (overlay != null && current == activity && overlay.isAttachedToWindow()) {
            overlay.reloadProfile();
            overlay.bringToFront();
            return;
        }
        detach();

        overlay = new PojavControlOverlay(activity, host);

        WindowManager windowManager = activity.getWindowManager();
        overlayParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        overlayParams.token = activity.getWindow().getDecorView().getWindowToken();
        overlayParams.gravity = Gravity.TOP | Gravity.START;
        if (hiddenFromRecording) {
            overlayParams.flags |= WindowManager.LayoutParams.FLAG_SECURE;
        }

        windowManager.addView(overlay, overlayParams);
        attachTouchableRegionTracking(overlay);
        attachedActivity = new WeakReference<>(activity);
    }

    private static void attachTouchableRegionTracking(PojavControlOverlay overlayView) {
        try {
            Class<?> insetsInfoClass = Class.forName("android.view.ViewTreeObserver$InternalInsetsInfo");
            Class<?> listenerClass = Class.forName("android.view.ViewTreeObserver$OnComputeInternalInsetsListener");

            java.lang.reflect.Field touchableRegionField = insetsInfoClass.getField("touchableRegion");
            java.lang.reflect.Field touchableInsetsRegionConstant = insetsInfoClass.getField("TOUCHABLE_INSETS_REGION");
            int regionConstant = touchableInsetsRegionConstant.getInt(null);
            java.lang.reflect.Method setTouchableInsets = insetsInfoClass.getMethod("setTouchableInsets", int.class);

            Object listenerProxy = java.lang.reflect.Proxy.newProxyInstance(
                    listenerClass.getClassLoader(),
                    new Class<?>[]{listenerClass},
                    (proxy, method, args) -> {
                        if ("onComputeInternalInsets".equals(method.getName()) && args != null && args.length == 1) {
                            Object insetInfo = args[0];
                            setTouchableInsets.invoke(insetInfo, regionConstant);
                            Region region = (Region) touchableRegionField.get(insetInfo);
                            region.setEmpty();
                            for (int i = 0; i < overlayView.getChildCount(); i++) {
                                View child = overlayView.getChildAt(i);
                                if (child.getVisibility() != View.VISIBLE) continue;
                                Rect rect = new Rect();
                                child.getHitRect(rect);
                                region.op(rect, Region.Op.UNION);
                            }
                        }
                        return null;
                    });

            java.lang.reflect.Method addListener = ViewTreeObserver.class.getMethod(
                    "addOnComputeInternalInsetsListener", listenerClass);
            addListener.invoke(overlayView.getViewTreeObserver(), listenerProxy);
            touchRegionListenerProxy = listenerProxy;
        } catch (ReflectiveOperationException e) {
            touchRegionListenerProxy = null;
        }
    }

    private static synchronized void showEditor(Activity activity) {
        if (editor != null) {
            if (editorActivity.get() == activity && editor.isAttachedToWindow()) {
                editor.bringToFront();
                return;
            }
            finishEditor();
        }
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) return;
        if (overlay != null) {
            overlay.releaseAll();
            overlay.setVisibility(View.GONE);
        }
        editor = new PojavControlsEditorView(activity, PojavControls::finishEditor);
        ((ViewGroup) content).addView(editor, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        editor.bringToFront();
        editorActivity = new WeakReference<>(activity);
    }

    private static synchronized void finishEditor() {
        if (editor != null && editor.getParent() instanceof ViewGroup) {
            ((ViewGroup) editor.getParent()).removeView(editor);
        }
        editor = null;
        editorActivity.clear();
        if (overlay != null) {
            overlay.reloadProfile();
            overlay.setVisibility(View.VISIBLE);
            overlay.bringToFront();
        }
    }

    private static void detach() {
        if (editor != null) editor.close();
        if (overlay != null) {
            overlay.releaseAll();
            if (touchRegionListenerProxy != null) {
                try {
                    Class<?> listenerClass = Class.forName("android.view.ViewTreeObserver$OnComputeInternalInsetsListener");
                    java.lang.reflect.Method removeListener = ViewTreeObserver.class.getMethod(
                            "removeOnComputeInternalInsetsListener", listenerClass);
                    removeListener.invoke(overlay.getViewTreeObserver(), touchRegionListenerProxy);
                } catch (ReflectiveOperationException ignored) {
                }
                touchRegionListenerProxy = null;
            }
            Activity activity = attachedActivity.get();
            if (activity != null) {
                try {
                    activity.getWindowManager().removeViewImmediate(overlay);
                } catch (IllegalArgumentException ignored) {
                }
            }
            overlay.dispose();
        }
        overlay = null;
        overlayParams = null;
        attachedActivity.clear();
    }
}
