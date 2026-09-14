package org.levimc.pojavcontrols;

/** Optional bridge to the native JavaInvPaperDoll module. */
public final class PaperDollBridge {
    private static boolean unavailable;

    private PaperDollBridge() {}

    public static void updateCursor(float x, float y) {
        if (unavailable) return;
        try {
            updateCursorNative(x, y);
        } catch (UnsatisfiedLinkError ignored) {
            // The paperdoll module is optional. Do not break Pojav Controls.
            unavailable = true;
        }
    }

    private static native void updateCursorNative(float x, float y);
}
