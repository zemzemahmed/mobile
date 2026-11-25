package com.example.medireminder.ui.fragment;

import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.medireminder.R;
import com.example.medireminder.util.AlarmScheduler;
import com.example.medireminder.util.PreferencesHelper;

/** Settings tab: small options stored in SharedPreferences. */
public class SettingsFragment extends Fragment {

    private static final int REQUEST_RINGTONE = 300;

    private PreferencesHelper prefs;
    private Switch darkSwitch;
    private Switch largeTextSwitch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        prefs = new PreferencesHelper(requireContext());

        darkSwitch = view.findViewById(R.id.darkModeSwitch);
        largeTextSwitch = view.findViewById(R.id.largeTextSwitch);

        darkSwitch.setChecked(prefs.isDarkMode());
        largeTextSwitch.setChecked(prefs.isLargeText());

        view.findViewById(R.id.ringtoneButton).setOnClickListener(v -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Reminder sound");
            String current = prefs.getRingtoneUri();
            if (!current.isEmpty()) {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(current));
            }
            startActivityForResult(intent, REQUEST_RINGTONE);
        });

        view.findViewById(R.id.rescheduleButton).setOnClickListener(v -> {
            AlarmScheduler.rescheduleAll(requireContext());
            Toast.makeText(requireContext(), R.string.alarms_rebuilt, Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.saveSettingsButton).setOnClickListener(v -> saveSettings());
        return view;
    }

    private void saveSettings() {
        prefs.saveBoolean(PreferencesHelper.KEY_DARK_MODE, darkSwitch.isChecked());
        prefs.saveBoolean(PreferencesHelper.KEY_LARGE_TEXT, largeTextSwitch.isChecked());

        AppCompatDelegate.setDefaultNightMode(darkSwitch.isChecked()
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
        Toast.makeText(requireContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show();
        requireActivity().recreate();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_RINGTONE || resultCode != -1 || data == null) {
            return; // Activity.RESULT_OK == -1
        }
        Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        prefs.saveString(PreferencesHelper.KEY_RINGTONE, uri == null ? "" : uri.toString());
        Toast.makeText(requireContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show();
    }
}