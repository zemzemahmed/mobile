package com.example.medireminder.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationCompat;

import com.example.medireminder.R;
import com.example.medireminder.ui.activity.MainActivity;

/** Builds the single, swipe-dismissible notification shown after a missed dose. */
public final class NotificationHelper {

    private static final String CHANNEL_ID = "medireminder_missed";
    private static final int MISSED_ID = 900002;

    private NotificationHelper() {
    }

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Missed doses",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("A missed dose. Swipe the notification away when you have read it.");
        channel.enableVibration(false);
        channel.setSound(null, null); // The alarm sound is played by AlarmSoundHelper, not here.
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    /** Removes only notifications from old builds. A current missed notification is kept. */
    public static void clearLegacyNotifications(Context context) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        PreferencesHelper prefs = new PreferencesHelper(context);
        if (!prefs.isLegacyCleared()) {
            // One-time cleanup of the old unremovable foreground notification.
            manager.cancelAll();
            prefs.markLegacyCleared();
            return;
        }
        for (StatusBarNotification notification : manager.getActiveNotifications()) {
            if (notification.getId() != MISSED_ID) {
                manager.cancel(notification.getId());
            }
        }
    }

    /** Shown only when the alarm timed out without being acknowledged. */
    public static void showMissedNotification(Context context, String doseText) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }

        Intent openApp = new Intent(context, MainActivity.class);
        openApp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(context, MISSED_ID, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String text = doseText == null || doseText.isEmpty()
                ? context.getString(R.string.missed_notification_text)
                : doseText.replace("\n", ", ");

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(context.getString(R.string.missed_notification_title))
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(content)
                .setOngoing(false)        // swipe to dismiss works
                .setAutoCancel(false)     // tapping opens the app; swiping removes it
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(Notification.CATEGORY_STATUS)
                .build();
        manager.notify(MISSED_ID, notification);
    }

    public static void cancelMissed(Context context) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(MISSED_ID);
        }
    }
}
