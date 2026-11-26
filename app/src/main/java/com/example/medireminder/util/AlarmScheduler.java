package com.example.medireminder.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.format.DateFormat;

import com.example.medireminder.data.entity.Medicine;
import com.example.medireminder.data.entity.Schedule;
import com.example.medireminder.data.repository.MedicineRepository;
import com.example.medireminder.ui.activity.AlarmActivity;
import com.example.medireminder.ui.activity.MainActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * All AlarmManager work lives here.
 * Each alarm kind has its own request-code range so alarms never overwrite each other:
 * reminder = scheduleId, missed-check = 100000 + scheduleId.
 */
public final class AlarmScheduler {

    public static final String EXTRA_SCHEDULE_ID = "scheduleId";
    public static final String EXTRA_SCHEDULED_TIME = "scheduledTime";

    private static final int MISSED_OFFSET = 100000;
    /** Backup check: the alarm screen times out after 15 seconds, this runs at 16. */
    private static final long MISSED_DELAY_MS = 16000L;

    private AlarmScheduler() {
    }

    /** Schedules (or re-schedules) all alarms of one medicine. Call off the main thread. */
    public static void scheduleAlarmsForMedicine(Context context, long medicineId) {
        AppExecutor.database().execute(() -> {
            MedicineRepository repository = new MedicineRepository(context);
            Medicine medicine = repository.getMedicineById(medicineId);
            List<Schedule> schedules = repository.getSchedulesForMedicineSync(medicineId);
            if (medicine == null || schedules == null) {
                return;
            }
            if (!medicine.active) {
                cancelAlarmsForSchedules(context, schedules);
                return;
            }
            for (Schedule schedule : schedules) {
                scheduleOne(context, medicine, schedule);
            }
        });
    }

    /** Used by BootReceiver after a reboot. Call off the main thread. */
    public static void rescheduleAll(Context context) {
        AppExecutor.database().execute(() -> {
            MedicineRepository repository = new MedicineRepository(context);
            List<Schedule> schedules = repository.getAllActiveSchedulesSync();
            if (schedules == null) {
                return;
            }
            for (Schedule schedule : schedules) {
                Medicine medicine = repository.getMedicineById(schedule.medicineId);
                if (medicine != null && medicine.active) {
                    scheduleOne(context, medicine, schedule);
                }
            }
        });
    }

    /** Schedules the next valid occurrence of one alarm. Call off the main thread. */
    public static void scheduleOne(Context context, Medicine medicine, Schedule schedule) {
        long nextTime = nextOccurrence(medicine, schedule);
        if (nextTime == -1) {
            return; // paused, outside the date range, or no selected day
        }
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null || !canScheduleExact(context, manager)) {
            return;
        }

        // AlarmManager itself opens AlarmActivity. Samsung allows this even while the app is in background.
        Intent alarmIntent = new Intent(context, AlarmActivity.class);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        alarmIntent.putExtra(EXTRA_SCHEDULE_ID, schedule.id);
        alarmIntent.putExtra(EXTRA_SCHEDULED_TIME, nextTime);
        PendingIntent operation = PendingIntent.getActivity(context, (int) schedule.id, alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent showIntent = new Intent(context, MainActivity.class);
        PendingIntent show = PendingIntent.getActivity(context, 900000 + (int) schedule.id, showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.setAlarmClock(new AlarmManager.AlarmClockInfo(nextTime, show), operation);
    }

    public static void scheduleMissedCheck(Context context, long scheduleId, long scheduledTime) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null || !canScheduleExact(context, manager)) {
            return;
        }
        long triggerAt = System.currentTimeMillis() + MISSED_DELAY_MS;

        Intent intent = new Intent(context, com.example.medireminder.receiver.MissedReceiver.class);
        intent.putExtra(EXTRA_SCHEDULE_ID, scheduleId);
        intent.putExtra(EXTRA_SCHEDULED_TIME, scheduledTime);
        PendingIntent pending = PendingIntent.getBroadcast(context,
                MISSED_OFFSET + (int) scheduleId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending);
    }

    public static void cancelReminder(Context context, long scheduleId) {
        // The scheduled alarm is an Activity PendingIntent, so cancel that same intent.
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) {
            return;
        }
        Intent intent = new Intent(context, AlarmActivity.class);
        PendingIntent pending = PendingIntent.getActivity(context, (int) scheduleId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.cancel(pending);
    }

    public static void cancelMissed(Context context, long scheduleId) {
        cancel(context, MISSED_OFFSET + (int) scheduleId,
                com.example.medireminder.receiver.MissedReceiver.class);
    }

    public static void cancelAlarmsForSchedules(Context context, List<Schedule> schedules) {
        if (schedules == null) {
            return;
        }
        for (Schedule schedule : schedules) {
            cancelReminder(context, schedule.id);
            cancelMissed(context, schedule.id);
        }
    }

    private static void cancel(Context context, int requestCode, Class<?> receiverClass) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) {
            return;
        }
        Intent intent = new Intent(context, receiverClass);
        PendingIntent pending = PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.cancel(pending);
    }

    private static boolean canScheduleExact(Context context, AlarmManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return manager.canScheduleExactAlarms();
        }
        return true;
    }

    /** First time at/after now that matches the selected days and the date range. */
    private static long nextOccurrence(Medicine medicine, Schedule schedule) {
        if (!medicine.active || schedule.daysOfWeek == null || schedule.daysOfWeek.isEmpty()) {
            return -1;
        }
        Calendar now = Calendar.getInstance();
        for (int offset = 0; offset <= 7; offset++) {
            Calendar candidate = (Calendar) now.clone();
            candidate.add(Calendar.DAY_OF_YEAR, offset);
            candidate.set(Calendar.HOUR_OF_DAY, schedule.hour);
            candidate.set(Calendar.MINUTE, schedule.minute);
            candidate.set(Calendar.SECOND, 0);
            candidate.set(Calendar.MILLISECOND, 0);

            if (candidate.getTimeInMillis() <= now.getTimeInMillis() + 1000) {
                continue;
            }
            if (!isSelectedDay(schedule.daysOfWeek, candidate)) {
                continue;
            }
            if (!isDateAllowed(medicine, candidate.getTimeInMillis())) {
                continue;
            }
            return candidate.getTimeInMillis();
        }
        return -1;
    }

    private static boolean isSelectedDay(String daysOfWeek, Calendar calendar) {
        int androidDay = calendar.get(Calendar.DAY_OF_WEEK);
        // Our app: 1 = Monday ... 7 = Sunday. Android: 1 = Sunday ... 7 = Saturday.
        int ourDay = androidDay == Calendar.SUNDAY ? 7 : androidDay - 1;
        for (String part : daysOfWeek.split(",")) {
            try {
                if (Integer.parseInt(part.trim()) == ourDay) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // Skip a badly stored value.
            }
        }
        return false;
    }

    private static boolean isDateAllowed(Medicine medicine, long time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        format.setLenient(false);
        String day = format.format(new java.util.Date(time));
        try {
            if (medicine.startDate != null && !medicine.startDate.isEmpty()
                    && format.parse(medicine.startDate).getTime() > format.parse(day).getTime()) {
                return false;
            }
            if (medicine.endDate != null && !medicine.endDate.isEmpty()
                    && format.parse(medicine.endDate).getTime() < format.parse(day).getTime()) {
                return false;
            }
        } catch (ParseException e) {
            return false;
        }
        return true;
    }
}
