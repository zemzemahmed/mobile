package com.example.medireminder.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.medireminder.data.dao.CaregiverDao;
import com.example.medireminder.data.dao.DoseLogDao;
import com.example.medireminder.data.dao.MedicineDao;
import com.example.medireminder.data.dao.ScheduleDao;
import com.example.medireminder.data.entity.Caregiver;
import com.example.medireminder.data.entity.DoseLog;
import com.example.medireminder.data.entity.Medicine;
import com.example.medireminder.data.entity.Schedule;

/**
 * The single entry point to the Room database.
 * "Singleton" means the whole app shares this one instance.
 */
@Database(
        entities = {Medicine.class, Schedule.class, DoseLog.class, Caregiver.class},
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract MedicineDao medicineDao();

    public abstract ScheduleDao scheduleDao();

    public abstract DoseLogDao doseLogDao();

    public abstract CaregiverDao caregiverDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "medireminder.db"
            ).build();
        }
        return instance;
    }
}
