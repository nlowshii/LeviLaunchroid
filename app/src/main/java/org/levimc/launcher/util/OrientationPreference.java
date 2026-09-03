package org.levimc.launcher.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;

public final class OrientationPreference {

    public static final String MODE_SYSTEM = "system";
    public static final String MODE_PORTRAIT = "portrait";
    public static final String MODE_LANDSCAPE = "landscape";

    private static final String PREFS_NAME = "settings";
    private static final String KEY_ORIENTATION_MODE = "orientation_mode";
    private static final String DEFAULT_MODE = MODE_LANDSCAPE;

    private OrientationPreference() {}

    public static String getMode(Context ctx) {
        return prefs(ctx).getString(KEY_ORIENTATION_MODE, DEFAULT_MODE);
    }

    public static void setMode(Context ctx, String mode) {
        prefs(ctx).edit().putString(KEY_ORIENTATION_MODE, mode).apply();
    }

    public static int toActivityInfoOrientation(String mode) {
        if (MODE_LANDSCAPE.equals(mode)) {
            return ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
        }
        if (MODE_SYSTEM.equals(mode)) {
            return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        }
        return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
