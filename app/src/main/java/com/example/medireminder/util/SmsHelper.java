package com.example.medireminder.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;

import androidx.core.content.ContextCompat;

import com.example.medireminder.data.entity.Caregiver;
import com.example.medireminder.data.entity.Medicine;
import com.example.medireminder.data.entity.Schedule;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Sends the missed-dose SMS. Uses the SIM card, so normal SMS rates may apply. */
public final class SmsHelper {

    private SmsHelper() {
    }

    public static void sendMissedAlert(Context context, Medicine medicine, Schedule schedule,
                                       long scheduledTime, List<Caregiver> caregivers) {
        if (medicine == null || schedule == null) {
            return;
        }
        List<Medicine> medicines = new ArrayList<>();
        medicines.add(medicine);
        List<Schedule> schedules = new ArrayList<>();
        schedules.add(schedule);
        sendMissedAlerts(context, medicines, schedules, scheduledTime, caregivers);
    }

    /** One SMS per caregiver, even when several doses were missed at the same time. */
    public static void sendMissedAlerts(Context context, List<Medicine> medicines,
                                        List<Schedule> schedules, long scheduledTime,
                                        List<Caregiver> caregivers) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            return; // permission was denied - the app simply does not send anything
        }
        if (caregivers == null || caregivers.isEmpty() || medicines == null || schedules == null
                || medicines.isEmpty() || schedules.isEmpty()) {
            return;
        }

        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(scheduledTime));
        StringBuilder message = new StringBuilder("MISSED DOSE ALERT\n");
        int count = Math.min(medicines.size(), schedules.size());
        for (int i = 0; i < count; i++) {
            Medicine medicine = medicines.get(i);
            Schedule schedule = schedules.get(i);
            if (medicine == null || schedule == null) {
                continue;
            }
            message.append("- ").append(medicine.name)
                    .append(" x").append(formatNumber(schedule.pillsPerDose))
                    .append(" pill(s)\n");
        }
        message.append("Planned at ").append(time).append(". Not confirmed in time.");
        sendToCaregivers(message.toString(), caregivers);
    }

    private static void sendToCaregivers(String message, List<Caregiver> caregivers) {
        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> parts = smsManager.divideMessage(message);
        for (Caregiver caregiver : caregivers) {
            if (caregiver.phone == null || caregiver.phone.trim().isEmpty()) {
                continue;
            }
            if (parts.size() == 1) {
                smsManager.sendTextMessage(caregiver.phone.trim(), null, message, null, null);
            } else {
                smsManager.sendMultipartTextMessage(caregiver.phone.trim(), null, parts, null, null);
            }
        }
    }

    private static String formatNumber(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
