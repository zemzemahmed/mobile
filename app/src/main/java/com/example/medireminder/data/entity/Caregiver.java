package com.example.medireminder.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * A person who receives an SMS when a dose is missed.
 */
@Entity(tableName = "caregivers")
public class Caregiver {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;
    public String phone;          // full number, e.g. "+212612345678"

    // Room requires an empty constructor.
    public Caregiver() {
    }

    @Ignore
    public Caregiver(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }
}
