package com.example.medireminder.util;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.example.medireminder.R;

/**
 * The single full-screen alarm window shown on top of other apps when
 * "draw over other apps" is allowed. The countdown belongs to AlarmController,
 * so this class only draws the screen and forwards the Taken tap.
 */
public final class AlarmOverlay {

    private static View currentView;
    private static WindowManager currentManager;
    private static TextView countdownView;

    private AlarmOverlay() {
    }

    public static void show(Context context, String doseText) {
        Context app = context.getApplicationContext();
        if (!Settings.canDrawOverlays(app)) {
            return; // AlarmActivity shows its own screen in this case.
        }
        dismiss();

        View view = LayoutInflater.from(app).inflate(R.layout.view_alarm_overlay, null);
        TextView doseView = view.findViewById(R.id.overlayDoseText);
        countdownView = view.findViewById(R.id.overlayCountdownText);
        Button takenButton = view.findViewById(R.id.overlayTakenButton);
        doseView.setText(doseText == null ? "" : doseText);
        countdownView.setText("");

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

        takenButton.setOnClickListener(v -> AlarmController.handleTaken());

        try {
            manager.addView(view, params);
        } catch (Exception e) {
            countdownView = null;
            return; // Permission was revoked between the check and addView().
        }
        currentView = view;
        currentManager = manager;
    }

    public static void updateCountdown(int seconds) {
        if (countdownView != null) {
            countdownView.setText(String.format(countdownView.getContext()
                    .getString(R.string.alarm_countdown), seconds));
        }
    }

    public static void dismiss() {
        if (currentView != null && currentManager != null) {
            try {
                currentManager.removeView(currentView);
            } catch (Exception ignored) {
                // The view may already be gone.
            }
            currentView = null;
            currentManager = null;
        }
        countdownView = null;
    }
}
