package com.example.medireminder.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medireminder.R;
import com.example.medireminder.ui.activity.AddEditMedicineActivity;
import com.example.medireminder.ui.activity.CaregiversActivity;
import com.example.medireminder.data.entity.MedicineWithSchedules;
import com.example.medireminder.data.repository.MedicineRepository;
import com.example.medireminder.ui.adapter.MedicineAdapter;
import com.example.medireminder.util.AlarmScheduler;
import com.example.medireminder.util.AppExecutor;
import com.example.medireminder.viewmodel.MedicineViewModel;

import java.util.List;

/** Shows all medicines read from Room. */
public class MedicinesFragment extends Fragment implements MedicineAdapter.OnMedicineClickListener {

    private MedicineAdapter adapter;
    private TextView emptyText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_medicines, container, false);

        emptyText = view.findViewById(R.id.emptyText);
        RecyclerView recycler = view.findViewById(R.id.medicinesRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new MedicineAdapter(this);
        recycler.setAdapter(adapter);

        view.findViewById(R.id.caregiversButton).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CaregiversActivity.class)));

        view.findViewById(R.id.addMedicineFab).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddEditMedicineActivity.class);
            startActivity(intent);
        });

        // The ViewModel gives us LiveData from the repository.
        MedicineViewModel viewModel = new ViewModelProvider(this).get(MedicineViewModel.class);
        viewModel.getMedicines().observe(getViewLifecycleOwner(),
                (List<MedicineWithSchedules> medicines) -> {
                    adapter.submitList(medicines);
                    boolean isEmpty = medicines == null || medicines.isEmpty();
                    emptyText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                });

        return view;
    }

    /** Tapping a card opens the same screen in edit mode. */
    @Override
    public void onMedicineClick(long medicineId) {
        Intent intent = new Intent(requireContext(), AddEditMedicineActivity.class);
        intent.putExtra(AddEditMedicineActivity.EXTRA_MEDICINE_ID, medicineId);
        startActivity(intent);
    }

    /** Pause cancels alarms; Resume rebuilds them from the saved schedules. */
    @Override
    public void onPauseClick(final MedicineWithSchedules item) {
        AppExecutor.database().execute(() -> {
            MedicineRepository repository = new MedicineRepository(requireContext());
            item.medicine.active = !item.medicine.active;
            repository.updateMedicineSync(item.medicine);

            if (item.medicine.active) {
                AlarmScheduler.scheduleAlarmsForMedicine(requireContext(), item.medicine.id);
            } else {
                AlarmScheduler.cancelAlarmsForSchedules(requireContext(), item.schedules);
            }
            requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(),
                    item.medicine.active ? R.string.medicine_resumed : R.string.medicine_paused,
                    Toast.LENGTH_SHORT).show());
        });
    }

    @Override
    public void onDeleteClick(final MedicineWithSchedules item) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_medicine_title)
                .setMessage(R.string.delete_medicine_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        AppExecutor.database().execute(() -> {
                            AlarmScheduler.cancelAlarmsForSchedules(requireContext(), item.schedules);
                            new MedicineRepository(requireContext()).deleteMedicineSync(item.medicine);
                            requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(),
                                    R.string.medicine_deleted, Toast.LENGTH_SHORT).show());
                        }))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
