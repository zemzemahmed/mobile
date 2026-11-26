package com.example.medireminder.util;

import android.content.Context;

import com.example.medireminder.data.entity.DoseLog;
import com.example.medireminder.data.entity.Medicine;
import com.example.medireminder.data.entity.Schedule;
import com.example.medireminder.data.repository.DoseLogRepository;
import com.example.medireminder.data.repository.MedicineRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Shared logic for the alarm screen and the Home "Taken" button. */
public final class DoseActionHelper {

    private DoseActionHelper() {
    }

    /** Marks the dose as taken, subtracts stock, and restores the ongoing notification. */
    public static void takeDose(Context context, long[] scheduleIds, long scheduledTime) {
        AppExecutor.database().execute(() -> {
            MedicineRepository medicines = new MedicineRepository(context);
            DoseLogRepository logs = new DoseLogRepository(context);

            for (long scheduleId : scheduleIds) {
                Schedule schedule = medicines.getScheduleById(scheduleId);
                if (schedule == null) {
                    continue;
                }
                DoseLog log = logs.findDose(scheduleId, scheduledTime);
                if (log != null && DoseLog.STATUS_TAKEN.equals(log.status)) {
                    continue; // already taken - never subtract stock twice
                }
                if (log == null) {
                    log = new DoseLog(schedule.medicineId, scheduleId, scheduledTime,
                            schedule.pillsPerDose, DoseLog.STATUS_TAKEN,
                            System.currentTimeMillis());
                    logs.insertLogSync(log);
                } else {
                    log.status = DoseLog.STATUS_TAKEN;
                    log.pillsTaken = schedule.pillsPerDose;
                    log.actualTime = System.currentTimeMillis();
                    logs.updateLogSync(log);
                }

                Medicine medicine = medicines.getMedicineById(schedule.medicineId);
                if (medicine != null) {
                    medicine.pillsInStock -= schedule.pillsPerDose;
                    medicines.updateMedicineSync(medicine);
                }

                AlarmScheduler.cancelMissed(context, scheduleId);
            }
            AlarmController.stopOnly();
            NotificationHelper.cancelMissed(context);
        });
    }

    /** Called after the 15-second alarm screen times out: log MISSED and keep the notification. */
    public static void markMissedNow(Context context, long[] scheduleIds, long scheduledTime) {
        AppExecutor.database().execute(() -> {
            MedicineRepository medicines = new MedicineRepository(context);
            DoseLogRepository logs = new DoseLogRepository(context);
            List<String> names = new ArrayList<>();

            for (long scheduleId : scheduleIds) {
                Schedule schedule = medicines.getScheduleById(scheduleId);
                if (schedule == null) {
                    continue;
                }
                Medicine medicine = medicines.getMedicineById(schedule.medicineId);
                if (medicine != null) {
                    names.add(medicine.name);
                }
                DoseLog log = logs.findDose(scheduleId, scheduledTime);
                if (log != null && (DoseLog.STATUS_TAKEN.equals(log.status)
                        || DoseLog.STATUS_MISSED.equals(log.status))) {
                    continue; // already handled by the user or the backup receiver
                }
                if (log == null) {
                    log = new DoseLog(schedule.medicineId, scheduleId, scheduledTime,
                            schedule.pillsPerDose, DoseLog.STATUS_MISSED, 0);
                    logs.insertLogSync(log);
                } else {
                    log.status = DoseLog.STATUS_MISSED;
                    log.actualTime = 0;
                    logs.updateLogSync(log);
                }
                AlarmScheduler.cancelMissed(context, scheduleId);
            }

            String time = new SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(new Date(scheduledTime));
            String text = joinNames(names) + " at " + time;
            AlarmController.stopOnly();
            NotificationHelper.showMissedNotification(context, text);
        });
    }

    private static String joinNames(List<String> names) {
        if (names.isEmpty()) {
            return "Dose";
        }
        return android.text.TextUtils.join(", ", names);
    }
}