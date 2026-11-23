package com.example.medireminder.data.entity;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

/**
 * A medicine together with all of its alarms.
 * Room fills the list automatically using Schedule.medicineId = Medicine.id.
 * This is the Room @Relation required by the specification.
 */
public class MedicineWithSchedules {

    @Embedded
    public Medicine medicine;

    @Relation(
            parentColumn = "id",          // Medicine.id
            entityColumn = "medicineId"   // Schedule.medicineId
    )
    public List<Schedule> schedules;
}
