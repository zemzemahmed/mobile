package com.example.medireminder.util;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.example.medireminder.R;

/**
 * A full-screen overlay shown by the foreground service. Samsung blocks activities
 * started from the background, but an overlay with "draw over other apps" permission
 * can always appear on top.
 */
public final class AlarmOverlay {

    private static final long TIMEOUT_MS = 15000;

    private static View currentView;
    private static WindowManager currentManager;
    private static CountDownTimer timer;

    private AlarmOverlay() {
    }

    public static void show(Context context, long[] scheduleIds, long scheduledTime, String doseText) {
        Context app = context.getApplicationContext();
        if (!Settings.canDrawOverlays(app)) {
            return; // MainActivity asks the user for this permission.
        }
        dismiss();

        View view = LayoutInflater.from(app).inflate(R.layout.view_alarm_overlay, null);
        TextView doseView = view.findViewById(R.id.overlayDoseText);
        TextView countdownView = view.findViewById(R.id.overlayCountdownText);
        Button takenButton = view.findViewById(R.id.overlayTakenButton);
        doseView.setText(doseText == null ? "" : doseText);

        WindowManager manager = (WindowManager) app.getSystemService(Context.WINDOW_SERVICE);
        if (manager == null) {
            return;
        }
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;

        final boolean[] handled = {false};
        takenButton.setOnClickListener(v -> {
            if (handled[0]) {
                return;
            }
            handled[0] = true;
            DoseActionHelper.takeDose(app, scheduleIds, scheduledTime);
            dismiss();
        });

        try {
            manager.addView(view, params);
        } catch (Exception e) {
            return; // Permission was revoked between the check and addView().
        }
        currentView = view;
        currentManager = manager;

        timer = new CountDownTimer(TIMEOUT_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                countdownView.setText(String.format(app.getString(R.string.alarm_countdown),
                        (int) Math.ceil(millisUntilFinished / 1000.0)));
            }

            @Override
            public void onFinish() {
                if (handled[0]) {
                    return;
                }
                handled[0] = true;
                DoseActionHelper.markMissedNow(app, scheduleIds, scheduledTime);
                dismiss();
            }
        };
        timer.start();
    }

    public static void dismiss() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (currentView != null && currentManager != null) {
            try {
                currentManager.removeView(currentView);
            } catch (Exception ignored) {
                // The view may already be gone.
            }
            currentView = null;
            currentManager = null;
        }
    }
}
