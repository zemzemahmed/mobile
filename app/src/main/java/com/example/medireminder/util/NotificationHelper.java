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
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationCompat;

import com.example.medireminder.R;
import com.example.medireminder.receiver.ActionReceiver;
import com.example.medireminder.ui.activity.AlarmActivity;
import com.example.medireminder.ui.activity.MainActivity;

/** Builds the one notification that stays in the shade while MediReminder runs. */
public final class NotificationHelper {

    public static final String CHANNEL_ID = "medireminder_reminders";
    public static final int PERSISTENT_ID = 900001;

    private NotificationHelper() {
    }

    /** Removes notifications left by older builds; only the ongoing one should remain. */
    public static void clearOldNotifications(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        for (StatusBarNotification notification : manager.getActiveNotifications()) {
            if (notification.getId() != PERSISTENT_ID) {
                manager.cancel(notification.getId());
            }
        }
    }

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Medication reminders",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Ongoing reminder for your medication doses");
        channel.enableVibration(true);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * Builds the ongoing notification.
     * When ringing is true it also carries the full-screen intent and the Taken action.
     */
    public static Notification buildPersistentNotification(Context context, String title, String text,
                                                           boolean ringing, long[] scheduleIds,
                                                           long scheduledTime) {
        Intent openApp = new Intent(context, MainActivity.class);
        openApp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int requestCode = ringing && scheduleIds != null ? (int) scheduleIds[0] : PERSISTENT_ID;
        PendingIntent content = PendingIntent.getActivity(context, requestCode, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title == null ? context.getString(R.string.app_name) : title)
                .setContentText(text == null ? "" : text.replace("\n", ", "))
                .setContentIntent(content)
                .setOngoing(true)
                .setAutoCancel(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(ringing ? NotificationCompat.PRIORITY_MAX
                        : NotificationCompat.PRIORITY_HIGH);

        if (ringing && scheduleIds != null && scheduleIds.length > 0) {
            Intent alarmIntent = new Intent(context, AlarmActivity.class);
            alarmIntent.putExtra(AlarmActivity.EXTRA_SCHEDULE_IDS, scheduleIds);
            alarmIntent.putExtra(AlarmActivity.EXTRA_SCHEDULED_TIME, scheduledTime);
            alarmIntent.putExtra(AlarmActivity.EXTRA_DOSE_TEXT, text);
            PendingIntent fullScreen = PendingIntent.getActivity(context, requestCode + 1, alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Intent takenIntent = new Intent(context, ActionReceiver.class)
                    .setAction(ActionReceiver.ACTION_TAKEN);
            takenIntent.putExtra(ActionReceiver.EXTRA_SCHEDULE_IDS, scheduleIds);
            takenIntent.putExtra(ActionReceiver.EXTRA_SCHEDULED_TIME, scheduledTime);
            PendingIntent taken = PendingIntent.getBroadcast(context, requestCode + 2,
                    takenIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            String savedSound = new PreferencesHelper(context).getRingtoneUri();
            Uri sound = savedSound.isEmpty() ? Settings.System.DEFAULT_ALARM_ALERT_URI : Uri.parse(savedSound);

            builder.setFullScreenIntent(fullScreen, true)
                    .setCategory(Notification.CATEGORY_ALARM)
                    .setSound(sound)
                    .setVibrate(new long[]{0, 500, 500})
                    .addAction(android.R.drawable.checkbox_on_background,
                            context.getString(R.string.action_taken), taken);
        } else {
            builder.setCategory(Notification.CATEGORY_SERVICE);
        }
        return builder.build();
    }
}