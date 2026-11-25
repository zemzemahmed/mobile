package com.example.medireminder.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.medireminder.service.AlarmForegroundService;
import com.example.medireminder.util.AlarmScheduler;

/** Restores all alarms and the ongoing notification after the phone boots. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) {
            return;
        }
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                || "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {
            AlarmForegroundService.start(context.getApplicationContext());
            AlarmScheduler.rescheduleAll(context.getApplicationContext());
        }
    }
}