package com.example.medireminder.ui.activity;

import android.app.AlarmManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.medireminder.R;
import com.example.medireminder.ui.fragment.HistoryFragment;
import com.example.medireminder.ui.fragment.HomeFragment;
import com.example.medireminder.ui.fragment.MedicinesFragment;
import com.example.medireminder.ui.fragment.SettingsFragment;
import com.example.medireminder.util.AlarmScheduler;
import com.example.medireminder.util.PermissionHelper;
import com.example.medireminder.util.PreferencesHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/** Hosts the four tabs of the app. */
public class MainActivity extends AppCompatActivity {

    /** Applies the "Large text" setting before the layout is created. */
    @Override
    protected void attachBaseContext(Context newBase) {
        PreferencesHelper prefs = new PreferencesHelper(newBase);
        Configuration config = newBase.getResources().getConfiguration();
        config.fontScale = prefs.isLargeText() ? 1.2f : 1.0f;
        super.attachBaseContext(newBase.createConfigurationContext(config));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            showFragment(new HomeFragment());
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                showFragment(new HomeFragment());
            } else if (id == R.id.nav_history) {
                showFragment(new HistoryFragment());
            } else if (id == R.id.nav_medicines) {
                showFragment(new MedicinesFragment());
            } else if (id == R.id.nav_settings) {
                showFragment(new SettingsFragment());
            } else {
                return false;
            }
            return true;
        });

        PermissionHelper.requestAll(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Rebuild alarms if the exact-alarm permission was granted in system settings.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            AlarmScheduler.rescheduleAll(this);
        } else {
            AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (manager != null && manager.canScheduleExactAlarms()) {
                AlarmScheduler.rescheduleAll(this);
            }
        }
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.navHost, fragment)
                .commit();
    }
}
