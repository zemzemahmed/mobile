package com.example.medireminder.ui.activity;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.medireminder.R;
import com.example.medireminder.data.entity.Medicine;
import com.example.medireminder.data.entity.Schedule;
import com.example.medireminder.data.repository.MedicineRepository;
import com.example.medireminder.util.AlarmController;
import com.example.medireminder.util.AlarmScheduler;
import com.example.medireminder.util.AppExecutor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Receives the alarm from AlarmManager and hands it to AlarmController. */
public class AlarmActivity extends AppCompatActivity {

    public static final String EXTRA_SCHEDULE_ID = "scheduleId";
    public static final String EXTRA_SCHEDULE_IDS = "scheduleIds";
    public static final String EXTRA_SCHEDULED_TIME = "scheduledTime";
    public static final String EXTRA_DOSE_TEXT = "doseText";

    private TextView doseView;
    private TextView countdownView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_ALARM);

        long[] ids = getIntent().getLongArrayExtra(EXTRA_SCHEDULE_IDS);
        long singleId = getIntent().getLongExtra(EXTRA_SCHEDULE_ID, -1);
        long time = getIntent().getLongExtra(EXTRA_SCHEDULED_TIME, -1);
        String doseText = getIntent().getStringExtra(EXTRA_DOSE_TEXT);

        if (time == -1 || (ids == null && singleId == -1)) {
            finish();
            return;
        }
        if (ids != null) {
            startAlarm(ids, time, doseText);
        } else {
            loadGroup(singleId, time);
        }
    }

    /**
     * Starts the alarm. When overlay permission is available the overlay is the only visible
     * screen; this Activity stays alive invisibly so Android keeps the sound playing.
     */
    private void startAlarm(long[] ids, long time, String doseText) {
        if (Settings.canDrawOverlays(this)) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            setContentView(new View(this)); // invisible: AlarmOverlay draws the alarm
        } else {
            setupActivityScreen();
        }

        if (doseView != null) {
            doseView.setText(doseText == null ? "Dose" : doseText);
        }
        AlarmController.registerActivity(this);
        AlarmController.start(getApplicationContext(), ids, time, doseText);
    }

    /** Fallback screen used only when "draw over other apps" was not granted. */
    private void setupActivityScreen() {
        setContentView(R.layout.activity_alarm);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        doseView = findViewById(R.id.alarmDoseText);
        countdownView = findViewById(R.id.alarmCountdownText);
        Button takenButton = findViewById(R.id.alarmTakenButton);
        takenButton.setOnClickListener(v -> {
            AlarmController.handleTaken();
            finish();
        });
    }

    /** AlarmManager launches us with one id; load every dose due at that same minute. */
    private void loadGroup(long scheduleId, long time) {
        AppExecutor.database().execute(() -> {
            MedicineRepository repository = new MedicineRepository(getApplicationContext());
            Schedule schedule = repository.getScheduleById(scheduleId);
            if (schedule == null) {
                runOnUiThread(this::finish);
                return;
            }
            Medicine medicine = repository.getMedicineById(schedule.medicineId);
            if (medicine == null || !medicine.active) {
                runOnUiThread(this::finish);
                return;
            }

            List<Schedule> due = repository.getSchedulesDueAt(schedule.hour, schedule.minute);
            if (due == null || due.isEmpty()) {
                due = java.util.Collections.singletonList(schedule);
            }
            long[] ids = new long[due.size()];
            StringBuilder text = new StringBuilder();
            String hour = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(time));
            for (int i = 0; i < due.size(); i++) {
                Schedule dose = due.get(i);
                ids[i] = dose.id;
                Medicine item = repository.getMedicineById(dose.medicineId);
                String name = item == null ? "Medicine" : item.name;
                if (i > 0) {
                    text.append("\n");
                }
                text.append(name).append(" x").append(formatNumber(dose.pillsPerDose));
            }
            text.append("\nat ").append(hour);

            AlarmScheduler.scheduleMissedCheck(getApplicationContext(), scheduleId, time);
            AlarmScheduler.scheduleOne(getApplicationContext(), medicine, schedule);
            String finalText = text.toString();
            runOnUiThread(() -> startAlarm(ids, time, finalText));
        });
    }

    private String formatNumber(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    /** Called by AlarmController so both possible screens show the same countdown. */
    public void updateCountdown(int seconds) {
        if (countdownView != null) {
            countdownView.setText(String.format(getString(R.string.alarm_countdown), seconds));
        }
    }

    @Override
    protected void onDestroy() {
        AlarmController.unregisterActivity(this);
        super.onDestroy();
    }
}
