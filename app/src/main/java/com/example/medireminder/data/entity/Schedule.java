package com.example.medireminder.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * One alarm belonging to one medicine.
 * A medicine with 3 alarms a day has 3 rows in this table.
 */
@Entity(
        tableName = "schedules",
        foreignKeys = @ForeignKey(
                entity = Medicine.class,
                parentColumns = "id",
                childColumns = "medicineId",
                onDelete = ForeignKey.CASCADE  // deleting a medicine deletes its alarms
        ),
        indices = {@Index("medicineId")}       // makes lookups by medicine faster
)
public class Schedule {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long medicineId;       // which medicine this alarm belongs to

    public int hour;              // 0 - 23
    public int minute;            // 0 - 59
    public double pillsPerDose;   // 1, 2, or 0.5

    // Days as comma-separated numbers: 1 = Monday ... 7 = Sunday.
    // Every day = "1,2,3,4,5,6,7".
    public String daysOfWeek;

    // Room requires an empty constructor.
    public Schedule() {
    }

    @Ignore
    public Schedule(long medicineId, int hour, int minute, double pillsPerDose, String daysOfWeek) {
        this.medicineId = medicineId;
        this.hour = hour;
        this.minute = minute;
        this.pillsPerDose = pillsPerDose;
        this.daysOfWeek = daysOfWeek;
    }
}
