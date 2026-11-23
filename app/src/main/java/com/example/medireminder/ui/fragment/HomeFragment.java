package com.example.medireminder.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medireminder.R;
import com.example.medireminder.data.entity.DoseLog;
import com.example.medireminder.data.entity.Medicine;
import com.example.medireminder.data.entity.Schedule;
import com.example.medireminder.data.repository.DoseLogRepository;
import com.example.medireminder.data.repository.MedicineRepository;
import com.example.medireminder.ui.adapter.DoseAdapter;
import com.example.medireminder.util.AppExecutor;
import com.example.medireminder.util.TimeUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Home tab: today's doses, the next one highlighted, and a low-stock warning. */
public class HomeFragment extends Fragment {

    private DoseAdapter adapter;
    private TextView nextDoseText;
    private TextView lowStockText;
    private TextView noDosesText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        nextDoseText = view.findViewById(R.id.nextDoseText);
        lowStockText = view.findViewById(R.id.lowStockText);
        noDosesText = view.findViewById(R.id.noDosesText);

        RecyclerView recycler = view.findViewById(R.id.dosesRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new DoseAdapter();
        recycler.setAdapter(adapter);
        // onResume() loads the data, so we do not load it twice.
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded()) {
            loadToday(); // refresh after coming back from Caregivers/Settings or a Taken tap
        }
    }

    private void loadToday() {
        AppExecutor.database().execute(() -> {
            MedicineRepository medicines = new MedicineRepository(requireContext());
            DoseLogRepository logs = new DoseLogRepository(requireContext());

            Calendar today = Calendar.getInstance();
            List<Medicine> activeMedicines = medicines.getActiveMedicinesSync();
            List<DoseAdapter.DoseItem> doses = new ArrayList<>();
            List<String> lowStockNames = new ArrayList<>();

            if (activeMedicines != null) {
                for (Medicine medicine : activeMedicines) {
                    List<Schedule> schedules = medicines.getSchedulesForMedicineSync(medicine.id);
                    if (schedules == null) {
                        continue;
                    }

                    // How many pills are needed for today + the next 2 days?
                    double neededFor3Days = 0;
                    for (int offset = 0; offset < 3; offset++) {
                        Calendar day = (Calendar) today.clone();
                        day.add(Calendar.DAY_OF_YEAR, offset);
                        for (Schedule schedule : schedules) {
                            if (TimeUtils.timeForSelectedDay(schedule, day) != -1) {
                                neededFor3Days += schedule.pillsPerDose;
                            }
                        }
                    }
                    if (medicine.pillsInStock < neededFor3Days) {
                        lowStockNames.add(medicine.name);
                    }

                    for (Schedule schedule : schedules) {
                        long time = TimeUtils.timeForSelectedDay(schedule, today);
                        if (time == -1) {
                            continue;
                        }
                        DoseLog log = logs.findDose(schedule.id, time);
                        String status = log == null ? "UPCOMING" : log.status;
                        doses.add(new DoseAdapter.DoseItem(medicine, schedule, time, status));
                    }
                }
            }

            Collections.sort(doses, (a, b) -> Long.compare(a.scheduledTime, b.scheduledTime));
            DoseAdapter.DoseItem next = null;
            for (DoseAdapter.DoseItem dose : doses) {
                if ("UPCOMING".equals(dose.status)) {
                    dose.isNext = true;
                    next = dose;
                    break;
                }
            }

            final DoseAdapter.DoseItem nextDose = next;
            final String warning = TextUtils.join(", ", lowStockNames);
            requireActivity().runOnUiThread(() -> {
                adapter.submitList(doses);
                noDosesText.setVisibility(doses.isEmpty() ? View.VISIBLE : View.GONE);

                if (nextDose == null) {
                    nextDoseText.setVisibility(View.GONE);
                } else {
                    String time = new SimpleDateFormat("HH:mm", Locale.getDefault())
                            .format(new Date(nextDose.scheduledTime));
                    nextDoseText.setText(getString(R.string.next_dose_label,
                            time + " " + nextDose.medicine.name));
                    nextDoseText.setVisibility(View.VISIBLE);
                }

                if (warning.isEmpty()) {
                    lowStockText.setVisibility(View.GONE);
                } else {
                    lowStockText.setText(getString(R.string.low_stock_warning, warning));
                    lowStockText.setVisibility(View.VISIBLE);
                }
            });
        });
    }
}
