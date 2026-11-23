package com.example.medireminder.util;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.content.ContextCompat;

import com.example.medireminder.R;

/** Asks for the permissions the app needs, with a short explanation first. */
public final class PermissionHelper {

    public static final int REQUEST_NOTIFICATIONS = 100;
    public static final int REQUEST_SMS = 101;

    private PermissionHelper() {
    }

    public static void requestAll(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.permission_notifications_title)
                    .setMessage(R.string.permission_notifications_message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) ->
                            activity.requestPermissions(
                                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                    REQUEST_NOTIFICATIONS))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.permission_sms_title)
                    .setMessage(R.string.permission_sms_message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) ->
                            activity.requestPermissions(
                                    new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager manager = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
            if (manager != null && !manager.canScheduleExactAlarms()) {
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.permission_exact_alarm_title)
                        .setMessage(R.string.permission_exact_alarm_message)
                        .setPositiveButton(R.string.open_settings, (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            intent.setData(Uri.parse("package:" + activity.getPackageName()));
                            activity.startActivity(intent);
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        }
    }
}
