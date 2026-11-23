package com.example.medireminder.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * The history of one dose: what was planned and what actually happened.
 * We keep old logs even if a medicine is deleted, so there is no foreign key here.
 */
@Entity(tableName = "dose_logs")
public class DoseLog {

    // The three possible statuses of a dose.
    public static final String STATUS_TAKEN = "TAKEN";
    public static final String STATUS_SNOOZED = "SNOOZED";
    public static final String STATUS_MISSED = "MISSED";

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long medicineId;
    public long scheduleId;

    public long scheduledTime;    // planned time, in milliseconds since 1970
    public double pillsTaken;     // 1, 2, or 0.5
    public String status;         // TAKEN / SNOOZED / MISSED
    public long actualTime;       // when the user tapped Taken; 0 if never

    // Room requires an empty constructor.
    public DoseLog() {
    }

    @Ignore
    public DoseLog(long medicineId, long scheduleId, long scheduledTime,
                   double pillsTaken, String status, long actualTime) {
        this.medicineId = medicineId;
        this.scheduleId = scheduleId;
        this.scheduledTime = scheduledTime;
        this.pillsTaken = pillsTaken;
        this.status = status;
        this.actualTime = actualTime;
    }
}
