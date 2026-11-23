package com.example.medireminder.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.medireminder.data.dao.DoseLogDao;
import com.example.medireminder.data.database.AppDatabase;
import com.example.medireminder.data.entity.DoseLog;
import com.example.medireminder.util.AppExecutor;

import java.util.List;

/** Reads and writes the dose history. */
public class DoseLogRepository {

    private final DoseLogDao dao;

    public DoseLogRepository(Context context) {
        dao = AppDatabase.getInstance(context).doseLogDao();
    }

    public void insertLog(DoseLog log) {
        AppExecutor.database().execute(() -> dao.insert(log));
    }

    /** Returns the new log id. Call from a background thread (e.g. inside an alarm receiver). */
    public long insertLogSync(DoseLog log) {
        return dao.insert(log);
    }

    public void updateLog(DoseLog log) {
        AppExecutor.database().execute(() -> dao.update(log));
    }

    public void updateLogSync(DoseLog log) {
        dao.update(log);
    }

    public LiveData<List<DoseLog>> getLogsBetween(long start, long end) {
        return dao.getLogsBetween(start, end);
    }

    public int countTakenBetween(long start, long end) {
        return dao.countTakenBetween(start, end);
    }

    public int countTotalBetween(long start, long end) {
        return dao.countTotalBetween(start, end);
    }

    /** Finds one precise dose, so we never log it twice. */
    public DoseLog findDose(long scheduleId, long scheduledTime) {
        return dao.findDose(scheduleId, scheduledTime);
    }

    public List<DoseLog> getLogsBetweenSync(long start, long end) {
        return dao.getLogsBetweenSync(start, end);
    }
}
