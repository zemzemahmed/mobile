package com.example.medireminder.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.medireminder.data.entity.Medicine;
import com.example.medireminder.data.entity.MedicineWithSchedules;

import java.util.List;

@Dao
public interface MedicineDao {

    @Insert
    long insert(Medicine medicine);

    @Update
    void update(Medicine medicine);

    @Delete
    void delete(Medicine medicine);

    @Query("SELECT * FROM medicines ORDER BY name")
    LiveData<List<Medicine>> getAllMedicines();

    /**
     * A medicine + all of its alarms in one object.
     * @Transaction is needed so Room builds the whole result at once.
     */
    @Transaction
    @Query("SELECT * FROM medicines WHERE active = 1 ORDER BY name")
    LiveData<List<MedicineWithSchedules>> getMedicinesWithSchedules();

    @Query("SELECT * FROM medicines WHERE id = :id")
    Medicine getMedicineById(long id);

    /** Used by the boot receiver to know which medicines are currently active. */
    @Query("SELECT * FROM medicines WHERE active = 1")
    List<Medicine> getActiveMedicinesSync();
}
