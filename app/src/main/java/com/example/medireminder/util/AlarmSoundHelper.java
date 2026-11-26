package com.example.medireminder.util;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;

/** Plays the chosen alarm sound directly, so the first alarm is never silent. */
public final class AlarmSoundHelper {

    private static MediaPlayer player;

    private AlarmSoundHelper() {
    }

    public static void start(Context context) {
        stop();
        Context app = context.getApplicationContext();
        Uri sound = getSound(app);

        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
        mediaPlayer.setLooping(true);
        player = mediaPlayer;

        try {
            mediaPlayer.setDataSource(app, sound);
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                stop();
                return true;
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            // Fall back to the phone's default alarm tone.
            stop();
            tryPlayDefault(app);
        }
    }

    private static Uri getSound(Context app) {
        String saved = new PreferencesHelper(app).getRingtoneUri();
        if (!saved.isEmpty()) {
            try {
                return Uri.parse(saved);
            } catch (Exception ignored) {
                // Use the default below.
            }
        }
        Uri alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        return alarm != null ? alarm : Settings.System.DEFAULT_ALARM_ALERT_URI;
    }

    private static void tryPlayDefault(Context app) {
        MediaPlayer fallback = new MediaPlayer();
        fallback.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
        fallback.setLooping(true);
        player = fallback;
        try {
            fallback.setDataSource(app, Settings.System.DEFAULT_ALARM_ALERT_URI);
            fallback.setOnPreparedListener(MediaPlayer::start);
            fallback.setOnErrorListener((mp, what, extra) -> {
                stop();
                return true;
            });
            fallback.prepareAsync();
        } catch (Exception e) {
            stop();
        }
    }

    public static void stop() {
        if (player != null) {
            try {
                if (player.isPlaying()) {
                    player.stop();
                }
                player.release();
            } catch (Exception ignored) {
                // Already released.
            }
            player = null;
        }
    }
}
