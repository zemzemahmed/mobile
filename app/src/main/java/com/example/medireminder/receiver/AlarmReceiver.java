package com.example.medireminder.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.medireminder.data.entity.Medicine;
import com.example.medireminder.data.entity.Schedule;
import com.example.medireminder.data.repository.MedicineRepository;
import com.example.medireminder.util.AlarmScheduler;
import com.example.medireminder.util.AppExecutor;
import com.example.medireminder.util.NotificationHelper;

import java.util.List;

/** Fired by AlarmManager when it is time to take a dose (or when a snooze expires). */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        long scheduleId = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, -1);
        long scheduledTime = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, -1);
        boolean isSnooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false);
        if (scheduleId == -1 || scheduledTime == -1) {
            return;
        }

        Context appContext = context.getApplicationContext();
        AppExecutor.database().execute(() -> {
            MedicineRepository repository = new MedicineRepository(appContext);
            Schedule schedule = repository.getScheduleById(scheduleId);
            if (schedule == null) {
                return;
            }
            Medicine medicine = repository.getMedicineById(schedule.medicineId);
            if (medicine == null || !medicine.active) {
                return;
            }

            List<Schedule> dueAtSameTime = repository.getSchedulesDueAt(schedule.hour, schedule.minute);
            NotificationHelper.showReminder(appContext, dueAtSameTime, scheduledTime);
            // A snooze fires later, so the missed-check timer starts from now, not the old time.
            long missedCheckBase = isSnooze ? System.currentTimeMillis() : scheduledTime;
            AlarmScheduler.scheduleMissedCheck(appContext, scheduleId, missedCheckBase);

            // A snooze is a one-off; a normal alarm schedules its next occurrence.
            if (!isSnooze) {
                AlarmScheduler.scheduleOne(appContext, medicine, schedule);
            }
        });
    }
}
