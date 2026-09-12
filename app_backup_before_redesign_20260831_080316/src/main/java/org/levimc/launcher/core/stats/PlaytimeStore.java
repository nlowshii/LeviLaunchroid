package org.levimc.launcher.core.stats;

import android.content.Context;
import android.content.SharedPreferences;

public final class PlaytimeStore {

    private static final String PREFS_NAME = "levi_playtime";
    private static final String KEY_TOTAL_MS = "total_playtime_ms";
    private static final long MIN_SESSION_MS = 5_000L;

    private PlaytimeStore() {}

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized long getTotalMillis(Context ctx) {
        return prefs(ctx).getLong(KEY_TOTAL_MS, 0L);
    }

    public static synchronized void addSessionMillis(Context ctx, long sessionMs) {
        if (sessionMs < MIN_SESSION_MS) return;
        long total = getTotalMillis(ctx) + sessionMs;
        prefs(ctx).edit().putLong(KEY_TOTAL_MS, total).apply();
    }

    public static String formatDuration(long millis) {
        long totalMinutes = millis / 60_000L;
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        if (hours <= 0) {
            return minutes + "m played";
        }
        return hours + "h " + minutes + "m played";
    }
}
