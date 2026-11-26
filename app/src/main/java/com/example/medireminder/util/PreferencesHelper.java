package com.example.medireminder.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Settings and the persistent notification text are stored in one small SharedPreferences file. */
public class PreferencesHelper {

    private static final String FILE_NAME = "medireminder_prefs";
    public static final String KEY_RINGTONE = "ringtone_uri";
    public static final String KEY_DARK_MODE = "dark_mode";
    public static final String KEY_LARGE_TEXT = "large_text";

    private static final String KEY_NOTIFY_TITLE = "notify_title";
    private static final String KEY_NOTIFY_TEXT = "notify_text";
    private static final String KEY_NOTIFY_TIME = "notify_time";
    private static final String KEY_LEGACY_CLEARED = "legacy_notifications_cleared";

    private final SharedPreferences prefs;

    public PreferencesHelper(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
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

    public String getNotificationTitle() {
        return prefs.getString(KEY_NOTIFY_TITLE, "MediReminder is active");
    }

    public String getNotificationText() {
        return prefs.getString(KEY_NOTIFY_TEXT, "Tap to open MediReminder.");
    }

    public long getNotificationTime() {
        return prefs.getLong(KEY_NOTIFY_TIME, -1);
    }

    public boolean isLegacyCleared() {
        return prefs.getBoolean(KEY_LEGACY_CLEARED, false);
    }

    public void markLegacyCleared() {
        prefs.edit().putBoolean(KEY_LEGACY_CLEARED, true).apply();
    }

    public void saveNotification(String title, String text, long scheduledTime) {
        prefs.edit()
                .putString(KEY_NOTIFY_TITLE, title)
                .putString(KEY_NOTIFY_TEXT, text)
                .putLong(KEY_NOTIFY_TIME, scheduledTime)
                .apply();
    }

    public void saveBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    public void saveString(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }
}