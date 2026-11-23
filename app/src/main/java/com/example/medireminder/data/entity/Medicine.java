package com.example.medireminder.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * One medicine, for example "Doliprane 500 mg".
 * Its alarms are stored separately in the Schedule table.
 */
@Entity(tableName = "medicines")
public class Medicine {

    // Simple text values for the "form" field.
    public static final String FORM_PILL = "pill";
    public static final String FORM_SYRUP = "syrup";
    public static final String FORM_INJECTION = "injection";

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;           // required, e.g. "Doliprane"
    public String strength;       // e.g. "500 mg"
    public String form;           // pill / syrup / injection
    public String notes;          // optional extra information

    // Dates are kept as simple text: "2026-11-23" (year-month-day).
    public String startDate;
    public String endDate;        // empty string means "no end date"

    public double pillsInStock;   // double so 0.5 pill works
    public boolean active;        // false = paused

    // Room requires an empty constructor.
    public Medicine() {
    }

    @Ignore
    public Medicine(String name, String strength, String form, String notes,
                    String startDate, String endDate, double pillsInStock, boolean active) {
        this.name = name;
        this.strength = strength;
        this.form = form;
        this.notes = notes;
        this.startDate = startDate;
        this.endDate = endDate;
        this.pillsInStock = pillsInStock;
        this.active = active;
    }
}
