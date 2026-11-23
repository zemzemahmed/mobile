package com.example.medireminder.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.medireminder.data.entity.MedicineWithSchedules;
import com.example.medireminder.data.repository.MedicineRepository;

import java.util.List;

/**
 * Holds the medicine list for the UI.
 * The Activity/Fragment survives screen rotation, and so does this ViewModel.
 */
public class MedicineViewModel extends AndroidViewModel {

    private final LiveData<List<MedicineWithSchedules>> medicines;

    public MedicineViewModel(@NonNull Application application) {
        super(application);
        MedicineRepository repository = new MedicineRepository(application);
        medicines = repository.getMedicinesWithSchedules();
    }

    public LiveData<List<MedicineWithSchedules>> getMedicines() {
        return medicines;
    }
}
