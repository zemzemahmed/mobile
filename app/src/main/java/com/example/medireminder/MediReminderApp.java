package com.example.medireminder;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.medireminder.service.AlarmForegroundService;
import com.example.medireminder.util.NotificationHelper;
import com.example.medireminder.util.PreferencesHelper;

/** Runs once when the app starts: create the channel, apply dark mode, keep the notification alive. */
public class MediReminderApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannel(this);
        NotificationHelper.clearOldNotifications(this);
        PreferencesHelper prefs = new PreferencesHelper(this);
        AppCompatDelegate.setDefaultNightMode(prefs.isDarkMode()
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
        AlarmForegroundService.start(this);
    }
}