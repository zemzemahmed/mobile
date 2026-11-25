package com.example.medireminder.ui.activity;

import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.medireminder.R;
import com.example.medireminder.data.entity.Medicine;
import com.example.medireminder.data.entity.Schedule;
import com.example.medireminder.data.repository.MedicineRepository;
import com.example.medireminder.service.AlarmForegroundService;
import com.example.medireminder.util.AlarmScheduler;
import com.example.medireminder.util.AppExecutor;
import com.example.medireminder.util.DoseActionHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Full-screen alarm shown when a dose is due. Taken, or missed after 15 seconds. */
public class AlarmActivity extends AppCompatActivity {

    public static final String EXTRA_SCHEDULE_ID = "scheduleId";
    public static final String EXTRA_SCHEDULE_IDS = "scheduleIds";
    public static final String EXTRA_SCHEDULED_TIME = "scheduledTime";
    public static final String EXTRA_DOSE_TEXT = "doseText";

    private static final long TIMEOUT_MS = 15000;

    private TextView doseView;
    private TextView countdownView;
    private CountDownTimer timer;
    private boolean handled;
    private long[] activeIds;
    private long activeTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        setVolumeControlStream(AudioManager.STREAM_ALARM);

        // Show over the lock screen and keep the screen awake.
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        doseView = findViewById(R.id.alarmDoseText);
        countdownView = findViewById(R.id.alarmCountdownText);

        long[] ids = getIntent().getLongArrayExtra(EXTRA_SCHEDULE_IDS);
        long singleId = getIntent().getLongExtra(EXTRA_SCHEDULE_ID, -1);
        activeTime = getIntent().getLongExtra(EXTRA_SCHEDULED_TIME, -1);
        String doseText = getIntent().getStringExtra(EXTRA_DOSE_TEXT);

        if (activeTime == -1 || (ids == null && singleId == -1)) {
            finish();
            return;
        }
        activeIds = ids != null ? ids : new long[]{singleId};
        doseView.setText(doseText == null ? "Preparing dose..." : doseText);

        Button takenButton = findViewById(R.id.alarmTakenButton);
        takenButton.setOnClickListener(v -> {
            if (handled) {
                return;
            }
            handled = true;
            DoseActionHelper.takeDose(getApplicationContext(), activeIds, activeTime);
            finish();
        });

        startTimer();

        // AlarmManager launches this activity with one schedule id. Load the group from Room here.
        if (ids == null) {
            loadGroup(singleId);
        }
    }

    private void startTimer() {
        timer = new CountDownTimer(TIMEOUT_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                countdownView.setText(String.format(getString(R.string.alarm_countdown),
                        (int) Math.ceil(millisUntilFinished / 1000.0)));
            }

            @Override
            public void onFinish() {
                if (handled) {
                    return;
                }
                handled = true;
                DoseActionHelper.markMissedNow(getApplicationContext(), activeIds, activeTime);
                finish();
            }
        };
        timer.start();
    }

    private void loadGroup(long scheduleId) {
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

            List<Schedule> dueAtSameTime = repository.getSchedulesDueAt(schedule.hour, schedule.minute);
            if (dueAtSameTime == null || dueAtSameTime.isEmpty()) {
                dueAtSameTime = java.util.Collections.singletonList(schedule);
            }
            long[] ids = new long[dueAtSameTime.size()];
            StringBuilder text = new StringBuilder();
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(activeTime));
            for (int i = 0; i < dueAtSameTime.size(); i++) {
                Schedule dose = dueAtSameTime.get(i);
                ids[i] = dose.id;
                Medicine item = repository.getMedicineById(dose.medicineId);
                String name = item == null ? "Medicine" : item.name;
                if (i > 0) {
                    text.append("\n");
                }
                text.append(name).append(" x").append(formatNumber(dose.pillsPerDose));
            }
            text.append("\nat ").append(time);
            activeIds = ids;

            AlarmScheduler.scheduleMissedCheck(getApplicationContext(), scheduleId, activeTime);
            AlarmScheduler.scheduleOne(getApplicationContext(), medicine, schedule);
            AlarmForegroundService.showRinging(getApplicationContext(), ids, activeTime, text.toString());
            String finalText = text.toString();
            runOnUiThread(() -> doseView.setText(finalText));
        });
    }

    private String formatNumber(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    @Override
    protected void onDestroy() {
        if (timer != null) {
            timer.cancel();
        }
        super.onDestroy();
    }
}