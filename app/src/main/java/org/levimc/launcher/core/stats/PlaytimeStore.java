package org.levimc.launcher.core.stats;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;

public final class PlaytimeStore {

    private static final String PREFS_NAME = "levi_playtime";
    private static final String KEY_TOTAL_MS = "total_playtime_ms";
    private static final String KEY_DAILY_PREFIX = "daily_ms_";
    private static final long MIN_SESSION_MS = 5_000L;
    private static final int DAILY_HISTORY_DAYS = 7;

    private PlaytimeStore() {}

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static long epochDay() {
        return TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis());
    }

    public static synchronized long getTotalMillis(Context ctx) {
        return prefs(ctx).getLong(KEY_TOTAL_MS, 0L);
    }

    public static synchronized long getTodayMillis(Context ctx) {
        return prefs(ctx).getLong(KEY_DAILY_PREFIX + epochDay(), 0L);
    }

    public static synchronized long[] getLastSevenDaysMillis(Context ctx) {
        long[] result = new long[DAILY_HISTORY_DAYS];
        long today = epochDay();
        SharedPreferences p = prefs(ctx);
        for (int i = 0; i < DAILY_HISTORY_DAYS; i++) {
            long day = today - (DAILY_HISTORY_DAYS - 1 - i);
            result[i] = p.getLong(KEY_DAILY_PREFIX + day, 0L);
        }
        return result;
    }

    public static synchronized void addSessionMillis(Context ctx, long sessionMs) {
        if (sessionMs < MIN_SESSION_MS) return;
        SharedPreferences.Editor editor = prefs(ctx).edit();
        long total = getTotalMillis(ctx) + sessionMs;
        editor.putLong(KEY_TOTAL_MS, total);
        long day = epochDay();
        long todayTotal = getTodayMillis(ctx) + sessionMs;
        editor.putLong(KEY_DAILY_PREFIX + day, todayTotal);
        editor.apply();
        pruneOldEntries(ctx);
    }

    private static void pruneOldEntries(Context ctx) {
        SharedPreferences p = prefs(ctx);
        long cutoff = epochDay() - DAILY_HISTORY_DAYS - 1;
        SharedPreferences.Editor editor = null;
        for (String key : p.getAll().keySet()) {
            if (!key.startsWith(KEY_DAILY_PREFIX)) continue;
            try {
                long day = Long.parseLong(key.substring(KEY_DAILY_PREFIX.length()));
                if (day < cutoff) {
                    if (editor == null) editor = p.edit();
                    editor.remove(key);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        if (editor != null) editor.apply();
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

    public static String formatHoursDecimal(long millis) {
        double hours = millis / 3_600_000.0;
        return String.format(java.util.Locale.US, "%.1f h", hours);
    }
}
