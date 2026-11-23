package com.example.medireminder.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.medireminder.data.entity.Caregiver;

import java.util.List;

@Dao
public interface CaregiverDao {

    @Insert
    long insert(Caregiver caregiver);

    @Update
    void update(Caregiver caregiver);

    @Delete
    void delete(Caregiver caregiver);

    @Query("SELECT * FROM caregivers ORDER BY name")
    LiveData<List<Caregiver>> getAllCaregivers();

    /** Used by the SMS receiver, from a background thread. */
    @Query("SELECT * FROM caregivers ORDER BY name")
    List<Caregiver> getAllCaregiversSync();
}
