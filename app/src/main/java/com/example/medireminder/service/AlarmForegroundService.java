package com.example.medireminder.service;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.medireminder.ui.activity.AlarmActivity;
import com.example.medireminder.util.AlarmOverlay;
import com.example.medireminder.util.NotificationHelper;
import com.example.medireminder.util.PreferencesHelper;

/**
 * A small persistent foreground service. It keeps one notification in the shade and opens
 * AlarmActivity when an alarm fires. It stays running so Android allows the alarm screen
 * to appear even when the app is in the background.
 */
public class AlarmForegroundService extends Service {

    public static final String ACTION_START = "com.example.medireminder.ALARM_START";
    public static final String ACTION_ALARM = "com.example.medireminder.ALARM_FIRE";
    public static final String ACTION_TAKEN = "com.example.medireminder.ALARM_TAKEN";
    public static final String ACTION_MISSED = "com.example.medireminder.ALARM_MISSED";
    public static final String ACTION_RINGING_ONLY = "com.example.medireminder.ALARM_RINGING_ONLY";

    public static final String EXTRA_SCHEDULE_IDS = "scheduleIds";
    public static final String EXTRA_SCHEDULED_TIME = "scheduledTime";
    public static final String EXTRA_DOSE_TEXT = "doseText";

    public static void start(Context context) {
        ContextCompat.startForegroundService(context.getApplicationContext(),
                new Intent(context.getApplicationContext(), AlarmForegroundService.class)
                        .setAction(ACTION_START));
    }

    public static void fireAlarm(Context context, long[] scheduleIds, long scheduledTime, String doseText) {
        Intent intent = new Intent(context.getApplicationContext(), AlarmForegroundService.class);
        intent.setAction(ACTION_ALARM);
        intent.putExtra(EXTRA_SCHEDULE_IDS, scheduleIds);
        intent.putExtra(EXTRA_SCHEDULED_TIME, scheduledTime);
        intent.putExtra(EXTRA_DOSE_TEXT, doseText);
        ContextCompat.startForegroundService(context.getApplicationContext(), intent);
    }

    /** Updates the notification only; used when AlarmActivity was already opened by AlarmManager. */
    public static void showRinging(Context context, long[] scheduleIds, long scheduledTime, String doseText) {
        Intent intent = new Intent(context.getApplicationContext(), AlarmForegroundService.class);
        intent.setAction(ACTION_RINGING_ONLY);
        intent.putExtra(EXTRA_SCHEDULE_IDS, scheduleIds);
        intent.putExtra(EXTRA_SCHEDULED_TIME, scheduledTime);
        intent.putExtra(EXTRA_DOSE_TEXT, doseText);
        ContextCompat.startForegroundService(context.getApplicationContext(), intent);
    }
    public static void doseTaken(Context context) {
        Intent intent = new Intent(context.getApplicationContext(), AlarmForegroundService.class);
        intent.setAction(ACTION_TAKEN);
        ContextCompat.startForegroundService(context.getApplicationContext(), intent);
    }

    public static void doseMissed(Context context, String doseText) {
        Intent intent = new Intent(context.getApplicationContext(), AlarmForegroundService.class);
        intent.setAction(ACTION_MISSED);
        intent.putExtra(EXTRA_DOSE_TEXT, doseText);
        ContextCompat.startForegroundService(context.getApplicationContext(), intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        PreferencesHelper prefs = new PreferencesHelper(this);

        if (ACTION_ALARM.equals(action) || ACTION_RINGING_ONLY.equals(action)) {
            long[] ids = intent.getLongArrayExtra(EXTRA_SCHEDULE_IDS);
            long time = intent.getLongExtra(EXTRA_SCHEDULED_TIME, -1);
            String text = intent.getStringExtra(EXTRA_DOSE_TEXT);
            if (ids != null && ids.length > 0 && time != -1) {
                prefs.saveNotification("TIME TO TAKE YOUR MEDICINE", text, time);
                showPersistentNotification("TIME TO TAKE YOUR MEDICINE", text, ids, time, true);
                AlarmOverlay.show(this, ids, time, text);
                if (ACTION_ALARM.equals(action)) {
                    Intent alarm = new Intent(this, AlarmActivity.class);
                    alarm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    alarm.putExtra(AlarmActivity.EXTRA_SCHEDULE_IDS, ids);
                    alarm.putExtra(AlarmActivity.EXTRA_SCHEDULED_TIME, time);
                    alarm.putExtra(AlarmActivity.EXTRA_DOSE_TEXT, text);
                    startActivity(alarm);
                }
                return START_STICKY;
            }
        } else if (ACTION_TAKEN.equals(action)) {
            AlarmOverlay.dismiss();
            prefs.saveNotification("MediReminder is active", "All doses are up to date. Tap to open.", -1);
        } else if (ACTION_MISSED.equals(action)) {
            AlarmOverlay.dismiss();
            String text = intent.getStringExtra(EXTRA_DOSE_TEXT);
            prefs.saveNotification("MISSED DOSE", text, -1);
        }

        showPersistentNotification(prefs.getNotificationTitle(), prefs.getNotificationText(),
                null, prefs.getNotificationTime(), false);
        return START_STICKY;
    }

    private void showPersistentNotification(String title, String text, long[] ids, long scheduledTime, boolean ringing) {
        Notification notification = NotificationHelper.buildPersistentNotification(
                this, title, text, ringing, ids, scheduledTime);
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NotificationHelper.PERSISTENT_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NotificationHelper.PERSISTENT_ID, notification);
        }
    }

    @Override
    public void onDestroy() {
        AlarmOverlay.dismiss();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}