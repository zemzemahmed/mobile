package com.example.medireminder.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.medireminder.data.dao.CaregiverDao;
import com.example.medireminder.data.database.AppDatabase;
import com.example.medireminder.data.entity.Caregiver;
import com.example.medireminder.util.AppExecutor;

import java.util.List;

/** Manages the list of people who receive the missed-dose SMS. */
public class CaregiverRepository {

    private final CaregiverDao dao;

    public CaregiverRepository(Context context) {
        dao = AppDatabase.getInstance(context).caregiverDao();
    }

    public LiveData<List<Caregiver>> getAllCaregivers() {
        return dao.getAllCaregivers();
    }

    public void insert(Caregiver caregiver) {
        AppExecutor.database().execute(() -> dao.insert(caregiver));
    }

    public void update(Caregiver caregiver) {
        AppExecutor.database().execute(() -> dao.update(caregiver));
    }

    public void delete(Caregiver caregiver) {
        AppExecutor.database().execute(() -> dao.delete(caregiver));
    }

    /** Called from the SMS receiver on a background thread. */
    public List<Caregiver> getAllCaregiversSync() {
        return dao.getAllCaregiversSync();
    }
}
