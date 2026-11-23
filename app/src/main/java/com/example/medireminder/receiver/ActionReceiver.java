package com.example.medireminder.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.medireminder.util.DoseActionHelper;

/** Handles the Taken and Snooze buttons of the notification. */
public class ActionReceiver extends BroadcastReceiver {

    public static final String ACTION_TAKEN = "com.example.medireminder.ACTION_TAKEN";
    public static final String ACTION_SNOOZE = "com.example.medireminder.ACTION_SNOOZE";
    public static final String EXTRA_SCHEDULE_IDS = "scheduleIds";
    public static final String EXTRA_SCHEDULED_TIME = "scheduledTime";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) {
            return;
        }
        long[] scheduleIds = intent.getLongArrayExtra(EXTRA_SCHEDULE_IDS);
        long scheduledTime = intent.getLongExtra(EXTRA_SCHEDULED_TIME, -1);
        if (scheduleIds == null || scheduleIds.length == 0 || scheduledTime == -1) {
            return;
        }

        Context appContext = context.getApplicationContext();
        if (ACTION_TAKEN.equals(intent.getAction())) {
            DoseActionHelper.takeDose(appContext, scheduleIds, scheduledTime);
        } else if (ACTION_SNOOZE.equals(intent.getAction())) {
            DoseActionHelper.snoozeDose(appContext, scheduleIds, scheduledTime);
        }
    }
}
