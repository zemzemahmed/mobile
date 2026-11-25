package com.example.medireminder.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.NotificationCompat;

import com.example.medireminder.R;
import com.example.medireminder.receiver.ActionReceiver;
import com.example.medireminder.ui.activity.AlarmActivity;

/** Builds the alarm notification that starts the full-screen Take/Missed screen. */
public final class NotificationHelper {

    public static final String CHANNEL_ID = "medireminder_reminders";
    private static final int NOTIFICATION_TAG = 300000;

    private NotificationHelper() {
    }

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Medication alarms",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Full-screen alarms to take your medication");
        channel.enableVibration(true);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    /** Builds the notification. Also used by AlarmForegroundService via startForeground(). */
    public static Notification buildAlarmNotification(Context context, long[] scheduleIds,
                                                      long scheduledTime, String doseText) {
        int id = notificationId(scheduledTime);
        Intent alarmIntent = new Intent(context, AlarmActivity.class);
        alarmIntent.putExtra(AlarmActivity.EXTRA_SCHEDULE_IDS, scheduleIds);
        alarmIntent.putExtra(AlarmActivity.EXTRA_SCHEDULED_TIME, scheduledTime);
        alarmIntent.putExtra(AlarmActivity.EXTRA_DOSE_TEXT, doseText);
        PendingIntent fullScreen = PendingIntent.getActivity(context, id, alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent takenIntent = new Intent(context, ActionReceiver.class).setAction(ActionReceiver.ACTION_TAKEN);
        takenIntent.putExtra(ActionReceiver.EXTRA_SCHEDULE_IDS, scheduleIds);
        takenIntent.putExtra(ActionReceiver.EXTRA_SCHEDULED_TIME, scheduledTime);
        PendingIntent taken = PendingIntent.getBroadcast(context, id,
                takenIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String savedSound = new PreferencesHelper(context).getRingtoneUri();
        Uri sound = savedSound.isEmpty() ? Settings.System.DEFAULT_ALARM_ALERT_URI : Uri.parse(savedSound);

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(doseText == null ? "" : doseText.replace("\n", ", "))
                .setContentIntent(fullScreen)
                .setFullScreenIntent(fullScreen, true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setSound(sound)
                .setVibrate(new long[]{0, 500, 500})
                .addAction(android.R.drawable.checkbox_on_background,
                        context.getString(R.string.action_taken), taken)
                .build();
    }

    /** Fallback for devices where the foreground service cannot be started. */
    public static void showAlarm(Context context, long[] scheduleIds, long scheduledTime, String doseText) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notificationId(scheduledTime),
                    buildAlarmNotification(context, scheduleIds, scheduledTime, doseText));
        }
    }

    public static void dismiss(Context context, long scheduledTime) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(notificationId(scheduledTime));
        }
    }

    public static int notificationId(long scheduledTime) {
        // One id per minute: doses at the same time replace each other -> one alarm.
        return NOTIFICATION_TAG + (int) (scheduledTime / 60000);
    }
}