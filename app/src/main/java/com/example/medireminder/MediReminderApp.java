package com.example.medireminder;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.medireminder.util.NotificationHelper;
import com.example.medireminder.util.PreferencesHelper;

/** Runs once when the app starts: create the notification channel and apply dark mode. */
public class MediReminderApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannel(this);
        PreferencesHelper prefs = new PreferencesHelper(this);
        AppCompatDelegate.setDefaultNightMode(prefs.isDarkMode()
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }
}
