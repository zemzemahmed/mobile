package com.example.medireminder.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.medireminder.data.entity.Caregiver;
import com.example.medireminder.data.entity.DoseLog;
import com.example.medireminder.data.entity.Medicine;
import com.example.medireminder.data.entity.Schedule;
import com.example.medireminder.data.repository.CaregiverRepository;
import com.example.medireminder.data.repository.DoseLogRepository;
import com.example.medireminder.data.repository.MedicineRepository;
import com.example.medireminder.util.AlarmScheduler;
import com.example.medireminder.util.AppExecutor;
import com.example.medireminder.util.NotificationHelper;
import com.example.medireminder.util.PreferencesHelper;
import com.example.medireminder.util.SmsHelper;

import java.util.List;

/** Fired X minutes after a dose if the user never tapped Taken. */
public class MissedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        long scheduleId = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, -1);
        long scheduledTime = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, -1);
        if (scheduleId == -1 || scheduledTime == -1) {
            return;
        }

        Context appContext = context.getApplicationContext();
        AppExecutor.database().execute(() -> {
            MedicineRepository medicines = new MedicineRepository(appContext);
            DoseLogRepository logs = new DoseLogRepository(appContext);

            DoseLog log = logs.findDose(scheduleId, scheduledTime);
            if (log != null && DoseLog.STATUS_TAKEN.equals(log.status)) {
                return; // the user took it in time
            }

            Schedule schedule = medicines.getScheduleById(scheduleId);
            if (schedule == null) {
                return;
            }
            Medicine medicine = medicines.getMedicineById(schedule.medicineId);
            if (medicine == null) {
                return;
            }

            if (log == null) {
                log = new DoseLog(medicine.id, scheduleId, scheduledTime,
                        schedule.pillsPerDose, DoseLog.STATUS_MISSED, 0);
                logs.insertLogSync(log);
            } else {
                log.status = DoseLog.STATUS_MISSED;
                log.actualTime = 0;
                logs.updateLogSync(log);
            }

            NotificationHelper.dismiss(appContext, scheduledTime);

            if (new PreferencesHelper(appContext).isAlertEnabled()) {
                List<Caregiver> caregivers =
                        new CaregiverRepository(appContext).getAllCaregiversSync();
                SmsHelper.sendMissedAlert(appContext, medicine, schedule, scheduledTime, caregivers);
            }
        });
    }
}
