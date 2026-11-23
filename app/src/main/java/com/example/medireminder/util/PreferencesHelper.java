package com.example.medireminder.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Settings are stored in one small SharedPreferences file. */
public class PreferencesHelper {

    private static final String FILE_NAME = "medireminder_prefs";
    public static final String KEY_SNOOZE = "snooze_minutes";
    public static final String KEY_ALERT_DELAY = "alert_delay_minutes";
    public static final String KEY_ALERT_ENABLED = "alert_enabled";
    public static final String KEY_RINGTONE = "ringtone_uri";
    public static final String KEY_DARK_MODE = "dark_mode";
    public static final String KEY_LARGE_TEXT = "large_text";
    public static final String KEY_DEBUG = "debug_short_delays";

    private final SharedPreferences prefs;

    public PreferencesHelper(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    public int getSnoozeMinutes() {
        if (isDebugShortDelays()) {
            return 1;
        }
        return prefs.getInt(KEY_SNOOZE, 10);
    }

    public int getAlertDelayMinutes() {
        if (isDebugShortDelays()) {
            return 1;
        }
        return prefs.getInt(KEY_ALERT_DELAY, 30);
    }

    public boolean isAlertEnabled() {
        return prefs.getBoolean(KEY_ALERT_ENABLED, true);
    }

    public String getRingtoneUri() {
        return prefs.getString(KEY_RINGTONE, "");
    }

    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    public boolean isLargeText() {
        return prefs.getBoolean(KEY_LARGE_TEXT, false);
    }

    public boolean isDebugShortDelays() {
        return prefs.getBoolean(KEY_DEBUG, false);
    }

    public void saveInt(String key, int value) {
        prefs.edit().putInt(key, value).apply();
    }

    public void saveBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    public void saveString(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }
}
