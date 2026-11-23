package com.example.medireminder.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.medireminder.data.entity.DoseLog;

import java.util.List;

@Dao
public interface DoseLogDao {

    @Insert
    long insert(DoseLog log);

    @Update
    void update(DoseLog log);

    @Delete
    void delete(DoseLog log);

    /** Logs between two timestamps (in milliseconds). Used for History and Statistics. */
    @Query("SELECT * FROM dose_logs WHERE scheduledTime >= :start AND scheduledTime < :end ORDER BY scheduledTime")
    LiveData<List<DoseLog>> getLogsBetween(long start, long end);

    /** Same query, but synchronous: called from a background thread by receivers. */
    @Query("SELECT * FROM dose_logs WHERE scheduledTime >= :start AND scheduledTime < :end ORDER BY scheduledTime")
    List<DoseLog> getLogsBetweenSync(long start, long end);

    @Query("SELECT COUNT(*) FROM dose_logs WHERE scheduledTime >= :start AND scheduledTime < :end")
    int countTotalBetween(long start, long end);

    @Query("SELECT COUNT(*) FROM dose_logs WHERE status = 'TAKEN' " +
            "AND scheduledTime >= :start AND scheduledTime < :end")
    int countTakenBetween(long start, long end);

    /** Finds the log of one precise dose, so the missed-check can avoid duplicates. */
    @Query("SELECT * FROM dose_logs WHERE scheduleId = :scheduleId " +
            "AND scheduledTime = :scheduledTime LIMIT 1")
    DoseLog findDose(long scheduleId, long scheduledTime);
}
