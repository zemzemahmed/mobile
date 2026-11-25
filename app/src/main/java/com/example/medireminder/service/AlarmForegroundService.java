package com.example.medireminder.service;

import android.app.Notification;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.example.medireminder.ui.activity.AlarmActivity;
import com.example.medireminder.util.NotificationHelper;

/** Short-lived foreground service that can open AlarmActivity even while the app is in the background. */
public class AlarmForegroundService extends Service {

    public static final String EXTRA_SCHEDULE_IDS = "scheduleIds";
    public static final String EXTRA_SCHEDULED_TIME = "scheduledTime";
    public static final String EXTRA_DOSE_TEXT = "doseText";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        long[] scheduleIds = intent.getLongArrayExtra(EXTRA_SCHEDULE_IDS);
        long scheduledTime = intent.getLongExtra(EXTRA_SCHEDULED_TIME, -1);
        String doseText = intent.getStringExtra(EXTRA_DOSE_TEXT);
        if (scheduleIds == null || scheduleIds.length == 0 || scheduledTime == -1) {
            stopSelf();
            return START_NOT_STICKY;
        }

        NotificationHelper.createChannel(this);
        int notificationId = NotificationHelper.notificationId(scheduledTime);
        Notification notification =
                NotificationHelper.buildAlarmNotification(this, scheduleIds, scheduledTime, doseText);

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(notificationId, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(notificationId, notification);
        }

        Intent alarmIntent = new Intent(this, AlarmActivity.class);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        alarmIntent.putExtra(AlarmActivity.EXTRA_SCHEDULE_IDS, scheduleIds);
        alarmIntent.putExtra(AlarmActivity.EXTRA_SCHEDULED_TIME, scheduledTime);
        alarmIntent.putExtra(AlarmActivity.EXTRA_DOSE_TEXT, doseText);

        Handler handler = new Handler(Looper.getMainLooper());
        // Give startForeground() a moment to mark this process as foreground.
        handler.postDelayed(() -> startActivity(alarmIntent), 300);
        // The alarm screen handles the dose within 5 seconds; keep the service alive a little longer.
        handler.postDelayed(this::stopSelf, 6300);
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}