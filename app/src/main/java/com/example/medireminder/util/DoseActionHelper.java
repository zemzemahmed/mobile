package com.example.medireminder.util;

import android.content.Context;

import com.example.medireminder.data.entity.Caregiver;
import com.example.medireminder.data.entity.DoseLog;
import com.example.medireminder.data.entity.Medicine;
import com.example.medireminder.data.entity.Schedule;
import com.example.medireminder.data.repository.CaregiverRepository;
import com.example.medireminder.data.repository.DoseLogRepository;
import com.example.medireminder.data.repository.MedicineRepository;

import java.util.ArrayList;
import java.util.List;

/** Shared logic for the alarm screen and the Home "Taken" button. */
public final class DoseActionHelper {

    private DoseActionHelper() {
    }

    /** Marks the dose as taken and subtracts one dose from the stock. */
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
            NotificationHelper.dismiss(context, scheduledTime);
        });
    }

    /** Called when the 5-second alarm screen times out: log MISSED and alert caregivers now. */
    public static void markMissedNow(Context context, long[] scheduleIds, long scheduledTime) {
        AppExecutor.database().execute(() -> {
            MedicineRepository medicines = new MedicineRepository(context);
            DoseLogRepository logs = new DoseLogRepository(context);
            List<Medicine> missedMedicines = new ArrayList<>();
            List<Schedule> missedSchedules = new ArrayList<>();

            for (long scheduleId : scheduleIds) {
                Schedule schedule = medicines.getScheduleById(scheduleId);
                if (schedule == null) {
                    continue;
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

                Medicine medicine = medicines.getMedicineById(schedule.medicineId);
                if (medicine != null) {
                    missedMedicines.add(medicine);
                    missedSchedules.add(schedule);
                }
                // Cancel only the backup missed timer. The next reminder is already scheduled
                // under the reminder request code and must stay alive.
                AlarmScheduler.cancelMissed(context, scheduleId);
            }

            NotificationHelper.dismiss(context, scheduledTime);

            if (new PreferencesHelper(context).isAlertEnabled()) {
                List<Caregiver> caregivers =
                        new CaregiverRepository(context).getAllCaregiversSync();
                SmsHelper.sendMissedAlerts(context, missedMedicines, missedSchedules,
                        scheduledTime, caregivers);
            }
        });
    }
}
