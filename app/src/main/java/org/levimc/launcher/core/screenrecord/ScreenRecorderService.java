package org.levimc.launcher.core.screenrecord;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import org.levimc.launcher.R;

public class ScreenRecorderService extends Service {

    public static final String ACTION_START = "org.levimc.launcher.screenrecord.START";
    public static final String ACTION_STOP = "org.levimc.launcher.screenrecord.STOP";
    public static final String EXTRA_RESULT_CODE = "extra_result_code";
    public static final String EXTRA_RESULT_DATA = "extra_result_data";

    private static final String CHANNEL_ID = "screen_recorder_channel";
    private static final int NOTIFICATION_ID = 4821;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
            Intent resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA);

            startForegroundCompat();

            if (resultData != null) {
                ScreenRecorderManager.getInstance().onPermissionResult(resultCode, resultData);
                ScreenRecorderManager.getInstance().startRecording(getApplicationContext());
            }
        } else if (ACTION_STOP.equals(action)) {
            ScreenRecorderManager.getInstance().stopRecording();
            stopForeground(true);
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void startForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            boolean canUseMediaProjectionType = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                    || checkSelfPermission(android.Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION)
                            == android.content.pm.PackageManager.PERMISSION_GRANTED;
            if (canUseMediaProjectionType) {
                try {
                    startForeground(NOTIFICATION_ID, buildNotification(),
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
                    return;
                } catch (SecurityException e) {
                    android.util.Log.w("ScreenRecorderService",
                            "FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION rejected, falling back", e);
                }
            } else {
                android.util.Log.w("ScreenRecorderService",
                        "FOREGROUND_SERVICE_MEDIA_PROJECTION not granted, starting without type");
            }
            startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE);
        } else {
            startForeground(NOTIFICATION_ID, buildNotification());
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.screen_recorder_notification_title))
                .setContentText(getString(R.string.screen_recorder_notification_text))
                .setSmallIcon(R.drawable.ic_nav_launch)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.screen_recorder_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
