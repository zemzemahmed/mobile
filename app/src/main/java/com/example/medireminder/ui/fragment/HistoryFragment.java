package com.example.medireminder.ui.fragment;

import android.os.Bundle;
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
import com.example.medireminder.data.repository.DoseLogRepository;
import com.example.medireminder.data.repository.MedicineRepository;
import com.example.medireminder.ui.adapter.LogAdapter;
import com.example.medireminder.ui.view.AdherenceChartView;
import com.example.medireminder.util.AppExecutor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** History tab: weekly adherence, a bar chart and the list of dose logs. */
public class HistoryFragment extends Fragment {

    private LogAdapter adapter;
    private TextView adherenceText;
    private AdherenceChartView chart;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        adherenceText = view.findViewById(R.id.adherenceText);
        chart = view.findViewById(R.id.chart);

        RecyclerView recycler = view.findViewById(R.id.logsRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new LogAdapter();
        recycler.setAdapter(adapter);
        // onResume() loads the data, so we do not load it twice.
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded()) {
            loadHistory();
        }
    }

    private void loadHistory() {
        AppExecutor.database().execute(() -> {
            MedicineRepository medicines = new MedicineRepository(requireContext());
            DoseLogRepository logs = new DoseLogRepository(requireContext());

            Calendar start = Calendar.getInstance();
            start.add(Calendar.DAY_OF_YEAR, -6);
            start.set(Calendar.HOUR_OF_DAY, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            start.set(Calendar.MILLISECOND, 0);
            long startMillis = start.getTimeInMillis();
            long endMillis = startMillis + 7L * 24 * 60 * 60 * 1000;

            List<DoseLog> weekLogs = logs.getLogsBetweenSync(startMillis, endMillis);
            if (weekLogs == null) {
                weekLogs = new ArrayList<>();
            }

            List<LogAdapter.LogItem> rows = new ArrayList<>();
            int[] takenPerDay = new int[7];
            int[] missedPerDay = new int[7];
            int takenCount = 0;

            for (DoseLog log : weekLogs) {
                Medicine medicine = medicines.getMedicineById(log.medicineId);
                String name = medicine == null ? "Medicine" : medicine.name;
                rows.add(new LogAdapter.LogItem(log, name));

                int index = (int) ((log.scheduledTime - startMillis) / (24L * 60 * 60 * 1000));
                if (index >= 0 && index < 7) {
                    if (DoseLog.STATUS_TAKEN.equals(log.status)) {
                        takenPerDay[index]++;
                        takenCount++;
                    } else if (DoseLog.STATUS_MISSED.equals(log.status)) {
                        missedPerDay[index]++;
                    }
                }
            }

            final int total = weekLogs.size();
            final int taken = takenCount;
            requireActivity().runOnUiThread(() -> {
                adapter.submitList(rows);
                chart.setData(takenPerDay, missedPerDay);
                int percent = total == 0 ? 0 : taken * 100 / total;
                adherenceText.setText(getString(R.string.adherence_label, percent, taken, total));
            });
        });
    }
}
