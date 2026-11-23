package com.example.medireminder.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.medireminder.R;
import com.example.medireminder.data.entity.Medicine;
import com.example.medireminder.data.entity.Schedule;
import com.example.medireminder.data.repository.MedicineRepository;
import com.example.medireminder.receiver.ActionReceiver;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Builds the reminder notification and its Taken / Snooze actions. */
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
                "Medication reminders",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Reminders to take your medication");
        channel.enableVibration(true);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    /** Called on a background thread (it reads Room). */
    public static void showReminder(Context context, List<Schedule> doses, long scheduledTime) {
        if (doses == null || doses.isEmpty()) {
            return;
        }

        MedicineRepository repository = new MedicineRepository(context);
        long[] scheduleIds = new long[doses.size()];
        StringBuilder text = new StringBuilder();
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(scheduledTime));
        text.append(time).append(": ");

        for (int i = 0; i < doses.size(); i++) {
            Schedule schedule = doses.get(i);
            scheduleIds[i] = schedule.id;
            Medicine medicine = repository.getMedicineById(schedule.medicineId);
            String name = medicine == null ? "Medicine" : medicine.name;
            if (i > 0) {
                text.append(", ");
            }
            text.append(name).append(" x").append(formatNumber(schedule.pillsPerDose));
        }

        int id = notificationId(scheduledTime);
        Intent takenIntent = new Intent(context, ActionReceiver.class).setAction(ActionReceiver.ACTION_TAKEN);
        takenIntent.putExtra(ActionReceiver.EXTRA_SCHEDULE_IDS, scheduleIds);
        takenIntent.putExtra(ActionReceiver.EXTRA_SCHEDULED_TIME, scheduledTime);
        PendingIntent taken = PendingIntent.getBroadcast(context, id,
                takenIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent snoozeIntent = new Intent(context, ActionReceiver.class).setAction(ActionReceiver.ACTION_SNOOZE);
        snoozeIntent.putExtra(ActionReceiver.EXTRA_SCHEDULE_IDS, scheduleIds);
        snoozeIntent.putExtra(ActionReceiver.EXTRA_SCHEDULED_TIME, scheduledTime);
        PendingIntent snooze = PendingIntent.getBroadcast(context, id + 1,
                snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(text.toString())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(false)
                .addAction(android.R.drawable.checkbox_on_background,
                        context.getString(R.string.action_taken), taken)
                .addAction(android.R.drawable.ic_menu_revert,
                        context.getString(R.string.action_snooze), snooze);

        String ringtone = new PreferencesHelper(context).getRingtoneUri();
        if (!ringtone.isEmpty()) {
            builder.setSound(Uri.parse(ringtone));
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(id, builder.build());
        }
    }

    public static void dismiss(Context context, long scheduledTime) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(notificationId(scheduledTime));
        }
    }

    private static int notificationId(long scheduledTime) {
        // One id per minute: doses at the same time replace each other -> one grouped notification.
        return NOTIFICATION_TAG + (int) (scheduledTime / 60000);
    }

    private static String formatNumber(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
