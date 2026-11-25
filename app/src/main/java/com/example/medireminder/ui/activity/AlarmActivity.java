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
import com.example.medireminder.util.DoseActionHelper;
import com.example.medireminder.util.NotificationHelper;

/** Full-screen alarm shown when a dose is due. Taken, or missed after 5 seconds. */
public class AlarmActivity extends AppCompatActivity {

    public static final String EXTRA_SCHEDULE_IDS = "scheduleIds";
    public static final String EXTRA_SCHEDULED_TIME = "scheduledTime";
    public static final String EXTRA_DOSE_TEXT = "doseText";

    private static final long TIMEOUT_MS = 5000;

    private CountDownTimer timer;
    private boolean handled;

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

        long[] scheduleIds = getIntent().getLongArrayExtra(EXTRA_SCHEDULE_IDS);
        long scheduledTime = getIntent().getLongExtra(EXTRA_SCHEDULED_TIME, -1);
        String doseText = getIntent().getStringExtra(EXTRA_DOSE_TEXT);
        if (scheduleIds == null || scheduleIds.length == 0 || scheduledTime == -1) {
            finish();
            return;
        }

        // The notification started the sound; the screen is now in charge.
        NotificationHelper.dismiss(getApplicationContext(), scheduledTime);

        TextView doseView = findViewById(R.id.alarmDoseText);
        TextView countdownView = findViewById(R.id.alarmCountdownText);
        doseView.setText(doseText == null ? "" : doseText);

        Button takenButton = findViewById(R.id.alarmTakenButton);
        takenButton.setOnClickListener(v -> {
            if (handled) {
                return;
            }
            handled = true;
            DoseActionHelper.takeDose(getApplicationContext(), scheduleIds, scheduledTime);
            finish();
        });

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
                DoseActionHelper.markMissedNow(getApplicationContext(), scheduleIds, scheduledTime);
                finish();
            }
        };
        timer.start();
    }

    @Override
    protected void onDestroy() {
        if (timer != null) {
            timer.cancel();
        }
        super.onDestroy();
    }
}
