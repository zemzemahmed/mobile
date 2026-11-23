package com.example.medireminder.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.medireminder.data.entity.Schedule;

import java.util.List;

@Dao
public interface ScheduleDao {

    @Insert
    long insert(Schedule schedule);

    @Update
    void update(Schedule schedule);

    @Delete
    void delete(Schedule schedule);

    @Query("SELECT * FROM schedules WHERE medicineId = :medicineId ORDER BY hour, minute")
    List<Schedule> getSchedulesForMedicine(long medicineId);

    @Query("SELECT * FROM schedules WHERE medicineId = :medicineId ORDER BY hour, minute")
    LiveData<List<Schedule>> getSchedulesForMedicineLive(long medicineId);

    /**
     * All alarms of medicines that are not paused.
     * Used after a reboot to restore the alarms.
     */
    @Query("SELECT s.* FROM schedules s " +
            "INNER JOIN medicines m ON s.medicineId = m.id " +
            "WHERE m.active = 1 ORDER BY s.hour, s.minute")
    List<Schedule> getAllActiveSchedules();

    @Query("DELETE FROM schedules WHERE medicineId = :medicineId")
    void deleteSchedulesForMedicine(long medicineId);

    @Query("SELECT * FROM schedules WHERE id = :id")
    Schedule getScheduleById(long id);

    /** All alarms of active medicines at a given time - used to group notifications. */
    @Query("SELECT s.* FROM schedules s INNER JOIN medicines m ON s.medicineId = m.id " +
            "WHERE m.active = 1 AND s.hour = :hour AND s.minute = :minute")
    List<Schedule> getSchedulesDueAt(int hour, int minute);
}
