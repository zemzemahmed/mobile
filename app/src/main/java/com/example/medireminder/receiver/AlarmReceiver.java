package com.example.medireminder.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.medireminder.data.entity.Medicine;
import com.example.medireminder.data.entity.Schedule;
import com.example.medireminder.data.repository.MedicineRepository;
import com.example.medireminder.service.AlarmForegroundService;
import com.example.medireminder.util.AlarmScheduler;
import com.example.medireminder.util.AppExecutor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Fired by AlarmManager: raises the full-screen Take/Missed alarm. */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        long scheduleId = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, -1);
        long scheduledTime = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, -1);
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
            if (dueAtSameTime == null || dueAtSameTime.isEmpty()) {
                return;
            }

            long[] scheduleIds = new long[dueAtSameTime.size()];
            StringBuilder text = new StringBuilder();
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(scheduledTime));
            for (int i = 0; i < dueAtSameTime.size(); i++) {
                Schedule dose = dueAtSameTime.get(i);
                scheduleIds[i] = dose.id;
                Medicine item = repository.getMedicineById(dose.medicineId);
                String name = item == null ? "Medicine" : item.name;
                if (i > 0) {
                    text.append("\n");
                }
                text.append(name).append(" x").append(formatNumber(dose.pillsPerDose));
            }
            text.append("\nat ").append(time);

            // The persistent foreground service opens the alarm screen over other apps.
            AlarmForegroundService.fireAlarm(appContext, scheduleIds, scheduledTime, text.toString());

            // Backup in case the alarm screen is closed before it can handle the timeout.
            AlarmScheduler.scheduleMissedCheck(appContext, scheduleId, scheduledTime);
            // Keep tomorrow's dose alive.
            AlarmScheduler.scheduleOne(appContext, medicine, schedule);
        });
    }

    private String formatNumber(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}