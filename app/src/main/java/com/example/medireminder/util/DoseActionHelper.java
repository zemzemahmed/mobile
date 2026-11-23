package com.example.medireminder.util;

import android.content.Context;

import com.example.medireminder.data.entity.DoseLog;
import com.example.medireminder.data.entity.Medicine;
import com.example.medireminder.data.entity.Schedule;
import com.example.medireminder.data.repository.DoseLogRepository;
import com.example.medireminder.data.repository.MedicineRepository;

/** Shared logic for the Taken and Snooze buttons (notification and Home screen). */
public final class DoseActionHelper {

    private DoseActionHelper() {
    }

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
                AlarmScheduler.cancelSnooze(context, scheduleId);
            }
            NotificationHelper.dismiss(context, scheduledTime);
        });
    }

    public static void snoozeDose(Context context, long[] scheduleIds, long scheduledTime) {
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
                    continue; // do not snooze a dose that was already taken
                }
                if (log == null) {
                    log = new DoseLog(schedule.medicineId, scheduleId, scheduledTime,
                            schedule.pillsPerDose, DoseLog.STATUS_SNOOZED, 0);
                    logs.insertLogSync(log);
                } else {
                    log.status = DoseLog.STATUS_SNOOZED;
                    log.actualTime = 0;
                    logs.updateLogSync(log);
                }

                AlarmScheduler.cancelMissed(context, scheduleId);
                AlarmScheduler.scheduleSnooze(context, scheduleId, scheduledTime);
            }
            NotificationHelper.dismiss(context, scheduledTime);
        });
    }
}
