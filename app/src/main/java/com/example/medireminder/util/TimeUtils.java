package com.example.medireminder.util;

import com.example.medireminder.data.entity.Schedule;

import java.util.Calendar;

/** Small date helpers shared by the Home screen. */
public final class TimeUtils {

    private TimeUtils() {
    }

    /** The timestamp of this schedule on the given day, or -1 if the day is not selected. */
    public static long timeForSelectedDay(Schedule schedule, Calendar day) {
        int androidDay = day.get(Calendar.DAY_OF_WEEK);
        int ourDay = androidDay == Calendar.SUNDAY ? 7 : androidDay - 1;
        boolean selected = false;
        for (String part : schedule.daysOfWeek.split(",")) {
            try {
                if (Integer.parseInt(part.trim()) == ourDay) {
                    selected = true;
                    break;
                }
            } catch (NumberFormatException ignored) {
                // Skip a badly stored value.
            }
        }
        if (!selected) {
            return -1;
        }
        Calendar result = (Calendar) day.clone();
        result.set(Calendar.HOUR_OF_DAY, schedule.hour);
        result.set(Calendar.MINUTE, schedule.minute);
        result.set(Calendar.SECOND, 0);
        result.set(Calendar.MILLISECOND, 0);
        return result.getTimeInMillis();
    }
}
