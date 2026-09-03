package org.levimc.launcher.core.screenrecord;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public final class ScreenRecorderManager {

    public interface RecordingStateListener {
        void onRecordingStateChanged(boolean recording);
    }

    private static ScreenRecorderManager instance;

    private final java.util.List<RecordingStateListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private MediaRecorder mediaRecorder;
    private boolean recording = false;
    private File outputFile;
    private int grantedResultCode;
    private Intent grantedResultData;

    private ScreenRecorderManager() {}

    public static synchronized ScreenRecorderManager getInstance() {
        if (instance == null) {
            instance = new ScreenRecorderManager();
        }
        return instance;
    }

    public boolean isRecording() {
        return recording;
    }

    public void addRecordingStateListener(RecordingStateListener listener) {
        listeners.add(listener);
    }

    public void removeRecordingStateListener(RecordingStateListener listener) {
        listeners.remove(listener);
    }

    private void notifyRecordingStateChanged() {
        for (RecordingStateListener listener : listeners) {
            listener.onRecordingStateChanged(recording);
        }
    }

    public boolean hasPermission() {
        return grantedResultData != null;
    }

    public Intent createPermissionIntent(Activity activity) {
        if (projectionManager == null) {
            projectionManager = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        }
        return projectionManager.createScreenCaptureIntent();
    }

    public void onPermissionResult(int resultCode, Intent data) {
        this.grantedResultCode = resultCode;
        this.grantedResultData = data;
    }

    public boolean startRecording(Context context) {
        if (recording) return false;
        if (grantedResultData == null) return false;
        if (projectionManager == null) {
            projectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        }

        mediaProjection = projectionManager.getMediaProjection(grantedResultCode, grantedResultData);
        if (mediaProjection == null) return false;

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        int density = metrics.densityDpi;

        outputFile = createOutputFile(context);

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setVideoSize(width, height);
        mediaRecorder.setVideoEncodingBitRate(8_000_000);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setOutputFile(outputFile.getAbsolutePath());

        try {
            mediaRecorder.prepare();
        } catch (Exception e) {
            mediaRecorder = null;
            mediaProjection.stop();
            mediaProjection = null;
            return false;
        }

        Surface surface = mediaRecorder.getSurface();
        virtualDisplay = mediaProjection.createVirtualDisplay(
                "LeviLauncherScreenRecord",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface, null, null);

        mediaRecorder.start();
        recording = true;
        notifyRecordingStateChanged();
        return true;
    }

    public File stopRecording() {
        if (!recording) return null;
        recording = false;
        notifyRecordingStateChanged();

        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.reset();
                mediaRecorder.release();
            }
        } catch (Exception ignored) {
        }
        mediaRecorder = null;

        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }

        return outputFile;
    }

    private File createOutputFile(Context context) {
        File dir = new File(context.getExternalFilesDir(null), "ScreenRecordings");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String name = "LeviLauncher_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new java.util.Date()) + ".mp4";
        return new File(dir, name);
    }

    public static WindowManager.LayoutParams applySecureFlagIfRecording(WindowManager.LayoutParams params) {
        if (getInstance().isRecording()) {
            params.flags |= WindowManager.LayoutParams.FLAG_SECURE;
        } else {
            params.flags &= ~WindowManager.LayoutParams.FLAG_SECURE;
        }
        return params;
    }
}
