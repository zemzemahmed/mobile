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
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            return; // permission was denied - the app simply does not send anything
        }
        if (caregivers == null || caregivers.isEmpty() || medicine == null || schedule == null) {
            return;
        }

        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(scheduledTime));
        String message = "Reminder: " + medicine.name + " - " + formatNumber(schedule.pillsPerDose)
                + " pill(s) planned at " + time + " were not confirmed.";

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
