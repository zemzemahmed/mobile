package com.example.medireminder.util;

import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;

import com.example.medireminder.ui.activity.AlarmActivity;

import java.lang.ref.WeakReference;

/**
 * Owns the 15-second alarm: one sound, one countdown, one interface.
 * AlarmActivity and the overlay both talk to this class, so they never create duplicate alarms.
 */
public final class AlarmController {

    private static final long TIMEOUT_MS = 15000;

    private static Context app;
    private static long[] ids;
    private static long scheduledTime;
    private static boolean handled;
    private static CountDownTimer timer;
    private static WeakReference<AlarmActivity> activityRef = new WeakReference<>(null);

    private AlarmController() {
    }

    /** Starts (or replaces) the current alarm. Safe to call from a background thread. */
    public static void start(Context context, long[] scheduleIds, long time, String doseText) {
        app = context.getApplicationContext();
        ids = scheduleIds;
        scheduledTime = time;
        handled = false;

        new Handler(Looper.getMainLooper()).post(() -> {
            stopNow();
            AlarmSoundHelper.start(app);
            if (doseText != null) {
                AlarmOverlay.show(app, doseText);
            }
            timer = new CountDownTimer(TIMEOUT_MS, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int seconds = (int) Math.ceil(millisUntilFinished / 1000.0);
                    AlarmOverlay.updateCountdown(seconds);
                    AlarmActivity activity = activityRef.get();
                    if (activity != null) {
                        activity.updateCountdown(seconds);
                    }
                }

                @Override
                public void onFinish() {
                    markMissed();
                }
            };
            timer.start();
        });
    }

    public static void registerActivity(AlarmActivity activity) {
        activityRef = new WeakReference<>(activity);
    }

    public static void unregisterActivity(AlarmActivity activity) {
        if (activityRef.get() == activity) {
            activityRef.clear();
        }
    }

    /** The user tapped Taken: stop everything and save the dose. */
    public static void handleTaken() {
        if (handled || app == null || ids == null) {
            return;
        }
        handled = true;
        stopOnly();
        dismissActivity();
        DoseActionHelper.takeDose(app, ids, scheduledTime);
    }

    /** The 15 seconds ran out: stop everything and log the dose as missed. */
    public static void markMissed() {
        if (handled || app == null || ids == null) {
            return;
        }
        handled = true;
        stopOnly();
        dismissActivity();
        DoseActionHelper.markMissedNow(app, ids, scheduledTime);
    }

    /** Stops sound, countdown and overlay without changing any dose log. */
    public static void stopOnly() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            stopNow();
        } else {
            new Handler(Looper.getMainLooper()).post(AlarmController::stopNow);
        }
    }

    private static void stopNow() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        AlarmSoundHelper.stop();
        AlarmOverlay.dismiss();
    }

    private static void dismissActivity() {
        new Handler(Looper.getMainLooper()).post(() -> {
            AlarmActivity activity = activityRef.get();
            if (activity != null) {
                activity.finish();
            }
        });
    }
}
