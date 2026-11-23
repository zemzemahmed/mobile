package com.example.medireminder.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.medireminder.data.dao.MedicineDao;
import com.example.medireminder.data.dao.ScheduleDao;
import com.example.medireminder.data.database.AppDatabase;
import com.example.medireminder.data.entity.Medicine;
import com.example.medireminder.data.entity.MedicineWithSchedules;
import com.example.medireminder.data.entity.Schedule;
import com.example.medireminder.util.AppExecutor;

import java.util.List;

/**
 * The UI talks to this class, never directly to the DAOs.
 * Writes happen on the background thread; reads return LiveData where possible.
 */
public class MedicineRepository {

    private final MedicineDao medicineDao;
    private final ScheduleDao scheduleDao;

    public MedicineRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        medicineDao = db.medicineDao();
        scheduleDao = db.scheduleDao();
    }

    public LiveData<List<Medicine>> getAllMedicines() {
        return medicineDao.getAllMedicines();
    }

    /** Medicines together with their alarms (the list shown in the app). */
    public LiveData<List<MedicineWithSchedules>> getMedicinesWithSchedules() {
        return medicineDao.getMedicinesWithSchedules();
    }

    public void insertMedicine(Medicine medicine) {
        AppExecutor.database().execute(() -> medicineDao.insert(medicine));
    }

    public void updateMedicine(Medicine medicine) {
        AppExecutor.database().execute(() -> medicineDao.update(medicine));
    }

    public void deleteMedicine(Medicine medicine) {
        AppExecutor.database().execute(() -> medicineDao.delete(medicine));
    }

    /**
     * Saves the medicine and replaces all of its alarms.
     * Used by the Add/Edit screen in a later part.
     */
    public interface OnSavedListener {
        void onSaved(long medicineId);
    }

    public void saveMedicineWithSchedules(Medicine medicine, List<Schedule> schedules,
                                          OnSavedListener listener) {
        AppExecutor.database().execute(() -> {
            if (medicine.id == 0) {
                // Room fills in the new id for us.
                medicine.id = medicineDao.insert(medicine);
            } else {
                medicineDao.update(medicine);
                scheduleDao.deleteSchedulesForMedicine(medicine.id);
            }

            for (Schedule schedule : schedules) {
                schedule.medicineId = medicine.id;
                scheduleDao.insert(schedule);
            }

            if (listener != null) {
                listener.onSaved(medicine.id);
            }
        });
    }

    // ---- Synchronous helpers: call these only from a background thread ----

    public Medicine getMedicineById(long id) {
        return medicineDao.getMedicineById(id);
    }

    public List<Medicine> getActiveMedicinesSync() {
        return medicineDao.getActiveMedicinesSync();
    }

    public List<Schedule> getSchedulesForMedicineSync(long medicineId) {
        return scheduleDao.getSchedulesForMedicine(medicineId);
    }

    public List<Schedule> getAllActiveSchedulesSync() {
        return scheduleDao.getAllActiveSchedules();
    }

    public Schedule getScheduleById(long id) {
        return scheduleDao.getScheduleById(id);
    }

    public List<Schedule> getSchedulesDueAt(int hour, int minute) {
        return scheduleDao.getSchedulesDueAt(hour, minute);
    }

    /** Used by the Taken action to write the new stock immediately. */
    public void updateMedicineSync(Medicine medicine) {
        medicineDao.update(medicine);
    }

    public void deleteMedicineSync(Medicine medicine) {
        medicineDao.delete(medicine);
    }
}
